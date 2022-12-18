// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractServiceManagerWithRaft implements Zeze.IModule {
    public static final int ModuleId = 11022;
    @Override public String getFullName() { return "Zeze.Services.ServiceManagerWithRaft"; }
    @Override public String getName() { return "ServiceManagerWithRaft"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.AllocateId>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.AllocateId::new;
            factoryHandle.Handle = this::ProcessAllocateIdRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAllocateIdRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAllocateIdRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47342648206403L, factoryHandle); // 11022, -776297405
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.CommitServiceList>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.CommitServiceList::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCommitServiceListResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCommitServiceListResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340049712890L, factoryHandle); // 11022, 920176378
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.KeepAlive>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.KeepAlive::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKeepAliveResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessKeepAliveResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47341226054794L, factoryHandle); // 11022, 2096518282
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.Login>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.Login::new;
            factoryHandle.Handle = this::ProcessLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLoginRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47339747890828L, factoryHandle); // 11022, 618354316
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.NotifyServiceList>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.NotifyServiceList::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessNotifyServiceListResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessNotifyServiceListResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47339587192283L, factoryHandle); // 11022, 457655771
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessOfflineNotifyResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessOfflineNotifyResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340558537840L, factoryHandle); // 11022, 1429001328
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.OfflineRegister>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.OfflineRegister::new;
            factoryHandle.Handle = this::ProcessOfflineRegisterRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessOfflineRegisterRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessOfflineRegisterRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340511174741L, factoryHandle); // 11022, 1381638229
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.ReadyServiceList>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.ReadyServiceList::new;
            factoryHandle.Handle = this::ProcessReadyServiceListRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReadyServiceListRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReadyServiceListRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47339697966893L, factoryHandle); // 11022, 568430381
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.Register>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.Register::new;
            factoryHandle.Handle = this::ProcessRegisterRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRegisterRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRegisterRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340640775066L, factoryHandle); // 11022, 1511238554
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad::new;
            factoryHandle.Handle = this::ProcessSetServerLoadRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSetServerLoadRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSetServerLoadRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47342529828679L, factoryHandle); // 11022, -894675129
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.Subscribe>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.Subscribe::new;
            factoryHandle.Handle = this::ProcessSubscribeRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSubscribeRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340271484727L, factoryHandle); // 11022, 1141948215
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.SubscribeFirstCommit>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.SubscribeFirstCommit::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeFirstCommitResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSubscribeFirstCommitResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47342788372847L, factoryHandle); // 11022, -636130961
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.UnRegister>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.UnRegister::new;
            factoryHandle.Handle = this::ProcessUnRegisterRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnRegisterRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUnRegisterRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47341011400112L, factoryHandle); // 11022, 1881863600
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe::new;
            factoryHandle.Handle = this::ProcessUnSubscribeRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUnSubscribeRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47339752276364L, factoryHandle); // 11022, 622739852
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.Update>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.Update::new;
            factoryHandle.Handle = this::ProcessUpdateRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUpdateRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUpdateRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340940316449L, factoryHandle); // 11022, 1810779937
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47342648206403L);
        service.getFactorys().remove(47340049712890L);
        service.getFactorys().remove(47341226054794L);
        service.getFactorys().remove(47339747890828L);
        service.getFactorys().remove(47339587192283L);
        service.getFactorys().remove(47340558537840L);
        service.getFactorys().remove(47340511174741L);
        service.getFactorys().remove(47339697966893L);
        service.getFactorys().remove(47340640775066L);
        service.getFactorys().remove(47342529828679L);
        service.getFactorys().remove(47340271484727L);
        service.getFactorys().remove(47342788372847L);
        service.getFactorys().remove(47341011400112L);
        service.getFactorys().remove(47339752276364L);
        service.getFactorys().remove(47340940316449L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
        rocks.registerTableTemplate("tAutoKey", String.class, Zeze.Builtin.ServiceManagerWithRaft.BAutoKey.class);
        rocks.registerTableTemplate("tLoadObservers", String.class, Zeze.Builtin.ServiceManagerWithRaft.BLoadObservers.class);
        rocks.registerTableTemplate("tServerState", String.class, Zeze.Builtin.ServiceManagerWithRaft.BServerState.class);
        rocks.registerTableTemplate("tSession", String.class, Zeze.Builtin.ServiceManagerWithRaft.BSession.class);
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogSet1<>(String.class));
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogMap1<>(String.class, Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoRocks.class));
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogMap1<>(String.class, Zeze.Builtin.ServiceManagerWithRaft.BSubscribeStateRocks.class));
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogMap1<>(String.class, Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotifyRocks.class));
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogMap1<>(Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoKeyRocks.class, Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoRocks.class));
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.Log1.LogBeanKey<>(Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoKeyRocks.class));
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogMap1<>(String.class, Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfoRocks.class));
    }

    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {
    }


    protected abstract long ProcessAllocateIdRequest(Zeze.Builtin.ServiceManagerWithRaft.AllocateId r) throws Throwable;
    protected abstract long ProcessLoginRequest(Zeze.Builtin.ServiceManagerWithRaft.Login r) throws Throwable;
    protected abstract long ProcessOfflineRegisterRequest(Zeze.Builtin.ServiceManagerWithRaft.OfflineRegister r) throws Throwable;
    protected abstract long ProcessReadyServiceListRequest(Zeze.Builtin.ServiceManagerWithRaft.ReadyServiceList r) throws Throwable;
    protected abstract long ProcessRegisterRequest(Zeze.Builtin.ServiceManagerWithRaft.Register r) throws Throwable;
    protected abstract long ProcessSetServerLoadRequest(Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad r) throws Throwable;
    protected abstract long ProcessSubscribeRequest(Zeze.Builtin.ServiceManagerWithRaft.Subscribe r) throws Throwable;
    protected abstract long ProcessUnRegisterRequest(Zeze.Builtin.ServiceManagerWithRaft.UnRegister r) throws Throwable;
    protected abstract long ProcessUnSubscribeRequest(Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe r) throws Throwable;
    protected abstract long ProcessUpdateRequest(Zeze.Builtin.ServiceManagerWithRaft.Update r) throws Throwable;
}