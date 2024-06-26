package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public class OfflineNotify extends Rpc<BOfflineNotify, EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(OfflineNotify.class.getName()); // 1813026175
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 1813026175

	static {
		register(TypeId_, OfflineNotify.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public OfflineNotify() {
		Argument = new BOfflineNotify();
		Result = EmptyBean.instance;
	}

	public OfflineNotify(BOfflineNotify argument) {
		Argument = argument;
		Result = EmptyBean.instance;
	}
}
