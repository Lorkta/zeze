// auto-generated
package Zezex.Provider;

public class ReportLoad extends Zeze.Net.Protocol1<Zezex.Provider.BLoad> {
    public final static int ModuleId_ = 10001;
    public final static int ProtocolId_ = 63960;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public ReportLoad() {
        Argument = new Zezex.Provider.BLoad();
    }

}
