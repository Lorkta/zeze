package Zeze.Services.GlobalCacheManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;

public class BAchillesHeelConfig extends Bean {
	public int maxNetPing;
	public int serverProcessTime;
	public int serverReleaseTimeout;

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(maxNetPing);
		bb.WriteInt(serverProcessTime);
		bb.WriteInt(serverReleaseTimeout);
	}

	@Override
	public void decode(ByteBuffer bb) {
		maxNetPing = bb.ReadInt();
		serverProcessTime = bb.ReadInt();
		serverReleaseTimeout = bb.ReadInt();
	}
}
