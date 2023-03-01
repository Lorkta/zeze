// auto-generated @formatter:off
package Zeze.Game;

public abstract class AbstractTaskBase implements Zeze.IModule {
    public static final int ModuleId = 11018;
    public static final String ModuleName = "TaskBase";
    public static final String ModuleFullName = "Zeze.Game.TaskBase";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    public static final int Disabled = 0; // 不可接取
    public static final int Init = 1; // 可接取
    public static final int Processing = 2; // 已经接取，未完成
    public static final int Finished = 3; // 已完成，未提交
    public static final int Committed = 4; // 已经提交
    public static final int TaskResultFailure = 0; // 使用二进制bit返回值，允许多重result返回。 1代表执行成功。
    public static final int TaskResultSuccess = 1;
    public static final int TaskResultNewRoleTasksCreated = 2;
    public static final int TaskResultAccepted = 4;
    public static final int TaskResultRejected = 8;
    public static final int TaskResultInvalidRoleId = 16;
    public static final int TaskResultTaskNotFound = 32;
    public static final int CompleteNPCTalk = 31;
    public static final int CompleteArriveArea = 32;
    public static final int CompleteCollectItem = 33;
    public static final int CompleteSubmitItem = 34;
    public static final int CompleteArriveNPC = 35;

    protected final Zeze.Builtin.Game.TaskBase.tEventClasses _tEventClasses = new Zeze.Builtin.Game.TaskBase.tEventClasses();
    protected final Zeze.Builtin.Game.TaskBase.tRoleTask _tRoleTask = new Zeze.Builtin.Game.TaskBase.tRoleTask();
    protected final Zeze.Builtin.Game.TaskBase.tTask _tTask = new Zeze.Builtin.Game.TaskBase.tTask();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.TaskBase.TriggerTaskEvent.class, Zeze.Builtin.Game.TaskBase.TriggerTaskEvent.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.TaskBase.TriggerTaskEvent::new;
            factoryHandle.Handle = this::ProcessTriggerTaskEventRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessTriggerTaskEventRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessTriggerTaskEventRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47323020635145L, factoryHandle); // 11018, 1070967817
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47323020635145L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tEventClasses.getName()).getDatabaseName(), _tEventClasses);
        zeze.addTable(zeze.getConfig().getTableConf(_tRoleTask.getName()).getDatabaseName(), _tRoleTask);
        zeze.addTable(zeze.getConfig().getTableConf(_tTask.getName()).getDatabaseName(), _tTask);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tEventClasses.getName()).getDatabaseName(), _tEventClasses);
        zeze.removeTable(zeze.getConfig().getTableConf(_tRoleTask.getName()).getDatabaseName(), _tRoleTask);
        zeze.removeTable(zeze.getConfig().getTableConf(_tTask.getName()).getDatabaseName(), _tTask);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessTriggerTaskEventRequest(Zeze.Builtin.Game.TaskBase.TriggerTaskEvent r) throws Exception;
}
