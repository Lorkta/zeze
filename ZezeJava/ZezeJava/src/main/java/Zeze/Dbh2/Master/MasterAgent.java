package Zeze.Dbh2.Master;

import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.CreateBucket;
import Zeze.Builtin.Dbh2.Master.CreateDatabase;
import Zeze.Builtin.Dbh2.Master.CreateSplitBucket;
import Zeze.Builtin.Dbh2.Master.CreateTable;
import Zeze.Builtin.Dbh2.Master.GetBuckets;
import Zeze.Builtin.Dbh2.Master.PublishSplitBucketNew;
import Zeze.Builtin.Dbh2.Master.PublishSplitBucketOld;
import Zeze.Builtin.Dbh2.Master.Register;
import Zeze.Builtin.Dbh2.Master.ReportLoad;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.Connector;
import Zeze.Net.ProtocolHandle;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutObject;

public class MasterAgent extends AbstractMasterAgent {
	public static final String eServiceName = "Zeze.Dbh2.Master.Agent";
	private final Service service;
	private ProtocolHandle<CreateBucket> createBucketHandle;

	public MasterAgent(Config config) {
		service = new Service(config);
		RegisterProtocols(service);
	}

	public MasterAgent(Config config, ProtocolHandle<CreateBucket> handle, Service service) {
		this.service = service;
		this.createBucketHandle = handle;
		RegisterProtocols(this.service);
	}

	public void startAndWaitConnectionReady() {
		try {
			service.start();
			service.getConfig().forEachConnector(Connector::WaitReady);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void stop() {
		try {
			service.stop();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void createDatabase(String database) {
		var r = new CreateDatabase();
		r.Argument.setDatabase(database);
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("createDatabase error=" + IModule.getErrorCode(r.getResultCode()));
	}

	public boolean createTable(String database, String table, OutObject<MasterTable.Data> out) {
		var r = new CreateTable();
		r.Argument.setDatabase(database);
		r.Argument.setTable(table);
		r.SendForWait(service.GetSocket()).await();
		out.value = r.Result;
		var rc = r.getResultCode();
		if (rc != 0 && rc != errorCode(eTableIsNew))
			throw new RuntimeException("fail module=" + IModule.getModuleId(rc) + " code=" + IModule.getErrorCode(rc));
		return IModule.getErrorCode(rc) == eTableIsNew;
	}

	public MasterTable.Data getBuckets(String database, String table) {
		var r = new GetBuckets();
		r.Argument.setDatabase(database);
		r.Argument.setTable(table);
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("getBuckets error=" + IModule.getErrorCode(r.getResultCode()));
		return r.Result;
	}

	public void register(String dbh2RaftAcceptorName) {
		var r = new Register();
		r.Argument.setDbh2RaftAcceptorName(dbh2RaftAcceptorName);
		r.SendForWait(service.GetSocket()); // 这里不能等待，现在直接在网络线程中运行。
	}

	@Override
	protected long ProcessCreateBucketRequest(CreateBucket r) throws Exception {
		if (null == createBucketHandle)
			return Procedure.NotImplement;
		return createBucketHandle.handle(r);
	}

	public static class Service extends Zeze.Net.Service {
		public Service(Config config) {
			super(eServiceName, config);
		}
	}

	public void reportLoad(double load) {
		var r = new ReportLoad();
		r.Argument.setLoad(load);
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("reportLoad error=" + IModule.getErrorCode(r.getResultCode()));
	}

	public BBucketMeta.Data createSplitBucket(BBucketMeta.Data bucket) {
		var r = new CreateSplitBucket();
		r.Argument = bucket;
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("error=" + IModule.getErrorCode(r.getResultCode()));
		return r.Result;
	}

	public void publishSplitBucketNew(BBucketMeta.Data bucket) {
		var r = new PublishSplitBucketNew();
		r.Argument = bucket;
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("error=" + IModule.getErrorCode(r.getResultCode()));
	}

	public void publishSplitBucketOld(BBucketMeta.Data bucket) {
		var r = new PublishSplitBucketOld();
		r.Argument = bucket;
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("error=" + IModule.getErrorCode(r.getResultCode()));
	}
}
