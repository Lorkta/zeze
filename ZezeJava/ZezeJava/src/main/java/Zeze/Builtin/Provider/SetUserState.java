// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class SetUserState extends Zeze.Net.Protocol<Zeze.Builtin.Provider.BSetUserState.Data> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = -1725914489; // 2569052807
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47281569047175
    static { register(TypeId_, SetUserState.class); }

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

    public SetUserState() {
        Argument = new Zeze.Builtin.Provider.BSetUserState.Data();
    }

    public SetUserState(Zeze.Builtin.Provider.BSetUserState.Data arg) {
        Argument = arg;
    }
}
