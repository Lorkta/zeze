// auto-generated rocks @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BServiceInfoRocks extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = 4382889566588005308L;

    private String _ServiceName;
    private String _ServiceIdentity;
    private String _PassiveIp;
    private int _PassivePort;
    private Zeze.Net.Binary _ExtraInfo;
    private Zeze.Net.Binary _Param;
    private String _SessionName;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

    public String getServiceName() {
        if (!isManaged())
            return _ServiceName;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _ServiceName;
        var log = txn.getLog(objectId() + 1);
        if (log == null)
            return _ServiceName;
        return ((Zeze.Raft.RocksRaft.Log1.LogString)log).value;
    }

    public void setServiceName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceName = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogString(this, 1, value));
    }

    public String getServiceIdentity() {
        if (!isManaged())
            return _ServiceIdentity;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _ServiceIdentity;
        var log = txn.getLog(objectId() + 2);
        if (log == null)
            return _ServiceIdentity;
        return ((Zeze.Raft.RocksRaft.Log1.LogString)log).value;
    }

    public void setServiceIdentity(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceIdentity = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogString(this, 2, value));
    }

    public String getPassiveIp() {
        if (!isManaged())
            return _PassiveIp;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _PassiveIp;
        var log = txn.getLog(objectId() + 3);
        if (log == null)
            return _PassiveIp;
        return ((Zeze.Raft.RocksRaft.Log1.LogString)log).value;
    }

    public void setPassiveIp(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _PassiveIp = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogString(this, 3, value));
    }

    public int getPassivePort() {
        if (!isManaged())
            return _PassivePort;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _PassivePort;
        var log = txn.getLog(objectId() + 4);
        if (log == null)
            return _PassivePort;
        return ((Zeze.Raft.RocksRaft.Log1.LogInt)log).value;
    }

    public void setPassivePort(int value) {
        if (!isManaged()) {
            _PassivePort = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogInt(this, 4, value));
    }

    public Zeze.Net.Binary getExtraInfo() {
        if (!isManaged())
            return _ExtraInfo;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _ExtraInfo;
        var log = txn.getLog(objectId() + 5);
        if (log == null)
            return _ExtraInfo;
        return ((Zeze.Raft.RocksRaft.Log1.LogBinary)log).value;
    }

    public void setExtraInfo(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ExtraInfo = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogBinary(this, 5, value));
    }

    public Zeze.Net.Binary getParam() {
        if (!isManaged())
            return _Param;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _Param;
        var log = txn.getLog(objectId() + 6);
        if (log == null)
            return _Param;
        return ((Zeze.Raft.RocksRaft.Log1.LogBinary)log).value;
    }

    public void setParam(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Param = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogBinary(this, 6, value));
    }

    public String getSessionName() {
        if (!isManaged())
            return _SessionName;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _SessionName;
        var log = txn.getLog(objectId() + 7);
        if (log == null)
            return _SessionName;
        return ((Zeze.Raft.RocksRaft.Log1.LogString)log).value;
    }

    public void setSessionName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _SessionName = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogString(this, 7, value));
    }

    public BServiceInfoRocks() {
        _ServiceName = "";
        _ServiceIdentity = "";
        _PassiveIp = "";
        _ExtraInfo = Zeze.Net.Binary.Empty;
        _Param = Zeze.Net.Binary.Empty;
        _SessionName = "";
    }

    public BServiceInfoRocks(String _ServiceName_, String _ServiceIdentity_, String _PassiveIp_, int _PassivePort_, Zeze.Net.Binary _ExtraInfo_, Zeze.Net.Binary _Param_, String _SessionName_) {
        if (_ServiceName_ == null)
            throw new IllegalArgumentException();
        _ServiceName = _ServiceName_;
        if (_ServiceIdentity_ == null)
            throw new IllegalArgumentException();
        _ServiceIdentity = _ServiceIdentity_;
        if (_PassiveIp_ == null)
            throw new IllegalArgumentException();
        _PassiveIp = _PassiveIp_;
        _PassivePort = _PassivePort_;
        if (_ExtraInfo_ == null)
            throw new IllegalArgumentException();
        _ExtraInfo = _ExtraInfo_;
        if (_Param_ == null)
            throw new IllegalArgumentException();
        _Param = _Param_;
        if (_SessionName_ == null)
            throw new IllegalArgumentException();
        _SessionName = _SessionName_;
    }

    public void assign(BServiceInfoRocks other) {
        setServiceName(other.getServiceName());
        setServiceIdentity(other.getServiceIdentity());
        setPassiveIp(other.getPassiveIp());
        setPassivePort(other.getPassivePort());
        setExtraInfo(other.getExtraInfo());
        setParam(other.getParam());
        setSessionName(other.getSessionName());
    }

    @Deprecated
    public void Assign(BServiceInfoRocks other) {
        assign(other);
    }

    public BServiceInfoRocks copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BServiceInfoRocks copy() {
        var copy = new BServiceInfoRocks();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BServiceInfoRocks Copy() {
        return copy();
    }

    public static void swap(BServiceInfoRocks a, BServiceInfoRocks b) {
        BServiceInfoRocks save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoRocks: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceName").append('=').append(getServiceName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceIdentity").append('=').append(getServiceIdentity()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PassiveIp").append('=').append(getPassiveIp()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PassivePort").append('=').append(getPassivePort()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ExtraInfo").append('=').append(getExtraInfo()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Param").append('=').append(getParam()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SessionName").append('=').append(getSessionName()).append(System.lineSeparator());
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
            String _x_ = getServiceName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getServiceIdentity();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getPassiveIp();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getPassivePort();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getExtraInfo();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = getParam();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = getSessionName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.BYTES);
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
            _ServiceName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ServiceIdentity = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _PassiveIp = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _PassivePort = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _ExtraInfo = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _Param = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            _SessionName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Raft.RocksRaft.Record.RootInfo root) {
    }

    @Override
    public void leaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {
        switch (vlog.getVariableId()) {
            case 1: _ServiceName = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
            case 2: _ServiceIdentity = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
            case 3: _PassiveIp = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
            case 4: _PassivePort = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).value; break;
            case 5: _ExtraInfo = ((Zeze.Raft.RocksRaft.Log1.LogBinary)vlog).value; break;
            case 6: _Param = ((Zeze.Raft.RocksRaft.Log1.LogBinary)vlog).value; break;
            case 7: _SessionName = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
        }
    }

    @Override
    public void followerApply(Zeze.Raft.RocksRaft.Log log) {
        var vars = ((Zeze.Raft.RocksRaft.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _ServiceName = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
                case 2: _ServiceIdentity = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
                case 3: _PassiveIp = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
                case 4: _PassivePort = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).value; break;
                case 5: _ExtraInfo = ((Zeze.Raft.RocksRaft.Log1.LogBinary)vlog).value; break;
                case 6: _Param = ((Zeze.Raft.RocksRaft.Log1.LogBinary)vlog).value; break;
                case 7: _SessionName = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
            }
        }
    }
}
