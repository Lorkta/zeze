// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BBag extends Zeze.Transaction.Bean implements BBagReadOnly {
    public static final long TYPEID = -5051317137860806350L;

    private int _Capacity;
    private final Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.Game.Bag.BItem> _Items; // key is bag position

    @Override
    public int getCapacity() {
        if (!isManaged())
            return _Capacity;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Capacity;
        var log = (Log__Capacity)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Capacity;
    }

    public void setCapacity(int value) {
        if (!isManaged()) {
            _Capacity = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Capacity(this, 1, value));
    }

    public Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.Game.Bag.BItem> getItems() {
        return _Items;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.Game.Bag.BItem, Zeze.Builtin.Game.Bag.BItemReadOnly> getItemsReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Items);
    }

    @SuppressWarnings("deprecation")
    public BBag() {
        _Items = new Zeze.Transaction.Collections.PMap2<>(Integer.class, Zeze.Builtin.Game.Bag.BItem.class);
        _Items.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BBag(int _Capacity_) {
        _Capacity = _Capacity_;
        _Items = new Zeze.Transaction.Collections.PMap2<>(Integer.class, Zeze.Builtin.Game.Bag.BItem.class);
        _Items.variableId(2);
    }

    public void assign(BBag other) {
        setCapacity(other.getCapacity());
        _Items.clear();
        for (var e : other._Items.entrySet())
            _Items.put(e.getKey(), e.getValue().copy());
    }

    @Deprecated
    public void Assign(BBag other) {
        assign(other);
    }

    public BBag copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBag copy() {
        var copy = new BBag();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BBag Copy() {
        return copy();
    }

    public static void swap(BBag a, BBag b) {
        BBag save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Capacity extends Zeze.Transaction.Logs.LogInt {
        public Log__Capacity(BBag bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBag)getBelong())._Capacity = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Bag.BBag: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Capacity=").append(getCapacity()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Items={");
        if (!_Items.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Items.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(System.lineSeparator());
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
            int _x_ = getCapacity();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Items;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
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
            setCapacity(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Items;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Game.Bag.BItem(), _t_);
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
        _Items.initRootInfo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getCapacity() < 0)
            return true;
        for (var _v_ : _Items.values()) {
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
                case 1: _Capacity = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _Items.followerApply(vlog); break;
            }
        }
    }
}
