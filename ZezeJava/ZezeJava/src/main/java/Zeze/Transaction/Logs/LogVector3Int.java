package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector3Int;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

public abstract class LogVector3Int extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<vector3int>");

	public Vector3Int Value;

	public LogVector3Int() {
		super(TYPE_ID);
	}

	public LogVector3Int(Bean belong, int varId, Vector3Int value) {
		super(TYPE_ID);
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteVector3Int(Value);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Value = bb.ReadVector3Int();
	}

	@Override
	public String toString() {
		return String.valueOf(Value);
	}
}
