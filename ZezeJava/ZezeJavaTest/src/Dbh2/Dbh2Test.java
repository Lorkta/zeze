package Dbh2;

import java.io.File;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Util.Task;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Assert;
import org.junit.Test;

// 测试桶(raft)，在同一个进程内构建3个桶，通过Dbh2Agent访问。
public class Dbh2Test {
	static {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	@Test
	public void testDbh2() throws Exception {
		Task.tryInitThreadPool(null, null, null);
		var raftConfigString = """
<?xml version="1.0" encoding="utf-8"?>

<raft Name="">
	<node Host="127.0.0.1" Port="10000"/>
	<node Host="127.0.0.1" Port="10001"/>
	<node Host="127.0.0.1" Port="10002"/>
</raft>
				""";
		var dbh2_1 = start(raftConfigString, "127.0.0.1:10000");
		var dbh2_2 = start(raftConfigString, "127.0.0.1:10001");
		var dbh2_3 = start(raftConfigString, "127.0.0.1:10002");

		var raftConfig = RaftConfig.loadFromString(raftConfigString);
		var agent = new Dbh2Agent(raftConfig);

		final var db = "database";
		final var tb = "table";
		try {
			var meta = new BBucketMeta.Data();
			meta.setDatabaseName(db);
			meta.setTableName(tb);
			meta.setRaftConfig(raftConfigString);
			meta.setMoving(false);
			meta.setKeyFirst(Binary.Empty);
			meta.setKeyLast(Binary.Empty);
			agent.setBucketMeta(meta); // 这个meta必须第一个设置。由于测试的meta一样，第二次运行重复设置是可以的。

			var key = new Binary(new byte[] { 1 });
			var value = new Binary(new byte[] { 1 });

			var tid = agent.beginTransaction(db, tb);
			Assert.assertNotNull(tid);
			agent.put(db, tb, tid, key, value);
			agent.commitTransaction(tid);

			var kv = agent.get(db, tb, key);
			Assert.assertTrue(kv.getKey());
			Assert.assertNotNull(kv.getValue());
			Assert.assertEquals(value, new Binary(kv.getValue().Bytes, kv.getValue().ReadIndex, kv.getValue().size()));

		} finally {
			agent.close();
			dbh2_3.close();
			dbh2_2.close();
			dbh2_1.close();

			// 这个测试不持久化，为了不影响其他测试（可能使用相同的配置），运行结束删除持久化的目录。
			LogSequence.deleteDirectory(new File(dbh2_3.getRaft().getRaftConfig().getDbHome()));
			LogSequence.deleteDirectory(new File(dbh2_2.getRaft().getRaftConfig().getDbHome()));
			LogSequence.deleteDirectory(new File(dbh2_1.getRaft().getRaftConfig().getDbHome()));
		}
	}

	private static Zeze.Dbh2.Dbh2 start(String config, String raftName) throws Exception {
		var raftConfig = RaftConfig.loadFromString(config);
		return new Zeze.Dbh2.Dbh2(raftName, raftConfig, null, false);
	}
}
