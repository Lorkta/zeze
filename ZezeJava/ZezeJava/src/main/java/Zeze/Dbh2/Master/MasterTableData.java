package Zeze.Dbh2.Master;

import java.util.Collection;
import java.util.TreeMap;
import Zeze.Builtin.Dbh2.BBucketMetaData;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Net.Binary;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

public class MasterTableData implements Serializable {
	final TreeMap<Binary, BBucketMetaData> buckets = new TreeMap<>(); // key is meta.first

	public Collection<BBucketMetaData> buckets() {
		return buckets.values();
	}

	public BBucketMetaData locate(Binary key) {
		var lower = buckets.floorEntry(key);
		return lower.getValue();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(buckets.size());
		for (var e : buckets.entrySet()) {
			bb.WriteBinary(e.getKey());
			e.getValue().encode(bb);
		}
	}

	@Override
	public void decode(ByteBuffer bb) {
		buckets.clear();
		for (var size = bb.ReadInt(); size > 0; --size) {
			var key = bb.ReadBinary();
			var value = new BBucketMetaData();
			value.decode(bb);
			buckets.put(key, value);
		}
	}
}
