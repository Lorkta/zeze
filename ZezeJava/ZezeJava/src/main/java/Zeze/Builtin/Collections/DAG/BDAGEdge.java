// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

import Zeze.Serialize.ByteBuffer;

// 有向图的边类型（如：任务的连接方式）
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BDAGEdge extends Zeze.Transaction.Bean implements BDAGEdgeReadOnly {
    public static final long TYPEID = -6222763240399548476L;

    private Zeze.Builtin.Collections.DAG.BDAGNodeKey _From; // 有向图中有向边的起点
    private Zeze.Builtin.Collections.DAG.BDAGNodeKey _To; // 有向图中有向边的终点

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGNodeKey getFrom() {
        if (!isManaged())
            return _From;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _From;
        var log = (Log__From)txn.getLog(objectId() + 1);
        return log != null ? log.value : _From;
    }

    public void setFrom(Zeze.Builtin.Collections.DAG.BDAGNodeKey value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _From = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__From(this, 1, value));
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGNodeKey getTo() {
        if (!isManaged())
            return _To;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _To;
        var log = (Log__To)txn.getLog(objectId() + 2);
        return log != null ? log.value : _To;
    }

    public void setTo(Zeze.Builtin.Collections.DAG.BDAGNodeKey value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _To = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__To(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BDAGEdge() {
        _From = new Zeze.Builtin.Collections.DAG.BDAGNodeKey();
        _To = new Zeze.Builtin.Collections.DAG.BDAGNodeKey();
    }

    @SuppressWarnings("deprecation")
    public BDAGEdge(Zeze.Builtin.Collections.DAG.BDAGNodeKey _From_, Zeze.Builtin.Collections.DAG.BDAGNodeKey _To_) {
        if (_From_ == null)
            throw new IllegalArgumentException();
        _From = _From_;
        if (_To_ == null)
            throw new IllegalArgumentException();
        _To = _To_;
    }

    public void assign(BDAGEdge other) {
        setFrom(other.getFrom());
        setTo(other.getTo());
    }

    @Deprecated
    public void Assign(BDAGEdge other) {
        assign(other);
    }

    public BDAGEdge copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDAGEdge copy() {
        var copy = new BDAGEdge();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BDAGEdge Copy() {
        return copy();
    }

    public static void swap(BDAGEdge a, BDAGEdge b) {
        BDAGEdge save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__From extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.DAG.BDAGNodeKey> {
        public Log__From(BDAGEdge bean, int varId, Zeze.Builtin.Collections.DAG.BDAGNodeKey value) { super(Zeze.Builtin.Collections.DAG.BDAGNodeKey.class, bean, varId, value); }

        @Override
        public void commit() { ((BDAGEdge)getBelong())._From = value; }
    }

    private static final class Log__To extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.DAG.BDAGNodeKey> {
        public Log__To(BDAGEdge bean, int varId, Zeze.Builtin.Collections.DAG.BDAGNodeKey value) { super(Zeze.Builtin.Collections.DAG.BDAGNodeKey.class, bean, varId, value); }

        @Override
        public void commit() { ((BDAGEdge)getBelong())._To = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.DAG.BDAGEdge: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("From=").append(System.lineSeparator());
        getFrom().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("To=").append(System.lineSeparator());
        getTo().buildString(sb, level + 4);
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getFrom().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getTo().encode(_o_);
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
            _o_.ReadBean(getFrom(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadBean(getTo(), _t_);
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
                case 1: _From = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.DAG.BDAGNodeKey>)vlog).value; break;
                case 2: _To = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.DAG.BDAGNodeKey>)vlog).value; break;
            }
        }
    }
}
