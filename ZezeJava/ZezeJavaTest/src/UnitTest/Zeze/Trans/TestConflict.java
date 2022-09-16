package UnitTest.Zeze.Trans;

import java.util.concurrent.Future;
import demo.Module1.BValue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import Zeze.Transaction.Procedure;

public class TestConflict {
	private int sum;

	@Before
	public final void testInit() throws Throwable {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Throwable {
		demo.App.getInstance().Stop();
	}

	@Test
	public final void testConflictAdd() throws Throwable {
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestConflict::ProcRemove, "ProcRemove").Call());
		Future<?>[] tasks = new Future[2000];
		for (int i = 0; i < 2000; ++i) {
			tasks[i]=Zeze.Util.Task.runUnsafe(demo.App.getInstance().Zeze.newProcedure(this::ProcAdd, "ProcAdd"), null, null);
		}
		for (Future<?> task : tasks) {
			try {
				task.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		sum = tasks.length;
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(this::ProcVerify, "ProcVerify").Call());
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestConflict::ProcRemove, "ProcRemove").Call());
	}

	private static long ProcRemove() {
		demo.App.getInstance().demo_Module1.getTable1().remove(123123L);
		return Procedure.Success;
	}

	private long ProcAdd() {
		BValue v = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(123123L);
		v.setInt1(v.getInt1() + 1);
		sum++;
		return Procedure.Success;
	}

	private long ProcVerify() {
		BValue v = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(123123L);
		Assert.assertEquals(v.getInt1(), sum);
		return Procedure.Success;
	}
}
