package Zeze.Services.GlobalCacheManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;

public class BAchillesHeel extends Bean {
	public int serverId; // 必须的。
	public String secureKey; // 安全验证
	public int globalCacheManagerHashIndex; // 安全验证

	@Override
	public void decode(ByteBuffer bb) {
		serverId = bb.ReadInt();
		secureKey = bb.ReadString();
		globalCacheManagerHashIndex = bb.ReadInt();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(serverId);
		bb.WriteString(secureKey);
		bb.WriteInt(globalCacheManagerHashIndex);
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
		return "BAchillesHeel{" + "ServerId=" + serverId + ", SecureKey='" + secureKey + '\'' +
				", GlobalCacheManagerHashIndex=" + globalCacheManagerHashIndex + '}';
	}
}
