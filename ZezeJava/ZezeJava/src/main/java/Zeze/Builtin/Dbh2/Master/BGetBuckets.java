// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BGetBuckets extends Zeze.Transaction.Bean implements BGetBucketsReadOnly {
    public static final long TYPEID = 2441476428484688763L;

    private String _Database;
    private String _Table;

    @Override
    public String getDatabase() {
        if (!isManaged())
            return _Database;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Database;
        var log = (Log__Database)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Database;
    }

    public void setDatabase(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Database = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Database(this, 1, value));
    }

    @Override
    public String getTable() {
        if (!isManaged())
            return _Table;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Table;
        var log = (Log__Table)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Table;
    }

    public void setTable(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Table = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Table(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BGetBuckets() {
        _Database = "";
        _Table = "";
    }

    @SuppressWarnings("deprecation")
    public BGetBuckets(String _Database_, String _Table_) {
        if (_Database_ == null)
            _Database_ = "";
        _Database = _Database_;
        if (_Table_ == null)
            _Table_ = "";
        _Table = _Table_;
    }

    @Override
    public void reset() {
        setDatabase("");
        setTable("");
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BGetBuckets.Data toData() {
        var data = new Zeze.Builtin.Dbh2.Master.BGetBuckets.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.Master.BGetBuckets.Data)other);
    }

    public void assign(BGetBuckets.Data other) {
        setDatabase(other._Database);
        setTable(other._Table);
    }

    public void assign(BGetBuckets other) {
        setDatabase(other.getDatabase());
        setTable(other.getTable());
    }

    public BGetBuckets copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BGetBuckets copy() {
        var copy = new BGetBuckets();
        copy.assign(this);
        return copy;
    }

    public static void swap(BGetBuckets a, BGetBuckets b) {
        BGetBuckets save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Database extends Zeze.Transaction.Logs.LogString {
        public Log__Database(BGetBuckets bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BGetBuckets)getBelong())._Database = value; }
    }

    private static final class Log__Table extends Zeze.Transaction.Logs.LogString {
        public Log__Table(BGetBuckets bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BGetBuckets)getBelong())._Table = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BGetBuckets: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Database=").append(getDatabase()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Table=").append(getTable()).append(System.lineSeparator());
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
            String _x_ = getDatabase();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getTable();
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
            setDatabase(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTable(_o_.ReadString(_t_));
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
                case 1: _Database = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _Table = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setDatabase(rs.getString(_parents_name_ + "Database"));
        if (getDatabase() == null)
            setDatabase("");
        setTable(rs.getString(_parents_name_ + "Table"));
        if (getTable() == null)
            setTable("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Database", getDatabase());
        st.appendString(_parents_name_ + "Table", getTable());
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 2441476428484688763L;

    private String _Database;
    private String _Table;

    public String getDatabase() {
        return _Database;
    }

    public void setDatabase(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Database = value;
    }

    public String getTable() {
        return _Table;
    }

    public void setTable(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Table = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Database = "";
        _Table = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _Database_, String _Table_) {
        if (_Database_ == null)
            _Database_ = "";
        _Database = _Database_;
        if (_Table_ == null)
            _Table_ = "";
        _Table = _Table_;
    }

    @Override
    public void reset() {
        _Database = "";
        _Table = "";
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BGetBuckets toBean() {
        var bean = new Zeze.Builtin.Dbh2.Master.BGetBuckets();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BGetBuckets)other);
    }

    public void assign(BGetBuckets other) {
        _Database = other.getDatabase();
        _Table = other.getTable();
    }

    public void assign(BGetBuckets.Data other) {
        _Database = other._Database;
        _Table = other._Table;
    }

    @Override
    public BGetBuckets.Data copy() {
        var copy = new BGetBuckets.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BGetBuckets.Data a, BGetBuckets.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BGetBuckets.Data clone() {
        return (BGetBuckets.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BGetBuckets: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Database=").append(_Database).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Table=").append(_Table).append(System.lineSeparator());
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
            String _x_ = _Database;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _Table;
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
            _Database = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Table = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
