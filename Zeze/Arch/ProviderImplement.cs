
using System;
using System.Threading.Tasks;
using Zeze.Builtin.Provider;
using Zeze.Net;
using Zeze.Services.ServiceManager;
using System.Collections.Generic;
using Zeze.Util;

namespace Zeze.Arch
{
    public abstract class ProviderImplement : AbstractProviderImplement
    {
        public ProviderApp ProviderApp { get; set; }

        /**
         * ע������֧�ֵ�ģ�����
         * ������̬��̬��
         * ע���ģ��ʱ��������Provider֮�����ӵ�ip��port��
         * <p>
         * ����Linkd����
         * Provider��������Linkd��
         */
        public async Task RegisterModulesAndSubscribeLinkd()
        {
            var sm = ProviderApp.Zeze.ServiceManager;
            var services = new Dictionary<string, BModule>();
            var appVersion = 0; // ProviderApp.Zeze.Schemas.AppVersion;
            var edit = new BEditService();
            // ע�᱾provider�ľ�̬����
            foreach (var it in ProviderApp.StaticBinds)
            {
                var name = $"{ProviderApp.ServerServiceNamePrefix}{it.Key}";
                var identity = ProviderApp.Zeze.Config.ServerId.ToString();
                edit.Add.Add(new ServiceInfo(name, identity, appVersion, ProviderApp.DirectIp, ProviderApp.DirectPort));
                services.Add(name, it.Value);
            }
            // ע�᱾provider�Ķ�̬����
            foreach (var it in ProviderApp.DynamicModules)
            {
                var name = $"{ProviderApp.ServerServiceNamePrefix}{it.Key}";
                var identity = ProviderApp.Zeze.Config.ServerId.ToString();
                edit.Add.Add(new ServiceInfo(name, identity, appVersion, ProviderApp.DirectIp, ProviderApp.DirectPort));
                services.Add(name, it.Value);
            }
            await sm.EditService(edit);

            var sub = new SubscribeArgument();
            // ����providerֱ�����ַ���
            foreach (var e in services)
                sub.Subs.Add(new SubscribeInfo(e.Key, appVersion));
            // ����linkd���ַ���
            sub.Subs.Add(new SubscribeInfo(ProviderApp.LinkdServiceName, 0)); // link without version

            await sm.SubscribeServices(sub);
        }


        public static void SendKick(AsyncSocket sender, long linkSid, int code, string desc)
        {
            var p = new Kick();
            p.Argument.Linksid = linkSid;
            p.Argument.Code = code;
            p.Argument.Desc = desc;
            p.Send(sender);
        }

        protected override async Task<long> ProcessDispatch(Protocol _p)
        {
            var p = _p as Dispatch;
            try
            {
                var factoryHandle = ProviderApp.ProviderService.FindProtocolFactoryHandle(p.Argument.ProtocolType);
                if (null == factoryHandle)
                {
                    SendKick(p.Sender, p.Argument.LinkSid, BKick.ErrorProtocolUnknown, "unknown protocol");
                    return ResultCode.LogicError;
                }
                var p2 = factoryHandle.Factory();
                p2.Service = p.Service;
                p2.Decode(Zeze.Serialize.ByteBuffer.Wrap(p.Argument.ProtocolData));
                p2.Sender = p.Sender;

                var session = new ProviderUserSession(
                    ProviderApp.ProviderService,
                    p.Argument.Account,
                    p.Argument.Context,
                    p.Sender,
                    p.Argument.LinkSid);

                p2.UserState = session;
                if (Transaction.Transaction.Current != null)
                {
                    // �Ѿ��������У�Ƕ��ִ�С���ʱ����p2��NoProcedure���á�
                    Transaction.Transaction.Current.TopProcedure.ActionName = p2.GetType().FullName;
                    Transaction.Transaction.Current.UserState = session;
                    return await Zeze.Util.Mission.CallAsync(
                        factoryHandle.Handle,
                        p2,
                        (p, code) => { p.ResultCode = code; session.SendResponse(p); });
                }

                if (p2.Sender.Service.Zeze == null || factoryHandle.NoProcedure)
                {
                    // Ӧ�ÿ�ܲ�֧���������Э�������ˡ�����Ҫ����
                    return await Zeze.Util.Mission.CallAsync(
                        factoryHandle.Handle,
                        p2,
                        (p, code) => { p.ResultCode = code; session.SendResponse(p); });
                }

                // �����洢���̲����ڵ�ǰ�߳��е��á�
                return await Zeze.Util.Mission.CallAsync(
                    p2.Sender.Service.Zeze.NewProcedure(
                        () => factoryHandle.Handle(p2),
                        p2.GetType().FullName,
                        factoryHandle.TransactionLevel,
                        session),
                    p2,
                    (p, code) => { p.ResultCode = code; session.SendResponse(p); }
                    );
            }
            catch (Exception ex)
            {
                SendKick(p.Sender, p.Argument.LinkSid, BKick.ErrorProtocolException, ex.ToString());
                throw;
            }
        }

        protected override Task<long> ProcessAnnounceLinkInfo(Zeze.Net.Protocol _p)
        {
            // reserve
            /*
            var protocol = _p as AnnounceLinkInfo;
            var linkSession = protocol.Sender.UserState as ProviderService.LinkSession;
            */
            return Task.FromResult(ResultCode.Success);
        }
    }
}
