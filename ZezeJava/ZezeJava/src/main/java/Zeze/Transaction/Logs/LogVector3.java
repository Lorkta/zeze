package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector3;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

public abstract class LogVector3 extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Transaction.Log<vector3>");

	public Vector3 value;

	public LogVector3(Bean belong, int varId, Vector3 value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteVector3(value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		value = bb.ReadVector3();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
