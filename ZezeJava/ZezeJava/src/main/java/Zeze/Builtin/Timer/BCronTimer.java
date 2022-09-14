// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BCronTimer extends Zeze.Transaction.Bean {
    private String _CronExpression;
    private long _NextExpectedTimeMills;
    private long _ExpectedTimeMills;
    private long _HappenTimeMills;

    public String getCronExpression() {
        if (!isManaged())
            return _CronExpression;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _CronExpression;
        var log = (Log__CronExpression)txn.GetLog(objectId() + 1);
        return log != null ? log.Value : _CronExpression;
    }

    public void setCronExpression(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _CronExpression = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__CronExpression(this, 1, value));
    }

    public long getNextExpectedTimeMills() {
        if (!isManaged())
            return _NextExpectedTimeMills;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NextExpectedTimeMills;
        var log = (Log__NextExpectedTimeMills)txn.GetLog(objectId() + 2);
        return log != null ? log.Value : _NextExpectedTimeMills;
    }

    public void setNextExpectedTimeMills(long value) {
        if (!isManaged()) {
            _NextExpectedTimeMills = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__NextExpectedTimeMills(this, 2, value));
    }

    public long getExpectedTimeMills() {
        if (!isManaged())
            return _ExpectedTimeMills;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ExpectedTimeMills;
        var log = (Log__ExpectedTimeMills)txn.GetLog(objectId() + 3);
        return log != null ? log.Value : _ExpectedTimeMills;
    }

    public void setExpectedTimeMills(long value) {
        if (!isManaged()) {
            _ExpectedTimeMills = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__ExpectedTimeMills(this, 3, value));
    }

    public long getHappenTimeMills() {
        if (!isManaged())
            return _HappenTimeMills;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HappenTimeMills;
        var log = (Log__HappenTimeMills)txn.GetLog(objectId() + 4);
        return log != null ? log.Value : _HappenTimeMills;
    }

    public void setHappenTimeMills(long value) {
        if (!isManaged()) {
            _HappenTimeMills = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__HappenTimeMills(this, 4, value));
    }

    @SuppressWarnings("deprecation")
    public BCronTimer() {
        _CronExpression = "";
    }

    @SuppressWarnings("deprecation")
    public BCronTimer(String _CronExpression_, long _NextExpectedTimeMills_, long _ExpectedTimeMills_, long _HappenTimeMills_) {
        if (_CronExpression_ == null)
            throw new IllegalArgumentException();
        _CronExpression = _CronExpression_;
        _NextExpectedTimeMills = _NextExpectedTimeMills_;
        _ExpectedTimeMills = _ExpectedTimeMills_;
        _HappenTimeMills = _HappenTimeMills_;
    }

    public void Assign(BCronTimer other) {
        setCronExpression(other.getCronExpression());
        setNextExpectedTimeMills(other.getNextExpectedTimeMills());
        setExpectedTimeMills(other.getExpectedTimeMills());
        setHappenTimeMills(other.getHappenTimeMills());
    }

    public BCronTimer CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BCronTimer Copy() {
        var copy = new BCronTimer();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BCronTimer a, BCronTimer b) {
        BCronTimer save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BCronTimer CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -6995089347718168392L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__CronExpression extends Zeze.Transaction.Logs.LogString {
        public Log__CronExpression(BCronTimer bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BCronTimer)getBelong())._CronExpression = Value; }
    }

    private static final class Log__NextExpectedTimeMills extends Zeze.Transaction.Logs.LogLong {
        public Log__NextExpectedTimeMills(BCronTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BCronTimer)getBelong())._NextExpectedTimeMills = Value; }
    }

    private static final class Log__ExpectedTimeMills extends Zeze.Transaction.Logs.LogLong {
        public Log__ExpectedTimeMills(BCronTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BCronTimer)getBelong())._ExpectedTimeMills = Value; }
    }

    private static final class Log__HappenTimeMills extends Zeze.Transaction.Logs.LogLong {
        public Log__HappenTimeMills(BCronTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BCronTimer)getBelong())._HappenTimeMills = Value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BCronTimer: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CronExpression").append('=').append(getCronExpression()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NextExpectedTimeMills").append('=').append(getNextExpectedTimeMills()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ExpectedTimeMills").append('=').append(getExpectedTimeMills()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HappenTimeMills").append('=').append(getHappenTimeMills()).append(System.lineSeparator());
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
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            String _x_ = getCronExpression();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getNextExpectedTimeMills();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getExpectedTimeMills();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getHappenTimeMills();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setCronExpression(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setNextExpectedTimeMills(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setExpectedTimeMills(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setHappenTimeMills(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void ResetChildrenRootInfo() {
    }

    @Override
    public boolean NegativeCheck() {
        if (getNextExpectedTimeMills() < 0)
            return true;
        if (getExpectedTimeMills() < 0)
            return true;
        if (getHappenTimeMills() < 0)
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void FollowerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _CronExpression = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 2: _NextExpectedTimeMills = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 3: _ExpectedTimeMills = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 4: _HappenTimeMills = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
            }
        }
    }
}
