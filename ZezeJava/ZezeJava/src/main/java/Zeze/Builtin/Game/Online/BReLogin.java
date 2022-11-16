// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BReLogin extends Zeze.Transaction.Bean implements BReLoginReadOnly {
    public static final long TYPEID = 8551355014943125267L;

    private long _RoleId;
    private long _ReliableNotifyConfirmIndex;

    @Override
    public long getRoleId() {
        if (!isManaged())
            return _RoleId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _RoleId;
        var log = (Log__RoleId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _RoleId;
    }

    public void setRoleId(long value) {
        if (!isManaged()) {
            _RoleId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__RoleId(this, 1, value));
    }

    @Override
    public long getReliableNotifyConfirmIndex() {
        if (!isManaged())
            return _ReliableNotifyConfirmIndex;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ReliableNotifyConfirmIndex;
        var log = (Log__ReliableNotifyConfirmIndex)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ReliableNotifyConfirmIndex;
    }

    public void setReliableNotifyConfirmIndex(long value) {
        if (!isManaged()) {
            _ReliableNotifyConfirmIndex = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ReliableNotifyConfirmIndex(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BReLogin() {
    }

    @SuppressWarnings("deprecation")
    public BReLogin(long _RoleId_, long _ReliableNotifyConfirmIndex_) {
        _RoleId = _RoleId_;
        _ReliableNotifyConfirmIndex = _ReliableNotifyConfirmIndex_;
    }

    public void assign(BReLogin other) {
        setRoleId(other.getRoleId());
        setReliableNotifyConfirmIndex(other.getReliableNotifyConfirmIndex());
    }

    @Deprecated
    public void Assign(BReLogin other) {
        assign(other);
    }

    public BReLogin copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BReLogin copy() {
        var copy = new BReLogin();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BReLogin Copy() {
        return copy();
    }

    public static void swap(BReLogin a, BReLogin b) {
        BReLogin save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__RoleId extends Zeze.Transaction.Logs.LogLong {
        public Log__RoleId(BReLogin bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReLogin)getBelong())._RoleId = value; }
    }

    private static final class Log__ReliableNotifyConfirmIndex extends Zeze.Transaction.Logs.LogLong {
        public Log__ReliableNotifyConfirmIndex(BReLogin bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReLogin)getBelong())._ReliableNotifyConfirmIndex = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Online.BReLogin: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("RoleId=").append(getRoleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyConfirmIndex=").append(getReliableNotifyConfirmIndex()).append(System.lineSeparator());
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
            long _x_ = getRoleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getReliableNotifyConfirmIndex();
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
            setRoleId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setReliableNotifyConfirmIndex(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void resetChildrenRootInfo() {
    }

    @Override
    public boolean negativeCheck() {
        if (getRoleId() < 0)
            return true;
        if (getReliableNotifyConfirmIndex() < 0)
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
                case 1: _RoleId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _ReliableNotifyConfirmIndex = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
