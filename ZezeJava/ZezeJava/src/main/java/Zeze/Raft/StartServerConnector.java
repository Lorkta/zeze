package Zeze.Raft;

import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

/**
 * 这条Rpc由Agent使用，用来主动查询Leader；
 * 【注意】
 * 这条Rpc目前仅用于Agent.detectLeader；
 */
public class StartServerConnector extends RaftRpc<EmptyBean, EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(StartServerConnector.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	static {
		register(TypeId_, StartServerConnector.class);
	}

	public StartServerConnector() {
		Argument = EmptyBean.instance;
		Result = EmptyBean.instance;
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}
}
