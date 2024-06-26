// auto-generated @formatter:off
package Zeze.Builtin.Game.Rank;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BRankValue extends Zeze.Transaction.Bean implements BRankValueReadOnly {
    public static final long TYPEID = 2276228832088785165L;

    private long _RoleId;
    private long _Value; // 含义由 BConcurrentKey.RankType 决定
    private Zeze.Net.Binary _ValueEx; // 自定义数据。

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
    public long getValue() {
        if (!isManaged())
            return _Value;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Value;
        var log = (Log__Value)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Value;
    }

    public void setValue(long value) {
        if (!isManaged()) {
            _Value = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Value(this, 2, value));
    }

    @Override
    public Zeze.Net.Binary getValueEx() {
        if (!isManaged())
            return _ValueEx;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ValueEx;
        var log = (Log__ValueEx)txn.getLog(objectId() + 3);
        return log != null ? log.value : _ValueEx;
    }

    public void setValueEx(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ValueEx = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ValueEx(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BRankValue() {
        _ValueEx = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BRankValue(long _RoleId_, long _Value_, Zeze.Net.Binary _ValueEx_) {
        _RoleId = _RoleId_;
        _Value = _Value_;
        if (_ValueEx_ == null)
            _ValueEx_ = Zeze.Net.Binary.Empty;
        _ValueEx = _ValueEx_;
    }

    @Override
    public void reset() {
        setRoleId(0);
        setValue(0);
        setValueEx(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    public void assign(BRankValue other) {
        setRoleId(other.getRoleId());
        setValue(other.getValue());
        setValueEx(other.getValueEx());
        _unknown_ = other._unknown_;
    }

    public BRankValue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRankValue copy() {
        var copy = new BRankValue();
        copy.assign(this);
        return copy;
    }

    public static void swap(BRankValue a, BRankValue b) {
        BRankValue save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__RoleId extends Zeze.Transaction.Logs.LogLong {
        public Log__RoleId(BRankValue bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BRankValue)getBelong())._RoleId = value; }
    }

    private static final class Log__Value extends Zeze.Transaction.Logs.LogLong {
        public Log__Value(BRankValue bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BRankValue)getBelong())._Value = value; }
    }

    private static final class Log__ValueEx extends Zeze.Transaction.Logs.LogBinary {
        public Log__ValueEx(BRankValue bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BRankValue)getBelong())._ValueEx = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Rank.BRankValue: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("RoleId=").append(getRoleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(getValue()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ValueEx=").append(getValueEx()).append(System.lineSeparator());
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

    private byte[] _unknown_;

    public byte[] unknown() {
        return _unknown_;
    }

    public void clearUnknown() {
        _unknown_ = null;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        var _ua_ = _unknown_;
        var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
        int _i_ = 0;
        {
            long _x_ = getRoleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getValue();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getValueEx();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setRoleId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setValue(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setValueEx(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BRankValue))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BRankValue)_o_;
        if (getRoleId() != _b_.getRoleId())
            return false;
        if (getValue() != _b_.getValue())
            return false;
        if (!getValueEx().equals(_b_.getValueEx()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getRoleId() < 0)
            return true;
        if (getValue() < 0)
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
                case 1: _RoleId = vlog.longValue(); break;
                case 2: _Value = vlog.longValue(); break;
                case 3: _ValueEx = vlog.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setRoleId(rs.getLong(_parents_name_ + "RoleId"));
        setValue(rs.getLong(_parents_name_ + "Value"));
        setValueEx(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "ValueEx")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "RoleId", getRoleId());
        st.appendLong(_parents_name_ + "Value", getValue());
        st.appendBinary(_parents_name_ + "ValueEx", getValueEx());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "RoleId", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Value", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ValueEx", "binary", "", ""));
        return vars;
    }
}
