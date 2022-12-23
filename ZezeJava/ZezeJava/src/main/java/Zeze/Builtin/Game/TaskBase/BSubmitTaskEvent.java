// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BSubmitTaskEvent extends Zeze.Transaction.Bean implements BSubmitTaskEventReadOnly {
    public static final long TYPEID = -2631835383026852161L;

    private long _taskId;

    @Override
    public long getTaskId() {
        if (!isManaged())
            return _taskId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _taskId;
        var log = (Log__taskId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _taskId;
    }

    public void setTaskId(long value) {
        if (!isManaged()) {
            _taskId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__taskId(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BSubmitTaskEvent() {
    }

    @SuppressWarnings("deprecation")
    public BSubmitTaskEvent(long _taskId_) {
        _taskId = _taskId_;
    }

    public void assign(BSubmitTaskEvent other) {
        setTaskId(other.getTaskId());
    }

    @Deprecated
    public void Assign(BSubmitTaskEvent other) {
        assign(other);
    }

    public BSubmitTaskEvent copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSubmitTaskEvent copy() {
        var copy = new BSubmitTaskEvent();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BSubmitTaskEvent Copy() {
        return copy();
    }

    public static void swap(BSubmitTaskEvent a, BSubmitTaskEvent b) {
        BSubmitTaskEvent save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__taskId extends Zeze.Transaction.Logs.LogLong {
        public Log__taskId(BSubmitTaskEvent bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSubmitTaskEvent)getBelong())._taskId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BSubmitTaskEvent: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("taskId=").append(getTaskId()).append(System.lineSeparator());
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
            long _x_ = getTaskId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
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
            setTaskId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void resetChildrenRootInfo() {
    }

    @Override
    public boolean negativeCheck() {
        if (getTaskId() < 0)
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
                case 1: _taskId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
