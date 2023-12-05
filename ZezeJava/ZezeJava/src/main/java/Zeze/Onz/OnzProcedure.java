package Zeze.Onz;

import java.util.Set;
import Zeze.Application;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.FuncLong;

public class OnzProcedure implements FuncLong {
	private final long onzTid;
	private final OnzProcedureStub<?, ?> stub;
	private final Bean argument;
	private final Bean result;

	public OnzProcedure(long onzTid, OnzProcedureStub<?, ?> stub, Bean argument, Bean result) {
		this.onzTid = onzTid;
		this.stub = stub;
		this.argument = argument;
		this.result = result;
	}

	public long getOnzTid() {
		return onzTid;
	}

	public OnzProcedureStub<?, ?> getStub() {
		return stub;
	}

	public Bean getArgument() {
		return argument;
	}

	public Bean getResult() {
		return result;
	}

	@Override
	public long call() throws Exception {
		// 这里实际上需要侵入Zeze.Transaction，在锁定，时戳检查完成后，
		// 发送result给调用者，完成ready状态，
		// Zeze.Transaction 需要同步进行等待。

		var txn = Transaction.getCurrent();
		if (null == txn)
			throw new RuntimeException("no transaction.");
		txn.setOnzProcedure(this);
		try {
			return stub.call(this, argument, result);
		} finally {
			txn.setOnzProcedure(null);
		}
	}

	public String getName() {
		return stub.getName();
	}

	public void sendReadyAndWait() {
		// 发送事务执行阶段的两段式提交的准备完成，同时等待一起提交的信号。
	}

	protected void sendFlushReady() {
		// 发送事务保存阶段的两段式提交的准备完成，同时等待一起提交的信号。
	}

	protected void flushWait() {

	}

	// helper
	public static void sendFlushAndWait(Set<OnzProcedure> onzProcedures) {
		// send all
		for (var onz : onzProcedures) {
			if (null != onz)
				onz.sendFlushReady();
		}
		// wait all
		for (var onz : onzProcedures) {
			if (null != onz)
				onz.flushWait();
		}
	}
}
