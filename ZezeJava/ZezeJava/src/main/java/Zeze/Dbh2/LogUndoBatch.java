package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.UndoBatch;
import Zeze.Net.Binary;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;

public class LogUndoBatch extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogUndoBatch.class.getName());

	private Binary tid;

	public LogUndoBatch() {
		this(Binary.Empty);
	}

	public LogUndoBatch(UndoBatch req) {
		super(null);
		if (null != req)
			this.tid = req.Argument.getTid();
	}

	public LogUndoBatch(Binary tid) {
		super(null);
		this.tid = tid;
	}

	@Override
	public long typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.undoBatch(tid);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		bb.WriteBinary(tid);
	}

	@Override
	public void decode(ByteBuffer bb) {
		super.decode(bb);
		tid = bb.ReadBinary();
	}
}
