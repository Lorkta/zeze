
using System;
using System.Threading.Tasks;
using Zeze.Beans.ProviderDirect;
using Zeze.Net;
using Zeze.Transaction;

namespace Zeze.Arch
{
    public abstract class ProviderDirect : AbstractProviderDirect
    {
        public ProviderApp ProviderApp { get; set; }

        private void SendResultCode(Protocol rpc, long rc)
        {
            rpc.ResultCode = rc;
            SendResult(rpc);
        }

        private void SendResult(Protocol rpc)
        {
            rpc.IsRequest = false;
            Send(rpc.Sender, rpc);
        }

        private void Send(AsyncSocket sender, Protocol rpc)
        {
            if (sender == null)
            {
                var service = ProviderApp.ProviderDirectService;
                rpc.Dispatch(service, service.FindProtocolFactoryHandle(rpc.TypeId));
            }
            rpc.Send(sender);
        }

        protected override async Task<long> ProcessModuleRedirectRequest(Protocol p)
        {
            var rpc = p as ModuleRedirect;
            rpc.Result.ModuleId = rpc.Argument.ModuleId;
            rpc.Result.ServerId = ProviderApp.Zz.Config.ServerId;
            if (false == ProviderApp.Zz.Redirect.Handles.TryGetValue(rpc.Argument.MethodFullName, out var handle))
            {
                SendResultCode(rpc, ModuleRedirect.ResultCodeMethodFullNameNotFound);
                return Procedure.LogicError;
            }
            Binary Params = Binary.Empty;
            switch (handle.RequestTransactionLevel)
            {
                case TransactionLevel.Serializable:
                case TransactionLevel.AllowDirtyWhenAllRead:
                    await ProviderApp.Zz.NewProcedure(async () =>
                    {
                        Params = handle.RequestHandle(rpc.SessionId, rpc.Argument.HashCode, rpc.Argument.Params);
                        return 0;
                    }, "ProcessModuleRedirectRequest").CallAsync();
                    break;
                default:
                    Params = handle.RequestHandle(rpc.SessionId, rpc.Argument.HashCode, rpc.Argument.Params);
                    break;
            }
            rpc.Result.Params = Params;

            // rpc �ɹ��ˣ�����handle�������Ҫ��ReturnCode��
            SendResultCode(rpc, ModuleRedirect.ResultCodeSuccess);
            return 0;
        }

        private void SendResultIfSizeExceed(Zeze.Net.AsyncSocket sender, ModuleRedirectAllResult result)
        {
            int size = 0;
            foreach (var hashResult in result.Argument.Hashs.Values)
            {
                size += hashResult.Params.Count;
            }
            if (size > 2 * 1024 * 1024) // 2M
            {
                Send(sender, result);
                result.Argument.Hashs.Clear();
            }
        }

        protected override async Task<long> ProcessModuleRedirectAllRequest(Protocol p)
        {
            var r = p as ModuleRedirectAllRequest;
            var result = new ModuleRedirectAllResult();
            // common parameters for result
            result.Argument.ModuleId = r.Argument.ModuleId;
            result.Argument.ServerId = ProviderApp.Zz.Config.ServerId;
            result.Argument.SourceProvider = r.Argument.SourceProvider;
            result.Argument.SessionId = r.Argument.SessionId;
            result.Argument.MethodFullName = r.Argument.MethodFullName;

            if (false == ProviderApp.Zz.Redirect.Handles.TryGetValue(
                r.Argument.MethodFullName, out var handle))
            {
                result.ResultCode = ModuleRedirect.ResultCodeMethodFullNameNotFound;
                // ʧ���ˣ���Ҫ��hash���ء���ʱ��û�д������ġ�
                foreach (var hash in r.Argument.HashCodes)
                {
                    result.Argument.Hashs.Add(hash, new BModuleRedirectAllHash()
                    {
                        ReturnCode = Procedure.NotImplement
                    });
                }
                Send(p.Sender, result);
                return Procedure.LogicError;
            }
            result.ResultCode = ModuleRedirect.ResultCodeSuccess;

            foreach (var hash in r.Argument.HashCodes)
            {
                // Ƕ�״洢���̣�ĳ�����鴦��ʧ�ܲ�Ӱ���������顣
                var hashResult = new BModuleRedirectAllHash();
                Binary Params = Binary.Empty;
                switch (handle.RequestTransactionLevel)
                {
                    case TransactionLevel.Serializable:
                    case TransactionLevel.AllowDirtyWhenAllRead:
                        await ProviderApp.Zz.NewProcedure(async () =>
                        {
                            Params = handle.RequestHandle(r.Argument.SessionId, hash, r.Argument.Params);
                            return 0;
                        }, "ProcessModuleRedirectAllRequest").CallAsync();
                        break;

                    default:
                        Params = handle.RequestHandle(r.Argument.SessionId, hash, r.Argument.Params);
                        break;
                }

                // �������鴦��ʧ�ܼ���ִ�С�XXX
                hashResult.Params = Params;
                hashResult.ReturnCode = 0;
                result.Argument.Hashs.Add(hash, hashResult);
                SendResultIfSizeExceed(p.Sender, result);
            }

            // send remain
            if (result.Argument.Hashs.Count > 0)
            {
                Send(p.Sender, result);
            }
            return Procedure.Success;
        }

        protected override async Task<long> ProcessAnnounceProviderInfoRequest(Zeze.Net.Protocol _p)
        {
            var r = _p as AnnounceProviderInfo;
            ProviderApp.ProviderDirectService.SetRelativeServiceReady(
                (ProviderSession)r.Sender.UserState, r.Argument.Ip, r.Argument.Port);
            return 0;
        }

        protected override async Task<long> ProcessModuleRedirectAllResult(Zeze.Net.Protocol p)
        {
            var protocol = p as ModuleRedirectAllResult;
            await ProviderApp.ProviderDirectService.TryGetManualContext<ModuleRedirectAllContext>(
                protocol.Argument.SessionId)?.ProcessResult(ProviderApp.Zz, protocol);
            return Procedure.Success;
        }
    }
}
