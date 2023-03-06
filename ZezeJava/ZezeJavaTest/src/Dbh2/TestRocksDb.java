package Dbh2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import Zeze.Raft.LogSequence;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.BitConverter;
import org.junit.Test;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

public class TestRocksDb {
	// 测试RocksDb：key + commit_ts 方式编码，但是能快速定位到最后一个key的能力。
	@Test
	public void testKeyGet() throws IOException, RocksDBException {
		var path = Path.of("TestRocksDb");
		LogSequence.deleteDirectory(path.toFile());
		var options = new Options().setCreateIfMissing(true);
		try (var db = OptimisticTransactionDB.open(options, path.toString())) {
			var key = "key".getBytes(StandardCharsets.UTF_8);
			var value = "value".getBytes(StandardCharsets.UTF_8);
			byte[] key1;
			{
				var bb = ByteBuffer.Allocate();
				bb.Append(key);
				bb.WriteLong(1);
				key1 = bb.Copy();
			}
			byte[] key2;
			{
				var bb = ByteBuffer.Allocate();
				bb.Append(key);
				bb.WriteLong(2);
				key2 = bb.Copy();
			}
			byte[] key3;
			{
				var bb = ByteBuffer.Allocate();
				bb.Append(key);
				bb.WriteLong(3);
				key3 = bb.Copy();
			}
			var writeOptions = new WriteOptions();
			try (var trans = db.beginTransaction(writeOptions)) {
				trans.put(key1, value);
				trans.put(key2, value);
				trans.commit();
			}
			try (var it = db.newIterator()) {
				it.seekForPrev(key3);
				while (it.isValid()) {
					System.out.println(BitConverter.toString(it.key()) + "->" + BitConverter.toString(it.value()));
					it.next();
				}
			}
		} finally {
			LogSequence.deleteDirectory(path.toFile());
		}
	}
}
