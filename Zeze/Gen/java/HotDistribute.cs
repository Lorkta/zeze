
using DotNext.Collections.Generic;
using System;
using System.Collections.Generic;
using System.IO;
using Zeze.Builtin.HotDistribute;
using Zeze.Gen.Types;
using Zeze.Util;

namespace Zeze.Gen.java
{
    public class HotDistribute : AbstractHotDistribute
    {
        private ClientService Client { get; } = new ClientService();
        private Net.Connector Connector { get; }
        public Project Project { get; }
        public string BaseDir { get; }

        public HotDistribute(string host, int port, Project project, string baseDir)
        {
            Client.Config.TryGetOrAddConnector(host, port, false, out var connector);
            Connector = connector;

            // �ȴ�500ms������Gen����������Ӧ��Ӱ�첻��
            // �Է������ܾ���ʱ��Ӧ�ø��̡�û����֤����
            Connector.TryGetReadySocket(500);

            Project = project;
            BaseDir = baseDir;
        }

        public bool HasDistribute()
        {
            return Connector.TryGetReadySocket() != null;
        }

        // lastVars, curVars ���ǰ�var.id����ġ�
        public static (List<int>, List<int>) diff(IList<BVariable> lastVars, IList<Variable> curVars)
        {
            var add = new List<int>();
            var remove = new List<int>();

            var lastIt = lastVars.GetEnumerator();
            var curIt = curVars.GetEnumerator();

            var lastHas = lastIt.MoveNext();
            var curHas = curIt.MoveNext();
            while (lastHas && curHas)
            {
                if (lastIt.Current.Id == curIt.Current.Id)
                {
                    lastHas = lastIt.MoveNext();
                    curHas = curIt.MoveNext();
                    continue;
                }

                if (lastIt.Current.Id < curIt.Current.Id)
                {
                    remove.Add(lastIt.Current.Id);
                    lastHas = lastIt.MoveNext();
                }
                else
                {
                    add.Add(curIt.Current.Id);
                    curHas = curIt.MoveNext();
                }
            }
            while (lastHas)
            {
                remove.Add(lastIt.Current.Id);
                lastHas = lastIt.MoveNext();
            }
            while (curHas)
            {
                add.Add(curIt.Current.Id);
                curHas = curIt.MoveNext();
            }
            return (add, remove);
        }

        public class ToPrevious
        {
            public BLastVersionBeanInfo LastVersion { get; set; }
            public List<int> Add { get; set; }
            public List<int> Remove { get; set; }
        }

        public void GenBean(Bean bean)
        {
			if (HasDistribute())
			{
				var lastVersion = GetLastVersionBean(bean.FullName); // ע�⣺������Ͳ���Bean��
				var curVersion = bean; // ��ǰxml��Bean��

				if (null == lastVersion)
				{
                    new BeanFormatter(bean).Make(BaseDir, Project);
					return; // done
				}
                // ����Ƿ�����޷��ȸ�(spring-loaded)���޸ġ�
                /*
				var lastVars = lastVersion.Variables;
				var curVars = curVersion.VariablesIdOrder;
				var (add, remove) = diff(lastVars, curVars);
                */
                new BeanFormatter(bean).Make(BaseDir, Project);
			}
            else
            {
                new BeanFormatter(bean).Make(BaseDir, Project); // GenWithoutToPrevious();
            }
        }

        private BLastVersionBeanInfo GetLastVersionBean(string fullName)
        {
            var r = new GetLastVersionBeanInfo();
            r.Argument.Name = fullName;
            r.SendAsync(Connector.TryGetReadySocket()).Wait();

            switch (r.ResultCode)
            {
                case ResultCode.LogicError:
                    return null;
                case ResultCode.Success:
                    return r.Result;
                default:
                    throw new System.Exception($"GetLastVersionBean {r.ResultCode}");
            }
        }

        public class ClientService : Zeze.Net.Service
        {
            public ClientService()
                : base("Zeze.Gen.java.Distribute.ClientService", (Config)null)
            {
            }
        }
    }
}
