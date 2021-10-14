package Game.Login;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public final class tonline extends Zeze.Transaction.Table<Long, Game.Login.BOnline> {
	public tonline() {
		super("Game_Login_tonline");
	}

	@Override
	public boolean isMemory() {
		return false;
	}
	@Override
	public boolean isAutoKey() {
		return false;
	}

	public static final int VAR_All = 0;
	public static final int VAR_LinkName = 1;
	public static final int VAR_LinkSid = 2;
	public static final int VAR_State = 3;
	public static final int VAR_ReliableNotifyMark = 4;
	public static final int VAR_ReliableNotifyQueue = 5;
	public static final int VAR_ReliableNotifyConfirmCount = 6;
	public static final int VAR_ReliableNotifyTotalCount = 7;
	public static final int VAR_ProviderId = 8;
	public static final int VAR_ProviderSessionId = 9;

	@Override
	public long DecodeKey(ByteBuffer _os_) {
		long _v_;
		_v_ = _os_.ReadLong();
		return _v_;
	}

	@Override
	public ByteBuffer EncodeKey(long _v_) {
		ByteBuffer _os_ = ByteBuffer.Allocate();
		_os_.WriteLong(_v_);
		return _os_;
	}

	@Override
	public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
		return switch (variableId) {
			case 0 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			case 1 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			case 2 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			case 3 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			case 4 -> new Zeze.Transaction.ChangeVariableCollectorSet();
			case 5 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			case 6 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			case 7 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			case 8 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			case 9 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			default -> null;
		};
	}


}