package demo;

import java.util.HashMap;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.LoadConfig;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderDirect;
import Zeze.Arch.ProviderDirectService;
import Zeze.Arch.ProviderImplement;
import Zeze.Arch.ProviderModuleBinds;
import Zeze.Arch.ProviderService;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Builtin.Provider.SendConfirm;
import Zeze.Builtin.ProviderDirect.Transmit;
import Zeze.Config;
import Zeze.Game.Bag;
import Zeze.Game.Rank;
import Zeze.IModule;
import Zeze.Net.Acceptor;
import Zeze.Net.Connector;
import Zeze.Net.ServiceConf;

// 简单的无需读配置文件的App
public class SimpleApp extends AppBase {
	private final Application zeze;
	private final ProviderApp providerApp;

	public Bag.Module bag;
	public Rank rank;

	public SimpleApp(int serverId) throws Throwable {
		var config = new Config();
		var serviceConf = new ServiceConf();
		serviceConf.AddConnector(new Connector("127.0.0.1", 5001)); // 连接本地ServiceManager
		config.getServiceConfMap().put("Zeze.Services.ServiceManager.Agent", serviceConf);
		serviceConf = new ServiceConf();
		serviceConf.AddAcceptor(new Acceptor(20000 + serverId, null));
		config.getServiceConfMap().put("ProviderDirectService", serviceConf); // 提供Provider之间直连服务
		config.setGlobalCacheManagerHostNameOrAddress("127.0.0.1"); // 连接本地GlobalServer
		config.setGlobalCacheManagerPort(5555);
		config.getDatabaseConfMap().put("", new Config.DatabaseConf()); // 默认内存数据库配置
		config.setDefaultTableConf(new Config.TableConf()); // 默认的Table配置
		config.setServerId(serverId); // 设置Provider服务器ID
		zeze = new Application("SimpleApp", config);

		providerApp = new ProviderApp(zeze, new ProviderImplement() {
			@Override
			protected long ProcessLinkBroken(LinkBroken p) {
				return 0;
			}

			@Override
			protected long ProcessSendConfirm(SendConfirm p) {
				return 0;
			}
		}, new ProviderService("ProviderService", zeze), "SimpleApp#", new ProviderDirect() {
			@Override
			protected long ProcessTransmit(Transmit p) {
				return 0;
			}
		}, new ProviderDirectService("ProviderDirectService", zeze), "Game.Linkd", new LoadConfig());
	}

	@Override
	public Application getZeze() {
		return zeze;
	}

	@Override
	public <T extends IModule> T ReplaceModuleInstance(T in) {
		return zeze.Redirect.ReplaceModuleInstance(this, in);
	}

	public void start() throws Throwable {
		var modules = new HashMap<String, IModule>();

		bag = ReplaceModuleInstance(new Bag.Module(zeze));
		bag.Initialize(this);
		modules.put(bag.getFullName(), bag);

		rank = ReplaceModuleInstance(new Rank());
		rank.Initialize(this);
		modules.put(rank.getFullName(), rank);

		if (GenModule.Instance.GenFileSrcRoot != null) {
			System.out.println("---------------");
			throw new RuntimeException("New Source File Has Generate. Re-Compile Need.");
		}

		providerApp.initialize(ProviderModuleBinds.Load(""), modules);
		zeze.Start();
		providerApp.ProviderService.Start();
		providerApp.ProviderDirectService.Start();
		providerApp.StartLast();
	}

	public void stop() throws Throwable {
		providerApp.ProviderDirectService.Stop();
		providerApp.ProviderService.Stop();
		zeze.Stop();
		if (rank != null) {
			rank.UnRegister();
			rank = null;
		}
		if (bag != null) {
			bag.UnRegister();
			bag = null;
		}
	}
}
