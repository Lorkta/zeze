package Temp;

import java.util.concurrent.ThreadLocalRandom;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DatabaseMySql;

public class TestJdbcHalt {
	private static final int tableCount = 5;
	private static final int recordCount = 10000;
	private static volatile long curKey;

	private static void updateLoop(DatabaseMySql mysql, DatabaseMySql.TableMysql[] tables, long i) throws Exception {
		var kb = ByteBuffer.Allocate();
		var vb = ByteBuffer.Allocate();
		//noinspection InfiniteLoopStatement
		for (; ; curKey = ++i) {
			try (var txn = mysql.beginTransaction()) {
				kb.Reset();
				kb.WriteLong(i % recordCount);
				vb.Reset();
				vb.WriteLong(i);
				for (int j = 0; j < tableCount; j++) {
					tables[j].replace(txn, kb, vb);
					// Thread.sleep(1);
				}
				txn.commit();
			}
		}
	}

	private static long check(DatabaseMySql.TableMysql[] tables) {
		var max = -1L;
		var kb = ByteBuffer.Allocate();
		int n = 0;
		for (int i = 0; i < recordCount; i++) {
			var v = -1L;
			for (int j = 0; j < tableCount; j++) {
				kb.Reset();
				kb.WriteLong(i);
				var vb = tables[j].find(kb);
				var v2 = vb != null ? vb.ReadLong() : -1L;
				if (j != 0 && v != v2)
					throw new AssertionError("check failed! key = " + i + ", value = " + v + " != " + v2);
				v = v2;
			}
			if (max < v)
				max = v;
			if (v >= 0)
				n++;
		}
		System.out.println("checked " + n + " records OK!");
		return max;
	}

	public static void main(String[] args) throws Exception {
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.MySql);
		dbConf.setDatabaseUrl("jdbc:mysql://"
				+ (args.length > 0 ? args[0] : "localhost:3306/devtest?user=dev&password=devtest12345")
				+ "&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
		dbConf.setName("mysql");
		dbConf.setDruidConf(new Config.DruidConf());

		System.out.println("init mysql client ...");
		var mysql = new DatabaseMySql(null, dbConf);
		var tables = new DatabaseMySql.TableMysql[tableCount];
		for (int i = 0; i < tableCount; i++)
			tables[i] = (DatabaseMySql.TableMysql)mysql.openTable("TestJdbcHalt" + i);

		System.out.println("check tables ...");
		var max = check(tables);
		curKey = max;
		new Thread(() -> {
			var ms = ThreadLocalRandom.current().nextInt(1000, 5000);
			System.out.println("wait " + ms + " ms");
			try {
				Thread.sleep(ms);
			} catch (InterruptedException ignored) {
			}
			System.out.println("halt! curKey = " + curKey);
			Runtime.getRuntime().halt(9);
		}).start();
		updateLoop(mysql, tables, max + 1);
	}
}
