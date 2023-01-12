// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BAccount extends Zeze.Transaction.Bean implements BAccountReadOnly {
    public static final long TYPEID = 3220082739597459764L;

    private long _LastLoginVersion; // 用来生成 role 登录版本号。每次递增。

    @Override
    public long getLastLoginVersion() {
        if (!isManaged())
            return _LastLoginVersion;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LastLoginVersion;
        var log = (Log__LastLoginVersion)txn.getLog(objectId() + 1);
        return log != null ? log.value : _LastLoginVersion;
    }

    public void setLastLoginVersion(long value) {
        if (!isManaged()) {
            _LastLoginVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LastLoginVersion(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BAccount() {
    }

    @SuppressWarnings("deprecation")
    public BAccount(long _LastLoginVersion_) {
        _LastLoginVersion = _LastLoginVersion_;
    }

    public void assign(BAccount other) {
        setLastLoginVersion(other.getLastLoginVersion());
    }

    @Deprecated
    public void Assign(BAccount other) {
        assign(other);
    }

    public BAccount copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAccount copy() {
        var copy = new BAccount();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BAccount Copy() {
        return copy();
    }

    public static void swap(BAccount a, BAccount b) {
        BAccount save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__LastLoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LastLoginVersion(BAccount bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAccount)getBelong())._LastLoginVersion = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Online.BAccount: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LastLoginVersion=").append(getLastLoginVersion()).append(System.lineSeparator());
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
            long _x_ = getLastLoginVersion();
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
            setLastLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getLastLoginVersion() < 0)
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
                case 1: _LastLoginVersion = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
