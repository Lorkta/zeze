// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class Broadcast extends Zeze.Net.Protocol<Zeze.Builtin.Provider.BBroadcast.Data> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = -886924798; // 3408042498
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47282408036866
    static { register(TypeId_, Broadcast.class); }

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

    public Broadcast() {
        Argument = new Zeze.Builtin.Provider.BBroadcast.Data();
    }

    public Broadcast(Zeze.Builtin.Provider.BBroadcast.Data arg) {
        Argument = arg;
    }
}
