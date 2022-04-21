// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractTestRocks extends Zeze.IModule {
    @Override public String getFullName() { return "Zeze.Builtin.TestRocks"; }
    @Override public String getName() { return "TestRocks"; }
    @Override public int getId() { return ModuleId; }
    public static final int ModuleId = 11002;

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
        rocks.RegisterTableTemplate("tRocks", int.class, Zeze.Builtin.TestRocks.Value.class);
        Zeze.Raft.RocksRaft.Rocks.RegisterLog(() -> new Zeze.Raft.RocksRaft.LogSet1<>(Integer.class));
        Zeze.Raft.RocksRaft.Rocks.RegisterLog(() -> new Zeze.Raft.RocksRaft.LogSet1<>(Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey.class));
        Zeze.Raft.RocksRaft.Rocks.RegisterLog(() -> new Zeze.Raft.RocksRaft.Log1.LogBeanKey<>(Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey.class));
        Zeze.Raft.RocksRaft.Rocks.RegisterLog(() -> new Zeze.Raft.RocksRaft.LogMap1<>(Integer.class, Integer.class));
        Zeze.Raft.RocksRaft.Rocks.RegisterLog(() -> new Zeze.Raft.RocksRaft.LogMap1<>(Integer.class, Zeze.Builtin.TestRocks.Value.class));
    }
}
