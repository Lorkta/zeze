package Temp;

import java.nio.file.Path;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Hot.HotManager;

public class TempApp extends AppBase {
	public final HotManager manager;

	public TempApp() throws Exception {
		var workingDir = Path.of("ZezeJavaTest\\hot");
		var distributeDir = Path.of("ZezeJavaTest\\hot\\distributes");
		manager = new HotManager(this, workingDir, distributeDir);
		manager.startModules();
	}

	@Override
	public Application getZeze() {
		return null;
	}
}
