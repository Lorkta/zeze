// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BBatchTid extends Zeze.Transaction.Bean implements BBatchTidReadOnly {
    public static final long TYPEID = -8862994320894252651L;

    private Zeze.Net.Binary _Tid;

    @Override
    public Zeze.Net.Binary getTid() {
        if (!isManaged())
            return _Tid;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Tid;
        var log = (Log__Tid)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Tid;
    }

    public void setTid(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Tid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Tid(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BBatchTid() {
        _Tid = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BBatchTid(Zeze.Net.Binary _Tid_) {
        if (_Tid_ == null)
            _Tid_ = Zeze.Net.Binary.Empty;
        _Tid = _Tid_;
    }

    @Override
    public void reset() {
        setTid(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.BBatchTid.Data toData() {
        var data = new Zeze.Builtin.Dbh2.BBatchTid.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BBatchTid.Data)other);
    }

    public void assign(BBatchTid.Data other) {
        setTid(other._Tid);
        _unknown_ = null;
    }

    public void assign(BBatchTid other) {
        setTid(other.getTid());
        _unknown_ = other._unknown_;
    }

    public BBatchTid copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBatchTid copy() {
        var copy = new BBatchTid();
        copy.assign(this);
        return copy;
    }

    public static void swap(BBatchTid a, BBatchTid b) {
        BBatchTid save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Tid extends Zeze.Transaction.Logs.LogBinary {
        public Log__Tid(BBatchTid bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBatchTid)getBelong())._Tid = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BBatchTid: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Tid=").append(getTid()).append(System.lineSeparator());
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
            var _x_ = getTid();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setTid(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
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
                case 1: _Tid = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTid(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Tid")));
        if (getTid() == null)
            setTid(Zeze.Net.Binary.Empty);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBinary(_parents_name_ + "Tid", getTid());
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -8862994320894252651L;

    private Zeze.Net.Binary _Tid;

    public Zeze.Net.Binary getTid() {
        return _Tid;
    }

    public void setTid(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Tid = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Tid = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _Tid_) {
        if (_Tid_ == null)
            _Tid_ = Zeze.Net.Binary.Empty;
        _Tid = _Tid_;
    }

    @Override
    public void reset() {
        _Tid = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.Dbh2.BBatchTid toBean() {
        var bean = new Zeze.Builtin.Dbh2.BBatchTid();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BBatchTid)other);
    }

    public void assign(BBatchTid other) {
        _Tid = other.getTid();
    }

    public void assign(BBatchTid.Data other) {
        _Tid = other._Tid;
    }

    @Override
    public BBatchTid.Data copy() {
        var copy = new BBatchTid.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BBatchTid.Data a, BBatchTid.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BBatchTid.Data clone() {
        return (BBatchTid.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BBatchTid: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Tid=").append(_Tid).append(System.lineSeparator());
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
            var _x_ = _Tid;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
            _Tid = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
