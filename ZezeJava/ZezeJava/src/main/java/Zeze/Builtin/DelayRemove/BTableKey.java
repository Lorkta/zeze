// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTableKey extends Zeze.Transaction.Bean implements BTableKeyReadOnly {
    public static final long TYPEID = 6060766480176216446L;

    private String _TableName;
    private Zeze.Net.Binary _EncodedKey;
    private long _EnqueueTime;

    @Override
    public String getTableName() {
        if (!isManaged())
            return _TableName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TableName;
        var log = (Log__TableName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TableName;
    }

    public void setTableName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TableName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TableName(this, 1, value));
    }

    @Override
    public Zeze.Net.Binary getEncodedKey() {
        if (!isManaged())
            return _EncodedKey;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _EncodedKey;
        var log = (Log__EncodedKey)txn.getLog(objectId() + 2);
        return log != null ? log.value : _EncodedKey;
    }

    public void setEncodedKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _EncodedKey = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__EncodedKey(this, 2, value));
    }

    @Override
    public long getEnqueueTime() {
        if (!isManaged())
            return _EnqueueTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _EnqueueTime;
        var log = (Log__EnqueueTime)txn.getLog(objectId() + 3);
        return log != null ? log.value : _EnqueueTime;
    }

    public void setEnqueueTime(long value) {
        if (!isManaged()) {
            _EnqueueTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__EnqueueTime(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BTableKey() {
        _TableName = "";
        _EncodedKey = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BTableKey(String _TableName_, Zeze.Net.Binary _EncodedKey_, long _EnqueueTime_) {
        if (_TableName_ == null)
            throw new IllegalArgumentException();
        _TableName = _TableName_;
        if (_EncodedKey_ == null)
            throw new IllegalArgumentException();
        _EncodedKey = _EncodedKey_;
        _EnqueueTime = _EnqueueTime_;
    }

    public void assign(BTableKey other) {
        setTableName(other.getTableName());
        setEncodedKey(other.getEncodedKey());
        setEnqueueTime(other.getEnqueueTime());
    }

    public BTableKey copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTableKey copy() {
        var copy = new BTableKey();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTableKey a, BTableKey b) {
        BTableKey save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TableName extends Zeze.Transaction.Logs.LogString {
        public Log__TableName(BTableKey bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTableKey)getBelong())._TableName = value; }
    }

    private static final class Log__EncodedKey extends Zeze.Transaction.Logs.LogBinary {
        public Log__EncodedKey(BTableKey bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTableKey)getBelong())._EncodedKey = value; }
    }

    private static final class Log__EnqueueTime extends Zeze.Transaction.Logs.LogLong {
        public Log__EnqueueTime(BTableKey bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTableKey)getBelong())._EnqueueTime = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.DelayRemove.BTableKey: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TableName=").append(getTableName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EncodedKey=").append(getEncodedKey()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EnqueueTime=").append(getEnqueueTime()).append(System.lineSeparator());
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
            String _x_ = getTableName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getEncodedKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = getEnqueueTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            setTableName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setEncodedKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setEnqueueTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getEnqueueTime() < 0)
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
                case 1: _TableName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _EncodedKey = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 3: _EnqueueTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTableName(rs.getString(_parents_name_ + "TableName"));
        setEncodedKey(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "EncodedKey")));
        setEnqueueTime(rs.getLong(_parents_name_ + "EnqueueTime"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "TableName", getTableName());
        st.appendBinary(_parents_name_ + "EncodedKey", getEncodedKey());
        st.appendLong(_parents_name_ + "EnqueueTime", getEnqueueTime());
    }
}
