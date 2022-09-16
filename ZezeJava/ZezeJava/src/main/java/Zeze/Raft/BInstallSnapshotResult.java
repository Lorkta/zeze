package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

final class BInstallSnapshotResult extends Bean {
	private long term;

	// 非标准Raft协议参数：用来支持续传。
	// >=0 : 让Leader从该位置继续传输数据。
	// -1  : 让Leader按自己顺序传输数据。
	private long offset = -1;

	public long getTerm() {
		return term;
	}

	public void setTerm(long value) {
		term = value;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long value) {
		offset = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(term);
		bb.WriteLong(offset);
	}

	@Override
	public void decode(ByteBuffer bb) {
		term = bb.ReadLong();
		offset = bb.ReadLong();
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void resetChildrenRootInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return String.format("(Term=%d Offset=%d)", term, offset);
	}
}
