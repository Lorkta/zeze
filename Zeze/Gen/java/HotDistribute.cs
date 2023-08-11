
namespace Zeze.Gen.java
{
    public class HotDistribute : AbstractHotDistribute
    {
        private ClientService Client { get; } = new ClientService();
        private Net.Connector Connector { get; }

        public HotDistribute(string host, int port)
        {
            Client.Config.TryGetOrAddConnector(host, port, false, out var connector);
            Connector = connector;

            // �ȴ�500ms������Gen����������Ӧ��Ӱ�첻��
            Connector.TryGetReadySocket(500);
        }

        public bool HasDistribute()
        {
            return Connector.TryGetReadySocket() != null;
        }

        public void GenBean(Bean bean)
        {
            /*
			if (HasDistribute())
			{
				var lastVersion = GetLastVersionBean(bean.FullName);
				var curVersion = bean; // ��ǰxml��Bean��

				if (null == lastVersion)
				{
					if (curVersion.Name is versioned)
						throw new Exception(); // ��һ���汾�����ð汾��ʽ������
					GenWithoutToPrevious();
					return; // done
				}
				var lastVars = lastVersion.Variables();
				var curVars = curVersion.Variables();
				var (add, remove) = diff(lastVars, curVars);
				var versionDistance = distanceVersion(lastVersion, curVersion); // �汾�Ų��졣
				if (versionDistance > 1)
					throw new Exception("version distance > 1");
				if (add.empty() && remove.empty() && versionDistance > 0)
					throw new Exception("var no change, but bean version changed."); // ������߾��漴�ɡ�

				if (versionDistance == 0)
					throw new Exception("var change, but bean version not change.");
				GenWithToPrevious(add, remove); // �������add,remove���ǿյģ��������ɾɰ���ݡ�

				return; // done
			}
			var curVersion = Gen.GetBean(); // ��ǰxml��Bean��
			if (curVersion.Name is versioned)
				throw new Exception(); // �����ڼ䣬�����ð汾��ʽ������
			GenWithoutToPrevious();
			*/
        }

        private void GetLastVersionBean(string fullName)
        {

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
