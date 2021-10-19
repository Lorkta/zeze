package Zeze;

import Zeze.Transaction.*;
import Zeze.Util.TaskCompletionSource;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Zeze.Services.ServiceManager.Agent;

public final class Application {
	private static final Logger logger = LogManager.getLogger(Application.class);

	private HashMap<String, Database> Databases = new HashMap<String, Database> ();
	public HashMap<String, Database> getDatabases() {
		return Databases;
	}

	private Config Conf;
	public Config getConfig() {
		return Conf;
	}
	private boolean IsStart;
	public boolean isStart() {
		return IsStart;
	}
	private void setStart(boolean value) {
		IsStart = value;
	}
	private Agent ServiceManagerAgent;
	public Agent getServiceManagerAgent() {
		return ServiceManagerAgent;
	}
	private void setServiceManagerAgent(Zeze.Services.ServiceManager.Agent value) {
		ServiceManagerAgent = value;
	}
	private GlobalAgent GlobalAgent;
	public GlobalAgent getGlobalAgent() {
		return GlobalAgent;
	}

	// 用来执行内部的一些重要任务，和系统默认 ThreadPool 分开，防止饥饿。
	ScheduledThreadPoolExecutor InternalThreadPool;

	public ScheduledThreadPoolExecutor __GetInternalThreadPoolUnsafe() {
		return InternalThreadPool;
	}

	private Checkpoint _checkpoint;
	public Checkpoint getCheckpoint() {
		return _checkpoint;
	}
	public void setCheckpoint(Checkpoint value) {
		synchronized (this) {
			if (null == value) {
				throw new NullPointerException();
			}
			if (isStart()) {
				throw new RuntimeException("Checkpoint only can setup before start.");
			}
			_checkpoint = value;
		}
	}

	private Schemas Schemas;
	public Schemas getSchemas() {
		return Schemas;
	}
	public void setSchemas(Schemas value) {
		Schemas = value;
	}
	private String SolutionName;
	public String getSolutionName() {
		return SolutionName;
	}


	public Application(String solutionName) {
		this(solutionName, null);
	}

	public Application(String solutionName, Config config) {
		SolutionName = solutionName;

		Conf = config;
		if (null == Conf) {
			Conf = Config.Load(null);
		}
		InternalThreadPool = new ScheduledThreadPoolExecutor(getConfig().getInternalThreadPoolWorkerCount());

		getConfig().CreateDatabase(getDatabases());
		GlobalAgent = new GlobalAgent(this);
		_checkpoint = new Checkpoint(getConfig().getCheckpointMode(), getDatabases().values());
	}

	public void AddTable(String dbName, Table table) {
		GetDatabase(dbName).AddTable(table);
	}

	public void RemoveTable(String dbName, Table table) {
		GetDatabase(dbName).RemoveTable(table);
	}

	public Table GetTable(String name) {
		for (Database db : getDatabases().values()) {
			Table t = db.GetTable(name);
			if (null != t) {
				return t;
			}
		}
		return null;
	}

	public Database GetDatabase(String name) {
		var db = getDatabases().get(name);
		if (null != db) {
			return db;
		}
		throw new RuntimeException(String.format("database not exist name=%1$s", name));
	}


	public Procedure NewProcedure(Callable<Integer> action, String actionName) {
		return NewProcedure(action, actionName, null);
	}

	public Procedure NewProcedure(Callable<Integer> action, String actionName, Object userState) {
		if (isStart()) {
			return new Procedure(this, action, actionName, userState);
		}
		throw new RuntimeException("App Not Start");
	}

	public void Start() {
		synchronized (this) {
			if (getConfig() != null) {
				getConfig().ClearInUseAndIAmSureAppStopped(getDatabases());
			}
			for (var db : getDatabases().values()) {
				db.getDirectOperates().SetInUse(getConfig().getServerId(), getConfig().getGlobalCacheManagerHostNameOrAddress());
			}

			if (isStart()) {
				return;
			}
			setStart(true);
			Zeze.Util.Task.tryInitThreadPool(this, null);

			var serviceManagerConf = getConfig().GetServiceConf(Agent.DefaultServiceName);
			if (null != serviceManagerConf) {
				setServiceManagerAgent(new Agent(getConfig()));
				getServiceManagerAgent().WaitConnectorReady();
			}

			Database defaultDb = GetDatabase("");
			for (var db : getDatabases().values()) {
				db.Open(this);
			}

			if (getConfig().getGlobalCacheManagerHostNameOrAddress().length() > 0) {
				getGlobalAgent().Start(getConfig().getGlobalCacheManagerHostNameOrAddress(), getConfig().getGlobalCacheManagerPort());
			}

			getCheckpoint().Start(getConfig().getCheckpointPeriod()); // 定时模式可以和其他模式混用。

			/////////////////////////////////////////////////////
			/** Schemas Check
			*/
			getSchemas().Compile();
			var keyOfSchemas = Zeze.Serialize.ByteBuffer.Allocate();
			keyOfSchemas.WriteString("zeze.Schemas." + getConfig().getServerId());
			while (true) {
				var dataVersion = defaultDb.getDirectOperates().GetDataWithVersion(keyOfSchemas);
				long version = 0;
				if (dataVersion !=null && null != dataVersion.Data) {
					var SchemasPrevious = new Schemas();
					try {
						SchemasPrevious.Decode(dataVersion.Data);
						SchemasPrevious.Compile();
					}
					catch (RuntimeException ex) {
						SchemasPrevious = null;
						logger.error("Schemas Implement Changed?", ex);
					}
					if (false == getSchemas().IsCompatible(SchemasPrevious, getConfig())) {
						throw new RuntimeException("Database Struct Not Compatible!");
					}
					version = dataVersion.Version;
				}
				var newdata = Zeze.Serialize.ByteBuffer.Allocate();
				getSchemas().Encode(newdata);
				var versionRc = defaultDb.getDirectOperates().SaveDataWithSameVersion(keyOfSchemas, newdata, version);
				if (versionRc.getValue())
					break;
			}
		}
	}

	public void Stop() {
		synchronized (this) {
			if (getGlobalAgent() != null) {
				getGlobalAgent().Stop();
			}

			if (false == isStart()) {
				return;
			}
			if (getConfig() != null) {
				getConfig().ClearInUseAndIAmSureAppStopped(getDatabases());
			}
			setStart(false);

			if (getCheckpoint() != null) {
				getCheckpoint().StopAndJoin();
			}
			for (var db : getDatabases().values()) {
				db.Close();
			}
			getDatabases().clear();
			getServiceManagerAgent().Stop();
		}
	}

	public void CheckpointRun() {
		_checkpoint.RunOnce();
	}

	public Application() {
		Runtime.getRuntime().addShutdownHook(new Thread("zeze.ShutdownHook") {
			@Override
			public void run() {
				logger.fatal("zeze stop start ... from ShutdownHook.");
				Stop();
			}
		});
	}

	private Zeze.Util.TaskOneByOneByKey TaskOneByOneByKey = new Zeze.Util.TaskOneByOneByKey();
	public Zeze.Util.TaskOneByOneByKey getTaskOneByOneByKey() {
		return TaskOneByOneByKey;
	}


	public TaskCompletionSource<Integer> Run(Callable<Integer> func, String actionName, TransactionModes mode) {
		return Run(func, actionName, mode, null);
	}

	public TaskCompletionSource<Integer> Run(Callable<Integer> func, String actionName, TransactionModes mode, Object oneByOneKey) {
		var future = new TaskCompletionSource<Integer>();
		try {
			switch (mode) {
				case ExecuteInTheCallerTransaction:
					future.SetResult(func.call());
					break;
	
				case ExecuteInNestedCall:
					future.SetResult(NewProcedure(func, actionName).Call());
					break;
	
				case ExecuteInAnotherThread:
					if (null != oneByOneKey) {
						getTaskOneByOneByKey().Execute(oneByOneKey,
								() -> future.SetResult(NewProcedure(func, actionName).Call()),
								actionName);
					}
					else {
						Zeze.Util.Task.Run(
								() -> future.SetResult(NewProcedure(func, actionName).Call()),
								actionName);
					}
					break;
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		return future;
	}
}