// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTimer extends Zeze.Transaction.Bean {
    public static final long TYPEID = -3755541261968580150L;

    private String _TimerName;
    private String _HandleName;
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
        throw new RuntimeException("Unknown Bean! dynamic@Zeze.Builtin.Timer.BTimer:TimerObj");
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_TimerObj(long typeId) {
        if (typeId == -6995089347718168392L)
            return new Zeze.Builtin.Timer.BCronTimer();
        if (typeId == 1832177636612857692L)
            return new Zeze.Builtin.Timer.BSimpleTimer();
        return null;
    }

    private final Zeze.Transaction.DynamicBean _CustomData;

    public static long getSpecialTypeIdFromBean_CustomData(Zeze.Transaction.Bean bean) {
        return Zeze.Component.Timer.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_CustomData(long typeId) {
        return Zeze.Component.Timer.createBeanFromSpecialTypeId(typeId);
    }

    private long _ConcurrentFireSerialNo;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

    public String getTimerName() {
        if (!isManaged())
            return _TimerName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TimerName;
        var log = (Log__TimerName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TimerName;
    }

    public void setTimerName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TimerName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TimerName(this, 1, value));
    }

    public String getHandleName() {
        if (!isManaged())
            return _HandleName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HandleName;
        var log = (Log__HandleName)txn.getLog(objectId() + 2);
        return log != null ? log.value : _HandleName;
    }

    public void setHandleName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _HandleName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__HandleName(this, 2, value));
    }

    public Zeze.Transaction.DynamicBean getTimerObj() {
        return _TimerObj;
    }

    public Zeze.Builtin.Timer.BCronTimer getTimerObj_Zeze_Builtin_Timer_BCronTimer() {
        return (Zeze.Builtin.Timer.BCronTimer)getTimerObj().getBean();
    }

    public void setTimerObj(Zeze.Builtin.Timer.BCronTimer value) {
        getTimerObj().setBean(value);
    }

    public Zeze.Builtin.Timer.BSimpleTimer getTimerObj_Zeze_Builtin_Timer_BSimpleTimer() {
        return (Zeze.Builtin.Timer.BSimpleTimer)getTimerObj().getBean();
    }

    public void setTimerObj(Zeze.Builtin.Timer.BSimpleTimer value) {
        getTimerObj().setBean(value);
    }

    public Zeze.Transaction.DynamicBean getCustomData() {
        return _CustomData;
    }

    public long getConcurrentFireSerialNo() {
        if (!isManaged())
            return _ConcurrentFireSerialNo;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ConcurrentFireSerialNo;
        var log = (Log__ConcurrentFireSerialNo)txn.getLog(objectId() + 5);
        return log != null ? log.value : _ConcurrentFireSerialNo;
    }

    public void setConcurrentFireSerialNo(long value) {
        if (!isManaged()) {
            _ConcurrentFireSerialNo = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ConcurrentFireSerialNo(this, 5, value));
    }

    @SuppressWarnings("deprecation")
    public BTimer() {
        _TimerName = "";
        _HandleName = "";
        _TimerObj = new Zeze.Transaction.DynamicBean(3, BTimer::getSpecialTypeIdFromBean_TimerObj, BTimer::createBeanFromSpecialTypeId_TimerObj);
        _CustomData = new Zeze.Transaction.DynamicBean(4, Zeze.Component.Timer::getSpecialTypeIdFromBean, Zeze.Component.Timer::createBeanFromSpecialTypeId);
    }

    @SuppressWarnings("deprecation")
    public BTimer(String _TimerName_, String _HandleName_, long _ConcurrentFireSerialNo_) {
        if (_TimerName_ == null)
            throw new IllegalArgumentException();
        _TimerName = _TimerName_;
        if (_HandleName_ == null)
            throw new IllegalArgumentException();
        _HandleName = _HandleName_;
        _TimerObj = new Zeze.Transaction.DynamicBean(3, BTimer::getSpecialTypeIdFromBean_TimerObj, BTimer::createBeanFromSpecialTypeId_TimerObj);
        _CustomData = new Zeze.Transaction.DynamicBean(4, Zeze.Component.Timer::getSpecialTypeIdFromBean, Zeze.Component.Timer::createBeanFromSpecialTypeId);
        _ConcurrentFireSerialNo = _ConcurrentFireSerialNo_;
    }

    public void assign(BTimer other) {
        setTimerName(other.getTimerName());
        setHandleName(other.getHandleName());
        getTimerObj().assign(other.getTimerObj());
        getCustomData().assign(other.getCustomData());
        setConcurrentFireSerialNo(other.getConcurrentFireSerialNo());
    }

    @Deprecated
    public void Assign(BTimer other) {
        assign(other);
    }

    public BTimer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTimer copy() {
        var copy = new BTimer();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BTimer Copy() {
        return copy();
    }

    public static void swap(BTimer a, BTimer b) {
        BTimer save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TimerName extends Zeze.Transaction.Logs.LogString {
        public Log__TimerName(BTimer bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTimer)getBelong())._TimerName = value; }
    }

    private static final class Log__HandleName extends Zeze.Transaction.Logs.LogString {
        public Log__HandleName(BTimer bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTimer)getBelong())._HandleName = value; }
    }

    private static final class Log__ConcurrentFireSerialNo extends Zeze.Transaction.Logs.LogLong {
        public Log__ConcurrentFireSerialNo(BTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTimer)getBelong())._ConcurrentFireSerialNo = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BTimer: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TimerName").append('=').append(getTimerName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HandleName").append('=').append(getHandleName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TimerObj").append('=').append(System.lineSeparator());
        getTimerObj().getBean().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("CustomData").append('=').append(System.lineSeparator());
        getCustomData().getBean().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ConcurrentFireSerialNo").append('=').append(getConcurrentFireSerialNo()).append(System.lineSeparator());
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
            String _x_ = getTimerName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getHandleName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getTimerObj();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            var _x_ = getCustomData();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            long _x_ = getConcurrentFireSerialNo();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
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
            setTimerName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setHandleName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadDynamic(getTimerObj(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadDynamic(getCustomData(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setConcurrentFireSerialNo(_o_.ReadLong(_t_));
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
        _CustomData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _TimerObj.resetRootInfo();
        _CustomData.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getTimerObj().negativeCheck())
            return true;
        if (getConcurrentFireSerialNo() < 0)
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
                case 1: _TimerName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _HandleName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _TimerObj.followerApply(vlog); break;
                case 4: _CustomData.followerApply(vlog); break;
                case 5: _ConcurrentFireSerialNo = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
