package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector4;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

public abstract class LogVector4 extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Transaction.Log<vector4>");

	public Vector4 value;

	public LogVector4(Bean belong, int varId, Vector4 value) {
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
		bb.WriteVector4(value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		value = bb.ReadVector4();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
