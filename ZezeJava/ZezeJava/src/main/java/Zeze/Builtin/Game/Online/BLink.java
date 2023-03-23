// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "RedundantSuppression", "MethodMayBeStatic", "PatternVariableCanBeUsed"})
public final class BLink implements Serializable, Comparable<BLink> {
    private String _LinkName;
    private long _LinkSid;

    // for decode only
    public BLink() {
        _LinkName = "";
    }

    public BLink(String _LinkName_, long _LinkSid_) {
        if (_LinkName_ == null)
            throw new IllegalArgumentException();
        this._LinkName = _LinkName_;
        this._LinkSid = _LinkSid_;
    }

    public String getLinkName() {
        return _LinkName;
    }

    public long getLinkSid() {
        return _LinkSid;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Online.BLink: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LinkName=").append(getLinkName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LinkSid=").append(getLinkSid()).append(System.lineSeparator());
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
            String _x_ = getLinkName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getLinkSid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            _LinkName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _LinkSid = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLink))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLink)_o_;
        if (!getLinkName().equals(_b_.getLinkName()))
            return false;
        if (getLinkSid() != _b_.getLinkSid())
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + _LinkName.hashCode();
        _h_ = _h_ * _p_ + Long.hashCode(_LinkSid);
        return _h_;
    }

    @Override
    public int compareTo(BLink _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = _LinkName.compareTo(_o_._LinkName);
            if (_c_ != 0)
                return _c_;
            _c_ = Long.compare(_LinkSid, _o_._LinkSid);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean negativeCheck() {
        if (getLinkSid() < 0)
            return true;
        return false;
    }
}