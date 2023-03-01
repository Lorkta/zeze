// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractAutoKeyOld implements Zeze.IModule {
    public static final int ModuleId = 11003;
    public static final String ModuleName = "AutoKeyOld";
    public static final String ModuleFullName = "Zeze.Component.AutoKeyOld";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    protected final Zeze.Builtin.AutoKeyOld.tAutoKeys _tAutoKeys = new Zeze.Builtin.AutoKeyOld.tAutoKeys();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tAutoKeys.getName()).getDatabaseName(), _tAutoKeys);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tAutoKeys.getName()).getDatabaseName(), _tAutoKeys);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
