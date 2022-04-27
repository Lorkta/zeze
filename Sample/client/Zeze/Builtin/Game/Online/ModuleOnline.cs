
using System.Collections.Concurrent;
using System.Threading.Tasks;

namespace Zeze.Builtin.Game.Online
{
    public interface IReliableNotify
    {
        public Task OnReliableNotify(Zeze.Net.Protocol p);
    }

    public partial class ModuleOnline : AbstractModule
    {
        public void Start(global::Client.App app)
        {
        }

        public void Stop(global::Client.App app)
        {
        }

        // û����ȫ�����߳�����
        private long ReliableNotifyTotalCount;
        private ConcurrentDictionary<long, IReliableNotify> ReliableNotifyMap { get; } = new();

        public void RegisterReliableNotify(long protocolTypeId, IReliableNotify handle)
        {
            if (false == ReliableNotifyMap.TryAdd(protocolTypeId, handle))
                throw new System.Exception($"Duplicate Protocol({Zeze.Net.Protocol.GetModuleId(protocolTypeId)}, {Zeze.Net.Protocol.GetProtocolId(protocolTypeId)})");
        }

        protected override async System.Threading.Tasks.Task<long> ProcessSReliableNotify(Zeze.Net.Protocol _p)
        {
            var protocol = _p as SReliableNotify;
            // TODO
            ReliableNotifyTotalCount += protocol.Argument.Notifies.Count;

            foreach (var notify in protocol.Argument.Notifies)
            {
                try
                {
                    var bb = Zeze.Serialize.ByteBuffer.Wrap(notify);
                    int typeId = bb.ReadInt4();
                    if (ReliableNotifyMap.TryGetValue(typeId, out var handle))
                    {
                        int size = bb.ReadInt4();
                        // ���е�notify���붨���˿ͻ��˴���
                        var factoryHandle = global::Client.App.Instance.ClientService.FindProtocolFactoryHandle(typeId);
                        var pNotify = factoryHandle.Factory();
                        pNotify.Decode(bb);
                        await handle.OnReliableNotify(pNotify);
                    }
                }
                catch (System.Exception)
                {
                    // TODO handle error here.
                }
            }

            // ����ȷ�ϡ����Կ��Ǵﵽһ���������߶�ʱ��
            var confirm = new ReliableNotifyConfirm();
            confirm.Argument.ReliableNotifyConfirmCount = ReliableNotifyTotalCount;
            protocol.Sender.Send(confirm);
            // process rpc result
            // TODO ͬ������ȷ��ʧ�ܣ�Ӧ�����¿�ʼһ����ɵĵ�¼���̡�

            return Zeze.Transaction.Procedure.Success;
        }

    }
}
