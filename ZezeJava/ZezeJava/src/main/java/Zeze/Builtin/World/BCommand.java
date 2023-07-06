// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

// 抽象的交互协议，用来封装任意客户端服务器Protocol,Rpc
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BCommand extends Zeze.Transaction.Bean implements BCommandReadOnly {
    public static final long TYPEID = 3225161952412454913L;

    public static final int eReserveCommandId = 2000; // 保留Id给组件内部用。自定义的必须大于这个值。
    public static final int eMoveMmo = 0;

    private int _CommandId;
    private Zeze.Net.Binary _Param;

    @Override
    public int getCommandId() {
        if (!isManaged())
            return _CommandId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _CommandId;
        var log = (Log__CommandId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _CommandId;
    }

    public void setCommandId(int value) {
        if (!isManaged()) {
            _CommandId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__CommandId(this, 1, value));
    }

    @Override
    public Zeze.Net.Binary getParam() {
        if (!isManaged())
            return _Param;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Param;
        var log = (Log__Param)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Param;
    }

    public void setParam(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Param = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Param(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BCommand() {
        _Param = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BCommand(int _CommandId_, Zeze.Net.Binary _Param_) {
        _CommandId = _CommandId_;
        if (_Param_ == null)
            _Param_ = Zeze.Net.Binary.Empty;
        _Param = _Param_;
    }

    @Override
    public void reset() {
        setCommandId(0);
        setParam(Zeze.Net.Binary.Empty);
    }

    @Override
    public Zeze.Builtin.World.BCommand.Data toData() {
        var data = new Zeze.Builtin.World.BCommand.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BCommand.Data)other);
    }

    public void assign(BCommand.Data other) {
        setCommandId(other._CommandId);
        setParam(other._Param);
    }

    public void assign(BCommand other) {
        setCommandId(other.getCommandId());
        setParam(other.getParam());
    }

    public BCommand copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCommand copy() {
        var copy = new BCommand();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCommand a, BCommand b) {
        BCommand save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__CommandId extends Zeze.Transaction.Logs.LogInt {
        public Log__CommandId(BCommand bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCommand)getBelong())._CommandId = value; }
    }

    private static final class Log__Param extends Zeze.Transaction.Logs.LogBinary {
        public Log__Param(BCommand bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCommand)getBelong())._Param = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BCommand: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CommandId=").append(getCommandId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Param=").append(getParam()).append(System.lineSeparator());
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
            int _x_ = getCommandId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getParam();
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
            setCommandId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setParam(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getCommandId() < 0)
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
                case 1: _CommandId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _Param = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setCommandId(rs.getInt(_parents_name_ + "CommandId"));
        setParam(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Param")));
        if (getParam() == null)
            setParam(Zeze.Net.Binary.Empty);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "CommandId", getCommandId());
        st.appendBinary(_parents_name_ + "Param", getParam());
    }

// 抽象的交互协议，用来封装任意客户端服务器Protocol,Rpc
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 3225161952412454913L;

    public static final int eReserveCommandId = 2000; // 保留Id给组件内部用。自定义的必须大于这个值。
    public static final int eMoveMmo = 0;

    private int _CommandId;
    private Zeze.Net.Binary _Param;

    public int getCommandId() {
        return _CommandId;
    }

    public void setCommandId(int value) {
        _CommandId = value;
    }

    public Zeze.Net.Binary getParam() {
        return _Param;
    }

    public void setParam(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Param = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Param = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(int _CommandId_, Zeze.Net.Binary _Param_) {
        _CommandId = _CommandId_;
        if (_Param_ == null)
            _Param_ = Zeze.Net.Binary.Empty;
        _Param = _Param_;
    }

    @Override
    public void reset() {
        _CommandId = 0;
        _Param = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.World.BCommand toBean() {
        var bean = new Zeze.Builtin.World.BCommand();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BCommand)other);
    }

    public void assign(BCommand other) {
        _CommandId = other.getCommandId();
        _Param = other.getParam();
    }

    public void assign(BCommand.Data other) {
        _CommandId = other._CommandId;
        _Param = other._Param;
    }

    @Override
    public BCommand.Data copy() {
        var copy = new BCommand.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCommand.Data a, BCommand.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BCommand.Data clone() {
        return (BCommand.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BCommand: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CommandId=").append(_CommandId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Param=").append(_Param).append(System.lineSeparator());
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
            int _x_ = _CommandId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Param;
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
            _CommandId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Param = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}