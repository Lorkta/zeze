package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class BAllocateIdArgument extends Bean {
	private String name;
	private int count;

	public String getName() {
		return name;
	}

	public void setName(String value) {
		name = value;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int value) {
		count = value;
	}

	@Override
	public void decode(ByteBuffer bb) {
		setName(bb.ReadString());
		setCount(bb.ReadInt());
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(getName());
		bb.WriteInt(getCount());
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void resetChildrenRootInfo() {
		throw new UnsupportedOperationException();
	}

	private static int _PRE_ALLOC_SIZE_ = 16;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}

	@Override
	public String toString() {
		return "BAllocateIdArgument{" + "Name='" + name + '\'' + ", Count=" + count + '}';
	}
}
