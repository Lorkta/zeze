package Zeze.Services.GlobalCacheManager;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;

public class BGlobalKeyState extends Bean {
	public Binary globalKey; // 没有初始化，使用时注意
	public int state;

	@Override
	public void decode(ByteBuffer bb) {
		globalKey = bb.ReadBinary();
		state = bb.ReadInt();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBinary(globalKey);
		bb.WriteInt(state);
	}

	@Override
	public String toString() {
		return globalKey + ":" + state;
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
}
