
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Arch;
using Zeze.Builtin.Game.Online;
using Zeze.Builtin.Provider;
using Zeze.Builtin.ProviderDirect;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;

namespace Zeze.Game
{
    public class Online : AbstractOnline
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public ProviderApp App { get; }

        public taccount TableAccount => _taccount;

        public Online(ProviderApp app)
        {
            this.App = app;

            RegisterProtocols(app.ProviderService);
            RegisterZezeTables(app.Zeze);
        }

        public override void UnRegister()
        {
            UnRegisterZezeTables(App.Zeze);
            UnRegisterProtocols(App.ProviderService);
        }

        public async Task<bool> AddRole(string account, long roleId)
        {
            BAccount bAccount = await _taccount.GetOrAddAsync(account);
            if (!bAccount.Name.Equals(account)) // �Ż�д����ͬ��ʱ���޸����ݡ�
                bAccount.Name = account;
            if (bAccount.Roles.Contains(roleId))
                return false;
            bAccount.Roles.Add(roleId);
            return true;
        }

        public async Task RemoveRole(String account, long roleId)
        {
            BAccount bAccount = await _taccount.GetAsync(account);
            bAccount?.Roles.Remove(roleId);
        }

        public async Task<bool> SetLastLoginRoleId(String account, long roleId)
        {
            BAccount bAccount = await _taccount.GetAsync(account);
            if (bAccount == null)
                return false;
            if (!bAccount.Roles.Contains(roleId))
                return false;
            bAccount.LastLoginRoleId = roleId;
            return true;
        }

        public async Task OnLinkBroken(long roleId, BLinkBroken arg)
        {
            var online = await _tonline.GetAsync(roleId);
            if (null == online || online.LinkSid != arg.LinkSid)
                return; // skip now owner

            // ����״̬���ӳ��Ժ�׼��ɾ����¼��
            online.State = BOnline.StateNetBroken;
            await Task.Delay(10 * 60 * 1000);

            // TryRemove
            await App.Zeze.NewProcedure(async () =>
            {
                // ����Ͽ����ӳ�ɾ������״̬��������ж�һ���Ƿ�StateNetBroken��
                // ����CLogin,CReLogin��ʱ��û��ȡ��Timeout�������п����ٴε�¼���ߺ󣬻ᱻ��һ�ζ��ߵ�Timeoutɾ����
                // ����ӳ�ʱ�䲻׼ȷ������Timeout�е㷳���������ɡ�
                var online = await _tonline.GetAsync(roleId);

                // ɾ��ǰ��飺
                // 1��State������Ͽ���StateNetBroken������ΪDelay�ڼ�����������Ӳ���¼��ReLogin����
                // 2��Owner����ǰLinkSid�ǹر����ӣ�����ͬ�ϡ�
                if (null != online && online.State == BOnline.StateNetBroken && online.LinkSid == arg.LinkSid)
                {
                    await _tonline.RemoveAsync(roleId);
                    //App.Instance.Load.LogoutCount.IncrementAndGet();
                }
                return Procedure.Success;
            }, "Onlines.OnLinkBroken").CallAsync();
        }

        public async Task AddReliableNotifyMark(long roleId, string listenerName)
        {
            var online = await _tonline.GetAsync(roleId);
            if (null == online || online.State != BOnline.StateOnline)
                throw new Exception("Not Online. AddReliableNotifyMark: " + listenerName);
            online.ReliableNotifyMark.Add(listenerName);
        }

        public async Task RemoveReliableNotifyMark(long roleId, string listenerName)
        {
            // �Ƴ�����ͨ���������κ��жϡ�
            (await _tonline.GetAsync(roleId))?.ReliableNotifyMark.Remove(listenerName);
        }

        public void SendReliableNotifyWhileCommit(
            long roleId, string listenerName, Protocol p, bool WaitConfirm = false)
        {
            Transaction.Transaction.Current.RunWhileCommit(
                () => SendReliableNotify(roleId, listenerName, p, WaitConfirm)
                );
        }

        public void SendReliableNotifyWhileCommit(
            long roleId, string listenerName, int typeId, Binary fullEncodedProtocol,
            bool WaitConfirm = false)
        {
            Transaction.Transaction.Current.RunWhileCommit(
                () => SendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol, WaitConfirm)
                );
        }

        public void SendReliableNotifyWhileRollback(
            long roleId, string listenerName, Protocol p,
            bool WaitConfirm = false)
        {
            Transaction.Transaction.Current.RunWhileRollback(
                () => SendReliableNotify(roleId, listenerName, p, WaitConfirm)
                );
        }

        public void SendReliableNotifyWhileRollback(
            long roleId, string listenerName, int typeId, Binary fullEncodedProtocol,
            bool WaitConfirm = false)
        {
            Transaction.Transaction.Current.RunWhileRollback(
                () => SendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol, WaitConfirm)
                );
        }

        public void SendReliableNotify(long roleId, string listenerName, Protocol p, bool WaitConfirm = false)
        {
            SendReliableNotify(roleId, listenerName, p.TypeId, new Binary(p.Encode()), WaitConfirm);
        }

        /// <summary>
        /// �������߿ɿ�Э�飬��������ߵȣ���Ȼ���ᷢ��Ŷ��
        /// </summary>
        /// <param name="roleId"></param>
        /// <param name="listenerName"></param>
        /// <param name="fullEncodedProtocol">Э������ȱ��룬��Ϊ�������</param>
        public void SendReliableNotify(
            long roleId, string listenerName, long typeId, Binary fullEncodedProtocol,
            bool WaitConfirm = false)
        {
            TaskCompletionSource<long> future = null;

            if (WaitConfirm)
                future = new TaskCompletionSource<long>();

            App.Zeze.TaskOneByOneByKey.Execute(
                listenerName,
                App.Zeze.NewProcedure(async () =>
                {
                    BOnline online = await _tonline.GetAsync(roleId);
                    if (null == online || online.State == BOnline.StateOffline)
                    {
                        return Procedure.Success;
                    }
                    if (false == online.ReliableNotifyMark.Contains(listenerName))
                    {
                        return Procedure.Success; // �������װ�ص�ʱ��Ҫͬ�����������
                    }

                    // �ȱ������ٷ��ͣ�Ȼ��ͻ��˻���ȷ�ϡ�
                    // see Game.Login.Module: CLogin CReLogin CReliableNotifyConfirm ��ʵ�֡�
                    online.ReliableNotifyQueue.Add(fullEncodedProtocol);
                    if (online.State == BOnline.StateOnline)
                    {
                        var notify = new SReliableNotify(); // ��ֱ�ӷ���Э�飬����Ϊ�ͻ�����Ҫʶ��ReliableNotify�����д�����������
                        notify.Argument.ReliableNotifyTotalCountStart = online.ReliableNotifyTotalCount;
                        notify.Argument.Notifies.Add(fullEncodedProtocol);

                        await SendInProcedure(roleId, notify.TypeId, new Binary(notify.Encode()), future);
                    }
                    online.ReliableNotifyTotalCount += 1; // ��ӣ�start �� Queue.Add ֮ǰ�ġ�
                    return Procedure.Success;
                },
                "SendReliableNotify." + listenerName
                ));

            future?.Task.Wait();
        }

        public class RoleOnLink
        {
            public string LinkName { get; set; } = ""; // empty when not online
            public AsyncSocket LinkSocket { get; set; } // null if not online
            public int ProviderId { get; set; } = -1;
            public long ProviderSessionId { get; set; }
            public Dictionary<long, BTransmitContext> Roles { get; }
                = new Dictionary<long, BTransmitContext>();
        }

        public async Task<ICollection<RoleOnLink>> GroupByLink(ICollection<long> roleIds)
        {
            var groups = new Dictionary<string, RoleOnLink>();
            var groupNotOnline = new RoleOnLink(); // LinkName is Empty And Socket is null.
            groups.Add(groupNotOnline.LinkName, groupNotOnline);

            foreach (var roleId in roleIds)
            {
                var online = await _tonline.GetAsync(roleId);
                if (null == online || online.State != BOnline.StateOnline)
                {
                    groupNotOnline.Roles.TryAdd(roleId, new BTransmitContext());
                    continue;
                }

                if (false == App.ProviderService.Links.TryGetValue(online.LinkName, out var connector))
                {
                    groupNotOnline.Roles.TryAdd(roleId, new BTransmitContext());
                    continue;
                }

                if (false == connector.IsHandshakeDone)
                {
                    groupNotOnline.Roles.TryAdd(roleId, new BTransmitContext());
                    continue;
                }
                // ���汣��connector.Socket��ʹ�ã����֮�����ӱ��رգ��Ժ���Э��ʧ�ܡ�
                if (false == groups.TryGetValue(online.LinkName, out var group))
                {
                    group = new RoleOnLink()
                    {
                        LinkName = online.LinkName,
                        LinkSocket = connector.Socket,
                        ProviderId = online.ProviderId,
                        ProviderSessionId = online.ProviderSessionId,
                    };
                    groups.Add(group.LinkName, group);
                }
                group.Roles.TryAdd(roleId, new BTransmitContext()
                {
                    LinkSid = online.LinkSid,
                    ProviderId = online.ProviderId,
                    ProviderSessionId = online.ProviderSessionId,
                }); // ʹ�� TryAdd�������ظ��� roleId��
            }
            return groups.Values;
        }

        private async Task SendInProcedure(
            long roleId, long typeId, Binary fullEncodedProtocol,
            TaskCompletionSource<long> future)
        {
            // ������ϢΪ������TaskOneByOne��ֻ��һ��һ�����ͣ�Ϊ���ٸĴ��룬��ʹ�þɵ�GroupByLink�ӿڡ�
            var groups = await GroupByLink(new List<long> { roleId });
            long serialId = 0;
            if (null != future)
            {
                var confrmContext = new ConfirmContext(App, future);
                // ��������������ǰȫ�����룬����Ҫ�Ƿ�������ܿ췵�أ�
                // �����첽���⣺�������Ϊ���� Confirm ���յ���
                foreach (var group in groups)
                {
                    if (group.LinkSocket == null)
                        continue; // skip not online

                    confrmContext.LinkNames.Add(group.LinkName);
                }
                serialId = App.ProviderService.AddManualContextWithTimeout(confrmContext, 5000);
            }

            foreach (var group in groups)
            {
                if (group.LinkSocket == null)
                    continue; // skip not online

                var send = new Send();
                send.Argument.ProtocolType = typeId;
                send.Argument.ProtocolWholeData = fullEncodedProtocol;
                send.Argument.ConfirmSerialId = serialId;

                foreach (var ctx in group.Roles.Values)
                {
                    send.Argument.LinkSids.Add(ctx.LinkSid);
                }
                group.LinkSocket.Send(send);
            }
        }

        private void Send(
            long roleId, long typeId, Binary fullEncodedProtocol,
            bool WaitConfirm)
        {
            TaskCompletionSource<long> future = null;

            if (WaitConfirm)
                future = new TaskCompletionSource<long>();

            // ����Э�������������������ִ�С�
            App.Zeze.TaskOneByOneByKey.Execute(roleId, () =>
                App.Zeze.NewProcedure(async () =>
                {
                    await SendInProcedure(roleId, typeId, fullEncodedProtocol, future);
                    return Procedure.Success;
                }, "Onlines.Send"));

            future?.Task.Wait();
        }

        public void Send(long roleId, Protocol p, bool WaitConfirm = false)
        {
            Send(roleId, p.TypeId, new Binary(p.Encode()), WaitConfirm);
        }

        public void Send(ICollection<long> roleIds, Protocol p)
        {
            foreach (var roleId in roleIds)
                Send(roleId, p.TypeId, new Binary(p.Encode()), false);
        }

        public void SendWhileCommit(long roleId, Protocol p, bool WaitConfirm = false)
        {
            Transaction.Transaction.Current.RunWhileCommit(() => Send(roleId, p, WaitConfirm));
        }

        public void SendWhileCommit(ICollection<long> roleIds, Protocol p)
        {
            Transaction.Transaction.Current.RunWhileCommit(() => Send(roleIds, p));
        }

        public void SendWhileRollback(long roleId, Protocol p, bool WaitConfirm = false)
        {
            Transaction.Transaction.Current.RunWhileRollback(() => Send(roleId, p, WaitConfirm));
        }

        public void SendWhileRollback(ICollection<long> roleIds, Protocol p)
        {
            Transaction.Transaction.Current.RunWhileRollback(() => Send(roleIds, p));
        }

        /// <summary>
        /// Func<sender, target, result>
        /// sender: ��ѯ�����ߣ�������͸�����
        /// target: ��ѯĿ���ɫ��
        /// result: ����ֵ��int������ͨ��������̷���ֵ����
        /// </summary>
        public ConcurrentDictionary<string, Func<long, long, Serializable, Task<long>>> TransmitActions { get; } = new();

        public ConcurrentDictionary<string, Func<string, Serializable>> TransmitParameterFactorys { get; } = new();

        /// <summary>
        /// ת����ѯ�����RoleId��
        /// </summary>
        /// <param name="sender">��ѯ�����ߣ�������͸�����</param>
        /// <param name="actionName">��ѯ�����ʵ��</param>
        /// <param name="roleId">Ŀ���ɫ</param>
        public void Transmit(long sender, string actionName, long roleId, Serializable parameter = null)
        {
            Transmit(sender, actionName, new List<long>() { roleId }, parameter);
        }

        public void ProcessTransmit(long sender, string actionName, IEnumerable<long> roleIds, Serializable parameter)
        {
            if (TransmitActions.TryGetValue(actionName, out var handle))
            {
                foreach (var target in roleIds)
                {
                    App.Zeze.NewProcedure(async () => await handle(sender, target, parameter), "Game.Online.Transmit:" + actionName).Execute();
                }
            }
        }

        private async Task TransmitInProcedure(long sender, string actionName, ICollection<long> roleIds, Serializable parameter)
        {
            if (App.Zeze.Config.GlobalCacheManagerHostNameOrAddress.Length == 0)
            {
                // û������cache-sync�����ϴ�����������
                ProcessTransmit(sender, actionName, roleIds, parameter);
                return;
            }

            var groups = await GroupByLink(roleIds);
            foreach (var group in groups)
            {
                if (group.ProviderId == App.Zeze.Config.ServerId
                    || null == group.LinkSocket // ���ڲ����ߵĽ�ɫ��ֱ���ڱ������С�
                    )
                {
                    // loopback ���ǵ�ǰgs.
                    ProcessTransmit(sender, actionName, group.Roles.Keys, parameter);
                    continue;
                }

                var transmit = new Transmit();

                transmit.Argument.ActionName = actionName;
                transmit.Argument.Sender = sender;
                transmit.Argument.ServiceNamePrefix = App.ServerServiceNamePrefix;
                transmit.Argument.Roles.AddRange(group.Roles);

                if (null != parameter)
                {
                    transmit.Argument.ParameterBeanName = parameter.GetType().FullName;
                    transmit.Argument.ParameterBeanValue = new Binary(Zeze.Serialize.ByteBuffer.Encode(parameter));
                }

                group.LinkSocket.Send(transmit);
            }
        }

        public void Transmit(long sender, string actionName, ICollection<long> roleIds, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);

            // ����Э�������������������ִ�С�
            _ = App.Zeze.NewProcedure(async () =>
            {
                await TransmitInProcedure(sender, actionName, roleIds, parameter);
                return Procedure.Success;
            }, "Onlines.Transmit").CallAsync();
        }

        public void TransmitWhileCommit(long sender, string actionName, long roleId, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);
            Transaction.Transaction.Current.RunWhileCommit(() => Transmit(sender, actionName, roleId, parameter));
        }

        public void TransmitWhileCommit(long sender, string actionName, ICollection<long> roleIds, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);
            Transaction.Transaction.Current.RunWhileCommit(() => Transmit(sender, actionName, roleIds, parameter));
        }

        public void TransmitWhileRollback(long sender, string actionName, long roleId, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);
            Transaction.Transaction.Current.RunWhileRollback(() => Transmit(sender, actionName, roleId, parameter));
        }

        public void TransmitWhileRollback(long sender, string actionName, ICollection<long> roleIds, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);
            Transaction.Transaction.Current.RunWhileRollback(() => Transmit(sender, actionName, roleIds, parameter));
        }

        public class ConfirmContext : Service.ManualContext
        {
            public HashSet<string> LinkNames { get; } = new HashSet<string>();
            public TaskCompletionSource<long> Future { get; }
            public ProviderApp App { get; }

            public ConfirmContext(ProviderApp app, TaskCompletionSource<long> future)
            {
                App = app;
                Future = future;
            }

            public override void OnRemoved()
            {
                lock (this)
                {
                    Future.SetResult(base.SessionId);
                }
            }

            public long ProcessLinkConfirm(string linkName)
            {
                lock (this)
                {
                    LinkNames.Remove(linkName);
                    if (LinkNames.Count == 0)
                    {
                        App.ProviderService.TryRemoveManualContext<ConfirmContext>(SessionId);
                    }
                    return Procedure.Success;
                }
            }
        }

        private void Broadcast(long typeId, Binary fullEncodedProtocol, int time, bool WaitConfirm)
        {
            TaskCompletionSource<long> future = null;
            long serialId = 0;
            if (WaitConfirm)
            {
                future = new TaskCompletionSource<long>();
                var confirmContext = new ConfirmContext(App, future);
                foreach (var link in App.ProviderService.Links.Values)
                {
                    if (link.Socket != null)
                        confirmContext.LinkNames.Add(link.Name);
                }
                serialId = App.ProviderService.AddManualContextWithTimeout(confirmContext, 5000);
            }

            var broadcast = new Broadcast();
            broadcast.Argument.ProtocolType = typeId;
            broadcast.Argument.ProtocolWholeData = fullEncodedProtocol;
            broadcast.Argument.ConfirmSerialId = serialId;
            broadcast.Argument.Time = time;

            foreach (var link in App.ProviderService.Links.Values)
            {
                link.Socket?.Send(broadcast);
            }

            future?.Task.Wait();
        }

        public void Broadcast(Protocol p, int time = 60 * 1000, bool WaitConfirm = false)
        {
            Broadcast(p.TypeId, new Binary(p.Encode()), time, WaitConfirm);
        }

        protected override async Task<long> ProcessLoginRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as Login;
            var session = ProviderUserSession.Get(rpc);

            BAccount account = await _taccount.GetAsync(session.Account);
            if (null == account)
                return ErrorCode(ResultCodeAccountNotExist);

            if (!account.Roles.Contains(rpc.Argument.RoleId))
                return ErrorCode(ResultCodeRoleNotExist);

            account.LastLoginRoleId = rpc.Argument.RoleId;

            BOnline online = await _tonline.GetOrAddAsync(rpc.Argument.RoleId);
            online.LinkName = session.LinkName;
            online.LinkSid = session.SessionId;
            online.State = BOnline.StateOnline;

            online.ReliableNotifyConfirmCount = 0;
            online.ReliableNotifyTotalCount = 0;
            online.ReliableNotifyMark.Clear();
            online.ReliableNotifyQueue.Clear();

            var linkSession = session.Link.UserState as ProviderService.LinkSession;
            online.ProviderId = App.Zeze.Config.ServerId;
            online.ProviderSessionId = linkSession.ProviderSessionId;

            // ���ύ���������״̬��
            // see linkd::Zezex.Provider.ModuleProvider��ProcessBroadcast
            session.SendResponseWhileCommit(rpc);
            Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new SetUserState();
                setUserState.Argument.LinkSid = session.SessionId;
                setUserState.Argument.States.Add(rpc.Argument.RoleId);
                rpc.Sender.Send(setUserState); // ֱ��ʹ��link���ӡ�
            });
            //App.Load.LoginCount.IncrementAndGet();
            return Procedure.Success;
        }

        protected override async Task<long> ProcessLogoutRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as Logout;
            var session = ProviderUserSession.Get(rpc);

            if (session.RoleId == null)
                return ErrorCode(ResultCodeNotLogin);

            await _tonline.RemoveAsync(session.RoleId.Value);

            // ������״̬���ٷ���Logout�����
            Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new SetUserState();
                setUserState.Argument.LinkSid = session.SessionId;
                rpc.Sender.Send(setUserState); // ֱ��ʹ��link���ӡ�
            });
            session.SendResponseWhileCommit(rpc);
            // �� OnLinkBroken ʱ��������ͬʱ���������쳣�������
            // App.Load.LogoutCount.IncrementAndGet();
            return Procedure.Success;
        }

        private int ReliableNotifySync(ProviderUserSession session, long ReliableNotifyConfirmCount, BOnline online, bool sync = true)
        {
            if (ReliableNotifyConfirmCount < online.ReliableNotifyConfirmCount
                || ReliableNotifyConfirmCount > online.ReliableNotifyTotalCount
                || ReliableNotifyConfirmCount - online.ReliableNotifyConfirmCount > online.ReliableNotifyQueue.Count)
            {
                return ResultCodeReliableNotifyConfirmCountOutOfRange;
            }

            int confirmCount = (int)(ReliableNotifyConfirmCount - online.ReliableNotifyConfirmCount);

            if (sync)
            {
                var notify = new SReliableNotify();
                notify.Argument.ReliableNotifyTotalCountStart = ReliableNotifyConfirmCount;
                for (int i = confirmCount; i < online.ReliableNotifyQueue.Count; ++i)
                    notify.Argument.Notifies.Add(online.ReliableNotifyQueue[i]);
                session.SendResponseWhileCommit(notify);
            }
            online.ReliableNotifyQueue.RemoveRange(0, confirmCount);
            online.ReliableNotifyConfirmCount = ReliableNotifyConfirmCount;
            return ResultCodeSuccess;
        }

        protected override async Task<long> ProcessReliableNotifyConfirmRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as ReliableNotifyConfirm;
            var session = ProviderUserSession.Get(rpc);

            BOnline online = await _tonline.GetAsync(session.RoleId.Value);
            if (null == online || online.State == BOnline.StateOffline)
                return ErrorCode(ResultCodeOnlineDataNotFound);

            session.SendResponseWhileCommit(rpc); // ͬ��ǰ�ύ��
            var syncResultCode = ReliableNotifySync(
                session,
                rpc.Argument.ReliableNotifyConfirmCount,
                online,
                false);

            if (ResultCodeSuccess != syncResultCode)
                return ErrorCode((ushort)syncResultCode);

            return Procedure.Success;
        }

        protected override async Task<long> ProcessReLoginRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as ReLogin;
            var session = ProviderUserSession.Get(rpc);

            BAccount account = await _taccount.GetAsync(session.Account);
            if (null == account)
                return ErrorCode(ResultCodeAccountNotExist);

            if (account.LastLoginRoleId != rpc.Argument.RoleId)
                return ErrorCode(ResultCodeNotLastLoginRoleId);

            if (!account.Roles.Contains(rpc.Argument.RoleId))
                return ErrorCode(ResultCodeRoleNotExist);

            BOnline online = await _tonline.GetAsync(rpc.Argument.RoleId);
            if (null == online)
                return ErrorCode(ResultCodeOnlineDataNotFound);

            online.LinkName = session.LinkName;
            online.LinkSid = session.SessionId;
            online.State = BOnline.StateOnline;

            // �ȷ�������ٷ���ͬ�����ݣ�ReliableNotifySync����
            // ��ʹ�� WhileCommit������ɹ������ύ��˳���ͣ�ʧ��ȫ�����ᷢ�͡�
            session.SendResponseWhileCommit(rpc);
            Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new SetUserState();
                setUserState.Argument.LinkSid = session.SessionId;
                setUserState.Argument.States.Add(rpc.Argument.RoleId);
                rpc.Sender.Send(setUserState); // ֱ��ʹ��link���ӡ�
            });

            var syncResultCode = ReliableNotifySync(
                session, rpc.Argument.ReliableNotifyConfirmCount,
                online);

            if (syncResultCode != ResultCodeSuccess)
                return ErrorCode((ushort)syncResultCode);

            //App.Load.LoginCount.IncrementAndGet();
            return Procedure.Success;
        }

    }
}
