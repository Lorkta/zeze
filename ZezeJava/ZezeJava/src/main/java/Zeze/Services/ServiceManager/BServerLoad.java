package Zeze.Services.ServiceManager;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class BServerLoad extends Bean {
	public String ip;
	public int port;
	public Binary param = Binary.Empty;

	public String getName() {
		return ip + ":" + port;
	}

	@Override
	public void decode(ByteBuffer bb) {
		ip = bb.ReadString();
		port = bb.ReadInt();
		param = bb.ReadBinary();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(ip);
		bb.WriteInt(port);
		bb.WriteBinary(param);
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
		return "BServerLoad{" + "Ip='" + ip + '\'' + ", Port=" + port + ", Param=" + param + '}';
	}
}
