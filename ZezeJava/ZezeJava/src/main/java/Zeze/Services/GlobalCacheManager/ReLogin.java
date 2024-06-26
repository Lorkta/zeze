package Zeze.Services.GlobalCacheManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.EmptyBean;

public class ReLogin extends Rpc<BLoginParam, EmptyBean> {
	public static final int ProtocolId_ = Zeze.Transaction.Bean.hash32(ReLogin.class.getName()); // 1197409195
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 1197409195

	static {
		register(TypeId_, ReLogin.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public ReLogin() {
		Argument = new BLoginParam();
		Result = EmptyBean.instance;
	}
}
