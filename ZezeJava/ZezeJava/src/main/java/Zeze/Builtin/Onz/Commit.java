// auto-generated @formatter:off
package Zeze.Builtin.Onz;

public class Commit extends Zeze.Net.Rpc<Zeze.Builtin.Onz.BCommit.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11038;
    public static final int ProtocolId_ = -1037801963; // 3257165333
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47411106178581
    static { register(TypeId_, Commit.class); }

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    @Override
    public long getTypeId() {
        return TypeId_;
    }

    public Commit() {
        Argument = new Zeze.Builtin.Onz.BCommit.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public Commit(Zeze.Builtin.Onz.BCommit.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
