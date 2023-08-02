package Zeze.Hot;

import java.io.File;
import java.io.IOException;
import java.util.WeakHashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import Zeze.AppBase;

// 目录管理规则（TODO，这个规则是一个限制，但能自动化，是否需要更自由配置方式？）：
// 1. 目录是一个模块目录时，开启一个新的热更单位；
// 2. 目录不是模块目录时，它就属于往上级目录方向的最近的热更模块。
public class HotModule extends ClassLoader {
	private final JarFile jar; // 模块的class（interface除外）必须打包成一个jar，只支持一个。
	private final HotService service;
	// todo Zeze.Util.WeakHashSet 没有遍历，先使用jdk的。
	private final WeakHashMap<HotModuleContext<?>, HotModuleContext<?>> refs = new WeakHashMap<>();

	public HotModule(AppBase app, HotManager parent, String namespace, File jarFile) throws Exception {
		super(namespace, parent);
		this.jar = new JarFile(jarFile);

		// App.ModuleClassName：MySolution.MyName.ModuleMyName，namespace=MySolution.MyName
		// MyName 一般就叫模块名字。
		var moduleClassName = namespace + ".Module" + last(namespace);
		service = (HotService)loadClass(moduleClassName).getConstructor(app.getClass()).newInstance(app);
	}

	private static String last(String namespace) {
		var ns = namespace.split("\\.");
		return ns[ns.length - 1];
	}

	public <T extends HotService> HotModuleContext<T> createContext() {
		var context = new HotModuleContext<T>(this);
		refs.put(context, context);
		return context;
	}

	public HotService getService() {
		return service;
	}

	// start 用来初始化，还没想好可能需要的初始化。
	public void start() throws Exception {
		service.start();
	}

	// stop 不能清除本地进程状态，后面需要用来升级。
	public void stop() throws Exception {
		service.stop();
		jar.close();
	}

	// 先用这个类管理所有热更需求。
	public void upgrade(HotModule old) throws Exception {
		refs.putAll(old.refs);
		for (var ref : refs.keySet()) {
			ref.setModule(this);
		}
		service.upgrade(old.service);
	}

	@Override
	protected Class<?> findClass(String className) {
		return loadModuleClass(className);
	}

	private Class<?> loadModuleClass(String className) {
		String classFileName = className.replace('.', '/') + ".class";
		var entry = jar.getEntry(classFileName);
		return loadModuleClass(className, entry);
	}

	private Class<?> loadModuleClass(String className, ZipEntry entry) {
		// 采用标准方式重载findClass以后，不需要判断这个了。
//		var loaded = findLoadedClass(className);
//		if (null != loaded)
//			return loaded;
		try (var inputStream = jar.getInputStream(entry)) {
			var bytes = inputStream.readAllBytes();
			return defineClass(className, bytes, 0, bytes.length);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
