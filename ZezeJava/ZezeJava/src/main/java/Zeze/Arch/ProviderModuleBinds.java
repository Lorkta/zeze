package Zeze.Arch;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import Zeze.Builtin.Provider.BModule;
import Zeze.IModule;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Util.IntHashMap;
import Zeze.Util.IntHashSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class ProviderModuleBinds {
	public static ProviderModuleBinds load() {
		return load(null);
	}

	public static ProviderModuleBinds load(String xmlFile) {
		if (xmlFile == null)
			xmlFile = "provider.module.binds.xml";

		if (Files.isRegularFile(Paths.get(xmlFile))) {
			DocumentBuilderFactory db = DocumentBuilderFactory.newInstance();
			db.setXIncludeAware(true);
			db.setNamespaceAware(true);
			try {
				Document doc = db.newDocumentBuilder().parse(xmlFile);
				return new ProviderModuleBinds(doc.getDocumentElement());
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		return new ProviderModuleBinds();
	}

	public static class Module {
		private final String fullName;
		private final int choiceType;
		private final int subscribeType;
		private final int configType; // 为了兼容，没有配置的话，从其他条件推导出来。
		private final IntHashSet providers = new IntHashSet();

		public final String getFullName() {
			return fullName;
		}

		public final int getChoiceType() {
			return choiceType;
		}

		public final int getSubscribeType() {
			return subscribeType;
		}

		public final int getConfigType() {
			return configType;
		}

		public final IntHashSet getProviders() {
			return providers;
		}

		private static int getChoiceType(Element self) {
			switch (self.getAttribute("ChoiceType")) {
			case "ChoiceTypeHashAccount":
				return BModule.ChoiceTypeHashAccount;

			case "ChoiceTypeHashRoleId":
				return BModule.ChoiceTypeHashRoleId;

			default:
				return BModule.ChoiceTypeDefault;
			}
		}

		// 这个订阅类型目前用于动态绑定的模块，所以默认为SubscribeTypeSimple。
		private static int getSubscribeType(Element self) {
			//noinspection SwitchStatementWithTooFewBranches
			switch (self.getAttribute("SubscribeType")) {
			case "SubscribeTypeReadyCommit":
				return BSubscribeInfo.SubscribeTypeReadyCommit;
			//case "SubscribeTypeSimple":
			//	return SubscribeInfo.SubscribeTypeSimple;
			default:
				return BSubscribeInfo.SubscribeTypeSimple;
			}
		}

		public Module(Element self) {
			fullName = self.getAttribute("name");
			choiceType = getChoiceType(self);
			subscribeType = getSubscribeType(self);

			ProviderModuleBinds.splitIntoSet(self.getAttribute("providers"), providers);

			String attr = self.getAttribute("ConfigType").trim();
			switch (attr) {
			case "":
				// 兼容，如果没有配置
				configType = providers.isEmpty() ? BModule.ConfigTypeDynamic : BModule.ConfigTypeSpecial;
				break;

			case "Special":
				configType = BModule.ConfigTypeSpecial;
				break;

			case "Dynamic":
				configType = BModule.ConfigTypeDynamic;
				break;

			case "Default":
				configType = BModule.ConfigTypeDefault;
				break;

			default:
				throw new UnsupportedOperationException("unknown ConfigType " + attr);
			}
		}
	}

	private final HashMap<String, Module> modules = new HashMap<>();
	private final IntHashSet providerNoDefaultModule = new IntHashSet();

	private ProviderModuleBinds() {
	}

	private ProviderModuleBinds(Element self) {
		if (!self.getNodeName().equals("ProviderModuleBinds"))
			throw new IllegalStateException("is it a ProviderModuleBinds config?");

		NodeList childNodes = self.getChildNodes();
		for (int i = 0, n = childNodes.getLength(); i < n; i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue;

			Element e = (Element)node;
			switch (e.getNodeName()) {
			case "module":
				var module = new Module(e);
				modules.put(module.fullName, module);
				break;

			case "ProviderNoDefaultModule":
				splitIntoSet(e.getAttribute("providers"), providerNoDefaultModule);
				break;

			default:
				throw new UnsupportedOperationException("unknown element name: " + e.getNodeName());
			}
		}
	}

	private static void splitIntoSet(String providers, IntHashSet set) {
		for (var provider : providers.split(",", -1)) {
			var p = provider.trim();
			if (!p.isEmpty())
				set.add(Integer.parseInt(p));
		}
	}

	public HashMap<String, Module> getModules() {
		return modules;
	}

	public IntHashSet getProviderNoDefaultModule() {
		return providerNoDefaultModule;
	}

	// 非动态模块都为静态模块, 其中声明ConfigType="Special"及providers不为空的只有指定providers会注册该模块
	// 声明ConfigType="Default"且providers为空的所有providers都会注册
	// 其它未在绑定配置定义的模块只要不在ProviderNoDefaultModule配置里的providers都会注册
	public void buildStaticBinds(HashMap<String, IModule> AllModules, int serverId, IntHashMap<BModule> out) {
		var noDefaultModule = providerNoDefaultModule.contains(serverId);
		for (var m : AllModules.values()) {
			var cm = modules.get(m.getFullName());
			if (cm == null) {
				if (noDefaultModule)
					continue;
			} else if (cm.configType == BModule.ConfigTypeDynamic)
				continue;
			else if (cm.configType == BModule.ConfigTypeSpecial) {
				if (!cm.providers.contains(serverId))
					continue;
			} else if (!cm.providers.isEmpty() && !cm.providers.contains(serverId)) // ConfigTypeDefault
				continue;
			out.put(m.getId(), cm != null ? new BModule(cm.choiceType, cm.configType, cm.subscribeType)
					: new BModule(BModule.ChoiceTypeDefault, BModule.ConfigTypeDefault,
					BSubscribeInfo.SubscribeTypeReadyCommit));
		}
	}

	// 动态模块必须在绑定配置里 声明ConfigType="Dynamic" 或 缺省的ConfigType并指定空的providers
	// 动态模块的providers为空则表示所有providers都可以注册该模块, 否则只有指定的providers可以注册
	public void buildDynamicBinds(HashMap<String, IModule> AllModules, int serverId, IntHashMap<BModule> out) {
		for (var m : AllModules.values()) {
			var cm = modules.get(m.getFullName());
			if (cm != null && cm.configType == BModule.ConfigTypeDynamic &&
					(cm.providers.isEmpty() || cm.providers.contains(serverId)))
				out.put(m.getId(), new BModule(cm.choiceType, BModule.ConfigTypeDynamic, cm.subscribeType));
		}
	}
}
