package Zeze.Game;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent;
import Zeze.Builtin.Game.TaskBase.BNPCTaskDynamics;
import Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent;
import Zeze.Builtin.Game.TaskBase.BTask;
import Zeze.Builtin.Game.TaskBase.BTaskKey;
import Zeze.Builtin.Game.TaskBase.BTaskPhase;
import Zeze.Builtin.Game.TaskBase.TriggerTaskEvent;
import Zeze.Collections.BeanFactory;
import Zeze.Game.Task.NPCTask;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action0;

public abstract class TaskBase<ExtendedBean extends Bean> {
	/**
	 * Task Module
	 */
	public static class Module extends AbstractTaskBase {
		/**
		 * 所有任务的Trigger Rpc，负责中转所有请求
		 */
		@Override
		protected long ProcessTriggerTaskEventRequest(TriggerTaskEvent r) throws Throwable {
			// 检查角色Id
			var roleId = r.Argument.getRoleId();
			var taskInfo = _tRoleTask.get(roleId);
			if (taskInfo == null) {
				r.Result.setResultCode(TaskResultInvalidRoleId);
				return Procedure.Success;
			}

			var eventTypeBean = r.Argument.getTaskEventTypeDynamic().getBean();
			var eventExtendedBean = r.Argument.getExtendedData().getBean();
			if (eventTypeBean instanceof BSpecificTaskEvent) {
				var specificTaskEventBean = (BSpecificTaskEvent) eventTypeBean; // 兼容JDK11
				// 检查任务Id
				var id = specificTaskEventBean.getTaskId();
				var taskBean = taskInfo.getProcessingTasksId().get(id);
				if (null == taskBean) {
					r.Result.setResultCode(TaskResultTaskNotFound);
					return Procedure.Success;
				}

				var task = tasks.get(id);
				task.loadBean(taskBean);
				if (task.accept(eventExtendedBean))
					r.Result.setResultCode(TaskResultAccepted);
				else
					r.Result.setResultCode(TaskResultRejected);
			} else if (eventTypeBean instanceof BBroadcastTaskEvent) {
				var broadcastTaskEventBean = (BBroadcastTaskEvent) eventTypeBean; // 兼容JDK11
				var taskBeanList = taskInfo.getProcessingTasksId().values();
				for (var taskBean : taskBeanList) {
					var id = taskBean.getTaskId();
					var task = tasks.get(id);
					task.loadBean(taskBean);
					if (task.accept(eventExtendedBean))
						if (broadcastTaskEventBean.isIsBreakIfAccepted())
							break;
				}
				r.Result.setResultCode(TaskResultAccepted);
			}
			return Procedure.Success;
		}

		/**
		 * 新建内置任务：NPCTask
		 * (当前public，后续应该改成protected，统一使用loadConfig(String taskConfigTable)读表加载)
		 */
		public NPCTask newNPCTask(TaskBaseOpt opt) {
			return open(opt, NPCTask.class, BNPCTaskDynamics.class);
		}

		/**
		 * 新建非内置任务
		 */
		public <ExtendedBean extends Bean, ExtendedTask extends TaskBase<ExtendedBean>> ExtendedTask newCustomTask(TaskBaseOpt opt, Class<ExtendedTask> extendedTaskClass, Class<ExtendedBean> extendedBeanClass) {
			return open(opt, extendedTaskClass, extendedBeanClass);
		}

		private final ConcurrentHashMap<Long, TaskBase<?>> tasks = new ConcurrentHashMap<>();
		public final ProviderApp providerApp;
		public final Application zeze;
		//		private final TaskGraphics taskGraphics;

		public Module(Application zeze) {
			this.zeze = zeze;
			this.providerApp = zeze.redirect.providerApp;
			RegisterZezeTables(zeze);
			RegisterProtocols(this.providerApp.providerService);
			providerApp.builtinModules.put(this.getFullName(), this);
//			taskGraphics = new TaskGraphics(this);
		}

		/**
		 * 加载任务配置表
		 */
		public void loadConfig(String taskConfigTable) {
		}

		public void register(Class<? extends Bean> cls) {
			beanFactory.register(cls);
			_tEventClasses.getOrAdd(1).getEventClasses().add(cls.getName());
		}

		@Override
		public void UnRegister() {
			if (null != zeze) {
				UnRegisterZezeTables(zeze);
			}
		}

		// 需要在事务内使用。使用完不要保存。
		@SuppressWarnings("unchecked")
		private <ExtendedBean extends Bean, ExtendedTask extends TaskBase<ExtendedBean>> ExtendedTask open(TaskBaseOpt opt, Class<ExtendedTask> extendedTaskClass, Class<ExtendedBean> extendedBeanClass) {
			return (ExtendedTask)tasks.computeIfAbsent(opt.id, key -> {
				try {
					var c = extendedTaskClass.getDeclaredConstructor(Module.class, TaskBase.TaskBaseOpt.class, Class.class);
					return c.newInstance(this, opt, extendedBeanClass);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	// @formatter:off
	/**
	 * Task Constructors
	 */
	public static class TaskBaseOpt{
		public long id;
		public int type;
		public String name;
		public String description;
		public long[] preTaskIds;
	}
	public TaskBase(Module module, TaskBaseOpt opt, Class<ExtendedBean> extendedBeanClass) {
		this.module = module;
		this.bean = this.module._tTask.getOrAdd(new BTaskKey(opt.id));
		this.bean.setTaskId(opt.id);
		this.bean.setTaskType(opt.type);
		this.bean.setTaskState(Module.Invalid);
		this.bean.setTaskName(opt.name);
		this.bean.setTaskDescription(opt.description);
		for (long id :opt.preTaskIds) {
			this.bean.getPreTaskIds().add(id);
		};
		this.bean.getExtendedData().setBean(new EmptyBean());

		MethodHandle extendedBeanConstructor = beanFactory.register(extendedBeanClass);
		bean.getExtendedData().setBean(BeanFactory.invoke(extendedBeanConstructor));

		currentPhase = null;
//		module.taskGraphics.addNewTask(this);
	}

	/**
	 * Task Info:
	 * 1. Task Id
	 * 2. Task Type
	 * 3. Task State
	 * 4. Task Name
	 * 5. Task Description
	 */
	public long getId() { return bean.getTaskId(); }
	public int getType() { return bean.getTaskType(); }
	public int getState() { return bean.getTaskState(); }
	public String getName() { return bean.getTaskName(); }
	public String getDescription() { return bean.getTaskDescription(); }
	public Module getModule() { return module; }
	private final Module module;
	public BTask getBean() { return bean; }
	private BTask bean;
	private final ConcurrentHashMap<Long, TaskPhase> phases = new ConcurrentHashMap<>();
	private TaskPhase currentPhase;
	public void setOnComplete(Action0 callback) { onCompleteUserCallback = callback; }
	Action0 onCompleteUserCallback;
	// @formatter:on

	/**
	 * Runtime方法：accept
	 * - 用于接收事件，改变数据库的数据
	 * - 当满足任务推进情况时，会自动推进任务
	 */
	public boolean accept(Bean eventBean) throws Throwable {
		if (!currentPhase.accept(eventBean))
			return false;

		if (currentPhase.isCompleted())
			if (currentPhase.isEndPhase())
				onComplete();
			else {
				currentPhase.onComplete();
				currentPhase = phases.get(currentPhase.getNextPhaseId());
			}
		return true;
	}

	/**
	 * Runtime方法：isCompleted
	 * - 用于判断任务是否完成
	 */
	public boolean isCompleted() {
		return currentPhase.isEndPhase() && currentPhase.isCompleted();
	}

	/**
	 * Runtime方法：reset
	 * - 将任务回归到初始化状态
	 */
	public void reset() {
		int startNodeSize = 0;
		BTaskPhase startBean = null;
		for (var phaseBean : bean.getTaskPhases().values()) {
			boolean isStart = true;
			for (var id : phaseBean.getAfterPhaseIds()) {
				// 如果没有phase依赖这个phase，意味着这个phase是开始的phase
				if (id == phaseBean.getPhaseId())
					isStart = false;
			}
			if (isStart) {
				++startNodeSize;
				startBean = phaseBean;
			}
		}
		// 理论上只允许一个开始节点，这里暂时不处理过多的开始节点的问题。
		currentPhase.loadBean(startBean);
		currentPhase.reset();
	}

	// ======================================== 任务初始化阶段的方法 ========================================

	/**
	 * loadBean
	 * 将Bean加载为配置表
	 */
	protected final void loadBean(BTask bean) {
		this.bean = bean;
		currentPhase.loadBean(bean.getTaskPhases().get(bean.getCurrentPhaseId()));
		loadExtendedData();
	}

	protected abstract void loadExtendedData();

	/**
	 * 在任务结束后调用的方法，比如：发放奖励。
	 */
	protected final void onComplete() throws Throwable {
		if (isCompleted() && null != onCompleteUserCallback) {
			onCompleteUserCallback.run();
		}
	}

	public TaskPhase addPhase(TaskPhase.TaskPhaseOpt opt) {
		var phase = new TaskPhase(this, opt);
		phases.put(phase.getPhaseId(), phase);
		bean.getTaskPhases().put(phase.getPhaseId(), phase.getBean());
		return phase;
	}

	public void addPhase(TaskPhase phase) {
		// 不能添加不是这个任务的phase
		if (phase.getTask() == this) {
			phases.put(phase.getPhaseId(), phase);
			bean.getTaskPhases().put(phase.getPhaseId(), phase.getBean());
		}
	}

	// ======================================== Private方法和一些不需要被注意的方法 ========================================
	// @formatter:off
	@SuppressWarnings("unchecked")
	public ExtendedBean getExtendedBean() { return (ExtendedBean)bean.getExtendedData().getBean(); }
	private final static BeanFactory beanFactory = new BeanFactory();
	public static long getSpecialTypeIdFromBean(Bean bean) { return BeanFactory.getSpecialTypeIdFromBean(bean); }
	public static Bean createBeanFromSpecialTypeId(long typeId) { return beanFactory.createBeanFromSpecialTypeId(typeId); }
}
