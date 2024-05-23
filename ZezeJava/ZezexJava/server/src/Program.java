import Zeze.Services.Daemon;

public class Program {
	public synchronized static void main(String[] args) throws Exception {
		System.setProperty(Daemon.propertyNameClearInUse, "true");
		//【用来生成出Redirect模块，调试用】
		// args = new String[] { "-GenFileSrcRoot", "C:\\code\\zeze\\ZezeJava\\ZezexJava\\server\\src" };
		Game.App.getInstance().Start(args);
		try {
			Program.class.wait();
		} finally {
			Game.App.getInstance().Stop();
		}
	}
}
