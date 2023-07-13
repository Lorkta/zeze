// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BAnnounceLinkInfo extends Zeze.Transaction.Bean implements BAnnounceLinkInfoReadOnly {
    public static final long TYPEID = 6291432069805514560L;

    @SuppressWarnings("deprecation")
    public BAnnounceLinkInfo() {
    }

    @Override
    public Zeze.Builtin.Provider.BAnnounceLinkInfo.Data toData() {
        var data = new Zeze.Builtin.Provider.BAnnounceLinkInfo.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Provider.BAnnounceLinkInfo.Data)other);
    }

    public void assign(BAnnounceLinkInfo.Data other) {
        _unknown_ = null;
    }

    public void assign(BAnnounceLinkInfo other) {
        _unknown_ = other._unknown_;
    }

    public BAnnounceLinkInfo copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAnnounceLinkInfo copy() {
        var copy = new BAnnounceLinkInfo();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAnnounceLinkInfo a, BAnnounceLinkInfo b) {
        BAnnounceLinkInfo save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BAnnounceLinkInfo: {").append(System.lineSeparator());
        level += 4;
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

    private ByteBuffer _unknown_;

    @Override
    public void encode(ByteBuffer _o_) {
        var _u_ = _unknown_;
        var _ui_ = _u_ != null ? (_u_ = ByteBuffer.Wrap(_u_)).readUnknownIndex() : Long.MAX_VALUE;
        int _i_ = 0;
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        _o_.ReadTagSize(_t_);
        _o_.skipAllUnknownFields(_t_);
    }

    @Override
    public void decodeWithUnknown(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log log) {
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 6291432069805514560L;

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @Override
    public void reset() {
    }

    @Override
    public Zeze.Builtin.Provider.BAnnounceLinkInfo toBean() {
        var bean = new Zeze.Builtin.Provider.BAnnounceLinkInfo();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BAnnounceLinkInfo)other);
    }

    public void assign(BAnnounceLinkInfo other) {
    }

    public void assign(BAnnounceLinkInfo.Data other) {
    }

    @Override
    public BAnnounceLinkInfo.Data copy() {
        var copy = new BAnnounceLinkInfo.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAnnounceLinkInfo.Data a, BAnnounceLinkInfo.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BAnnounceLinkInfo.Data clone() {
        return (BAnnounceLinkInfo.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BAnnounceLinkInfo: {").append(System.lineSeparator());
        level += 4;
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
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        _o_.ReadTagSize(_t_);
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
