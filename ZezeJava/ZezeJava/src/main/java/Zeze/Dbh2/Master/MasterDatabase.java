package Zeze.Dbh2.Master;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMetaDaTa;
import Zeze.Dbh2.Bucket;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutObject;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class MasterDatabase {
	private final String databaseName;
	private final ConcurrentHashMap<String, MasterTableDaTa> tables = new ConcurrentHashMap<>();
	private final RocksDB db;

	public MasterDatabase(String databaseName) {
		try {
			this.databaseName = databaseName;
			this.db = RocksDB.open(databaseName);

			try (var it = this.db.newIterator(Bucket.getDefaultReadOptions())) {
				while (it.isValid()) {
					var tableName = new String(it.key(), StandardCharsets.UTF_8);
					var bTable = new MasterTableDaTa();
					var bb = ByteBuffer.Wrap(it.value());
					bTable.decode(bb);
					tables.put(tableName, bTable);
					it.next();
				}
			}
		} catch (RocksDBException ex) {
			throw new RuntimeException(ex);
		}
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public MasterTableDaTa getTable(String tableName) {
		return tables.get(tableName);
	}

	public BBucketMetaDaTa locateBucket(String tableName, Binary key) {
		var bTable = getTable(tableName);
		if (null == bTable)
			return null;
		return bTable.locate(key);
	}

	public MasterTableDaTa createTable(String tableName, OutObject<Boolean> outIsNew) throws RocksDBException {
		outIsNew.value = false;
		var table = tables.get(tableName);
		if (table != null)
			return table; // table exist

		outIsNew.value = true;
		table = new MasterTableDaTa();
		var bucket = new BBucketMetaDaTa();
		// todo allocate first bucket service and setup table

		bucket.setKeyFirst(Binary.Empty);
		bucket.setKeyLast(Binary.Empty);
		table.buckets.put(bucket.getKeyFirst(), bucket);

		// master数据马上存数据库。
		var bbValue = ByteBuffer.Allocate();
		table.encode(bbValue);
		var key = tableName.getBytes(StandardCharsets.UTF_8);
		this.db.put(Bucket.getDefaultWriteOptions(), key, 0, key.length, bbValue.Bytes, bbValue.ReadIndex, bbValue.size());

		// 保存在内存中，用来快速查询。
		this.tables.put(tableName, table);
		return table;
	}
}
