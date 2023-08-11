
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

        public static int distanceVersion(Bean curVersion, BLastVersionBeanInfo lastVersion)
        {
            return Bean.GetVersion(curVersion.Name) - Bean.GetVersion(lastVersion.Name);
        }

        public class ToPrevious
        {
            public BLastVersionBeanInfo LastVersion { get; set; }
            public List<int> Add { get; set; }
            public List<int> Remove { get; set; }

            public void Gen(Bean bean, StreamWriter sw, string prefix)
            {
                sw.WriteLine($"{prefix}@Override");
                sw.WriteLine($"{prefix}public Zeze.Transaction.Bean toPrevious() {{");
                sw.WriteLine($"{prefix}    var previous = new {LastVersion.Name}();");
                foreach (var var in LastVersion.Variables)
                {
                    if (Remove.Contains(var.Id))
                    {
                        sw.WriteLine($"{prefix}    // remove={var.Id}:{var.Name}");
                    }
                    else
                    {
                        sw.WriteLine($"{prefix}    previous.set{Program.Upper1(var.Name)}({Program.Upper1(bean.GetVariable(var.Id).Name)});");
                    }
                }
                foreach (var id in Add)
                {
                    var var = bean.GetVariable(id);
                    sw.WriteLine($"{prefix}    // add={var.Id}:{var.Name}");
                }
                sw.WriteLine($"{prefix}    return previous;");
                sw.WriteLine($"{prefix}}}");
            }
        }

        public void GenBean(Bean bean)
        {
			if (HasDistribute())
			{
				var lastVersion = GetLastVersionBean(bean.FullName); // ע�⣺������Ͳ���Bean��
				var curVersion = bean; // ��ǰxml��Bean��

				if (null == lastVersion)
				{
					if (Bean.GetVersion(curVersion.Name) > 0) // curVersion.Name is versioned
						throw new Exception("First Version Bean With Versioned Name."); // ��һ���汾�����ð汾��ʽ������
                    new BeanFormatter(bean, null).Make(BaseDir, Project); // GenWithoutToPrevious();
					return; // done
				}

				var lastVars = lastVersion.Variables;
				var curVars = curVersion.VariablesIdOrder;
				var (add, remove) = diff(lastVars, curVars);
				var versionDistance = distanceVersion(curVersion, lastVersion); // �汾�Ų��졣
				if (versionDistance > 1 || versionDistance < 0)
					throw new Exception("version distance > 1 or < 0");

                // ������߾��漴�ɡ�
                if (add.Count == 0 && remove.Count == 0 && versionDistance > 0)
					throw new Exception("var no change, but bean version changed.");

				if ((add.Count > 0 || remove.Count > 0) && (versionDistance == 0))
					throw new Exception("var change, but bean version not change.");

                // GenWithToPrevious(add, remove); ������ܰ���add,remove���ǿյģ��������ɾɰ���ݡ�
                var toPrevious = new ToPrevious()
                {
                    LastVersion = lastVersion,
                    Add = add,
                    Remove = remove
                };
                new BeanFormatter(bean, toPrevious).Make(BaseDir, Project);
			}
            else
            {
                var curVersion = bean; // ��ǰxml��Bean��
                if (Bean.GetVersion(curVersion.Name) > 0) // curVersion.Name is versioned)
                    throw new Exception("no distribute, but bean name is versioned."); // �����ڼ䣬�����ð汾��ʽ������
                new BeanFormatter(bean, null).Make(BaseDir, Project); // GenWithoutToPrevious();
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
