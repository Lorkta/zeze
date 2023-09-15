package UnitTest.Zeze.Trans;

import demo.App;
import demo.Bean1;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestMemorySize {
	@Before
	public void before() throws Exception {
		demo.App.getInstance().Start();
	}

	@Test
	public void testMemorySize() throws Exception {
		Assert.assertEquals(0, App.Instance.demo_Module1.tMemorySize().getCacheSize());
		App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.tMemorySize().insert(1L, new Bean1());
			return 0;
		}, "Insert").call();
		Assert.assertEquals(1, App.Instance.demo_Module1.tMemorySize().getCacheSize());
		App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.tMemorySize().insert(2L, new Bean1());
			return 0;
		}, "Insert").call();
		Assert.assertEquals(2, App.Instance.demo_Module1.tMemorySize().getCacheSize());
		App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.tMemorySize().remove(2L);
			return 0;
		}, "Insert").call();
		Assert.assertEquals(1, App.Instance.demo_Module1.tMemorySize().getCacheSize());
		App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.tMemorySize().remove(1L);
			return 0;
		}, "Insert").call();
		Assert.assertEquals(0, App.Instance.demo_Module1.tMemorySize().getCacheSize());
	}
}
