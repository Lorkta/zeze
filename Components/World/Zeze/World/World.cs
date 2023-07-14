
using Zeze.Serialize;
using Zeze.Builtin.World.Static;
using Zeze.Util;
using Zeze.Net;
using Zeze.Builtin.World;
using System.Collections.Concurrent;

namespace Zeze.World
{
    public class World : AbstractWorld, ICommand
    {
        public static BeanFactory<Util.ConfBean> BeanFactory { get; } = new();
        public Application Zeze { get; }
        public Service Service { get; }

        public WorldStatic WorldStatic { get; }
        public Map Map { get; private set; }

        private ConcurrentDictionary<int, ICommand> Commands = new ConcurrentDictionary<int, ICommand>();

        public static long GetSpecialTypeIdFromBean(ConfBean bean)
        {
            return bean.TypeId;
        }

        public static ConfBean CreateBeanFromSpecialTypeId(long typeId)
        {
            return BeanFactory.Create(typeId);
        }

        protected override Task<long> ProcessCommand(Zeze.Net.Protocol _p)
        {
            var p = _p as Command;
            if (!Commands.TryGetValue(p.Argument.CommandId, out var command))
                return Task.FromResult<long>(GetErrorCode(eCommandHandlerMissing));
            return command.Handle(p.Argument);
        }

        /// <summary>
        /// Application����������Config��
        /// Service������������������Э�飻
        /// </summary>
        /// <param name="zeze"></param>
        /// <param name="service"></param>
        public World(Application zeze, Service service)
        {
            Zeze = zeze;
            Service = service;

            WorldStatic = new WorldStatic(service); // Ŀǰ��������ע��Э�顣

            RegisterProtocols(service);
            Register(BCommand.eEnterWorld, this); // ���������Լ�����
        }

        public void Register(int commandId, ICommand command)
        {
            if (false == Commands.TryAdd(commandId, command))
                throw new Exception($"Duplicate Command={commandId}");
        }

        /// <summary>
        /// �л���ͼ.
        /// 
        /// һ��mapId�Ȳ���Ӧ���Ƿ������������ͻ��˲�������ָ����
        /// ��������ͻ������⴫�͡�
        /// �����ȶ����������
        /// ������������⴫�ͣ����������к��Բ�����
        /// </summary>
        /// <param name="mapId"></param>
        /// <param name="position"></param>
        /// <param name="direcet"></param>
        /// <returns></returns>
        /// <exception cref="Exception"></exception>
        public async Task<long> SwitchWorld(int mapId, Vector3 position, Vector3 direcet)
        {
            var r = new SwitchWorld();
            r.Argument.MapId = mapId;
            r.Argument.Position = position;
            r.Argument.Direct = direcet;

            await r.SendAsync(Service.GetSocket());
            if (r.ResultCode != ResultCode.Success)
                throw new Exception($"SwitchWorld Error={GetErrorCode(r.ResultCode)}");

            return r.Result.MapInstanceId;
        }

        public Task<long> Handle(BCommand c)
        {
            switch (c.CommandId)
            {
                case BCommand.eEnterWorld:
                    var enter = new BEnterWorld();
                    var bb = ByteBuffer.Wrap(c.Param);
                    enter.Decode(bb);

                    if (null != Map)
                        Console.WriteLine("Trigger Unload Map");

                    Map = new Map(this, enter.MapInstanceId);
                    Map.ProcessEnter(enter.PriorityData);

                    Console.WriteLine("Trigger Load Map");

                    var confirm = new BEnterConfirm();
                    confirm.MapInstanceId = enter.MapInstanceId;
                    confirm.EntityId = enter.EntityId;

                    Map.SendCommand(BCommand.eEnterConfirm, confirm);

                    break;
            }
            return Task.FromResult(0L);
        }
    }
}
