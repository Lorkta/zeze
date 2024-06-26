// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BReduceParam extends Zeze.Transaction.Bean implements BReduceParamReadOnly {
    public static final long TYPEID = -7052326232144455304L;

    private Zeze.Net.Binary _GlobalKey;
    private int _State;
    private Zeze.Util.Id128 _ReduceTid;

    @Override
    public Zeze.Net.Binary getGlobalKey() {
        if (!isManaged())
            return _GlobalKey;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _GlobalKey;
        var log = (Log__GlobalKey)txn.getLog(objectId() + 1);
        return log != null ? log.value : _GlobalKey;
    }

    public void setGlobalKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _GlobalKey = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__GlobalKey(this, 1, value));
    }

    @Override
    public int getState() {
        if (!isManaged())
            return _State;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _State;
        var log = (Log__State)txn.getLog(objectId() + 2);
        return log != null ? log.value : _State;
    }

    public void setState(int value) {
        if (!isManaged()) {
            _State = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__State(this, 2, value));
    }

    @Override
    public Zeze.Util.Id128 getReduceTid() {
        if (!isManaged())
            return _ReduceTid;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ReduceTid;
        var log = (Log__ReduceTid)txn.getLog(objectId() + 3);
        return log != null ? log.value : _ReduceTid;
    }

    public void setReduceTid(Zeze.Util.Id128 value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ReduceTid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ReduceTid(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BReduceParam() {
        _GlobalKey = Zeze.Net.Binary.Empty;
        _ReduceTid = new Zeze.Util.Id128();
    }

    @SuppressWarnings("deprecation")
    public BReduceParam(Zeze.Net.Binary _GlobalKey_, int _State_, Zeze.Util.Id128 _ReduceTid_) {
        if (_GlobalKey_ == null)
            _GlobalKey_ = Zeze.Net.Binary.Empty;
        _GlobalKey = _GlobalKey_;
        _State = _State_;
        if (_ReduceTid_ == null)
            _ReduceTid_ = new Zeze.Util.Id128();
        _ReduceTid = _ReduceTid_;
    }

    @Override
    public void reset() {
        setGlobalKey(Zeze.Net.Binary.Empty);
        setState(0);
        setReduceTid(new Zeze.Util.Id128());
        _unknown_ = null;
    }

    public void assign(BReduceParam other) {
        setGlobalKey(other.getGlobalKey());
        setState(other.getState());
        setReduceTid(other.getReduceTid());
        _unknown_ = other._unknown_;
    }

    public BReduceParam copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BReduceParam copy() {
        var copy = new BReduceParam();
        copy.assign(this);
        return copy;
    }

    public static void swap(BReduceParam a, BReduceParam b) {
        BReduceParam save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__GlobalKey extends Zeze.Transaction.Logs.LogBinary {
        public Log__GlobalKey(BReduceParam bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReduceParam)getBelong())._GlobalKey = value; }
    }

    private static final class Log__State extends Zeze.Transaction.Logs.LogInt {
        public Log__State(BReduceParam bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReduceParam)getBelong())._State = value; }
    }

    private static final class Log__ReduceTid extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Util.Id128> {
        public Log__ReduceTid(BReduceParam bean, int varId, Zeze.Util.Id128 value) { super(Zeze.Util.Id128.class, bean, varId, value); }

        @Override
        public void commit() { ((BReduceParam)getBelong())._ReduceTid = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.GlobalCacheManagerWithRaft.BReduceParam: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("GlobalKey=").append(getGlobalKey()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("State=").append(getState()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReduceTid=").append(System.lineSeparator());
        getReduceTid().buildString(sb, level + 4);
        sb.append(System.lineSeparator());
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
            var _x_ = getGlobalKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _x_ = getState();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 3, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getReduceTid().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
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
            setGlobalKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setState(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadBean(getReduceTid(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BReduceParam))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BReduceParam)_o_;
        if (!getGlobalKey().equals(_b_.getGlobalKey()))
            return false;
        if (getState() != _b_.getState())
            return false;
        if (!getReduceTid().equals(_b_.getReduceTid()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getState() < 0)
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
                case 1: _GlobalKey = vlog.binaryValue(); break;
                case 2: _State = vlog.intValue(); break;
                case 3: _ReduceTid = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Util.Id128>)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setGlobalKey(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "GlobalKey")));
        setState(rs.getInt(_parents_name_ + "State"));
        parents.add("ReduceTid");
        getReduceTid().decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBinary(_parents_name_ + "GlobalKey", getGlobalKey());
        st.appendInt(_parents_name_ + "State", getState());
        parents.add("ReduceTid");
        getReduceTid().encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "GlobalKey", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "State", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ReduceTid", "Zeze.Util.Id128", "", ""));
        return vars;
    }
}
