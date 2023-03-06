// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BRollbackTransactionArgument extends Zeze.Transaction.Bean implements BRollbackTransactionArgumentReadOnly {
    public static final long TYPEID = -3144088224662789521L;

    private long _TransactionId;

    @Override
    public long getTransactionId() {
        if (!isManaged())
            return _TransactionId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TransactionId;
        var log = (Log__TransactionId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TransactionId;
    }

    public void setTransactionId(long value) {
        if (!isManaged()) {
            _TransactionId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TransactionId(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BRollbackTransactionArgument() {
    }

    @SuppressWarnings("deprecation")
    public BRollbackTransactionArgument(long _TransactionId_) {
        _TransactionId = _TransactionId_;
    }

    @Override
    public Zeze.Builtin.Dbh2.BRollbackTransactionArgumentData toData() {
        var data = new Zeze.Builtin.Dbh2.BRollbackTransactionArgumentData();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BRollbackTransactionArgumentData)other);
    }

    public void assign(BRollbackTransactionArgumentData other) {
        setTransactionId(other.getTransactionId());
    }

    public void assign(BRollbackTransactionArgument other) {
        setTransactionId(other.getTransactionId());
    }

    @Deprecated
    public void Assign(BRollbackTransactionArgument other) {
        assign(other);
    }

    public BRollbackTransactionArgument copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRollbackTransactionArgument copy() {
        var copy = new BRollbackTransactionArgument();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BRollbackTransactionArgument Copy() {
        return copy();
    }

    public static void swap(BRollbackTransactionArgument a, BRollbackTransactionArgument b) {
        BRollbackTransactionArgument save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TransactionId extends Zeze.Transaction.Logs.LogLong {
        public Log__TransactionId(BRollbackTransactionArgument bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BRollbackTransactionArgument)getBelong())._TransactionId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BRollbackTransactionArgument: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TransactionId=").append(getTransactionId()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
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
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            long _x_ = getTransactionId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setTransactionId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getTransactionId() < 0)
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _TransactionId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
