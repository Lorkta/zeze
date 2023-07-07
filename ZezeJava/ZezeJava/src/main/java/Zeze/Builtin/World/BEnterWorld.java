// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BEnterWorld extends Zeze.Transaction.Bean implements BEnterWorldReadOnly {
    public static final long TYPEID = -4883142059980084950L;

    private int _MapId;
    private long _MapInstanceId;
    private Zeze.Serialize.Vector3 _Position;
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.World.BPutData> _PriorityData; // 优先数据，服务器第一次进入的时候就跟随EnterWorld发送给客户端。

    @Override
    public int getMapId() {
        if (!isManaged())
            return _MapId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _MapId;
        var log = (Log__MapId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _MapId;
    }

    public void setMapId(int value) {
        if (!isManaged()) {
            _MapId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__MapId(this, 1, value));
    }

    @Override
    public long getMapInstanceId() {
        if (!isManaged())
            return _MapInstanceId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _MapInstanceId;
        var log = (Log__MapInstanceId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _MapInstanceId;
    }

    public void setMapInstanceId(long value) {
        if (!isManaged()) {
            _MapInstanceId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__MapInstanceId(this, 2, value));
    }

    @Override
    public Zeze.Serialize.Vector3 getPosition() {
        if (!isManaged())
            return _Position;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Position;
        var log = (Log__Position)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Position;
    }

    public void setPosition(Zeze.Serialize.Vector3 value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Position = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Position(this, 3, value));
    }

    public Zeze.Builtin.World.BPutData getPriorityData() {
        return _PriorityData.getValue();
    }

    public void setPriorityData(Zeze.Builtin.World.BPutData value) {
        _PriorityData.setValue(value);
    }

    @Override
    public Zeze.Builtin.World.BPutDataReadOnly getPriorityDataReadOnly() {
        return _PriorityData.getValue();
    }

    @SuppressWarnings("deprecation")
    public BEnterWorld() {
        _Position = Zeze.Serialize.Vector3.ZERO;
        _PriorityData = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.World.BPutData(), Zeze.Builtin.World.BPutData.class);
        _PriorityData.variableId(4);
    }

    @SuppressWarnings("deprecation")
    public BEnterWorld(int _MapId_, long _MapInstanceId_, Zeze.Serialize.Vector3 _Position_) {
        _MapId = _MapId_;
        _MapInstanceId = _MapInstanceId_;
        if (_Position_ == null)
            _Position_ = Zeze.Serialize.Vector3.ZERO;
        _Position = _Position_;
        _PriorityData = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.World.BPutData(), Zeze.Builtin.World.BPutData.class);
        _PriorityData.variableId(4);
    }

    @Override
    public void reset() {
        setMapId(0);
        setMapInstanceId(0);
        setPosition(Zeze.Serialize.Vector3.ZERO);
        _PriorityData.reset();
    }

    @Override
    public Zeze.Builtin.World.BEnterWorld.Data toData() {
        var data = new Zeze.Builtin.World.BEnterWorld.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BEnterWorld.Data)other);
    }

    public void assign(BEnterWorld.Data other) {
        setMapId(other._MapId);
        setMapInstanceId(other._MapInstanceId);
        setPosition(other._Position);
        Zeze.Builtin.World.BPutData data_PriorityData = new Zeze.Builtin.World.BPutData();
        data_PriorityData.assign(other._PriorityData);
        _PriorityData.setValue(data_PriorityData);
    }

    public void assign(BEnterWorld other) {
        setMapId(other.getMapId());
        setMapInstanceId(other.getMapInstanceId());
        setPosition(other.getPosition());
        _PriorityData.assign(other._PriorityData);
    }

    public BEnterWorld copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BEnterWorld copy() {
        var copy = new BEnterWorld();
        copy.assign(this);
        return copy;
    }

    public static void swap(BEnterWorld a, BEnterWorld b) {
        BEnterWorld save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__MapId extends Zeze.Transaction.Logs.LogInt {
        public Log__MapId(BEnterWorld bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BEnterWorld)getBelong())._MapId = value; }
    }

    private static final class Log__MapInstanceId extends Zeze.Transaction.Logs.LogLong {
        public Log__MapInstanceId(BEnterWorld bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BEnterWorld)getBelong())._MapInstanceId = value; }
    }

    private static final class Log__Position extends Zeze.Transaction.Logs.LogVector3 {
        public Log__Position(BEnterWorld bean, int varId, Zeze.Serialize.Vector3 value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BEnterWorld)getBelong())._Position = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BEnterWorld: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MapId=").append(getMapId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("MapInstanceId=").append(getMapInstanceId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Position=").append(getPosition()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PriorityData=").append(System.lineSeparator());
        _PriorityData.buildString(sb, level + 4);
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

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            int _x_ = getMapId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getMapInstanceId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getPosition();
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 4, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _PriorityData.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setMapId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setMapInstanceId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setPosition(_o_.ReadVector3(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadBean(_PriorityData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _PriorityData.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _PriorityData.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getMapId() < 0)
            return true;
        if (getMapInstanceId() < 0)
            return true;
        if (_PriorityData.negativeCheck())
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
                case 1: _MapId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _MapInstanceId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _Position = ((Zeze.Transaction.Logs.LogVector3)vlog).value; break;
                case 4: _PriorityData.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setMapId(rs.getInt(_parents_name_ + "MapId"));
        setMapInstanceId(rs.getLong(_parents_name_ + "MapInstanceId"));
        parents.add("Position");
        setPosition(Zeze.Serialize.Helper.decodeVector3(parents, rs));
        parents.remove(parents.size() - 1);
        parents.add("PriorityData");
        _PriorityData.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "MapId", getMapId());
        st.appendLong(_parents_name_ + "MapInstanceId", getMapInstanceId());
        parents.add("Position");
        Zeze.Serialize.Helper.encodeVector3(getPosition(), parents, st);
        parents.remove(parents.size() - 1);
        parents.add("PriorityData");
        _PriorityData.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4883142059980084950L;

    private int _MapId;
    private long _MapInstanceId;
    private Zeze.Serialize.Vector3 _Position;
    private Zeze.Builtin.World.BPutData.Data _PriorityData; // 优先数据，服务器第一次进入的时候就跟随EnterWorld发送给客户端。

    public int getMapId() {
        return _MapId;
    }

    public void setMapId(int value) {
        _MapId = value;
    }

    public long getMapInstanceId() {
        return _MapInstanceId;
    }

    public void setMapInstanceId(long value) {
        _MapInstanceId = value;
    }

    public Zeze.Serialize.Vector3 getPosition() {
        return _Position;
    }

    public void setPosition(Zeze.Serialize.Vector3 value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Position = value;
    }

    public Zeze.Builtin.World.BPutData.Data getPriorityData() {
        return _PriorityData;
    }

    public void setPriorityData(Zeze.Builtin.World.BPutData.Data value) {
        if (value == null)
            throw new IllegalArgumentException();
        _PriorityData = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Position = Zeze.Serialize.Vector3.ZERO;
        _PriorityData = new Zeze.Builtin.World.BPutData.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(int _MapId_, long _MapInstanceId_, Zeze.Serialize.Vector3 _Position_, Zeze.Builtin.World.BPutData.Data _PriorityData_) {
        _MapId = _MapId_;
        _MapInstanceId = _MapInstanceId_;
        if (_Position_ == null)
            _Position_ = Zeze.Serialize.Vector3.ZERO;
        _Position = _Position_;
        if (_PriorityData_ == null)
            _PriorityData_ = new Zeze.Builtin.World.BPutData.Data();
        _PriorityData = _PriorityData_;
    }

    @Override
    public void reset() {
        _MapId = 0;
        _MapInstanceId = 0;
        _Position = Zeze.Serialize.Vector3.ZERO;
        _PriorityData.reset();
    }

    @Override
    public Zeze.Builtin.World.BEnterWorld toBean() {
        var bean = new Zeze.Builtin.World.BEnterWorld();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BEnterWorld)other);
    }

    public void assign(BEnterWorld other) {
        _MapId = other.getMapId();
        _MapInstanceId = other.getMapInstanceId();
        _Position = other.getPosition();
        _PriorityData.assign(other._PriorityData.getValue());
    }

    public void assign(BEnterWorld.Data other) {
        _MapId = other._MapId;
        _MapInstanceId = other._MapInstanceId;
        _Position = other._Position;
        _PriorityData.assign(other._PriorityData);
    }

    @Override
    public BEnterWorld.Data copy() {
        var copy = new BEnterWorld.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BEnterWorld.Data a, BEnterWorld.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BEnterWorld.Data clone() {
        return (BEnterWorld.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BEnterWorld: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MapId=").append(_MapId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("MapInstanceId=").append(_MapInstanceId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Position=").append(_Position).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PriorityData=").append(System.lineSeparator());
        _PriorityData.buildString(sb, level + 4);
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

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            int _x_ = _MapId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = _MapInstanceId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Position;
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 4, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _PriorityData.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _MapId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _MapInstanceId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Position = _o_.ReadVector3(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadBean(_PriorityData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
