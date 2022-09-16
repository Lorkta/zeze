// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BAcquireParam extends Zeze.Transaction.Bean {
    private Zeze.Net.Binary _GlobalKey;
    private int _State;

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

    @SuppressWarnings("deprecation")
    public BAcquireParam() {
        _GlobalKey = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BAcquireParam(Zeze.Net.Binary _GlobalKey_, int _State_) {
        if (_GlobalKey_ == null)
            throw new IllegalArgumentException();
        _GlobalKey = _GlobalKey_;
        _State = _State_;
    }

    public void assign(BAcquireParam other) {
        setGlobalKey(other.getGlobalKey());
        setState(other.getState());
    }

    @Deprecated
    public void Assign(BAcquireParam other) {
        assign(other);
    }

    public BAcquireParam copyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BAcquireParam copy() {
        var copy = new BAcquireParam();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BAcquireParam Copy() {
        return copy();
    }

    public static void swap(BAcquireParam a, BAcquireParam b) {
        BAcquireParam save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public BAcquireParam copyBean() {
        return Copy();
    }

    public static final long TYPEID = -8330630345134214646L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__GlobalKey extends Zeze.Transaction.Logs.LogBinary {
        public Log__GlobalKey(BAcquireParam bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAcquireParam)getBelong())._GlobalKey = value; }
    }

    private static final class Log__State extends Zeze.Transaction.Logs.LogInt {
        public Log__State(BAcquireParam bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAcquireParam)getBelong())._State = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.GlobalCacheManagerWithRaft.BAcquireParam: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("GlobalKey").append('=').append(getGlobalKey()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("State").append('=').append(getState()).append(System.lineSeparator());
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
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
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
                case 1: _GlobalKey = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 2: _State = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }
}
