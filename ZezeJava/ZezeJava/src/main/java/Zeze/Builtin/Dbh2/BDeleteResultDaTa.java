// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BDeleteResultDaTa extends Zeze.Transaction.Data {
    public static final long TYPEID = 8209931211572490098L;

    private String _RaftConfig;

    public String getRaftConfig() {
        return _RaftConfig;
    }

    public void setRaftConfig(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _RaftConfig = value;
    }

    @SuppressWarnings("deprecation")
    public BDeleteResultDaTa() {
        _RaftConfig = "";
    }

    @SuppressWarnings("deprecation")
    public BDeleteResultDaTa(String _RaftConfig_) {
        if (_RaftConfig_ == null)
            throw new IllegalArgumentException();
        _RaftConfig = _RaftConfig_;
    }

    @Override
    public Zeze.Builtin.Dbh2.BDeleteResult toBean() {
        var bean = new Zeze.Builtin.Dbh2.BDeleteResult();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BDeleteResult)other);
    }

    public void assign(BDeleteResult other) {
        setRaftConfig(other.getRaftConfig());
    }

    public void assign(BDeleteResultDaTa other) {
        setRaftConfig(other.getRaftConfig());
    }

    @Override
    public BDeleteResultDaTa copy() {
        var copy = new BDeleteResultDaTa();
        copy.assign(this);
        return copy;
    }

    public static void swap(BDeleteResultDaTa a, BDeleteResultDaTa b) {
        var save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BDeleteResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("RaftConfig=").append(getRaftConfig()).append(System.lineSeparator());
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
            String _x_ = getRaftConfig();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
            setRaftConfig(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}