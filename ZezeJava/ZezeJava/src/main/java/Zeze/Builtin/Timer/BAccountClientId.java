// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "RedundantSuppression", "MethodMayBeStatic", "PatternVariableCanBeUsed"})
public final class BAccountClientId implements Serializable, Comparable<BAccountClientId> {
    private String _Account;
    private String _ClientId;

    // for decode only
    public BAccountClientId() {
        _Account = "";
        _ClientId = "";
    }

    public BAccountClientId(String _Account_, String _ClientId_) {
        if (_Account_ == null)
            throw new IllegalArgumentException();
        if (_Account_.length() > 256)
            throw new IllegalArgumentException();
        this._Account = _Account_;
        if (_ClientId_ == null)
            throw new IllegalArgumentException();
        if (_ClientId_.length() > 256)
            throw new IllegalArgumentException();
        this._ClientId = _ClientId_;
    }

    public String getAccount() {
        return _Account;
    }

    public String getClientId() {
        return _ClientId;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BAccountClientId: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Account=").append(getAccount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ClientId=").append(getClientId()).append(System.lineSeparator());
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
            String _x_ = getAccount();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getClientId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Account = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ClientId = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BAccountClientId))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAccountClientId)_o_;
        if (!getAccount().equals(_b_.getAccount()))
            return false;
        if (!getClientId().equals(_b_.getClientId()))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + getAccount().hashCode();
        _h_ = _h_ * _p_ + getClientId().hashCode();
        return _h_;
    }

    @Override
    public int compareTo(BAccountClientId _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = _Account.compareTo(_o_._Account);
            if (_c_ != 0)
                return _c_;
            _c_ = _ClientId.compareTo(_o_._ClientId);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean negativeCheck() {
        return false;
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        _Account = rs.getString(_parents_name_ + "Account");
        if (getAccount() == null)
            _Account = "";
        _ClientId = rs.getString(_parents_name_ + "ClientId");
        if (getClientId() == null)
            _ClientId = "";
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Account", getAccount());
        st.appendString(_parents_name_ + "ClientId", getClientId());
    }
}
