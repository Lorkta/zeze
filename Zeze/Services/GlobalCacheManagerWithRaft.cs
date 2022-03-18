
using System;
using Zeze.Raft.RocksRaft;
using Zeze.Beans.GlobalCacheManagerWithRaft;
using System.Collections.Generic;
using System.Collections.Concurrent;
using Zeze.Net;

namespace Zeze.Services
{
    public class GlobalCacheManagerWithRaft : AbstractGlobalCacheManagerWithRaft, IDisposable
    {
        public const int GlobalSerialIdAtomicLongIndex = 0;

        protected override long ProcessAcquireRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as Acquire;
            rpc.Result.GlobalTableKey = rpc.Argument.GlobalTableKey;
            rpc.Result.State = rpc.Argument.State; // default success
            rpc.ResultCode = 0;

            if (rpc.Sender.UserState == null)
            {
                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                // û�е�¼��������¼��Agent�Զ����̵�һ���֣�Ӧ���Ժ����ԡ�
                rpc.SendResultCode(Zeze.Transaction.Procedure.RaftRetry);
                return 0;
            }

            new Procedure(Rocks,
                () =>
                {
                    switch (rpc.Argument.State)
                    {
                        case GlobalCacheManagerServer.StateInvalid: // realease
                            rpc.Result.State = _Release(rpc.Sender.UserState as CacheHolder, rpc.Argument.GlobalTableKey, true);
                            return 0;

                        case GlobalCacheManagerServer.StateShare:
                            return AcquireShare(rpc);

                        case GlobalCacheManagerServer.StateModify:
                            return AcquireModify(rpc);

                        default:
                            rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                            return GlobalCacheManagerServer.AcquireErrorState;
                    }
                })
            {
                // �����Զ�����rpc�����������Ψһ��顣
                AutoResponse = rpc,
            }
            .Call();

            return 0; // has handle all error.
        }

        private long AcquireShare(Acquire rpc)
        {
            CacheHolder sender = (CacheHolder)rpc.Sender.UserState;

            while (true)
            {
                var lockey = Zeze.Raft.RocksRaft.Transaction.Current.AddPessimismLock(Locks.Get(rpc.Argument.GlobalTableKey));

                CacheState cs = GlobalStates.GetOrAdd(rpc.Argument.GlobalTableKey);
                if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                    continue;

                if (cs.Modify != -1 && cs.Share.Count > 0)
                    throw new Exception("CacheState state error");

                while (cs.AcquireStatePending != GlobalCacheManagerServer.StateInvalid
                    && cs.AcquireStatePending != GlobalCacheManagerServer.StateRemoved)
                {
                    switch (cs.AcquireStatePending)
                    {
                        case GlobalCacheManagerServer.StateShare:
                            if (cs.Modify == -1)
                                throw new Exception("CacheState state error");

                            if (cs.Modify == sender.ServerId)
                            {
                                logger.Debug("1 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                                return GlobalCacheManagerServer.AcquireShareDeadLockFound;
                            }
                            break;

                        case GlobalCacheManagerServer.StateModify:
                            if (cs.Modify == sender.ServerId || cs.Share.Contains(sender.ServerId))
                            {
                                logger.Debug("2 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                                return GlobalCacheManagerServer.AcquireShareDeadLockFound;
                            }
                            break;

                        case GlobalCacheManagerServer.StateRemoving:
                            break;
                    }
                    logger.Debug("3 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    lockey.Wait();
                    if (cs.Modify != -1 && cs.Share.Count > 0)
                        throw new Exception("CacheState state error");
                }

                if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                    continue; // concurrent release.

                cs.AcquireStatePending = GlobalCacheManagerServer.StateShare;
                cs.GlobalSerialId = Rocks.AtomicLongIncrementAndGet(GlobalSerialIdAtomicLongIndex);
                var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(sender.ServerId);
                if (cs.Modify != -1)
                {
                    if (cs.Modify == sender.ServerId)
                    {
                        cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                        logger.Debug("4 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.Result.State = GlobalCacheManagerServer.StateModify;
                        // �Ѿ���Modify�����룬������sender�쳣�رգ�
                        // ���������ϡ�����һ�¡�Ӧ���ǲ���Ҫ�ġ�
                        SenderAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateModify });
                        rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                        return GlobalCacheManagerServer.AcquireShareAlreadyIsModify;
                    }

                    int reduceResultState = GlobalCacheManagerServer.StateReduceNetError; // Ĭ���������
                    if (CacheHolder.Reduce(Sessions, cs.Modify,
                        rpc.Argument.GlobalTableKey, GlobalCacheManagerServer.StateInvalid, cs.GlobalSerialId,
                        (p) =>
                        {
                            var r = p as Reduce;
                            reduceResultState = r.IsTimeout ? GlobalCacheManagerServer.StateReduceRpcTimeout : r.Result.State;
                            lockey.Enter();
                            try
                            {
                                lockey.PulseAll();
                            }
                            finally
                            {
                                lockey.Exit();
                            }
                            return 0;
                        }))
                    {
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        lockey.Wait();
                    }

                    var ModifyAcquired = ServerAcquiredTemplate.OpenTableWithType(cs.Modify);
                    switch (reduceResultState)
                    {
                        case GlobalCacheManagerServer.StateShare:
                            ModifyAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateShare });
                            cs.Share.Add(cs.Modify); // �����ɹ���
                            break;

                        case GlobalCacheManagerServer.StateInvalid:
                            // ������ Invalid����ʱ�Ͳ���Ҫ���� Share �ˡ�
                            ModifyAcquired.Remove(rpc.Argument.GlobalTableKey);
                            break;

                        default:
                            // ����Э�鷵�ش����ֵ�������
                            // case StateReduceRpcTimeout:
                            // case StateReduceException:
                            // case StateReduceNetError:
                            cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;

                            logger.Error("XXX 8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                            rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                            lockey.Pulse();
                            return GlobalCacheManagerServer.AcquireShareFailed;
                    }

                    cs.Modify = -1;
                    SenderAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateShare });
                    cs.Share.Add(sender.ServerId);
                    cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                    lockey.Pulse();
                    logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                    return 0; // �ɹ�Ҳ���Զ����ͽ��.
                }

                SenderAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateShare });
                cs.Share.Add(sender.ServerId);
                cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                lockey.Pulse();
                logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                return 0; // �ɹ�Ҳ���Զ����ͽ��.
            }
        }

        private long AcquireModify(Acquire rpc)
        {
            CacheHolder sender = (CacheHolder)rpc.Sender.UserState;

            while (true)
            {
                var lockey = Zeze.Raft.RocksRaft.Transaction.Current.AddPessimismLock(Locks.Get(rpc.Argument.GlobalTableKey));

                CacheState cs = GlobalStates.GetOrAdd(rpc.Argument.GlobalTableKey);
                if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                    continue;

                if (cs.Modify != -1 && cs.Share.Count > 0)
                    throw new Exception("CacheState state error");

                while (cs.AcquireStatePending != GlobalCacheManagerServer.StateInvalid
                    && cs.AcquireStatePending != GlobalCacheManagerServer.StateRemoved)
                {
                    switch (cs.AcquireStatePending)
                    {
                        case GlobalCacheManagerServer.StateShare:
                            if (cs.Modify == -1)
                            {
                                logger.Error("cs state must be modify");
                                throw new Exception("CacheState state error");
                            }
                            if (cs.Modify == sender.ServerId)
                            {
                                logger.Debug("1 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                                return GlobalCacheManagerServer.AcquireModifyDeadLockFound;
                            }
                            break;
                        case GlobalCacheManagerServer.StateModify:
                            if (cs.Modify == sender.ServerId || cs.Share.Contains(sender.ServerId))
                            {
                                logger.Debug("2 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                                return GlobalCacheManagerServer.AcquireModifyDeadLockFound;
                            }
                            break;
                        case GlobalCacheManagerServer.StateRemoving:
                            break;
                    }
                    logger.Debug("3 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    lockey.Wait();

                    if (cs.Modify != -1 && cs.Share.Count > 0)
                        throw new Exception("CacheState state error");
                }
                if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                    continue; // concurrent release

                cs.AcquireStatePending = GlobalCacheManagerServer.StateModify;
                cs.GlobalSerialId = Rocks.AtomicLongIncrementAndGet(GlobalSerialIdAtomicLongIndex);
                var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(sender.ServerId);
                if (cs.Modify != -1)
                {
                    if (cs.Modify == sender.ServerId)
                    {
                        logger.Debug("4 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        // �Ѿ���Modify�����룬������sender�쳣�رգ����������ϡ�
                        // ����һ�¡�Ӧ���ǲ���Ҫ�ġ�
                        SenderAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateModify });
                        rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                        cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                        lockey.Pulse();
                        return GlobalCacheManagerServer.AcquireModifyAlreadyIsModify;
                    }

                    int reduceResultState = GlobalCacheManagerServer.StateReduceNetError; // Ĭ���������
                    if (CacheHolder.Reduce(Sessions, cs.Modify,
                        rpc.Argument.GlobalTableKey, GlobalCacheManagerServer.StateInvalid, cs.GlobalSerialId,
                        (p) =>
                        {
                            var r = p as Reduce;
                            reduceResultState = r.IsTimeout ? GlobalCacheManagerServer.StateReduceRpcTimeout : r.Result.State;
                            lockey.Enter();
                            try
                            {
                                lockey.PulseAll();
                            }
                            finally
                            {
                                lockey.Exit();
                            }
                            return 0;
                        }))
                    {
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        lockey.Wait();
                    }

                    var ModifyAcquired = ServerAcquiredTemplate.OpenTableWithType(cs.Modify);
                    switch (reduceResultState)
                    {
                        case GlobalCacheManagerServer.StateInvalid:
                            ModifyAcquired.Remove(rpc.Argument.GlobalTableKey);
                            break; // reduce success

                        default:
                            // case StateReduceRpcTimeout:
                            // case StateReduceException:
                            // case StateReduceNetError:
                            cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                            lockey.Pulse();

                            logger.Error("XXX 9 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                            rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                            return GlobalCacheManagerServer.AcquireModifyFailed;
                    }

                    cs.Modify = sender.ServerId;
                    cs.Share.Remove(sender.ServerId);
                    SenderAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateModify });
                    cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                    lockey.Pulse();

                    logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                    return 0;
                }

                List<Util.KV<CacheHolder, Reduce>> reducePending = new();
                HashSet<CacheHolder> reduceSucceed = new();
                bool senderIsShare = false;
                // �Ȱѽ�������ȫ�����͸���ȥ��
                foreach (var c in cs.Share)
                {
                    if (c == sender.ServerId)
                    {
                        senderIsShare = true;
                        reduceSucceed.Add(sender);
                        continue;
                    }
                    Reduce reduce = CacheHolder.ReduceWaitLater(Sessions, c, out var session,
                        rpc.Argument.GlobalTableKey, GlobalCacheManagerServer.StateInvalid, cs.GlobalSerialId);
                    if (null != reduce)
                    {
                        reducePending.Add(Util.KV.Create(session, reduce));
                    }
                    else
                    {
                        // �����������Ϊ�ɹ�����������ʧ�ܣ�Ҫ�жϽ�����
                        // �Ѿ�����ȥ�Ľ�������Ҫ�ȴ��������������洦��
                        break;
                    }
                }
                // �����������Ҫ��reduce
                // 1. share�ǿյ�, ����ֱ����ΪModify
                // 2. sender��share, ����reducePending��size��0
                if (!(cs.Share.Count == 0) && (!senderIsShare || reducePending.Count > 0))
                {
                    Zeze.Util.Task.Run(
                    () =>
                    {
                        // һ�����ȴ��Ƿ�ɹ���WaitAll ��������֪����ô����ģ�
                        // Ӧ��Ҳ��ȴ���������������������󣩡�
                        foreach (var reduce in reducePending)
                        {
                            try
                            {
                                reduce.Value.Future.Task.Wait();
                                if (reduce.Value.Result.State == GlobalCacheManagerServer.StateInvalid)
                                {
                                    // ���滹�и��ɹ��Ĵ���ѭ��������������ܰ���sender��
                                    // ��������°ɡ�
                                    var KeyAcquired = ServerAcquiredTemplate.OpenTableWithType(reduce.Key.ServerId);
                                    KeyAcquired.Remove(rpc.Argument.GlobalTableKey);
                                    reduceSucceed.Add(reduce.Key);
                                }
                                else
                                {
                                    reduce.Key.SetError();
                                }
                            }
                            catch (Exception ex)
                            {
                                reduce.Key.SetError();
                                // �ȴ�ʧ�ܲ��ٿ����ɹ���
                                logger.Error(ex, "Reduce {0} {1} {2} {3}", sender, rpc.Argument.State, cs, reduce.Value.Argument);
                            }
                        }
                        lockey.Enter();
                        try
                        {
                                // ��Ҫ���ѵȴ���������ģ���û��ָ����ֻ��ȫ�����ѡ�
                                lockey.PulseAll();
                        }
                        finally
                        {
                            lockey.Exit();
                        }
                    },
                    "GlobalCacheManager.AcquireModify.WaitReduce");
                    logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    lockey.Wait();
                }

                // �Ƴ��ɹ��ġ�
                foreach (CacheHolder succeed in reduceSucceed)
                {
                    cs.Share.Remove(succeed.ServerId);
                }

                // ���ǰ�潵�������ж�(break)������Ͳ���Ϊ0��
                if (cs.Share.Count == 0)
                {
                    cs.Modify = sender.ServerId;
                    SenderAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateModify });
                    cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                    lockey.Pulse(); // Pending ����������һ�������Ϳ����ˡ�

                    logger.Debug("8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                    return 0;
                }

                // senderIsShare ��ʧ�ܵ�ʱ��Acquired û�б仯������Ҫ���¡�
                // ʧ���ˣ�Ҫ��ԭ����share��sender�ָ����������ɡ�
                if (senderIsShare)
                    cs.Share.Add(sender.ServerId);

                cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                lockey.Pulse(); // Pending ����������һ�������Ϳ����ˡ�

                logger.Error("XXX 10 {0} {1} {2}", sender, rpc.Argument.State, cs);

                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                return GlobalCacheManagerServer.AcquireModifyFailed;
            }
        }

        private int Release(CacheHolder sender, GlobalTableKey gkey, bool noWait)
        {
            int result = 0;
            Rocks.NewProcedure(() =>
            {
                result = _Release(sender, gkey, noWait);
                return 0;
            }).Call();
            return result;
        }

        private int _Release(CacheHolder sender, GlobalTableKey gkey, bool noWait)
        {
            while (true)
            {
                var lockey = Zeze.Raft.RocksRaft.Transaction.Current.AddPessimismLock(Locks.Get(gkey));

                CacheState cs = GlobalStates.GetOrAdd(gkey);
                if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                    continue; // ����ǲ����ܵģ���Ϊ��Release���������ζ�ſ϶���ӵ����(share or modify)����ʱ�����ܽ���StateRemoved��

                while (cs.AcquireStatePending != GlobalCacheManagerServer.StateInvalid
                    && cs.AcquireStatePending != GlobalCacheManagerServer.StateRemoved)
                {
                    switch (cs.AcquireStatePending)
                    {
                        case GlobalCacheManagerServer.StateShare:
                        case GlobalCacheManagerServer.StateModify:
                            logger.Debug("Release 0 {} {} {}", sender, gkey, cs);
                            if (noWait)
                                return GetSenderCacheState(cs, sender);
                            break;
                        case GlobalCacheManagerServer.StateRemoving:
                            // release ���ᵼ���������ȴ����ɡ�
                            break;
                    }
                    lockey.Wait();
                }
                if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                {
                    continue;
                }
                cs.AcquireStatePending = GlobalCacheManagerServer.StateRemoving;

                if (cs.Modify == sender.ServerId)
                    cs.Modify = -1;
                cs.Share.Remove(sender.ServerId); // always try remove

                if (cs.Modify == -1
                    && cs.Share.Count == 0
                    && cs.AcquireStatePending == GlobalCacheManagerServer.StateInvalid)
                {
                    // ��ȫ�Ĵ�global��ɾ����û�в������⡣
                    cs.AcquireStatePending = GlobalCacheManagerServer.StateRemoved;
                    GlobalStates.Remove(gkey);
                }
                else
                {
                    cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                }
                var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(sender.ServerId);
                SenderAcquired.Remove(gkey);
                lockey.Pulse();
                return GetSenderCacheState(cs, sender);
            }
        }

        private int GetSenderCacheState(CacheState cs, CacheHolder sender)
        {
            if (cs.Modify == sender.ServerId)
                return GlobalCacheManagerServer.StateModify;
            if (cs.Share.Contains(sender.ServerId))
                return GlobalCacheManagerServer.StateShare;
            return GlobalCacheManagerServer.StateInvalid;
        }

        protected override long ProcessLoginRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as Login;
            var session = Sessions.GetOrAdd(rpc.Argument.ServerId,
                (_) => new CacheHolder() { GlobalInstance = this, ServerId = rpc.Argument.ServerId });

            lock (session) // ͬһ���ڵ㻥�⡣��ͬ�ڵ�Bind����Ҫ���⣬Release��Raft-LeaderΨһ���ṩ������
            {
                if (false == session.TryBindSocket(rpc.Sender, rpc.Argument.GlobalCacheManagerHashIndex))
                {
                    rpc.SendResultCode(GlobalCacheManagerServer.LoginBindSocketFail);
                    return 0;
                }
                // new login, �����߼�������������release old acquired.
                var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(session.ServerId);
                SenderAcquired.Walk((key, value) =>
                {
                    Release(session, key, false);
                    return true; // continue walk
                });
                rpc.SendResultCode(0);
                logger.Info($"Login {Rocks.Raft.Name} {rpc.Sender}.");
                return 0;
            }
        }

        protected override long ProcessReLoginRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as ReLogin;
            var session = Sessions.GetOrAdd(rpc.Argument.ServerId, 
                (key) => new CacheHolder() { GlobalInstance = this, ServerId = rpc.Argument.ServerId });

            lock (session) // ͬһ���ڵ㻥�⡣
            {
                if (false == session.TryBindSocket(rpc.Sender, rpc.Argument.GlobalCacheManagerHashIndex))
                {
                    rpc.SendResultCode(GlobalCacheManagerServer.ReLoginBindSocketFail);
                    return 0;
                }
                rpc.SendResultCode(0);
                logger.Info($"ReLogin {Rocks.Raft.Name} {rpc.Sender}.");
                return 0;
            }
        }

        protected override long ProcessNormalCloseRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as NormalClose;
            if (rpc.Sender.UserState is not CacheHolder session)
            {
                rpc.SendResultCode(GlobalCacheManagerServer.AcquireNotLogin);
                return 0; // not login
            }

            lock (session) // ͬһ���ڵ㻥�⡣��ͬ�ڵ�Bind����Ҫ���⣬Release��Raft-LeaderΨһ���ṩ������
            {
                if (false == session.TryUnBindSocket(rpc.Sender))
                {
                    rpc.SendResultCode(GlobalCacheManagerServer.NormalCloseUnbindFail);
                    return 0;
                }
                // TODO ȷ��Walk��ɾ����¼�Ƿ������⡣
                var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(session.ServerId);
                SenderAcquired.Walk((key, value) =>
                {
                    Release(session, key, false);
                    return true; // continue walk
                });
                rpc.SendResultCode(0);
                logger.Info($"NormalClose {Rocks.Raft.Name} {rpc.Sender}");
                return 0;
            }
        }

        protected override long ProcessCleanupRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as Cleanup;

            // ��ȫ���Ժ��ǿ��
            if (false == rpc.Argument.SecureKey.Equals("Ok! verify secure."))
            {
                rpc.SendResultCode(GlobalCacheManagerServer.CleanupErrorSecureKey);
                return 0;
            }

            var session = Sessions.GetOrAdd(rpc.Argument.ServerId, (key) => new CacheHolder() { GlobalInstance = this });
            if (session.GlobalCacheManagerHashIndex != rpc.Argument.GlobalCacheManagerHashIndex)
            {
                // �����֤
                rpc.SendResultCode(GlobalCacheManagerServer.CleanupErrorGlobalCacheManagerHashIndex);
                return 0;
            }

            if (Rocks.Raft.Server.GetSocket(session.SessionId) != null)
            {
                // ���Ӵ��ڣ���ֹcleanup��
                rpc.SendResultCode(GlobalCacheManagerServer.CleanupErrorHasConnection);
                return 0;
            }

            // ���и���ķ�ֹ������ֶ���

            // XXX verify danger
            Zeze.Util.Scheduler.Instance.Schedule(
                (ThisTask) =>
                {
                    var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(session.ServerId);
                    SenderAcquired.Walk((key, value) =>
                    {
                        Release(session, key, false);
                        return true; // continue release;
                    });
                    rpc.SendResultCode(0);
                },
                5 * 60 * 1000); // delay 5 mins

            return 0;
        }

        protected override long ProcessKeepAliveRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as KeepAlive;
            rpc.SendResultCode(Zeze.Transaction.Procedure.NotImplement);
            return 0;
        }

        private Rocks Rocks { get; }
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        private readonly Locks Locks = new Locks();

        /// <summary>
        /// ȫ�ּ�¼����״̬��
        /// </summary>
        private readonly Table<GlobalTableKey, CacheState> GlobalStates;

        /// <summary>
        /// ÿ���������ѷ����¼��
        /// ���Ǹ�Tableģ�壬ʹ�õ�ʱ�����ServerId�������Ĵ洢��
        /// </summary>
        private readonly TableTemplate<GlobalTableKey, AcquiredState> ServerAcquiredTemplate;

        /*
         * �Ự��
         * key�� LogicServer.Id�����ڵ�ʵ�־���Zeze.Config.ServerId��
         * �����ӽ������յ���Login Or ReLogin �����á�
         * ÿ���Ự����Ҫ��¼�ûỰ��Socket.SessionId�����������½���ʱ���¡�
         * ����GetOrAdd����ɾ���������ڵ�cache-sync��ƣ�
         * ServerId �Ǽ������޵ġ�����һֱ������
         * ��ʵ�֡�
         */
        private readonly ConcurrentDictionary<int, CacheHolder> Sessions = new ConcurrentDictionary<int, CacheHolder>();

        public GlobalCacheManagerWithRaft(
            string raftName,
            Raft.RaftConfig raftconf = null,
            Config config = null,
            bool RocksDbWriteOptionSync = false)
        { 
            Rocks = new Rocks(raftName, RocksMode.Pessimism, raftconf, config, RocksDbWriteOptionSync);

            RegisterRocksTables(Rocks);
            RegisterProtocols(Rocks.Raft.Server);

            GlobalStates = Rocks.GetTableTemplate("Global").OpenTable<GlobalTableKey, CacheState>(0);
            ServerAcquiredTemplate = Rocks.GetTableTemplate("Session") as TableTemplate<GlobalTableKey, AcquiredState>;

            Rocks.Raft.Server.Start();
        }

        public void Dispose()
        {
            Rocks.Raft.Shutdown();
        }

        public sealed class CacheHolder
        {
            public long SessionId { get; private set; }
            public int GlobalCacheManagerHashIndex { get; private set; }
            public int ServerId { get; internal set; }
            public GlobalCacheManagerWithRaft GlobalInstance { get; set; }

            public bool TryBindSocket(AsyncSocket newSocket, int _GlobalCacheManagerHashIndex)
            {
                if (newSocket.UserState != null && newSocket.UserState != this)
                    return false; // �����ظ�login|relogin�����������л�ServerId��

                var socket = GlobalInstance.Rocks.Raft.Server.GetSocket(SessionId);
                if (null == socket || socket == newSocket)
                {
                    // old socket not exist or has lost.
                    SessionId = newSocket.SessionId;
                    newSocket.UserState = this;
                    GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
                    return true;
                }
                // ÿ��ServerIdֻ����һ��ʵ�����Ѿ��������Ժ󣬾ɵ�ʵ������״̬����ֹ�µ�ʵ����¼�ɹ���
                return false;
            }

            public bool TryUnBindSocket(AsyncSocket oldSocket)
            {
                // ������Ƚ��ϸ񣬵�����Щ���Ӧ�ö�������֡�

                if (oldSocket.UserState != this)
                    return false; // not bind to this

                var socket = GlobalInstance.Rocks.Raft.Server.GetSocket(SessionId);
                if (socket != oldSocket)
                    return false; // not same socket

                SessionId = 0;
                return true;
            }

            public static bool Reduce(ConcurrentDictionary<int, CacheHolder> sessions, int serverId,
                GlobalTableKey gkey, int state, long globalSerialId, Func<Protocol, long> response)
            { 
                if (sessions.TryGetValue(serverId, out var session))
                    return session.Reduce(gkey, state, globalSerialId, response);

                return false;
            }

            public static Reduce ReduceWaitLater(ConcurrentDictionary<int, CacheHolder> sessions,
                int serverId, out CacheHolder session,
                GlobalTableKey gkey, int state, long globalSerialId)
            {
                if (sessions.TryGetValue(serverId, out session))
                    return session.ReduceWaitLater(gkey, state, globalSerialId);

                return null;
            }

            public bool Reduce(GlobalTableKey gkey, int state, long globalSerialId, Func<Protocol, long> response)
            {
                try
                {
                    lock (this)
                    {
                        if (Util.Time.NowUnixMillis - LastErrorTime < ForbidPeriod)
                            return false;
                    }
                    AsyncSocket peer = GlobalInstance.Rocks.Raft.Server.GetSocket(SessionId);
                    if (null != peer)
                    {
                        var reduce = new Reduce();
                        reduce.Argument.GlobalTableKey = gkey;
                        reduce.Argument.State = state;
                        reduce.Argument.GlobalSerialId = globalSerialId;
                        if (reduce.Send(peer, response, 10000))
                            return true;
                    }
                }
                catch (Exception ex)
                {
                    // ������쳣ֻӦ�������緢���쳣��
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
                    long now = Util.Time.NowUnixMillis;
                    if (now - LastErrorTime > ForbidPeriod)
                        LastErrorTime = now;
                }
            }
            /// <summary>
            /// ����null��ʾ������������󣬻���Ӧ�÷������Ѿ��رա�
            /// </summary>
            /// <param name="gkey"></param>
            /// <param name="state"></param>
            /// <returns></returns>
            public Reduce ReduceWaitLater(GlobalTableKey gkey, int state, long globalSerialId)
            {
                try
                {
                    lock (this)
                    {
                        if (Util.Time.NowUnixMillis - LastErrorTime < ForbidPeriod)
                            return null;
                    }
                    AsyncSocket peer = GlobalInstance.Rocks.Raft.Server.GetSocket(SessionId);
                    if (null != peer)
                    {
                        var reduce = new Reduce();
                        reduce.Argument.GlobalTableKey = gkey;
                        reduce.Argument.State = state;
                        reduce.Argument.GlobalSerialId = globalSerialId;
                        reduce.SendForWait(peer, 10000);
                        return reduce;
                    }
                }
                catch (Exception ex)
                {
                    // ������쳣ֻӦ�������緢���쳣��
                    logger.Error(ex, "ReduceWaitLater Exception {0}", gkey);
                }
                SetError();
                return null;
            }

            public override string ToString()
            {
                return $"{SessionId}@{ServerId}";
            }
        }
    }
}
