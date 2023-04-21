package UnitTest.Zeze.Trans;

import UnitTest.Zeze.BMyBean;
import Zeze.Serialize.Vector2;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Record1;
import Zeze.Transaction.TableKey;
import demo.App;
import demo.Module1.BSimple;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestProcdure {
	private final BMyBean bean = new BMyBean();

	public final long ProcTrue() {
		bean.setI(123);
		Assert.assertEquals(bean.getI(), 123);
		return Procedure.Success;
	}

	public final long ProcFalse() {
		bean.setI(456);
		Assert.assertEquals(bean.getI(), 456);
		return Procedure.Unknown;
	}

	public final long ProcNest() throws Exception {
		Assert.assertEquals(bean.getI(), 0);
		bean.setI(1);
		Assert.assertEquals(bean.getI(), 1);
		{
			long r = demo.App.getInstance().Zeze.newProcedure(this::ProcFalse, "ProcFalse").call();
			Assert.assertNotEquals(r, Procedure.Success);
			Assert.assertEquals(bean.getI(), 1);
		}

		{
			long r = demo.App.getInstance().Zeze.newProcedure(this::ProcTrue, "ProcFalse").call();
			Assert.assertEquals(r, Procedure.Success);
			Assert.assertEquals(bean.getI(), 123);
		}

		return Procedure.Success;
	}

	@Before
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	@Test
	public final void test1() throws Exception {
		TableKey root = new TableKey(1, 1);
		// 特殊测试，拼凑一个record用来提供需要的信息。
		var r = new Record1<>(null, 1L, bean);
		bean.initRootInfo(r.createRootInfoIfNeed(root), null);
		long rc = demo.App.getInstance().Zeze.newProcedure(this::ProcNest, "ProcNest").call();
		Assert.assertEquals(rc, Procedure.Success);
		// 最后一个 Call，事务外，bean 已经没法访问事务支持的属性了。直接访问内部变量。
		Assert.assertEquals(bean._i, 123);
	}

	@Test
	public final void testVector() throws Exception {
		App.getInstance().Zeze.newProcedure(() -> {
			var v = App.getInstance().demo_Module1.getTable1().getOrAdd(999L);
			v.setVector2(new Vector2(1, 2));
			Assert.assertEquals(new Vector2(1, 2), v.getVector2());
			return 0;
		}, "testVector1").call();

		App.getInstance().Zeze.newProcedure(() -> {
			var v = App.getInstance().demo_Module1.getTable1().getOrAdd(999L);
			Assert.assertEquals(new Vector2(1, 2), v.getVector2());
			v.setVector2(new Vector2(3, 4));
			Assert.assertEquals(new Vector2(3, 4), v.getVector2());
			return Procedure.LogicError;
		}, "testVector2").call();

		App.getInstance().Zeze.newProcedure(() -> {
			var v = App.getInstance().demo_Module1.getTable1().getOrAdd(999L);
			Assert.assertEquals(new Vector2(1, 2), v.getVector2());
			App.getInstance().demo_Module1.getTable1().remove(999L);
			return 0;
		}, "testVector3").call();
	}

	@Test
	public void testNestLogOneLogDynamic() throws Exception {
		Assert.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var value = App.Instance.demo_Module1.getTable1().getOrAdd(18989L);
			value.setBean12(new BSimple());
			value.getDynamic14().setBean(new BSimple());
			value.getSet10().add(1);
			value.getMap15().put(1L, 1L);
			value.getList9().add(new demo.Bean1());
			Assert.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
				var value2 = App.Instance.demo_Module1.getTable1().getOrAdd(18989L);
				value2.setBean12(new BSimple());
				value2.getDynamic14().setBean(new BSimple());
				value2.getSet10().add(1);
				value2.getMap15().put(1L, 1L);
				value2.getList9().add(new demo.Bean1());
				return 0;
			}, "Nest").call());
			return 0;
		}, "testNestLogOneLogDynamic").call());
	}
}
