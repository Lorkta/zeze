
namespace Game.Fight
{
    public partial class ModuleFight : AbstractModule
    {
        public void Start(global::Zeze.App app)
        {
        }

        public void Stop(global::Zeze.App app)
        {
        }

        protected override System.Threading.Tasks.Task<long> ProcessAreYouFightRequest(Zeze.Net.Protocol _p)
        {
            // ����Zezex���Է����������һ����������ͻ��˷����rpc����
            // Ϊ�˲�Ӱ��Zezex�������Լ������ĵ�Ԫ���ԣ�������֧��һ�¡�
            // ��WorldDemo������Ҫ���ģ�顿
            var p = _p as AreYouFight;
            p.SendResult();
            return Task.FromResult<long>(0);
        }

    }
}
