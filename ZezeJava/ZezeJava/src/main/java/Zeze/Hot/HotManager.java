package Zeze.Hot;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import Zeze.Util.FewModifyMap;
import Zeze.Util.FewModifySortedMap;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * 装载所有的模块接口。
 *
 * 0. 参数指定工作目录和更新来源目录，
 * 1. 监视更新来源目录，自动安装升级。
 * 2. 一般全局一个实例。
 * 3. jar覆盖的时候，能装载里面新加入的class，但是同名的已经loadClass的类不会改变。
 */
public class HotManager extends ClassLoader {
	private final Path workingDir;
	private final Path distributeDir;

	private final FewModifyMap<File, JarFile> jars = new FewModifyMap<>();

	// module namespace -> HotModule
	private final FewModifySortedMap<String, HotModule> modules = new FewModifySortedMap<>();
	private volatile boolean running = true;
	private final Thread worker;

	// 采用其他管理措施以后，这个方法很可能不需要了。
	private HotModule find(String className) {
		// 因为存在子模块：
		// 优先匹配长的名字。
		// TreeMap是否有更优算法？
		for (var e : modules.descendingMap().entrySet()) {
			if (className.startsWith(e.getKey()))
				return e.getValue();
		}
		return null; // throw ?
	}

	public HotModule getModule(String moduleNamespace) {
		return modules.get(moduleNamespace);
	}

	public HotModule install(String namespace) throws Exception {
		// 使用临时文件名拷贝文件到工作目录。后面rename，减少锁定时间。
		var moduleSrc = Path.of(distributeDir.toString(), namespace + ".jar");
		var moduleTmp = Path.of(workingDir.toString(), "modules", namespace + ".install.jar");
		var interfaceSrc = Path.of(distributeDir.toString(), namespace + ".interface.jar");
		var interfaceTmp = Path.of(workingDir.toString(), "interfaces", namespace + ".interface.install.jar");
		Files.copy(moduleSrc, moduleTmp);
		Files.copy(interfaceSrc, interfaceTmp);

		// todo 同步方式需要修改成读写锁，跟系统运行互斥。
		synchronized (this) {
			// todo 生命期管理，确定服务是否可用，等等。
			// 安装 interface
			var interfaceDstAbsolute = Path.of(workingDir.toString(), "interfaces", namespace + ".interface.jar")
					.toFile().getAbsoluteFile();
			var oldI = jars.remove(interfaceDstAbsolute);
			if (null != oldI)
				oldI.close();
			Files.deleteIfExists(interfaceDstAbsolute.toPath());
			if (!interfaceTmp.toFile().renameTo(interfaceDstAbsolute))
				throw new RuntimeException("rename fail. " + interfaceTmp + "->" + interfaceDstAbsolute);
			jars.put(interfaceDstAbsolute, new JarFile(interfaceDstAbsolute));

			// 安装 module
			HotModule exist = modules.remove(namespace);
			var moduleDst = Path.of(workingDir.toString(), "modules", namespace + ".jar").toFile();
			if (exist != null)
				exist.stop();
			Files.deleteIfExists(moduleDst.toPath());
			if (!moduleTmp.toFile().renameTo(moduleDst))
				throw new RuntimeException("rename fail. " + moduleTmp + "->" + moduleDst);
			var module = new HotModule(this, namespace, moduleDst);
			if (exist != null)
				module.upgrade(exist);

			module.start();
			modules.put(module.getName(), module);
			return module;
		}
	}

	public HotManager(Path workingDir, Path distributeDir) throws Exception {
		this.workingDir = workingDir;
		this.distributeDir = distributeDir;

		this.loadExistJar(workingDir.toFile());
		var watcher = FileSystems.getDefault().newWatchService();
		workingDir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		worker = new Thread(() -> this.watch(watcher));
		worker.setDaemon(true);
		worker.start();;
	}

	private void loadExistJar(File dir) throws IOException {
		var files = dir.listFiles();
		if (null == files)
			return;

		for (var file : files) {
			if (file.getName().endsWith(".jar"))
				jars.put(file.getAbsoluteFile(), new JarFile(file));

			if (file.isDirectory())
				loadExistJar(file);
		}
	}

	public Path getWorkingDir() {
		return workingDir;
	}

	public Path getDistributeDir() {
		return distributeDir;
	}

	@Override
	protected Class<?> findClass(String className) throws ClassNotFoundException {
		for (var jar : jars.values()) {
			var loaded = loadInterfaceClass(className, jar);
			if (null != loaded)
				return loaded;
		}
		return super.findClass(className);
	}

	private Class<?> loadInterfaceClass(String className, JarFile jar) {
		String classFileName = className.replace('.', '/') + ".class";
		var entry = jar.getEntry(classFileName);
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

	// 严格的话，最好调用这个停止监视线程。
	public void stopAndJoin() throws InterruptedException {
		running = false;
		worker.join();
	}

	private void watch(WatchService watcher) {
		try {
			doWatch(watcher);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void doWatch(WatchService watcher) throws Exception {
		while (running) {
			var key = watcher.poll(200, TimeUnit.MILLISECONDS);
			if (null == key)
				continue;
			for (var event: key.pollEvents()) {
				var kind = event.kind();

				// This key is registered only for ENTRY_CREATE events,
				// but an OVERFLOW event can occur regardless if events
				// are lost or discarded.
				if (kind == OVERFLOW) {
					continue;
				}

				// The filename is the context of the event.
				var ev = (WatchEvent<Path>)event;
				var filename = ev.context().toAbsolutePath(); // 获得稳定的相对路径名。
				if (filename.endsWith(".jar")) {
					System.out.println(filename + " ++++++++++++");
					// todo 检查更新安装包准备好（Module的两个jar都存在），自动调用install。
					// install("");
				}
			}

			// Reset the key -- this step is critical if you want to
			// receive further watch events.  If the key is no longer valid,
			// the directory is inaccessible so exit the loop.
			if (!key.reset()) {
				break;
			}
		}
	}
}