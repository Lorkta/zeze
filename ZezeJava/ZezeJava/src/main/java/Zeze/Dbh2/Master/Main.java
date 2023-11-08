package Zeze.Dbh2.Master;

import Zeze.Config;
import Zeze.Util.PerfCounter;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

public class Main {
	private static final Logger logger = LogManager.getLogger(Main.class);

	private final MasterService service;
	private final Master master;

	public Master getMaster() {
		return master;
	}

	public MasterService getService() {
		return service;
	}

	public Main(String configXml) throws RocksDBException {
		var config = Config.load(configXml);
		service = new MasterService(this, config);
		master = new Master("master");
		master.RegisterProtocols(service);
	}

	public void start() throws Exception {
		service.start();
		ShutdownHook.add(this, this::stop);
	}

	public void stop() throws Exception {
		ShutdownHook.remove(this);
		service.stop();
		master.close();
	}

	public static void main(String[] args) {
		try {
			Task.tryInitThreadPool();

			var selector = 1;

			for (int i = 1; i < args.length; ++i) {
				//noinspection SwitchStatementWithTooFewBranches,EnhancedSwitchMigration
				switch (args[i]) {
				case "-selector":
					selector = Integer.parseInt(args[++i]);
					break;
				default:
					throw new RuntimeException("unknown option: " + args[i]);
				}
			}

			Zeze.Net.Selectors.getInstance().add(selector - 1);
			PerfCounter.instance.tryStartScheduledLog();

			var main = new Main(args[0]);
			main.start();

			synchronized (Thread.currentThread()) {
				Thread.currentThread().wait();
			}
		} catch (Exception e) {
			logger.error("", e);
		}
	}
}
