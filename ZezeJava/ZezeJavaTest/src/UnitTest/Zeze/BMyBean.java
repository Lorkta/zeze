package UnitTest.Zeze;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;
import org.junit.Assert;

public class BMyBean extends Bean {
	@Override
	public void decode(ByteBuffer bb) {
		_i = bb.ReadInt();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(_i);
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
	}

	@Override
	protected void resetChildrenRootInfo() {
	}

	public int _i;

	private static class MyLog extends Zeze.Transaction.Logs.LogInt {
		public MyLog(BMyBean bean, int value) {
			super(bean, 0, value);
		}

		@Override
		public void commit() {
			((BMyBean)getBean())._i = value;
		}
	}

	public final int getI() {
		var txn = Transaction.getCurrent();
		if (null == txn)
			return _i;
		BMyBean.MyLog log = (BMyBean.MyLog)txn.getLog(this.objectId());
		return (null != log) ? log.value : _i;
	}

	public final void setI(int value) {
		var txn = Transaction.getCurrent();
		Assert.assertNotNull(txn);
		txn.putLog(new BMyBean.MyLog(this, value));
	}
}
