
using Zeze.Net;

namespace Zeze.World
{
    /// <summary>
    /// ���ģ�����������ͼ�����Ҫ�ľ�̬�󶨵�Э�顣
    /// ����ֻ�з����������SwitchWorldЭ�飬���ڷ��ʹ���д��������������Ǹ�ģ���޹أ�
    /// ���Э��ķ��ʹ���д��Worldģ�����ˡ�
    /// �Ժ󣬵���Ҫ���ӿͻ��˴����Э��ʱ��������ڻ������ģ�����档
    /// </summary>
    public class WorldStatic : AbstractWorldStatic
    {
        public WorldStatic(Service service)
        {
            RegisterProtocols(service);
        }
    }
}
