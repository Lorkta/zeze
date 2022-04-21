﻿using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Net;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Xml;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Services.GlobalCacheManager;
using Zeze.Transaction;

namespace Zeze.Services
{
    public sealed class GlobalCacheManagerServer
    {
        public const int StateInvalid = 0;
        public const int StateShare = 1;
        public const int StateModify = 2;
        public const int StateRemoving = 3;

        public const int StateRemoved = 10; // 从容器(Cache或Global)中删除后设置的状态，最后一个状态。
        public const int StateReduceRpcTimeout = 11; // 用来表示 reduce 超时失败。不是状态。
        public const int StateReduceException = 12; // 用来表示 reduce 异常失败。不是状态。
        public const int StateReduceNetError = 13;  // 用来表示 reduce 网络失败。不是状态。
        public const int StateReduceDuplicate = 14; // 用来表示重复的 reduce。错误报告，不是状态。
        public const int StateReduceSessionNotFound = 15;

        public const int AcquireShareDeadLockFound = 21;
        public const int AcquireShareAlreadyIsModify = 22;
        public const int AcquireModifyDeadLockFound = 23;
        public const int AcquireErrorState = 24;
        public const int AcquireModifyAlreadyIsModify = 25;
        public const int AcquireShareFailed = 26;
        public const int AcquireModifyFailed = 27;
        public const int AcquireException = 28;
        public const int AcquireInvalidFailed = 29;
        public const int AcquireNotLogin = 30;

        public const int ReduceErrorState = 41;
        public const int ReduceShareAlreadyIsInvalid = 42;
        public const int ReduceShareAlreadyIsShare = 43;
        public const int ReduceInvalidAlreadyIsInvalid = 44;

        public const int CleanupErrorSecureKey = 60;
        public const int CleanupErrorGlobalCacheManagerHashIndex = 61;
        public const int CleanupErrorHasConnection = 62;

        public const int ReLoginBindSocketFail = 80;

        public const int NormalCloseUnbindFail = 100;

        public const int LoginBindSocketFail = 120;

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public static GlobalCacheManagerServer Instance { get; } = new GlobalCacheManagerServer();
        public ServerService Server { get; private set; }
        public AsyncSocket ServerSocket { get; private set; }
        private ConcurrentDictionary<Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey, CacheState> global;
        private readonly Util.AtomicLong SerialIdGenerator = new();

        /*
         * 会话。
         * key是 LogicServer.Id，现在的实现就是Zeze.Config.ServerId。
         * 在连接建立后收到的Login Or ReLogin 中设置。
         * 每个会话记住分配给自己的GlobalTableKey，用来在正常退出的时候释放。
         * 每个会话还需要记录该会话的Socket.SessionId。在连接重新建立时更新。
         * 总是GetOrAdd，不删除。按现在的cache-sync设计，
         * ServerId 是及其有限的。不会一直增长。
         * 简化实现。
         */
        private ConcurrentDictionary<int, CacheHolder> Sessions;

        private GlobalCacheManagerServer()
        { 
        }

        public class GCMConfig : Config.ICustomize
        {
            public string Name => "GlobalCacheManager";

            public int ConcurrencyLevel { get; set; } = 1024;
            // 设置了这么大，开始使用后，大概会占用700M的内存，作为全局服务器，先这么大吧。
            // 尽量不重新调整ConcurrentDictionary。
            public int InitialCapacity { get; set; } = 10000000;

            public void Parse(XmlElement self)
            {
                string attr;

                attr = self.GetAttribute("ConcurrencyLevel");
                if (attr.Length > 0)
                    ConcurrencyLevel = int.Parse(attr);
                if (ConcurrencyLevel < Environment.ProcessorCount)
                    ConcurrencyLevel = Environment.ProcessorCount;

                attr = self.GetAttribute("InitialCapacity");
                if (attr.Length > 0)
                    InitialCapacity = int.Parse(attr);
                if (InitialCapacity < 31)
                    InitialCapacity = 31;
            }
        }

        public GCMConfig Config { get; } = new GCMConfig();

        public void Start(IPAddress ipaddress, int port, Config config = null)
        {
            lock (this)
            {
                if (Server != null)
                    return;

                if (null == config)
                {
                    config = new Config();
                    config.AddCustomize(Config);
                    config.LoadAndParse();
                }
                Sessions = new ConcurrentDictionary<int, CacheHolder>(Config.ConcurrencyLevel, 4096);
                global = new ConcurrentDictionary<Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey, CacheState>
                    (Config.ConcurrencyLevel, Config.InitialCapacity);

                Server = new ServerService(config);

                Server.AddFactoryHandle(
                    new Acquire().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new Acquire(),
                        Handle = ProcessAcquireRequest,
                    });

                Server.AddFactoryHandle(
                    new Reduce().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new Reduce(),
                    });

                Server.AddFactoryHandle(
                    new Login().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new Login(),
                        Handle = ProcessLogin,
                    });

                Server.AddFactoryHandle(
                    new ReLogin().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new ReLogin(),
                        Handle = ProcessReLogin,
                    });

                Server.AddFactoryHandle(
                    new NormalClose().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new NormalClose(),
                        Handle = ProcessNormalClose,
                    });

                // 临时注册到这里，安全起见应该起一个新的Service，并且仅绑定到 localhost。
                Server.AddFactoryHandle(
                    new Cleanup().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new Cleanup(),
                        Handle = ProcessCleanup,
                    });

                ServerSocket = Server.NewServerSocket(ipaddress, port, null);

            }
        }

        public void Stop()
        {
            lock (this)
            {
                if (null == Server)
                    return;
                ServerSocket.Dispose();
                ServerSocket = null;
                Server.Stop();
                Server = null;
            }
        }

        /// <summary>
        /// 报告错误的时候带上相关信息（包括GlobalCacheManager和LogicServer等等）
        /// 手动Cleanup时，连接正确的服务器执行。
        /// </summary>
        /// <param name="p"></param>
        /// <returns></returns>
        private async Task<long> ProcessCleanup(Protocol p)
        {
            var rpc = p as Cleanup;

            // 安全性以后加强。
            if (false == rpc.Argument.SecureKey.Equals("Ok! verify secure."))
            {
                rpc.SendResultCode(CleanupErrorSecureKey);
                return 0;
            }

            var session = Sessions.GetOrAdd(rpc.Argument.ServerId, (key) => new CacheHolder(Config));
            if (session.GlobalCacheManagerHashIndex != rpc.Argument.GlobalCacheManagerHashIndex)
            {
                // 多点验证
                rpc.SendResultCode(CleanupErrorGlobalCacheManagerHashIndex);
                return 0;
            }

            if (this.Server.GetSocket(session.SessionId) != null)
            {
                // 连接存在，禁止cleanup。
                rpc.SendResultCode(CleanupErrorHasConnection);
                return 0;
            }

            // 还有更多的防止出错的手段吗？
            await Task.Delay(5 * 60 * 1000); // delay 5 mins

            foreach (var e in session.Acquired)
            {
                // ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
                await ReleaseAsync(session, e.Key, false);
            }
            rpc.SendResultCode(0);
            return 0;
        }

        private async Task<long> ProcessLogin(Protocol p)
        {
            var rpc = p as Login;
            var session = Sessions.GetOrAdd(rpc.Argument.ServerId, (_) => new CacheHolder(Config));
            using var lockss = await session.Mutex.LockAsync();
            {
                if (false == session.TryBindSocket(p.Sender, rpc.Argument.GlobalCacheManagerHashIndex))
                {
                    rpc.SendResultCode(LoginBindSocketFail);
                    return 0;
                }
                // new login, 比如逻辑服务器重启。release old acquired.
                foreach (var e in session.Acquired)
                {
                    // ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
                    await ReleaseAsync(session, e.Key, false);
                }
                rpc.SendResultCode(0);
            }
            return 0;
        }

        private async Task<long> ProcessReLogin(Protocol p)
        {
            var rpc = p as ReLogin;
            var session = Sessions.GetOrAdd(rpc.Argument.ServerId, (_) => new CacheHolder(Config));
            using var lockss = await session.Mutex.LockAsync();
            {
                if (false == session.TryBindSocket(p.Sender, rpc.Argument.GlobalCacheManagerHashIndex))
                {
                    rpc.SendResultCode(ReLoginBindSocketFail);
                    return 0;
                }
                rpc.SendResultCode(0);
            }
            return 0;
        }
        
        private async Task<long> ProcessNormalClose(Protocol p)
        {
            var rpc = p as NormalClose;
            if (rpc.Sender.UserState is not CacheHolder session)
            {
                rpc.SendResultCode(AcquireNotLogin);
                return 0; // not login
            }

            using var lockss = await session.Mutex.LockAsync();
            {
                if (false == session.TryUnBindSocket(p.Sender))
                {
                    rpc.SendResultCode(NormalCloseUnbindFail);
                    return 0;
                }
                foreach (var e in session.Acquired)
                {
                    // ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
                    await ReleaseAsync(session, e.Key, false);
                }
                rpc.SendResultCode(0);
                logger.Debug("After NormalClose global.Count={0}", global.Count);
            }
            return 0;
        }

        private async Task<long> ProcessAcquireRequest(Protocol p)
        {
            Acquire rpc = (Acquire)p;
            rpc.Result.GlobalTableKey = rpc.Argument.GlobalTableKey;
            rpc.Result.State = rpc.Argument.State; // default success

            if (rpc.Sender.UserState == null)
            {
                rpc.Result.State = StateInvalid;
                rpc.SendResultCode(AcquireNotLogin);
                return 0;
            }
            try
            {
                switch (rpc.Argument.State)
                {
                    case StateInvalid: // realease
                        rpc.Result.State = await ReleaseAsync(rpc.Sender.UserState as CacheHolder, rpc.Argument.GlobalTableKey, true);
                        rpc.SendResult();
                        return 0;

                    case StateShare:
                        return await AcquireShareAsync(rpc);

                    case StateModify:
                        return await AcquireModifyAsync(rpc);

                    default:
                        rpc.Result.State = StateInvalid;
                        rpc.SendResultCode(AcquireErrorState);
                        return 0;
                }
            }
            catch (Exception e)
            {
                logger.Error(e);
                rpc.Result.State = StateInvalid;
                rpc.SendResultCode(AcquireException);
                return 0;
            }
        }

        private async Task<int> ReleaseAsync(CacheHolder sender, Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey gkey, bool noWait)
        {
            while (true)
            {
                CacheState cs = global.GetOrAdd(gkey, (tabkeKeyNotUsed) => new CacheState());
                using var lockcs = await cs.Monitor.EnterAsync();

                if (cs.AcquireStatePending == StateRemoved)
                    continue; // 这个是不可能的，因为有Release请求进来意味着肯定有拥有者(share or modify)，此时不可能进入StateRemoved。

                while (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved)
                {
                    switch (cs.AcquireStatePending)
                    {
                        case StateShare:
                        case StateModify:
                            logger.Debug("Release 0 {} {} {}", sender, gkey, cs);
                            if (noWait)
                                return cs.GetSenderCacheState(sender);
                            break;
                        case StateRemoving:
                            // release 不会导致死锁，等待即可。
                            break;
                    }
                    await cs.Monitor.WaitAsync();
                }
                if (cs.AcquireStatePending == StateRemoved)
                {
                    continue;
                }
                cs.AcquireStatePending = StateRemoving;

                if (cs.Modify == sender)
                    cs.Modify = null;
                cs.Share.Remove(sender); // always try remove

                if (cs.Modify == null && cs.Share.Count == 0)
                {
                    // 安全的从global中删除，没有并发问题。
                    cs.AcquireStatePending = StateRemoved;
                    global.TryRemove(gkey, out var _);
                }
                else
                {
                    cs.AcquireStatePending = StateInvalid;
                }
                sender.Acquired.TryRemove(gkey, out var _);
                cs.Monitor.PulseAll();
                return cs.GetSenderCacheState(sender);
            }
        }

        private async Task<int> AcquireShareAsync(Acquire rpc)
        {
            CacheHolder sender = (CacheHolder)rpc.Sender.UserState;

            while (true)
            {
                CacheState cs = global.GetOrAdd(rpc.Argument.GlobalTableKey, (_) => new CacheState());
                using var lockcs = await cs.Monitor.EnterAsync();

                if (cs.AcquireStatePending == StateRemoved)
                    continue;

                if (cs.Modify != null && cs.Share.Count > 0)
                    throw new Exception("CacheState state error");

                while (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved)
                {
                    switch (cs.AcquireStatePending)
                    {
                        case StateShare:
                            if (cs.Modify == null)
                                throw new Exception("CacheState state error");

                            if (cs.Modify == sender)
                            {
                                logger.Debug("1 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = StateInvalid;
                                rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                                rpc.SendResultCode(AcquireShareDeadLockFound);
                                return 0;
                            }
                            break;
                        case StateModify:
                            if (cs.Modify == sender || cs.Share.Contains(sender))
                            {
                                logger.Debug("2 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = StateInvalid;
                                rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                                rpc.SendResultCode(AcquireShareDeadLockFound);
                                return 0;
                            }
                            break;
                        case StateRemoving:
                            break;
                    }
                    logger.Debug("3 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    await cs.Monitor.WaitAsync();
                    if (cs.Modify != null && cs.Share.Count > 0)
                        throw new Exception("CacheState state error");
                }
                if (cs.AcquireStatePending == StateRemoved)
                    continue; // concurrent release.

                cs.AcquireStatePending = StateShare;
                cs.GlobalSerialId = SerialIdGenerator.IncrementAndGet();

                if (cs.Modify != null)
                {
                    if (cs.Modify == sender)
                    {
                        cs.AcquireStatePending = StateInvalid;
                        logger.Debug("4 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.Result.State = StateModify;
                        // 已经是Modify又申请，可能是sender异常关闭，
                        // 又重启连上。更新一下。应该是不需要的。
                        sender.Acquired[rpc.Argument.GlobalTableKey] = StateModify;
                        rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                        rpc.SendResultCode(AcquireShareAlreadyIsModify);
                        return 0;
                    }

                    int reduceResultState = StateReduceNetError; // 默认网络错误。
                    if (cs.Modify.Reduce(rpc.Argument.GlobalTableKey, StateInvalid, cs.GlobalSerialId,
                        async (p) =>
                        {
                            var r = p as Reduce;
                            reduceResultState = r.IsTimeout ? StateReduceRpcTimeout : r.Result.State;
                            using var lockcs = await cs.Monitor.EnterAsync();
                            cs.Monitor.PulseAll();
                            return 0;
                        }))
                    {
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        await cs.Monitor.WaitAsync();
                    }

                    switch (reduceResultState)
                    {
                        case StateShare:
                            cs.Modify.Acquired[rpc.Argument.GlobalTableKey] = StateShare;
                            cs.Share.Add(cs.Modify); // 降级成功。
                            break;

                        case StateInvalid:
                            // 降到了 Invalid，此时就不需要加入 Share 了。
                            cs.Modify.Acquired.TryRemove(rpc.Argument.GlobalTableKey, out _);
                            break;

                        default:
                            // 包含协议返回错误的值的情况。
                            // case StateReduceRpcTimeout:
                            // case StateReduceException:
                            // case StateReduceNetError:
                            cs.AcquireStatePending = StateInvalid;
                            cs.Monitor.PulseAll();

                            logger.Error("XXX 8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = StateInvalid;
                            rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                            rpc.SendResultCode(AcquireShareFailed);
                            return 0;
                    }

                    cs.Modify = null;
                    sender.Acquired[rpc.Argument.GlobalTableKey] = StateShare;
                    cs.Share.Add(sender);
                    cs.AcquireStatePending = StateInvalid;
                    cs.Monitor.PulseAll();
                    logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                    rpc.SendResult();
                    return 0;
                }

                sender.Acquired[rpc.Argument.GlobalTableKey] = StateShare;
                cs.Share.Add(sender);
                cs.AcquireStatePending = StateInvalid;
                cs.Monitor.PulseAll();
                logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                rpc.SendResult();

                return 0;
            }
        }

        private async Task<int> AcquireModifyAsync(Acquire rpc)
        {
            CacheHolder sender = (CacheHolder)rpc.Sender.UserState;
            while (true)
            {
                CacheState cs = global.GetOrAdd(rpc.Argument.GlobalTableKey, (tabkeKeyNotUsed) => new CacheState());
                using var lockcs = await cs.Monitor.EnterAsync();

                if (cs.AcquireStatePending == StateRemoved)
                    continue;

                if (cs.Modify != null && cs.Share.Count > 0)
                    throw new Exception("CacheState state error");

                while (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved)
                {
                    switch (cs.AcquireStatePending)
                    {
                        case StateShare:
                            if (cs.Modify == null)
                            {
                                logger.Error("cs state must be modify");
                                throw new Exception("CacheState state error");
                            }
                            if (cs.Modify == sender)
                            {
                                logger.Debug("1 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = StateInvalid;
                                rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                                rpc.SendResultCode(AcquireModifyDeadLockFound);
                                return 0;
                            }
                            break;
                        case StateModify:
                            if (cs.Modify == sender || cs.Share.Contains(sender))
                            {
                                logger.Debug("2 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = StateInvalid;
                                rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                                rpc.SendResultCode(AcquireModifyDeadLockFound);
                                return 0;
                            }
                            break;
                        case StateRemoving:
                            break;
                    }
                    logger.Debug("3 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    await cs.Monitor.WaitAsync();

                    if (cs.Modify != null && cs.Share.Count > 0)
                        throw new Exception("CacheState state error");
                }
                if (cs.AcquireStatePending == StateRemoved)
                    continue; // concurrent release

                cs.AcquireStatePending = StateModify;
                cs.GlobalSerialId = SerialIdGenerator.IncrementAndGet();

                if (cs.Modify != null)
                {
                    if (cs.Modify == sender)
                    {
                        logger.Debug("4 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        // 已经是Modify又申请，可能是sender异常关闭，又重启连上。
                        // 更新一下。应该是不需要的。
                        sender.Acquired[rpc.Argument.GlobalTableKey] = StateModify;
                        rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                        rpc.SendResultCode(AcquireModifyAlreadyIsModify);
                        cs.AcquireStatePending = StateInvalid;
                        cs.Monitor.PulseAll();
                        return 0;
                    }

                    int reduceResultState = StateReduceNetError; // 默认网络错误。
                    if (cs.Modify.Reduce(rpc.Argument.GlobalTableKey, StateInvalid, cs.GlobalSerialId,
                        async (p) =>
                        {
                            var r = p as Reduce;
                            reduceResultState = r.IsTimeout ? StateReduceRpcTimeout : r.Result.State;
                            using var lockcs = await cs.Monitor.EnterAsync();
                            cs.Monitor.PulseAll();
                            return 0;
                        }))
                    {
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        await cs.Monitor.WaitAsync();
                    }

                    switch (reduceResultState)
                    {
                        case StateInvalid:
                            cs.Modify.Acquired.TryRemove(rpc.Argument.GlobalTableKey, out _);
                            break; // reduce success

                        default:
                            // case StateReduceRpcTimeout:
                            // case StateReduceException:
                            // case StateReduceNetError:
                            cs.AcquireStatePending = StateInvalid;
                            cs.Monitor.PulseAll();

                            logger.Error("XXX 9 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = StateInvalid;
                            rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                            rpc.SendResultCode(AcquireModifyFailed);
                            return 0;
                    }

                    cs.Modify = sender;
                    cs.Share.Remove(sender);
                    sender.Acquired[rpc.Argument.GlobalTableKey] = StateModify;
                    cs.AcquireStatePending = StateInvalid;
                    cs.Monitor.PulseAll();

                    logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                    rpc.SendResult();
                    return 0;
                }

                List<Util.KV<CacheHolder, Reduce>> reducePending = new();
                HashSet<CacheHolder> reduceSucceed = new();
                bool senderIsShare = false;
                // 先把降级请求全部发送给出去。
                foreach (CacheHolder c in cs.Share)
                {
                    if (c == sender)
                    {
                        // 申请者不需要降级，直接加入成功。
                        senderIsShare = true;
                        reduceSucceed.Add(sender);
                        continue;
                    }
                    Reduce reduce = c.ReduceWaitLater(rpc.Argument.GlobalTableKey, StateInvalid, cs.GlobalSerialId);
                    if (null != reduce)
                    {
                        reducePending.Add(Util.KV.Create(c, reduce));
                    }
                    else
                    {
                        // 网络错误不再认为成功。整个降级失败，要中断降级。
                        // 已经发出去的降级请求要等待并处理结果。后面处理。
                        break;
                    }
                }
                // 两种情况不需要发reduce
                // 1. share是空的, 可以直接升为Modify
                // 2. sender是share, 而且reducePending的size是0
                if (!(cs.Share.Count == 0) && (!senderIsShare || reducePending.Count > 0))
                {
                    // 必须放到另外的线程执行，后面cs.Monitor.WaitAsync();会释放锁。
                    // 这是必须的。
                    _ = Task.Run(async () =>
                    {
                        // 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，
                        // 应该也会等待所有任务结束（包括错误）。
                        foreach (var reduce in reducePending)
                        {
                            try
                            {
                                await reduce.Value.Future.Task;
                                if (reduce.Value.Result.State == StateInvalid)
                                    reduceSucceed.Add(reduce.Key);
                                else
                                    reduce.Key.SetError();
                            }
                            catch (Exception ex)
                            {
                                reduce.Key.SetError();
                                // 等待失败不再看作成功。
                                logger.Error(ex, "Reduce {0} {1} {2} {3}", sender, rpc.Argument.State, cs, reduce.Value.Argument);
                            }
                        }
                        using var lockcs = await cs.Monitor.EnterAsync();
                        cs.Monitor.PulseAll();
                    });
                    logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    await cs.Monitor.WaitAsync();
                }

                // 移除成功的。
                foreach (CacheHolder succeed in reduceSucceed)
                {
                    if (succeed != sender)
                    {
                        // sender 不移除：
                        // 1. 如果申请成功，后面会更新到Modify状态。
                        // 2. 如果申请不成功，恢复 cs.Share，保持 Acquired 不变。
                        succeed.Acquired.TryRemove(rpc.Argument.GlobalTableKey, out var _);
                    }
                    cs.Share.Remove(succeed);
                }

                // 如果前面降级发生中断(break)，这里就不会为0。
                if (cs.Share.Count == 0)
                {
                    cs.Modify = sender;
                    sender.Acquired[rpc.Argument.GlobalTableKey] = StateModify;
                    cs.AcquireStatePending = StateInvalid;
                    cs.Monitor.PulseAll();

                    logger.Debug("8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                    rpc.SendResult();
                }
                else
                {
                    // senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
                    // 失败了，要把原来是share的sender恢复。先这样吧。
                    if (senderIsShare)
                        cs.Share.Add(sender);

                    cs.AcquireStatePending = StateInvalid;
                    cs.Monitor.PulseAll();
                    logger.Error("XXX 10 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.Result.State = StateInvalid;
                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                    rpc.SendResultCode(AcquireModifyFailed);
                }
                return 0;
            }
        }

        public sealed class CacheState
        {
            internal CacheHolder Modify { get; set; }
            internal int AcquireStatePending { get; set; } = StateInvalid;
            internal long GlobalSerialId { get; set; }
            internal HashSet<CacheHolder> Share { get; } = new HashSet<CacheHolder>();
            internal Nito.AsyncEx.AsyncMonitor Monitor { get; } = new Nito.AsyncEx.AsyncMonitor();

            public override string ToString()
            {
                StringBuilder sb = new();
                ByteBuffer.BuildString(sb, Share);
                return $"P{AcquireStatePending} M{Modify} S{sb}";
            }

            public int GetSenderCacheState(CacheHolder sender)
            {
                if (Modify == sender)
                    return StateModify;
                if (Share.Contains(sender))
                    return StateShare;
                return StateInvalid;
            }
        }

        public sealed class CacheHolder
        {
            private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

            public long SessionId { get; private set; }
            public int GlobalCacheManagerHashIndex { get; private set; } // UnBind 的时候不会重置，会一直保留到下一次Bind。

            public ConcurrentDictionary<Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey, int> Acquired { get; }
            public Nito.AsyncEx.AsyncLock Mutex { get; } = new Nito.AsyncEx.AsyncLock();

            public CacheHolder(GCMConfig config)
            {
                Acquired = new ConcurrentDictionary<Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey, int>(
                    config.ConcurrencyLevel, config.InitialCapacity);
            }

            public bool TryBindSocket(AsyncSocket newSocket, int _GlobalCacheManagerHashIndex)
            {
                if (newSocket.UserState != null)
                    return false; // 不允许再次绑定。Login Or ReLogin 只能发一次。

                var socket = GlobalCacheManagerServer.Instance.Server.GetSocket(SessionId);
                if (null == socket)
                {
                    // old socket not exist or has lost.
                    SessionId = newSocket.SessionId;
                    newSocket.UserState = this;
                    GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
                    return true;
                }
                // 每个AutoKeyLocalId只允许一个实例，已经存在了以后，旧的实例上有状态，阻止新的实例登录成功。
                return false;
            }

            public bool TryUnBindSocket(AsyncSocket oldSocket)
            {
                // 这里检查比较严格，但是这些检查应该都不会出现。

                if (oldSocket.UserState != this)
                    return false; // not bind to this

                var socket = GlobalCacheManagerServer.Instance.Server.GetSocket(SessionId);
                if (socket != oldSocket)
                    return false; // not same socket

                SessionId = 0;
                return true;
            }
            public override string ToString()
            {
                return "" + SessionId;
            }

            public bool Reduce(Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey gkey,
                int state, long globalSerialId, Func<Protocol, Task<long>> response)
            {
                try
                {
                    lock (this)
                    {
                        if (global::Zeze.Util.Time.NowUnixMillis - LastErrorTime < ForbidPeriod)
                            return false;
                    }
                    AsyncSocket peer = GlobalCacheManagerServer.Instance.Server.GetSocket(SessionId);
                    if (null != peer)
                    {
                        Reduce reduce = new(gkey, state, globalSerialId);
                        if (reduce.Send(peer, response, 10000))
                            return true;
                    }
                }
                catch (Exception ex)
                {
                    // 这里的异常只应该是网络发送异常。
                    logger.Error(ex, "ReduceWaitLater Exception {0}", gkey);
                }
                SetError();
                return false;
            }

            public const long ForbidPeriod = 10 * 1000; // 10 seconds
            private long LastErrorTime = 0;

            public void SetError()
            {
                lock (this)
                {
                    long now = global::Zeze.Util.Time.NowUnixMillis;
                    if (now - LastErrorTime > ForbidPeriod)
                        LastErrorTime = now;
                }
            }

            /// <summary>
            /// 返回null表示发生了网络错误，或者应用服务器已经关闭。
            /// </summary>
            /// <param name="gkey"></param>
            /// <param name="state"></param>
            /// <returns></returns>
            public Reduce ReduceWaitLater(Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey gkey, int state, long globalSerialId)
            {
                try
                {
                    lock (this)
                    {
                        if (global::Zeze.Util.Time.NowUnixMillis - LastErrorTime < ForbidPeriod)
                            return null;
                    }
                    AsyncSocket peer = GlobalCacheManagerServer.Instance.Server.GetSocket(SessionId);
                    if (null != peer)
                    {
                        Reduce reduce = new(gkey, state, globalSerialId);
                        _ = reduce.SendAsync(peer, 10000);
                        return reduce;
                    }
                }
                catch (Exception ex)
                {
                    // 这里的异常只应该是网络发送异常。
                    logger.Error(ex, "ReduceWaitLater Exception {0}", gkey);
                }
                SetError();
                return null;
            }
        }
    }
}

namespace Zeze.Services.GlobalCacheManager
{
    public sealed class Param : Bean
    {
        public Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey GlobalTableKey { get; set; } // 没有初始化，使用时注意
        public int State { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            if (null == GlobalTableKey)
                GlobalTableKey = new Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey();
            GlobalTableKey.Decode(bb);
            State = bb.ReadInt();
        }

        public override void Encode(ByteBuffer bb)
        {
            GlobalTableKey.Encode(bb);
            bb.WriteInt(State);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            throw new NotImplementedException();
        }

        public override string ToString()
        {
            return GlobalTableKey + ":" + State;
        }
    }

    public sealed class Param2 : Bean
    {
        public Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey GlobalTableKey { get; set; } // 没有初始化，使用时注意
        public int State { get; set; }
        public long GlobalSerialId { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            if (null == GlobalTableKey)
                GlobalTableKey = new Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey();
            GlobalTableKey.Decode(bb);
            State = bb.ReadInt();

            GlobalSerialId = bb.ReadLong();
        }

        public override void Encode(ByteBuffer bb)
        {
            GlobalTableKey.Encode(bb);
            bb.WriteInt(State);

            bb.WriteLong(GlobalSerialId);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            throw new NotImplementedException();
        }

        public override string ToString()
        {
            return GlobalTableKey + ":" + State;
        }
    }

    public sealed class Acquire : Rpc<Param, Param2>
    {
        public readonly static int ProtocolId_ = Bean.Hash32(typeof(Acquire).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;

        public Acquire()
        {
        }

        public Acquire(Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey gkey, int state)
        {
            Argument.GlobalTableKey = gkey;
            Argument.State = state;
        }
    }

    public class Reduce : Rpc<Param2, Param2>
    {
        public readonly static int ProtocolId_ = Bean.Hash32(typeof(Reduce).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;

        public Reduce()
        {
        }

        public Reduce(Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey gkey, int state, long globalSerialId)
        {
            Argument.GlobalTableKey = gkey;
            Argument.State = state;
            Argument.GlobalSerialId = globalSerialId;
        }
    }

    public sealed class LoginParam : Bean
    {
        public int ServerId { get; set; }

        // GlobalCacheManager 本身没有编号。
        // 启用多个进程，使用 GlobalTableKey.GetHashCode() 分配负载后，报告错误需要这个来识别哪个进程。
        // 当然识别还可以根据 ServerService 绑定的ip和port。
        // 给每个实例加配置不容易维护。
        public int GlobalCacheManagerHashIndex { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            ServerId = bb.ReadInt();
            GlobalCacheManagerHashIndex = bb.ReadInt();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteInt(ServerId);
            bb.WriteInt(GlobalCacheManagerHashIndex);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            throw new NotImplementedException();
        }
    }

    public sealed class Login : Rpc<LoginParam, EmptyBean>
    {
        public readonly static int ProtocolId_ = Bean.Hash32(typeof(Login).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;

        public Login()
        {
        }

        public Login(int id)
        {
            Argument.ServerId = id;
        }
    }

    public sealed class ReLogin : Rpc<LoginParam, EmptyBean>
    {
        public readonly static int ProtocolId_ = Bean.Hash32(typeof(ReLogin).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;

        public ReLogin()
        {
        }

        public ReLogin(int id)
        {
            Argument.ServerId = id;
        }
    }

    public sealed class NormalClose : Rpc<EmptyBean, EmptyBean>
    {
        public readonly static int ProtocolId_ = Bean.Hash32(typeof(NormalClose).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

    public sealed class AchillesHeel : Bean
    {
        public int ServerId { get; set; } // 必须的。

        public string SecureKey { get; set; } // 安全验证
        public int GlobalCacheManagerHashIndex { get; set; } // 安全验证

        public override void Decode(ByteBuffer bb)
        {
            ServerId = bb.ReadInt();
            SecureKey = bb.ReadString();
            GlobalCacheManagerHashIndex = bb.ReadInt();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteInt(ServerId);
            bb.WriteString(SecureKey);
            bb.WriteInt(GlobalCacheManagerHashIndex);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            throw new NotImplementedException();
        }
    }

    public sealed class Cleanup : Rpc<AchillesHeel, EmptyBean>
    {
        public readonly static int ProtocolId_ = Bean.Hash32(typeof(Cleanup).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

    public sealed class ServerService : Service
    {
        public ServerService(Config config) : base("GlobalCacheManager", config)
        {
        }

        public override void OnSocketAccept(AsyncSocket so)
        {
            // so.UserState = new CacheHolder(so.SessionId); // Login ReLogin 的时候初始化。
            base.OnSocketAccept(so);
        }

        public override void OnSocketClose(AsyncSocket so, Exception e)
        {
            var session = (GlobalCacheManagerServer.CacheHolder)so.UserState;
            if (null == session)
            {
                // unbind when login
                session.TryUnBindSocket(so);
            }
            base.OnSocketClose(so, e);
        }
    }

    /*
    public sealed class GlobalTableKey : IComparable<GlobalTableKey>, Serializable
    {
        public string TableName { get; private set; }
        public byte[] Key { get; private set; }

        public GlobalTableKey()
        {
        }

        public GlobalTableKey(string tableName, ByteBuffer key) : this(tableName, key.Copy())
        {
        }

        public GlobalTableKey(string tableName, byte[] key)
        {
            TableName = tableName;
            Key = key;
        }

        public int CompareTo(GlobalTableKey other)
        {
            int c = this.TableName.CompareTo(other.TableName);
            if (c != 0)
                return c;

            return ByteBuffer.Compare(Key, other.Key);
        }

        public override bool Equals(object obj)
        {
            if (this == obj)
                return true;

            if (obj is GlobalTableKey another)
                return TableName.Equals(another.TableName) && ByteBuffer.Equals(Key, another.Key);

            return false;
        }

        public override int GetHashCode()
        {
            const int prime = 31;
            int result = 17;
            result = prime * result + ByteBuffer.calc_hashnr(TableName);
            result = prime * result + ByteBuffer.calc_hashnr(Key, 0, Key.Length);
            return result;
        }

        public override string ToString()
        {
            return $"({TableName},{BitConverter.ToString(Key)})";
        }

        public void Decode(ByteBuffer bb)
        {
            TableName = bb.ReadString();
            Key = bb.ReadBytes();
        }

        public void Encode(ByteBuffer bb)
        {
            bb.WriteString(TableName);
            bb.WriteBytes(Key);
        }
    }
    */

    /// <summary>
    /// AchillesHeel!
    /// GlobalAgent 定时发送KeepAlive，只要发现GlobalCacheManager没有相应，
    ///     就释放本地从该GlobalCacheManager申请的资源。
    /// GlobalCacheManager 一定时间（大于客户端发送间隔的两倍）没有收到某个GlobalAgent的KeepAlive，
    ///     就释放该GlobalAgent拥有的资源。【关键】这样定义是否足够，有没有数据安全问题？
    /// 【问题】
    ///     a) 如果GlobalAgent发送KeepAlive的代码死了（不能正确清理本地资源的状态），
    ///     但是其他执行事务的模块还活着，此时就需要把执行事务的模块通过检查一个标志，禁止活动，
    ///     检查这个这个标志在多个GlobalCacheManager时不容易高效实现。
    ///     b) 实行事务时检查标志的代码可能也会某些原因失效，那就更复杂了。
    ///     c) 另外本地要在KeepAlive失败时自动清理，需要记录锁修改状态，并且能正确Checkpoint。
    ///     这在某些异常原因导致本地服务器死掉时很可能无法正常进行。而此时GlobalCacheManager
    ///     超时就清理还是有风险。
    ///     *) 总之，可能的情况太多，KeepAlive还是不够安全。
    ///     所以先不实现了。
    /// </summary>
    public sealed class KeepAlive : Rpc<EmptyBean, EmptyBean>
    {
        public readonly static int ProtocolId_ = Bean.Hash32(typeof(KeepAlive).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }
}