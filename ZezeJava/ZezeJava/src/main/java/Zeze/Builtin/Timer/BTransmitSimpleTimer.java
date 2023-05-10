// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTransmitSimpleTimer extends Zeze.Transaction.Bean implements BTransmitSimpleTimerReadOnly {
    public static final long TYPEID = 8003695312942175548L;

    private String _TimerId;
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Timer.BSimpleTimer> _SimpleTimer;
    private String _HandleClass;
    private String _CustomClass;
    private Zeze.Net.Binary _CustomBean;
    private long _LoginVersion;

    @Override
    public String getTimerId() {
        if (!isManaged())
            return _TimerId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TimerId;
        var log = (Log__TimerId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TimerId;
    }

    public void setTimerId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TimerId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TimerId(this, 1, value));
    }

    public Zeze.Builtin.Timer.BSimpleTimer getSimpleTimer() {
        return _SimpleTimer.getValue();
    }

    public void setSimpleTimer(Zeze.Builtin.Timer.BSimpleTimer value) {
        _SimpleTimer.setValue(value);
    }

    @Override
    public Zeze.Builtin.Timer.BSimpleTimerReadOnly getSimpleTimerReadOnly() {
        return _SimpleTimer.getValue();
    }

    @Override
    public String getHandleClass() {
        if (!isManaged())
            return _HandleClass;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HandleClass;
        var log = (Log__HandleClass)txn.getLog(objectId() + 3);
        return log != null ? log.value : _HandleClass;
    }

    public void setHandleClass(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _HandleClass = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__HandleClass(this, 3, value));
    }

    @Override
    public String getCustomClass() {
        if (!isManaged())
            return _CustomClass;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _CustomClass;
        var log = (Log__CustomClass)txn.getLog(objectId() + 4);
        return log != null ? log.value : _CustomClass;
    }

    public void setCustomClass(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _CustomClass = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__CustomClass(this, 4, value));
    }

    @Override
    public Zeze.Net.Binary getCustomBean() {
        if (!isManaged())
            return _CustomBean;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _CustomBean;
        var log = (Log__CustomBean)txn.getLog(objectId() + 5);
        return log != null ? log.value : _CustomBean;
    }

    public void setCustomBean(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _CustomBean = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__CustomBean(this, 5, value));
    }

    @Override
    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LoginVersion;
        var log = (Log__LoginVersion)txn.getLog(objectId() + 6);
        return log != null ? log.value : _LoginVersion;
    }

    public void setLoginVersion(long value) {
        if (!isManaged()) {
            _LoginVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LoginVersion(this, 6, value));
    }

    @SuppressWarnings("deprecation")
    public BTransmitSimpleTimer() {
        _TimerId = "";
        _SimpleTimer = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Timer.BSimpleTimer(), Zeze.Builtin.Timer.BSimpleTimer.class);
        _SimpleTimer.variableId(2);
        _HandleClass = "";
        _CustomClass = "";
        _CustomBean = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BTransmitSimpleTimer(String _TimerId_, String _HandleClass_, String _CustomClass_, Zeze.Net.Binary _CustomBean_, long _LoginVersion_) {
        if (_TimerId_ == null)
            throw new IllegalArgumentException();
        _TimerId = _TimerId_;
        _SimpleTimer = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Timer.BSimpleTimer(), Zeze.Builtin.Timer.BSimpleTimer.class);
        _SimpleTimer.variableId(2);
        if (_HandleClass_ == null)
            throw new IllegalArgumentException();
        _HandleClass = _HandleClass_;
        if (_CustomClass_ == null)
            throw new IllegalArgumentException();
        _CustomClass = _CustomClass_;
        if (_CustomBean_ == null)
            throw new IllegalArgumentException();
        _CustomBean = _CustomBean_;
        _LoginVersion = _LoginVersion_;
    }

    public void assign(BTransmitSimpleTimer other) {
        setTimerId(other.getTimerId());
        _SimpleTimer.assign(other._SimpleTimer);
        setHandleClass(other.getHandleClass());
        setCustomClass(other.getCustomClass());
        setCustomBean(other.getCustomBean());
        setLoginVersion(other.getLoginVersion());
    }

    public BTransmitSimpleTimer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTransmitSimpleTimer copy() {
        var copy = new BTransmitSimpleTimer();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTransmitSimpleTimer a, BTransmitSimpleTimer b) {
        BTransmitSimpleTimer save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TimerId extends Zeze.Transaction.Logs.LogString {
        public Log__TimerId(BTransmitSimpleTimer bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmitSimpleTimer)getBelong())._TimerId = value; }
    }

    private static final class Log__HandleClass extends Zeze.Transaction.Logs.LogString {
        public Log__HandleClass(BTransmitSimpleTimer bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmitSimpleTimer)getBelong())._HandleClass = value; }
    }

    private static final class Log__CustomClass extends Zeze.Transaction.Logs.LogString {
        public Log__CustomClass(BTransmitSimpleTimer bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmitSimpleTimer)getBelong())._CustomClass = value; }
    }

    private static final class Log__CustomBean extends Zeze.Transaction.Logs.LogBinary {
        public Log__CustomBean(BTransmitSimpleTimer bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmitSimpleTimer)getBelong())._CustomBean = value; }
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LoginVersion(BTransmitSimpleTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmitSimpleTimer)getBelong())._LoginVersion = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BTransmitSimpleTimer: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TimerId=").append(getTimerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SimpleTimer=").append(System.lineSeparator());
        _SimpleTimer.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HandleClass=").append(getHandleClass()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("CustomClass=").append(getCustomClass()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("CustomBean=").append(getCustomBean()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LoginVersion=").append(getLoginVersion()).append(System.lineSeparator());
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
            String _x_ = getTimerId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _SimpleTimer.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            String _x_ = getHandleClass();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getCustomClass();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getCustomBean();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = getLoginVersion();
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
            setTimerId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadBean(_SimpleTimer, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setHandleClass(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setCustomClass(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setCustomBean(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _SimpleTimer.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _SimpleTimer.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_SimpleTimer.negativeCheck())
            return true;
        if (getLoginVersion() < 0)
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
                case 1: _TimerId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _SimpleTimer.followerApply(vlog); break;
                case 3: _HandleClass = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _CustomClass = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 5: _CustomBean = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 6: _LoginVersion = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTimerId(rs.getString(_parents_name_ + "TimerId"));
        if (getTimerId() == null)
            setTimerId("");
        parents.add("SimpleTimer");
        _SimpleTimer.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        setHandleClass(rs.getString(_parents_name_ + "HandleClass"));
        if (getHandleClass() == null)
            setHandleClass("");
        setCustomClass(rs.getString(_parents_name_ + "CustomClass"));
        if (getCustomClass() == null)
            setCustomClass("");
        setCustomBean(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "CustomBean")));
        if (getCustomBean() == null)
            setCustomBean(Zeze.Net.Binary.Empty);
        setLoginVersion(rs.getLong(_parents_name_ + "LoginVersion"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "TimerId", getTimerId());
        parents.add("SimpleTimer");
        _SimpleTimer.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
        st.appendString(_parents_name_ + "HandleClass", getHandleClass());
        st.appendString(_parents_name_ + "CustomClass", getCustomClass());
        st.appendBinary(_parents_name_ + "CustomBean", getCustomBean());
        st.appendLong(_parents_name_ + "LoginVersion", getLoginVersion());
    }
}
