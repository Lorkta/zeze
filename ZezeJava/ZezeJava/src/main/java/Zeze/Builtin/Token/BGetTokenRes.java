// auto-generated @formatter:off
package Zeze.Builtin.Token;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BGetTokenRes extends Zeze.Transaction.Bean implements BGetTokenResReadOnly {
    public static final long TYPEID = 4780430105301681046L;

    private Zeze.Net.Binary _context; // token绑定的自定义上下文. token已失效(状态已清除)时为空
    private long _count; // 此token已被GetToken访问的次数(包括当前访问). token已失效(状态已清除)时为0
    private long _time; // 此token已存活时间(毫秒). token已失效(状态已清除)时为-1

    @Override
    public Zeze.Net.Binary getContext() {
        if (!isManaged())
            return _context;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _context;
        var log = (Log__context)txn.getLog(objectId() + 1);
        return log != null ? log.value : _context;
    }

    public void setContext(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _context = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__context(this, 1, value));
    }

    @Override
    public long getCount() {
        if (!isManaged())
            return _count;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _count;
        var log = (Log__count)txn.getLog(objectId() + 2);
        return log != null ? log.value : _count;
    }

    public void setCount(long value) {
        if (!isManaged()) {
            _count = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__count(this, 2, value));
    }

    @Override
    public long getTime() {
        if (!isManaged())
            return _time;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _time;
        var log = (Log__time)txn.getLog(objectId() + 3);
        return log != null ? log.value : _time;
    }

    public void setTime(long value) {
        if (!isManaged()) {
            _time = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__time(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BGetTokenRes() {
        _context = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BGetTokenRes(Zeze.Net.Binary _context_, long _count_, long _time_) {
        if (_context_ == null)
            _context_ = Zeze.Net.Binary.Empty;
        _context = _context_;
        _count = _count_;
        _time = _time_;
    }

    @Override
    public Zeze.Builtin.Token.BGetTokenRes.Data toData() {
        var data = new Zeze.Builtin.Token.BGetTokenRes.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Token.BGetTokenRes.Data)other);
    }

    public void assign(BGetTokenRes.Data other) {
        setContext(other._context);
        setCount(other._count);
        setTime(other._time);
    }

    public void assign(BGetTokenRes other) {
        setContext(other.getContext());
        setCount(other.getCount());
        setTime(other.getTime());
    }

    public BGetTokenRes copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BGetTokenRes copy() {
        var copy = new BGetTokenRes();
        copy.assign(this);
        return copy;
    }

    public static void swap(BGetTokenRes a, BGetTokenRes b) {
        BGetTokenRes save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__context extends Zeze.Transaction.Logs.LogBinary {
        public Log__context(BGetTokenRes bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BGetTokenRes)getBelong())._context = value; }
    }

    private static final class Log__count extends Zeze.Transaction.Logs.LogLong {
        public Log__count(BGetTokenRes bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BGetTokenRes)getBelong())._count = value; }
    }

    private static final class Log__time extends Zeze.Transaction.Logs.LogLong {
        public Log__time(BGetTokenRes bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BGetTokenRes)getBelong())._time = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Token.BGetTokenRes: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("context=").append(getContext()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("count=").append(getCount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("time=").append(getTime()).append(System.lineSeparator());
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
            var _x_ = getContext();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = getCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getTime();
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
            setContext(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getCount() < 0)
            return true;
        if (getTime() < 0)
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
                case 1: _context = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 2: _count = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _time = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setContext(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "context")));
        if (getContext() == null)
            setContext(Zeze.Net.Binary.Empty);
        setCount(rs.getLong(_parents_name_ + "count"));
        setTime(rs.getLong(_parents_name_ + "time"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBinary(_parents_name_ + "context", getContext());
        st.appendLong(_parents_name_ + "count", getCount());
        st.appendLong(_parents_name_ + "time", getTime());
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4780430105301681046L;

    private Zeze.Net.Binary _context; // token绑定的自定义上下文. token已失效(状态已清除)时为空
    private long _count; // 此token已被GetToken访问的次数(包括当前访问). token已失效(状态已清除)时为0
    private long _time; // 此token已存活时间(毫秒). token已失效(状态已清除)时为-1

    public Zeze.Net.Binary getContext() {
        return _context;
    }

    public void setContext(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _context = value;
    }

    public long getCount() {
        return _count;
    }

    public void setCount(long value) {
        _count = value;
    }

    public long getTime() {
        return _time;
    }

    public void setTime(long value) {
        _time = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _context = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _context_, long _count_, long _time_) {
        if (_context_ == null)
            _context_ = Zeze.Net.Binary.Empty;
        _context = _context_;
        _count = _count_;
        _time = _time_;
    }

    @Override
    public Zeze.Builtin.Token.BGetTokenRes toBean() {
        var bean = new Zeze.Builtin.Token.BGetTokenRes();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BGetTokenRes)other);
    }

    public void assign(BGetTokenRes other) {
        _context = other.getContext();
        _count = other.getCount();
        _time = other.getTime();
    }

    public void assign(BGetTokenRes.Data other) {
        _context = other._context;
        _count = other._count;
        _time = other._time;
    }

    @Override
    public BGetTokenRes.Data copy() {
        var copy = new BGetTokenRes.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BGetTokenRes.Data a, BGetTokenRes.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BGetTokenRes.Data clone() {
        return (BGetTokenRes.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Token.BGetTokenRes: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("context=").append(_context).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("count=").append(_count).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("time=").append(_time).append(System.lineSeparator());
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
            var _x_ = _context;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = _count;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = _time;
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
            _context = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _count = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _time = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
