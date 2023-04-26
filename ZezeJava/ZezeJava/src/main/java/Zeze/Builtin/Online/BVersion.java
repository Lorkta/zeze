// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BVersion extends Zeze.Transaction.Bean implements BVersionReadOnly {
    public static final long TYPEID = 4746858989717188809L;

    private long _LoginVersion;
    private final Zeze.Transaction.Collections.PSet1<String> _ReliableNotifyMark;
    private long _ReliableNotifyIndex;
    private long _ReliableNotifyConfirmIndex;
    private int _ServerId;
    private long _LogoutVersion;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

    @Override
    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LoginVersion;
        var log = (Log__LoginVersion)txn.getLog(objectId() + 1);
        return log != null ? log.value : _LoginVersion;
    }

    public void setLoginVersion(long value) {
        if (!isManaged()) {
            _LoginVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LoginVersion(this, 1, value));
    }

    public Zeze.Transaction.Collections.PSet1<String> getReliableNotifyMark() {
        return _ReliableNotifyMark;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<String> getReliableNotifyMarkReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_ReliableNotifyMark);
    }

    @Override
    public long getReliableNotifyIndex() {
        if (!isManaged())
            return _ReliableNotifyIndex;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ReliableNotifyIndex;
        var log = (Log__ReliableNotifyIndex)txn.getLog(objectId() + 3);
        return log != null ? log.value : _ReliableNotifyIndex;
    }

    public void setReliableNotifyIndex(long value) {
        if (!isManaged()) {
            _ReliableNotifyIndex = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ReliableNotifyIndex(this, 3, value));
    }

    @Override
    public long getReliableNotifyConfirmIndex() {
        if (!isManaged())
            return _ReliableNotifyConfirmIndex;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ReliableNotifyConfirmIndex;
        var log = (Log__ReliableNotifyConfirmIndex)txn.getLog(objectId() + 4);
        return log != null ? log.value : _ReliableNotifyConfirmIndex;
    }

    public void setReliableNotifyConfirmIndex(long value) {
        if (!isManaged()) {
            _ReliableNotifyConfirmIndex = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ReliableNotifyConfirmIndex(this, 4, value));
    }

    @Override
    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServerId;
        var log = (Log__ServerId)txn.getLog(objectId() + 5);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int value) {
        if (!isManaged()) {
            _ServerId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServerId(this, 5, value));
    }

    @Override
    public long getLogoutVersion() {
        if (!isManaged())
            return _LogoutVersion;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LogoutVersion;
        var log = (Log__LogoutVersion)txn.getLog(objectId() + 6);
        return log != null ? log.value : _LogoutVersion;
    }

    public void setLogoutVersion(long value) {
        if (!isManaged()) {
            _LogoutVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LogoutVersion(this, 6, value));
    }

    @SuppressWarnings("deprecation")
    public BVersion() {
        _ReliableNotifyMark = new Zeze.Transaction.Collections.PSet1<>(String.class);
        _ReliableNotifyMark.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BVersion(long _LoginVersion_, long _ReliableNotifyIndex_, long _ReliableNotifyConfirmIndex_, int _ServerId_, long _LogoutVersion_) {
        _LoginVersion = _LoginVersion_;
        _ReliableNotifyMark = new Zeze.Transaction.Collections.PSet1<>(String.class);
        _ReliableNotifyMark.variableId(2);
        _ReliableNotifyIndex = _ReliableNotifyIndex_;
        _ReliableNotifyConfirmIndex = _ReliableNotifyConfirmIndex_;
        _ServerId = _ServerId_;
        _LogoutVersion = _LogoutVersion_;
    }

    public void assign(BVersion other) {
        setLoginVersion(other.getLoginVersion());
        _ReliableNotifyMark.clear();
        _ReliableNotifyMark.addAll(other._ReliableNotifyMark);
        setReliableNotifyIndex(other.getReliableNotifyIndex());
        setReliableNotifyConfirmIndex(other.getReliableNotifyConfirmIndex());
        setServerId(other.getServerId());
        setLogoutVersion(other.getLogoutVersion());
    }

    public BVersion copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BVersion copy() {
        var copy = new BVersion();
        copy.assign(this);
        return copy;
    }

    public static void swap(BVersion a, BVersion b) {
        BVersion save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LoginVersion(BVersion bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BVersion)getBelong())._LoginVersion = value; }
    }

    private static final class Log__ReliableNotifyIndex extends Zeze.Transaction.Logs.LogLong {
        public Log__ReliableNotifyIndex(BVersion bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BVersion)getBelong())._ReliableNotifyIndex = value; }
    }

    private static final class Log__ReliableNotifyConfirmIndex extends Zeze.Transaction.Logs.LogLong {
        public Log__ReliableNotifyConfirmIndex(BVersion bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BVersion)getBelong())._ReliableNotifyConfirmIndex = value; }
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BVersion bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BVersion)getBelong())._ServerId = value; }
    }

    private static final class Log__LogoutVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LogoutVersion(BVersion bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BVersion)getBelong())._LogoutVersion = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Online.BVersion: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LoginVersion=").append(getLoginVersion()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyMark={");
        if (!_ReliableNotifyMark.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _ReliableNotifyMark) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyIndex=").append(getReliableNotifyIndex()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyConfirmIndex=").append(getReliableNotifyConfirmIndex()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId=").append(getServerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LogoutVersion=").append(getLogoutVersion()).append(System.lineSeparator());
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
            long _x_ = getLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _ReliableNotifyMark;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_)
                    _o_.WriteString(_v_);
            }
        }
        {
            long _x_ = getReliableNotifyIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getReliableNotifyConfirmIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getLogoutVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
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
            setLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _ReliableNotifyMark;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setReliableNotifyIndex(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setReliableNotifyConfirmIndex(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setLogoutVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _ReliableNotifyMark.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _ReliableNotifyMark.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getLoginVersion() < 0)
            return true;
        if (getReliableNotifyIndex() < 0)
            return true;
        if (getReliableNotifyConfirmIndex() < 0)
            return true;
        if (getServerId() < 0)
            return true;
        if (getLogoutVersion() < 0)
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
                case 1: _LoginVersion = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _ReliableNotifyMark.followerApply(vlog); break;
                case 3: _ReliableNotifyIndex = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _ReliableNotifyConfirmIndex = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 5: _ServerId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 6: _LogoutVersion = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setLoginVersion(rs.getLong(_parents_name_ + "LoginVersion"));
        Zeze.Serialize.Helper.decodeJsonSet(_ReliableNotifyMark, String.class, rs.getString(_parents_name_ + "ReliableNotifyMark"));
        setReliableNotifyIndex(rs.getLong(_parents_name_ + "ReliableNotifyIndex"));
        setReliableNotifyConfirmIndex(rs.getLong(_parents_name_ + "ReliableNotifyConfirmIndex"));
        setServerId(rs.getInt(_parents_name_ + "ServerId"));
        setLogoutVersion(rs.getLong(_parents_name_ + "LogoutVersion"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "LoginVersion", getLoginVersion());
        st.appendString(_parents_name_ + "ReliableNotifyMark", Zeze.Serialize.Helper.encodeJson(_ReliableNotifyMark));
        st.appendLong(_parents_name_ + "ReliableNotifyIndex", getReliableNotifyIndex());
        st.appendLong(_parents_name_ + "ReliableNotifyConfirmIndex", getReliableNotifyConfirmIndex());
        st.appendInt(_parents_name_ + "ServerId", getServerId());
        st.appendLong(_parents_name_ + "LogoutVersion", getLogoutVersion());
    }
}
