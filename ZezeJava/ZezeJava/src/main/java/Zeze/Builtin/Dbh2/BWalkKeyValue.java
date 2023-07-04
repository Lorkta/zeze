// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BWalkKeyValue extends Zeze.Transaction.Bean implements BWalkKeyValueReadOnly {
    public static final long TYPEID = -7541456034311652913L;

    private Zeze.Net.Binary _Key;
    private Zeze.Net.Binary _Value;

    @Override
    public Zeze.Net.Binary getKey() {
        if (!isManaged())
            return _Key;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Key;
        var log = (Log__Key)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Key;
    }

    public void setKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Key = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Key(this, 1, value));
    }

    @Override
    public Zeze.Net.Binary getValue() {
        if (!isManaged())
            return _Value;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Value;
        var log = (Log__Value)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Value;
    }

    public void setValue(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Value = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Value(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BWalkKeyValue() {
        _Key = Zeze.Net.Binary.Empty;
        _Value = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BWalkKeyValue(Zeze.Net.Binary _Key_, Zeze.Net.Binary _Value_) {
        if (_Key_ == null)
            _Key_ = Zeze.Net.Binary.Empty;
        _Key = _Key_;
        if (_Value_ == null)
            _Value_ = Zeze.Net.Binary.Empty;
        _Value = _Value_;
    }

    @Override
    public void reset() {
        setKey(Zeze.Net.Binary.Empty);
        setValue(Zeze.Net.Binary.Empty);
    }

    @Override
    public Zeze.Builtin.Dbh2.BWalkKeyValue.Data toData() {
        var data = new Zeze.Builtin.Dbh2.BWalkKeyValue.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BWalkKeyValue.Data)other);
    }

    public void assign(BWalkKeyValue.Data other) {
        setKey(other._Key);
        setValue(other._Value);
    }

    public void assign(BWalkKeyValue other) {
        setKey(other.getKey());
        setValue(other.getValue());
    }

    public BWalkKeyValue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BWalkKeyValue copy() {
        var copy = new BWalkKeyValue();
        copy.assign(this);
        return copy;
    }

    public static void swap(BWalkKeyValue a, BWalkKeyValue b) {
        BWalkKeyValue save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Key extends Zeze.Transaction.Logs.LogBinary {
        public Log__Key(BWalkKeyValue bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BWalkKeyValue)getBelong())._Key = value; }
    }

    private static final class Log__Value extends Zeze.Transaction.Logs.LogBinary {
        public Log__Value(BWalkKeyValue bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BWalkKeyValue)getBelong())._Value = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BWalkKeyValue: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(getKey()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(getValue()).append(System.lineSeparator());
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
            var _x_ = getKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = getValue();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setValue(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
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
                case 1: _Key = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 2: _Value = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setKey(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Key")));
        if (getKey() == null)
            setKey(Zeze.Net.Binary.Empty);
        setValue(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Value")));
        if (getValue() == null)
            setValue(Zeze.Net.Binary.Empty);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBinary(_parents_name_ + "Key", getKey());
        st.appendBinary(_parents_name_ + "Value", getValue());
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -7541456034311652913L;

    private Zeze.Net.Binary _Key;
    private Zeze.Net.Binary _Value;

    public Zeze.Net.Binary getKey() {
        return _Key;
    }

    public void setKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Key = value;
    }

    public Zeze.Net.Binary getValue() {
        return _Value;
    }

    public void setValue(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Value = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Key = Zeze.Net.Binary.Empty;
        _Value = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _Key_, Zeze.Net.Binary _Value_) {
        if (_Key_ == null)
            _Key_ = Zeze.Net.Binary.Empty;
        _Key = _Key_;
        if (_Value_ == null)
            _Value_ = Zeze.Net.Binary.Empty;
        _Value = _Value_;
    }

    @Override
    public void reset() {
        _Key = Zeze.Net.Binary.Empty;
        _Value = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.Dbh2.BWalkKeyValue toBean() {
        var bean = new Zeze.Builtin.Dbh2.BWalkKeyValue();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BWalkKeyValue)other);
    }

    public void assign(BWalkKeyValue other) {
        _Key = other.getKey();
        _Value = other.getValue();
    }

    public void assign(BWalkKeyValue.Data other) {
        _Key = other._Key;
        _Value = other._Value;
    }

    @Override
    public BWalkKeyValue.Data copy() {
        var copy = new BWalkKeyValue.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BWalkKeyValue.Data a, BWalkKeyValue.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BWalkKeyValue.Data clone() {
        return (BWalkKeyValue.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BWalkKeyValue: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_Key).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_Value).append(System.lineSeparator());
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
            var _x_ = _Key;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _Value;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Key = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Value = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
