// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractStatistics implements Zeze.IModule {
    public static final int ModuleId = 11020;
    public static final String ModuleName = "Statistics";
    public static final String ModuleFullName = "Zeze.Component.Statistics";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }
    private final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }


    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        httpServer.addHandler("/Zeze/Builtin/Statistics/Query", 8192,
                _reflect.getTransactionLevel("OnServletQuery", Zeze.Transaction.TransactionLevel.Serializable),
                _reflect.getDispatchMode("OnServletQuery", Zeze.Transaction.DispatchMode.Normal),
                this::OnServletQuery);
    }

    protected abstract void OnServletQuery(Zeze.Netty.HttpExchange x) throws Exception;
}
