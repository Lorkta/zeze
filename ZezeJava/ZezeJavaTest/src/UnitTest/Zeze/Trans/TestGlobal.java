package UnitTest.Zeze.Trans;

import java.util.concurrent.Future;
import Zeze.Config;
import Zeze.Transaction.DispatchMode;
import demo.Module1.BValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log1;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import junit.framework.TestCase;
import org.junit.Assert;

public class TestGlobal extends TestCase {
	public static class PrintLog extends Log1<BValue, BValue> {
		private static final Logger logger = LogManager.getLogger(TestGlobal.class);

		private static volatile int lastInt = -1;
		private final int oldInt;
		private final int appId;
		private final boolean eq;

		public PrintLog(Bean bean, BValue value, int appId) {
			super(bean, 0, value);
			oldInt = getValue().getInt1();
			eq = lastInt == oldInt;
			this.appId = appId;
		}

		@Override
		public int getTypeId() {
			return 0; // 现在Log1仅用于特殊目的，不支持相关日志系列化。
		}

		@Override
		public long getLogKey() {
			return this.getBean().objectId() + 100;
		}

		@Override
		public void commit() {
			if (eq) {
				logger.debug("xxxeq {} {}", oldInt, appId);
			} else {
				//logger.debug("xxx {} {}", oldInt, appId);
			}

			lastInt = oldInt;
		}
	}

	public final void testNone() {
		var rname = Zeze.Services.ServiceManager.Register.class.getTypeName();
		System.out.println(rname);
		var x = Zeze.Transaction.Bean.hash32(rname);
		System.out.println(x);
		var i = x & 0xffff;
		System.out.println(i);
	}

	public final void test2App() throws Exception {
		demo.App app1 = demo.App.getInstance();
		demo.App app2 = new demo.App();
		var config1 = Config.load("zeze.xml");
		var config2 = Config.load("zeze.xml");
		config2.setServerId(config1.getServerId() + 1);

		app1.Start(config1);
		app2.Start(config2);
		try {
			// 只删除一个app里面的记录就够了。
			Assert.assertEquals(Procedure.Success, app1.Zeze.newProcedure(() -> {
				app1.demo_Module1.getTable1().remove(6785L);
				return Procedure.Success;
			}, "RemoveClean").call());

			Future<?>[] task2 = new Future[2];
			int count = 2000;
			task2[0] = Zeze.Util.Task.runUnsafe(() -> ConcurrentAdd(app1, count, 1), "TestGlobal.ConcurrentAdd1", DispatchMode.Normal);
			task2[1] = Zeze.Util.Task.runUnsafe(() -> ConcurrentAdd(app2, count, 2), "TestGlobal.ConcurrentAdd2", DispatchMode.Normal);
			try {
				task2[0].get();
				task2[1].get();
			} catch (Exception e) {
				e.printStackTrace();
			}
			int countall = count * 2;
			Assert.assertEquals(Procedure.Success, app1.Zeze.newProcedure(() -> {
				int last1 = app1.demo_Module1.getTable1().get(6785L).getInt1();
				System.out.println("app1 " + last1);
				Assert.assertEquals(countall, last1);
				return Procedure.Success;
			}, "CheckResult1").call());
			Assert.assertEquals(Procedure.Success, app2.Zeze.newProcedure(() -> {
				int last2 = app2.demo_Module1.getTable1().get(6785L).getInt1();
				System.out.println("app2 " + last2);
				Assert.assertEquals(countall, last2);
				return Procedure.Success;
			}, "CheckResult2").call());
		} finally {
			app1.Stop();
			app2.Stop();
		}
	}

	private static void ConcurrentAdd(demo.App app, int count, int appId) {
		Future<?>[] tasks = new Future[count];
		for (int i = 0; i < tasks.length; ++i) {
			tasks[i] = Zeze.Util.Task.runUnsafe(app.Zeze.newProcedure(() -> {
				BValue b = app.demo_Module1.getTable1().getOrAdd(6785L);
				b.setInt1(b.getInt1() + 1);
				PrintLog log = new PrintLog(b, b, appId);
				//noinspection DataFlowIssue
				Transaction.getCurrent().putLog(log);
				return Procedure.Success;
			}, "ConcurrentAdd" + appId), null, null);
		}
		for (Future<?> task : tasks) {
			try {
				task.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
