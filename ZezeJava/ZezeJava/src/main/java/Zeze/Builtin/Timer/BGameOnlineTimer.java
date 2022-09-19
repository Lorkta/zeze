// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BGameOnlineTimer extends Zeze.Transaction.Bean {
    private long _RoleId;
    private final Zeze.Transaction.DynamicBean _TimerObj;
    public static final long DynamicTypeId_TimerObj_Zeze_Builtin_Timer_BCronTimer = -6995089347718168392L;
    public static final long DynamicTypeId_TimerObj_Zeze_Builtin_Timer_BSimpleTimer = 1832177636612857692L;

    public static long getSpecialTypeIdFromBean_TimerObj(Zeze.Transaction.Bean bean) {
        var _typeId_ = bean.typeId();
        if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_typeId_ == -6995089347718168392L)
            return -6995089347718168392L; // Zeze.Builtin.Timer.BCronTimer
        if (_typeId_ == 1832177636612857692L)
            return 1832177636612857692L; // Zeze.Builtin.Timer.BSimpleTimer
        throw new RuntimeException("Unknown Bean! dynamic@Zeze.Builtin.Timer.BGameOnlineTimer:TimerObj");
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_TimerObj(long typeId) {
        if (typeId == -6995089347718168392L)
            return new Zeze.Builtin.Timer.BCronTimer();
        if (typeId == 1832177636612857692L)
            return new Zeze.Builtin.Timer.BSimpleTimer();
        return null;
    }

    private long _LoginVersion;
    private String _NamedName;

    public long getRoleId() {
        if (!isManaged())
            return _RoleId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _RoleId;
        var log = (Log__RoleId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _RoleId;
    }

    public void setRoleId(long value) {
        if (!isManaged()) {
            _RoleId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__RoleId(this, 1, value));
    }

    public Zeze.Transaction.DynamicBean getTimerObj() {
        return _TimerObj;
    }

    public Zeze.Builtin.Timer.BCronTimer getTimerObj_Zeze_Builtin_Timer_BCronTimer(){
        return (Zeze.Builtin.Timer.BCronTimer)getTimerObj().getBean();
    }

    public void setTimerObj(Zeze.Builtin.Timer.BCronTimer value) {
        getTimerObj().setBean(value);
    }

    public Zeze.Builtin.Timer.BSimpleTimer getTimerObj_Zeze_Builtin_Timer_BSimpleTimer(){
        return (Zeze.Builtin.Timer.BSimpleTimer)getTimerObj().getBean();
    }

    public void setTimerObj(Zeze.Builtin.Timer.BSimpleTimer value) {
        getTimerObj().setBean(value);
    }

    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LoginVersion;
        var log = (Log__LoginVersion)txn.getLog(objectId() + 3);
        return log != null ? log.value : _LoginVersion;
    }

    public void setLoginVersion(long value) {
        if (!isManaged()) {
            _LoginVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LoginVersion(this, 3, value));
    }

    public String getNamedName() {
        if (!isManaged())
            return _NamedName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NamedName;
        var log = (Log__NamedName)txn.getLog(objectId() + 4);
        return log != null ? log.value : _NamedName;
    }

    public void setNamedName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _NamedName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__NamedName(this, 4, value));
    }

    @SuppressWarnings("deprecation")
    public BGameOnlineTimer() {
        _TimerObj = new Zeze.Transaction.DynamicBean(2, BGameOnlineTimer::getSpecialTypeIdFromBean_TimerObj, BGameOnlineTimer::createBeanFromSpecialTypeId_TimerObj);
        _NamedName = "";
    }

    @SuppressWarnings("deprecation")
    public BGameOnlineTimer(long _RoleId_, long _LoginVersion_, String _NamedName_) {
        _RoleId = _RoleId_;
        _TimerObj = new Zeze.Transaction.DynamicBean(2, BGameOnlineTimer::getSpecialTypeIdFromBean_TimerObj, BGameOnlineTimer::createBeanFromSpecialTypeId_TimerObj);
        _LoginVersion = _LoginVersion_;
        if (_NamedName_ == null)
            throw new IllegalArgumentException();
        _NamedName = _NamedName_;
    }

    public void assign(BGameOnlineTimer other) {
        setRoleId(other.getRoleId());
        getTimerObj().assign(other.getTimerObj());
        setLoginVersion(other.getLoginVersion());
        setNamedName(other.getNamedName());
    }

    @Deprecated
    public void Assign(BGameOnlineTimer other) {
        assign(other);
    }

    public BGameOnlineTimer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    public BGameOnlineTimer copy() {
        var copy = new BGameOnlineTimer();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BGameOnlineTimer Copy() {
        return copy();
    }

    public static void swap(BGameOnlineTimer a, BGameOnlineTimer b) {
        BGameOnlineTimer save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public BGameOnlineTimer copyBean() {
        return copy();
    }

    public static final long TYPEID = -3455653027701280193L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__RoleId extends Zeze.Transaction.Logs.LogLong {
        public Log__RoleId(BGameOnlineTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BGameOnlineTimer)getBelong())._RoleId = value; }
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LoginVersion(BGameOnlineTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BGameOnlineTimer)getBelong())._LoginVersion = value; }
    }

    private static final class Log__NamedName extends Zeze.Transaction.Logs.LogString {
        public Log__NamedName(BGameOnlineTimer bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BGameOnlineTimer)getBelong())._NamedName = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BGameOnlineTimer: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("RoleId").append('=').append(getRoleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TimerObj").append('=').append(System.lineSeparator());
        getTimerObj().getBean().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LoginVersion").append('=').append(getLoginVersion()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NamedName").append('=').append(getNamedName()).append(System.lineSeparator());
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
            long _x_ = getRoleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getTimerObj();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            long _x_ = getLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getNamedName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
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
            setRoleId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadDynamic(getTimerObj(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setNamedName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _TimerObj.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _TimerObj.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getRoleId() < 0)
            return true;
        if (getTimerObj().negativeCheck())
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
                case 1: _RoleId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _TimerObj.followerApply(vlog); break;
                case 3: _LoginVersion = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _NamedName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }
}