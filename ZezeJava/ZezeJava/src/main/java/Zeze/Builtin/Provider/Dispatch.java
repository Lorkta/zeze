// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class Dispatch extends Zeze.Net.Protocol<Zeze.Builtin.Provider.BDispatch.Data> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = 1285307417;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47280285301785
    static { register(TypeId_, Dispatch.class); }

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

    public Dispatch() {
        Argument = new Zeze.Builtin.Provider.BDispatch.Data();
    }

    public Dispatch(Zeze.Builtin.Provider.BDispatch.Data arg) {
        Argument = arg;
    }
}
