// auto-generated @formatter:off
package Zeze.Arch;

public abstract class AbstractOnline implements Zeze.IModule {
    public static final int ModuleId = 11100;
    public static final String ModuleName = "Online";
    public static final String ModuleFullName = "Zeze.Arch.Online";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    public static final int ResultCodeSuccess = 0;
    public static final int ResultCodeCreateRoleDuplicateRoleName = 1;
    public static final int ResultCodeAccountNotExist = 2;
    public static final int ResultCodeRoleNotExist = 3;
    public static final int ResultCodeNotLastLoginRoleId = 4;
    public static final int ResultCodeOnlineDataNotFound = 5;
    public static final int ResultCodeReliableNotifyConfirmIndexOutOfRange = 6;
    public static final int ResultCodeNotLogin = 7;

    protected final Zeze.Builtin.Online.taccount _taccount = new Zeze.Builtin.Online.taccount();
    protected final Zeze.Builtin.Online.tlocal _tlocal = new Zeze.Builtin.Online.tlocal();
    protected final Zeze.Builtin.Online.tonline _tonline = new Zeze.Builtin.Online.tonline();
    protected final Zeze.Builtin.Online.tversion _tversion = new Zeze.Builtin.Online.tversion();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Online.Login>();
            factoryHandle.Factory = Zeze.Builtin.Online.Login::new;
            factoryHandle.Handle = this::ProcessLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLoginRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47676933001134L, factoryHandle); // 11100, -1498951762
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Online.Logout>();
            factoryHandle.Factory = Zeze.Builtin.Online.Logout::new;
            factoryHandle.Handle = this::ProcessLogoutRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLogoutRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLogoutRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47676519983553L, factoryHandle); // 11100, -1911969343
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Online.ReliableNotifyConfirm>();
            factoryHandle.Factory = Zeze.Builtin.Online.ReliableNotifyConfirm::new;
            factoryHandle.Handle = this::ProcessReliableNotifyConfirmRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReliableNotifyConfirmRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReliableNotifyConfirmRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47678187220010L, factoryHandle); // 11100, -244732886
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Online.ReLogin>();
            factoryHandle.Factory = Zeze.Builtin.Online.ReLogin::new;
            factoryHandle.Handle = this::ProcessReLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReLoginRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47675064884515L, factoryHandle); // 11100, 927898915
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47676933001134L);
        service.getFactorys().remove(47676519983553L);
        service.getFactorys().remove(47678187220010L);
        service.getFactorys().remove(47675064884515L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_taccount.getName()).getDatabaseName(), _taccount);
        zeze.addTable(zeze.getConfig().getTableConf(_tlocal.getName()).getDatabaseName(), _tlocal);
        zeze.addTable(zeze.getConfig().getTableConf(_tonline.getName()).getDatabaseName(), _tonline);
        zeze.addTable(zeze.getConfig().getTableConf(_tversion.getName()).getDatabaseName(), _tversion);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_taccount.getName()).getDatabaseName(), _taccount);
        zeze.removeTable(zeze.getConfig().getTableConf(_tlocal.getName()).getDatabaseName(), _tlocal);
        zeze.removeTable(zeze.getConfig().getTableConf(_tonline.getName()).getDatabaseName(), _tonline);
        zeze.removeTable(zeze.getConfig().getTableConf(_tversion.getName()).getDatabaseName(), _tversion);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {
    }


    protected abstract long ProcessLoginRequest(Zeze.Builtin.Online.Login r) throws Exception;
    protected abstract long ProcessLogoutRequest(Zeze.Builtin.Online.Logout r) throws Exception;
    protected abstract long ProcessReliableNotifyConfirmRequest(Zeze.Builtin.Online.ReliableNotifyConfirm r) throws Exception;
    protected abstract long ProcessReLoginRequest(Zeze.Builtin.Online.ReLogin r) throws Exception;
}
