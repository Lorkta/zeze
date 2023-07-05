package UnitTest.Zeze.Component;

import java.nio.charset.StandardCharsets;
import Zeze.Net.Binary;
import Zeze.Services.Token;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class TestToken {
	private static final Logger logger = LogManager.getLogger(TestToken.class);

	@Test
	public void testToken() throws Exception {
		Task.tryInitThreadPool(null, null, null);
		var tokenServer = new Token().start(null, null, 5003);
		try {
			var tokenClient = new Token.TokenClient(null).start("127.0.0.1", 5003);
			try {
				tokenClient.waitReady();

				var token = tokenClient.newToken(new Binary("abc"), 5000).get().getToken();
				logger.info("token: '{}'", token);
				Assert.assertEquals(24, token.length());

				var res = tokenClient.getToken(token, 1).get();
				Assert.assertEquals("abc", res.getContext().toString(StandardCharsets.UTF_8));
				Assert.assertEquals(1, res.getCount());
				Assert.assertTrue(res.getTime() >= 0);

				res = tokenClient.getToken(token, 1).get();
				Assert.assertEquals("", res.getContext().toString(StandardCharsets.UTF_8));
				Assert.assertEquals(0, res.getCount());
				Assert.assertTrue(res.getTime() < 0);
			} finally {
				tokenClient.stop();
			}
		} finally {
			tokenServer.stop();
		}
	}
}
