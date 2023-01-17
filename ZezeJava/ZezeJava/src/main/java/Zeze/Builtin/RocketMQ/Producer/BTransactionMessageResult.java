// auto-generated @formatter:off
package Zeze.Builtin.RocketMQ.Producer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTransactionMessageResult extends Zeze.Transaction.Bean implements BTransactionMessageResultReadOnly {
    public static final long TYPEID = 9172956284242602104L;

    private boolean _Result;
    private long _Timestamp;

    @Override
    public boolean isResult() {
        if (!isManaged())
            return _Result;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Result;
        var log = (Log__Result)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Result;
    }

    public void setResult(boolean value) {
        if (!isManaged()) {
            _Result = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Result(this, 1, value));
    }

    @Override
    public long getTimestamp() {
        if (!isManaged())
            return _Timestamp;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Timestamp;
        var log = (Log__Timestamp)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Timestamp;
    }

    public void setTimestamp(long value) {
        if (!isManaged()) {
            _Timestamp = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Timestamp(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BTransactionMessageResult() {
    }

    @SuppressWarnings("deprecation")
    public BTransactionMessageResult(boolean _Result_, long _Timestamp_) {
        _Result = _Result_;
        _Timestamp = _Timestamp_;
    }

    public void assign(BTransactionMessageResult other) {
        setResult(other.isResult());
        setTimestamp(other.getTimestamp());
    }

    @Deprecated
    public void Assign(BTransactionMessageResult other) {
        assign(other);
    }

    public BTransactionMessageResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTransactionMessageResult copy() {
        var copy = new BTransactionMessageResult();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BTransactionMessageResult Copy() {
        return copy();
    }

    public static void swap(BTransactionMessageResult a, BTransactionMessageResult b) {
        BTransactionMessageResult save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Result extends Zeze.Transaction.Logs.LogBool {
        public Log__Result(BTransactionMessageResult bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransactionMessageResult)getBelong())._Result = value; }
    }

    private static final class Log__Timestamp extends Zeze.Transaction.Logs.LogLong {
        public Log__Timestamp(BTransactionMessageResult bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransactionMessageResult)getBelong())._Timestamp = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Result=").append(isResult()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Timestamp=").append(getTimestamp()).append(System.lineSeparator());
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
            boolean _x_ = isResult();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            long _x_ = getTimestamp();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setResult(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTimestamp(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getTimestamp() < 0)
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
                case 1: _Result = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
                case 2: _Timestamp = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}