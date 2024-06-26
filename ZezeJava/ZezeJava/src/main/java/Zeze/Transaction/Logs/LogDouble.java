package Zeze.Transaction.Logs;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public class LogDouble extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<double>");

	public double value;

	public LogDouble(Bean belong, int varId, double value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	public LogDouble() {
	}

	@Override
	public @NotNull Category category() {
		return Category.eHistory;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public void commit() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteDouble(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = bb.ReadDouble();
	}

	@Override
	public @NotNull String toString() {
		return String.valueOf(value);
	}

	@Override
	public long longValue() {
		return (long)value;
	}

	@Override
	public double doubleValue() {
		return value;
	}
}
