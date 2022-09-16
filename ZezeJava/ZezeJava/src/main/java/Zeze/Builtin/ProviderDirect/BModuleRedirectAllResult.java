// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BModuleRedirectAllResult extends Zeze.Transaction.Bean {
    private int _ModuleId;
    private int _ServerId; // 目标server的id。
    private long _SourceProvider; // 从BModuleRedirectAllRequest里面得到。
    private String _MethodFullName; // format="ModuleFullName:MethodName"
    private long _SessionId; // 发起请求者初始化，返回结果时带回。
    private final Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash> _Hashs; // 发送给具体进程时需要处理的分组hash-index（目前由linkd填写）

    public int getModuleId() {
        if (!isManaged())
            return _ModuleId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ModuleId;
        var log = (Log__ModuleId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ModuleId;
    }

    public void setModuleId(int value) {
        if (!isManaged()) {
            _ModuleId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ModuleId(this, 1, value));
    }

    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServerId;
        var log = (Log__ServerId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int value) {
        if (!isManaged()) {
            _ServerId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServerId(this, 2, value));
    }

    public long getSourceProvider() {
        if (!isManaged())
            return _SourceProvider;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SourceProvider;
        var log = (Log__SourceProvider)txn.getLog(objectId() + 3);
        return log != null ? log.value : _SourceProvider;
    }

    public void setSourceProvider(long value) {
        if (!isManaged()) {
            _SourceProvider = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__SourceProvider(this, 3, value));
    }

    public String getMethodFullName() {
        if (!isManaged())
            return _MethodFullName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _MethodFullName;
        var log = (Log__MethodFullName)txn.getLog(objectId() + 4);
        return log != null ? log.value : _MethodFullName;
    }

    public void setMethodFullName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _MethodFullName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__MethodFullName(this, 4, value));
    }

    public long getSessionId() {
        if (!isManaged())
            return _SessionId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SessionId;
        var log = (Log__SessionId)txn.getLog(objectId() + 5);
        return log != null ? log.value : _SessionId;
    }

    public void setSessionId(long value) {
        if (!isManaged()) {
            _SessionId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__SessionId(this, 5, value));
    }

    public Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash> getHashs() {
        return _Hashs;
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectAllResult() {
        _MethodFullName = "";
        _Hashs = new Zeze.Transaction.Collections.PMap2<>(Integer.class, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.class);
        _Hashs.variableId(6);
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectAllResult(int _ModuleId_, int _ServerId_, long _SourceProvider_, String _MethodFullName_, long _SessionId_) {
        _ModuleId = _ModuleId_;
        _ServerId = _ServerId_;
        _SourceProvider = _SourceProvider_;
        if (_MethodFullName_ == null)
            throw new IllegalArgumentException();
        _MethodFullName = _MethodFullName_;
        _SessionId = _SessionId_;
        _Hashs = new Zeze.Transaction.Collections.PMap2<>(Integer.class, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.class);
        _Hashs.variableId(6);
    }

    public void assign(BModuleRedirectAllResult other) {
        setModuleId(other.getModuleId());
        setServerId(other.getServerId());
        setSourceProvider(other.getSourceProvider());
        setMethodFullName(other.getMethodFullName());
        setSessionId(other.getSessionId());
        getHashs().clear();
        for (var e : other.getHashs().entrySet())
            getHashs().put(e.getKey(), e.getValue().copy());
    }

    @Deprecated
    public void Assign(BModuleRedirectAllResult other) {
        assign(other);
    }

    public BModuleRedirectAllResult copyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BModuleRedirectAllResult copy() {
        var copy = new BModuleRedirectAllResult();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BModuleRedirectAllResult Copy() {
        return copy();
    }

    public static void swap(BModuleRedirectAllResult a, BModuleRedirectAllResult b) {
        BModuleRedirectAllResult save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public BModuleRedirectAllResult copyBean() {
        return Copy();
    }

    public static final long TYPEID = -6979067915808179070L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ModuleId extends Zeze.Transaction.Logs.LogInt {
        public Log__ModuleId(BModuleRedirectAllResult bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllResult)getBelong())._ModuleId = value; }
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BModuleRedirectAllResult bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllResult)getBelong())._ServerId = value; }
    }

    private static final class Log__SourceProvider extends Zeze.Transaction.Logs.LogLong {
        public Log__SourceProvider(BModuleRedirectAllResult bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllResult)getBelong())._SourceProvider = value; }
    }

    private static final class Log__MethodFullName extends Zeze.Transaction.Logs.LogString {
        public Log__MethodFullName(BModuleRedirectAllResult bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllResult)getBelong())._MethodFullName = value; }
    }

    private static final class Log__SessionId extends Zeze.Transaction.Logs.LogLong {
        public Log__SessionId(BModuleRedirectAllResult bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllResult)getBelong())._SessionId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ModuleId").append('=').append(getModuleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId").append('=').append(getServerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SourceProvider").append('=').append(getSourceProvider()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("MethodFullName").append('=').append(getMethodFullName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SessionId").append('=').append(getSessionId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Hashs").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getHashs().entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().buildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(System.lineSeparator());
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
            int _x_ = getModuleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getSourceProvider();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getMethodFullName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getSessionId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getHashs();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                }
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setModuleId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setSourceProvider(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setMethodFullName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setSessionId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            var _x_ = getHashs();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Hashs.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _Hashs.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getModuleId() < 0)
            return true;
        if (getServerId() < 0)
            return true;
        if (getSourceProvider() < 0)
            return true;
        if (getSessionId() < 0)
            return true;
        for (var _v_ : getHashs().values()) {
            if (_v_.negativeCheck())
                return true;
        }
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
                case 1: _ModuleId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _ServerId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _SourceProvider = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _MethodFullName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 5: _SessionId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 6: _Hashs.followerApply(vlog); break;
            }
        }
    }
}
