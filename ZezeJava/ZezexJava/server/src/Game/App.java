package Game;

import java.nio.file.Files;
import java.nio.file.Paths;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.LoadConfig;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderModuleBinds;
import Zeze.Config;
import Zeze.Game.ProviderDirectWithTransmit;
import Zeze.Game.ProviderWithOnline;
import Zeze.Game.TaskBase;
import Zeze.Net.AsyncSocket;
import Zeze.Util.JsonReader;
import Zeze.Util.PersistentAtomicLong;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.nio.file.Path;

public final class App extends Zeze.AppBase {
	private static final Logger logger = LogManager.getLogger(App.class);

	public static final App Instance = new App();

	public static App getInstance() {
		return Instance;
	}

	public ProviderWithOnline Provider;
	public ProviderApp ProviderApp;
	public ProviderDirectWithTransmit ProviderDirect;

	public ProviderWithOnline getProvider() {
		return Provider;
	}

	private static LoadConfig LoadConfig() {
		try {
			byte[] bytes = Files.readAllBytes(Paths.get("linkd.json"));
			return new JsonReader().buf(bytes).parse(LoadConfig.class);
			// return new ObjectMapper().readValue(bytes, LoadConfig.class);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return new LoadConfig();
	}

	public void Start(String[] args) throws Exception {
		int serverId = -1;
		int providerDirectPort = -1;
		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-ServerId":
				serverId = Integer.parseInt(args[++i]);
				break;
			case "-GenFileSrcRoot":
				GenModule.instance.genFileSrcRoot = args[++i];
				break;
			case "-ProviderDirectPort":
				providerDirectPort = Integer.parseInt(args[++i]);
				break;
			}
		}
		Start(serverId, providerDirectPort);
	}

	public void Start(int serverId, int providerDirectPort) throws Exception {

		var config = Config.load("server.xml");
		if (serverId != -1) {
			config.setServerId(serverId); // replace from args
		}
		var commitService = config.getServiceConf("Zeze.Dbh2.Commit");
		if (null != commitService) {
			commitService.forEachAcceptor((a) -> {
				a.setPort(a.getPort() + config.getServerId());
			});
		}
		if (providerDirectPort != -1) {
			final int port = providerDirectPort;
			config.getServiceConfMap().get("ServerDirect").forEachAcceptor((a) -> a.setPort(port));
		}
		// create
		createZeze(config);
		createService();
		Provider = new ProviderWithOnline();
		ProviderDirect = new ProviderDirectWithTransmit();
		ProviderApp = new ProviderApp(Zeze, Provider, Server,
				"Game.Server.Module#",
				ProviderDirect, ServerDirect, "Game.Linkd", LoadConfig());
		Provider.create(this);

		createModules();
		if (GenModule.instance.genFileSrcRoot != null) {
			System.out.println("---------------");
			System.out.println("New Source File Has Generate. Re-Compile Need.");
			System.exit(0);
		}
		taskModule = new TaskBase.Module(getZeze());

		// start
		Zeze.start(); // 启动数据库
		startModules(); // 启动模块，装载配置什么的。
		Provider.start();

		PersistentAtomicLong socketSessionIdGen = PersistentAtomicLong.getOrAdd("Game.Server." + config.getServerId());
		AsyncSocket.setSessionIdGenFunc(socketSessionIdGen::next);
		startService(); // 启动网络
		// 服务准备好以后才注册和订阅。
		ProviderApp.startLast(ProviderModuleBinds.load(), modules);
	}

	public void Stop() throws Exception {
		if (Provider != null)
			Provider.stop();
		stopService(); // 关闭网络
		stopModules(); // 关闭模块，卸载配置什么的。
		if (Zeze != null)
			Zeze.stop(); // 关闭数据库
		destroyModules();
		destroyServices();
		destroyZeze();
	}

	public TaskBase.Module taskModule;

	// ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.Application Zeze;
    public final java.util.HashMap<String, Zeze.IModule> modules = new java.util.HashMap<>();

    public Game.Server Server;
    public Game.ServerDirect ServerDirect;


    @Override
    public Zeze.Application getZeze() {
        return Zeze;
    }

    public void createZeze() throws Exception {
        createZeze(null);
    }

    public synchronized void createZeze(Zeze.Config config) throws Exception {
        if (Zeze != null)
            throw new IllegalStateException("Zeze Has Created!");

        Zeze = new Zeze.Application("server", config);
    }

    public synchronized void createService() {
        Server = new Game.Server(Zeze);
        ServerDirect = new Game.ServerDirect(Zeze);
    }

    public synchronized void createModules() throws Exception {
        Zeze.initialize(this);
        Zeze.setHotManager(new Zeze.Hot.HotManager(this, Zeze.getConfig().getHotWorkingDir(), Zeze.getConfig().getHotDistributeDir()));
        Zeze.getHotManager().initialize(modules);
        var _modules_ = createRedirectModules(new Class[] {
        });
        if (_modules_ == null)
            return;

        Zeze.setSchemas(new Game.Schemas());
    }

    public synchronized void destroyModules() {
        if (null != Zeze.getHotManager()) {
            Zeze.getHotManager().destroyModules();
            Zeze.setHotManager(null);
        }
        modules.clear();
    }

    public synchronized void destroyServices() {
        Server = null;
        ServerDirect = null;
    }

    public synchronized void destroyZeze() {
        Zeze = null;
    }

    public synchronized void startModules() throws Exception {
        if (null != Zeze.getHotManager()) {
            var definedOrder = new java.util.HashSet<String>();
            Zeze.getHotManager().startModulesExcept(definedOrder);
        }
    }

    public synchronized void stopModules() throws Exception {
        if (null != Zeze.getHotManager()) {
            var definedOrder = new java.util.HashSet<String>();
            Zeze.getHotManager().stopModulesExcept(definedOrder);
        }
    }

    public synchronized void startService() throws Exception {
        Server.start();
        ServerDirect.start();
    }

    public synchronized void stopService() throws Exception {
        if (Server != null)
            Server.stop();
        if (ServerDirect != null)
            ServerDirect.stop();
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
