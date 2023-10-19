package Zeze.Util;

import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import Zeze.Application;
import Zeze.Hot.HotGuard;
import Zeze.IModule;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolErrorHandle;
import Zeze.Net.Service;
import Zeze.Raft.RaftRetryException;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.ProcedureStatistics;
import Zeze.Transaction.Transaction;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Task {
	// 默认不开启热更，这个实现希望能被优化掉，几乎不造成影响。
	// 开启热更时，由App.HotManager初始化的时候设置。
	@SuppressWarnings("CanBeFinal")
	public static volatile Factory<HotGuard> hotGuard = () -> null;

	public interface ILogAction {
		void run(Throwable ex, long result, Protocol<?> p, String actionName);
	}

	@SuppressWarnings("CanBeFinal")
	public static volatile long defaultTimeout = 120_000; // 2 minutes

	static final Logger logger = LogManager.getLogger(Task.class);
	private static ExecutorService threadPoolDefault;
	private static ScheduledExecutorService threadPoolScheduled;
	private static ExecutorService threadPoolCritical; // 用来执行内部的一些重要任务，和系统默认 ThreadPool 分开，防止饥饿。
	// private static final ThreadPoolExecutor rpcResponseThreadPool
	//			= (ThreadPoolExecutor)Executors.newCachedThreadPool(new ThreadFactoryWithName("ZezeRespPool"));
	@SuppressWarnings("CanBeFinal")
	public static ILogAction logAction = Task::DefaultLogAction;

	static {
		ShutdownHook.init();
	}

	public static boolean isVirtualThreadEnabled() {
		return ThreadFactoryWithName.isVirtualThreadEnabled();
	}

	public static ExecutorService getThreadPool() {
		return threadPoolDefault;
	}

	public static ScheduledExecutorService getScheduledThreadPool() {
		return threadPoolScheduled;
	}

	public static @NotNull ExecutorService getCriticalThreadPool() {
		return threadPoolCritical;
	}

	// 固定数量的线程池, 普通优先级, 自动支持虚拟线程, 用于处理普通任务
	public static @NotNull ExecutorService newFixedThreadPool(int threadCount, @NotNull String threadNamePrefix) {
		return Executors.newFixedThreadPool(threadCount, ThreadDiagnosable.newFactory(threadNamePrefix));
		/*
		try {
			//noinspection JavaReflectionMemberAccess
			var r = (ExecutorService)Executors.class.getMethod("newVirtualThreadPerTaskExecutor",
					(Class<?>[])null).invoke(null);
			logger.info("newFixedThreadPool({},{}) use virtual thread pool", threadCount, threadNamePrefix);
			return r;
		} catch (ReflectiveOperationException ignored) {
		}

		return Executors.newFixedThreadPool(threadCount, new ThreadFactoryWithName(threadNamePrefix));
		*/
	}

	// 关键线程池, 普通优先级+1, 不使用虚拟线程, 线程数按需增长, 用于处理关键任务, 比普通任务的处理更及时
	public static @NotNull ExecutorService newCriticalThreadPool(@NotNull String threadNamePrefix) {
		return Executors.newCachedThreadPool(ThreadDiagnosable.newFactory(threadNamePrefix, Thread.NORM_PRIORITY + 2));
		/*
		return Executors.newCachedThreadPool(new ThreadFactoryWithName(threadNamePrefix) {
			@Override
			public Thread newThread(Runnable r) {
				var t = new Thread(null, r, namePrefix + threadNumber.getAndIncrement(), 0);
				t.setDaemon(true);
				t.setPriority(Thread.NORM_PRIORITY + 2);
				t.setUncaughtExceptionHandler((__, e) -> logger.error("uncaught exception", e));
				return t;
			}
		});
		*/
	}

	public static synchronized void initThreadPool(@NotNull ExecutorService pool,
												   @NotNull ScheduledExecutorService scheduled) {
		//noinspection ConstantValue
		if (pool == null || scheduled == null)
			throw new IllegalArgumentException();

		if (threadPoolDefault != null || threadPoolScheduled != null)
			throw new IllegalStateException("ThreadPool Has Inited.");
		threadPoolDefault = pool;
		threadPoolScheduled = scheduled;
		threadPoolCritical = newCriticalThreadPool("ZezeCriticalPool");
		ThreadDiagnosable.startDiagnose(30_000);
	}

	public static synchronized boolean tryInitThreadPool(@Nullable Application app, @Nullable ExecutorService pool,
														 @Nullable ScheduledExecutorService scheduled) {
		if (threadPoolDefault != null || threadPoolScheduled != null)
			return false;

		if (pool == null) {
			int workerThreads = app == null ? 240 : (app.getConfig().getWorkerThreads() > 0
					? app.getConfig().getWorkerThreads() : Runtime.getRuntime().availableProcessors() * 30);
			threadPoolDefault = newFixedThreadPool(workerThreads, "ZezeTaskPool");
		} else
			threadPoolDefault = pool;

		if (scheduled == null) {
			int workerThreads = app == null ? 8 : (app.getConfig().getScheduledThreads() > 0
					? app.getConfig().getScheduledThreads() : Runtime.getRuntime().availableProcessors());
			threadPoolScheduled = Executors.newScheduledThreadPool(workerThreads,
					ThreadDiagnosable.newFactory("ZezeScheduledPool"));
			/*
			threadPoolScheduled = Executors.newScheduledThreadPool(workerThreads,
					new ThreadFactoryWithName("ZezeScheduledPool"));
			*/
		} else
			threadPoolScheduled = scheduled;
		threadPoolCritical = newCriticalThreadPool("ZezeCriticalPool");
		ThreadDiagnosable.startDiagnose(30_000);
		return true;
	}

	public static ThreadDiagnosable.Timeout createTimeout(long timeout) {
		var thread = Thread.currentThread();
		return thread instanceof ThreadDiagnosable ? ((ThreadDiagnosable)thread).createTimeout(timeout) : null;
	}

	public static ThreadDiagnosable.Critical enterCritical(boolean critical) {
		var thread = Thread.currentThread();
		return thread instanceof ThreadDiagnosable ? ((ThreadDiagnosable)thread).enterCritical(critical) : null;
	}

	public static void call(@NotNull Action0 action, @Nullable String name) {
		var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
		try {
			action.run();
		} catch (Exception ex) {
			//noinspection ConstantValue
			logger.error("{}", name != null ? name : action != null ? action.getClass().getName() : "", ex);
		} finally {
			//noinspection ConstantValue
			if (PerfCounter.ENABLE_PERF && action != null)
				PerfCounter.instance.addRunInfo(name != null ? name : action.getClass(), System.nanoTime() - timeBegin);
		}
	}

	public static long call(@NotNull FuncLong func, @Nullable String name) {
		var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
		try {
			return func.call();
		} catch (Exception ex) {
			//noinspection ConstantValue
			logger.error("{}", name != null ? name : func != null ? func.getClass().getName() : "", ex);
			return Procedure.Exception;
		} finally {
			//noinspection ConstantValue
			if (PerfCounter.ENABLE_PERF && func != null)
				PerfCounter.instance.addRunInfo(name != null ? name : func.getClass(), System.nanoTime() - timeBegin);
		}
	}

	public static void run(@NotNull Action0 action, @Nullable String name) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			t.runWhileCommit(() -> executeUnsafe(action, name));
		else
			executeUnsafe(action, name);
	}

	public static void run(@NotNull Action0 action, @Nullable String name, @Nullable DispatchMode mode) {
		Transaction t;
		if (mode != DispatchMode.Direct && (t = Transaction.getCurrent()) != null && t.isRunning())
			t.runWhileCommit(() -> executeUnsafe(action, name, mode));
		else
			executeUnsafe(action, name, mode);
	}

	// 注意: 以Unsafe结尾的方法在事务中也会立即异步执行,即使之后该事务redo或rollback也无法撤销,很可能不是想要的结果,所以小心使用
	public static @NotNull Future<?> runUnsafe(@NotNull Action0 action, @Nullable String name) {
		return runUnsafe(action, name, DispatchMode.Normal);
	}

	public static @NotNull Future<?> runUnsafe(@NotNull Action0 action, @Nullable String name,
											   @Nullable DispatchMode mode) {
		return runUnsafe(action, name, mode, defaultTimeout);
	}

	public static @NotNull Future<?> runUnsafe(@NotNull Action0 action, @Nullable String name,
											   @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			var future = new TaskCompletionSource<Long>();
			try {
				action.run();
				future.setResult(0L);
			} catch (Exception e) {
				//noinspection ConstantValue
				logger.error("{}", name != null ? name : action != null ? action.getClass().getName() : "", e);
				future.setException(e);
			} finally {
				//noinspection ConstantValue
				if (PerfCounter.ENABLE_PERF && action != null) {
					PerfCounter.instance.addRunInfo(name != null ? name : action.getClass(),
							System.nanoTime() - timeBegin);
				}
			}
			return future;
		}

		return (mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).submit(() -> {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				action.run();
			} catch (Throwable e) { // logger.error
				//noinspection ConstantValue
				logger.error("{}", name != null ? name : action != null ? action.getClass().getName() : "", e);
			} finally {
				//noinspection ConstantValue
				if (PerfCounter.ENABLE_PERF && action != null) {
					PerfCounter.instance.addRunInfo(name != null ? name : action.getClass(),
							System.nanoTime() - timeBegin);
				}
			}
		});
	}

	public static void executeUnsafe(@NotNull Action0 action, @Nullable String name) {
		executeUnsafe(action, name, DispatchMode.Normal);
	}

	public static void executeUnsafe(@NotNull Action0 action, @Nullable String name,
									 @Nullable DispatchMode mode) {
		return executeUnsafe(action, name, mode, defaultTimeout);
	}

	public static void executeUnsafe(@NotNull Action0 action, @Nullable String name,
									 @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			try {
				action.run();
			} catch (Exception e) {
				//noinspection ConstantValue
				logger.error("{}", name != null ? name : action != null ? action.getClass().getName() : "", e);
			} finally {
				//noinspection ConstantValue
				if (PerfCounter.ENABLE_PERF && action != null) {
					PerfCounter.instance.addRunInfo(name != null ? name : action.getClass(),
							System.nanoTime() - timeBegin);
				}
			}
			return;
		}

		(mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).execute(() -> {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				action.run();
			} catch (Throwable e) { // logger.error
				//noinspection ConstantValue
				logger.error("{}", name != null ? name : action != null ? action.getClass().getName() : "", e);
			} finally {
				//noinspection ConstantValue
				if (PerfCounter.ENABLE_PERF && action != null) {
					PerfCounter.instance.addRunInfo(name != null ? name : action.getClass(),
							System.nanoTime() - timeBegin);
				}
			}
		});
	}

	public static void schedule(long initialDelay, @NotNull Action0 action) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			t.runWhileCommit(() -> scheduleUnsafe(initialDelay, action));
		else
			scheduleUnsafe(initialDelay, action);
	}

	public static @NotNull ScheduledFuture<?> scheduleUnsafe(long initialDelay, @NotNull Action0 action) {
		return scheduleUnsafe(initialDelay, action, defaultTimeout);
	}

	public static @NotNull ScheduledFuture<?> scheduleUnsafe(long initialDelay, @NotNull Action0 action, long timeout) {
		return threadPoolScheduled.schedule(() -> {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				action.run();
			} catch (Throwable e) { // logger.error
				logger.error("schedule", e);
			} finally {
				//noinspection ConstantValue
				if (PerfCounter.ENABLE_PERF && action != null)
					PerfCounter.instance.addRunInfo(action.getClass(), System.nanoTime() - timeBegin);
			}
		}, initialDelay, TimeUnit.MILLISECONDS);
	}

	public static <R> @NotNull Future<R> scheduleUnsafe(long initialDelay, @NotNull Func0<R> func) {
		return scheduleUnsafe(initialDelay, func, defaultTimeout);
	}

	public static <R> @NotNull Future<R> scheduleUnsafe(long initialDelay, @NotNull Func0<R> func, long timeout) {
		return threadPoolScheduled.schedule(() -> {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				return func.call();
			} catch (Throwable e) { // logger.error
				logger.error("schedule", e);
				forceThrow(e);
				return null; // never run here
			} finally {
				//noinspection ConstantValue
				if (PerfCounter.ENABLE_PERF && func != null)
					PerfCounter.instance.addRunInfo(func.getClass(), System.nanoTime() - timeBegin);
			}
		}, initialDelay, TimeUnit.MILLISECONDS);
	}

	public static void scheduleAt(int hour, int minute, @NotNull Action0 action) {
		scheduleAt(hour, minute, -1, action);
	}

	public static void scheduleAt(int hour, int minute, long period, @NotNull Action0 action) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			t.runWhileCommit(() -> scheduleAtUnsafe(hour, minute, period, action));
		else
			scheduleAtUnsafe(hour, minute, period, action);
	}

	public static @NotNull ScheduledFuture<?> scheduleAtUnsafe(int hour, int minute, @NotNull Action0 action) {
		return scheduleAtUnsafe(hour, minute, -1, action);
	}

	public static @NotNull ScheduledFuture<?> scheduleAtUnsafe(int hour, int minute, long period,
															   @NotNull Action0 action) {
		var firstTime = Calendar.getInstance();
		firstTime.set(Calendar.HOUR_OF_DAY, hour);
		firstTime.set(Calendar.MINUTE, minute);
		firstTime.set(Calendar.SECOND, 0);
		firstTime.set(Calendar.MILLISECOND, 0);
		if (firstTime.before(Calendar.getInstance())) // 如果第一次的时间比当前时间早，推到明天。
			firstTime.add(Calendar.DAY_OF_MONTH, 1); // tomorrow!
		var delay = firstTime.getTime().getTime() - System.currentTimeMillis();
		if (period > 0)
			return scheduleUnsafe(delay, period, action);
		return scheduleUnsafe(delay, action);
	}

	public static void schedule(long initialDelay, long period, @NotNull Action0 action) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			t.runWhileCommit(() -> scheduleUnsafe(initialDelay, period, action));
		else
			scheduleUnsafe(initialDelay, period, action);
	}

	public static @NotNull TimerFuture<?> scheduleUnsafe(long initialDelay, long period, @NotNull Action0 action) {
		return scheduleUnsafe(initialDelay, period, action, defaultTimeout);
	}

	public static @NotNull TimerFuture<?> scheduleUnsafe(long initialDelay, long period, @NotNull Action0 action, long timeout) {
		var future = new TimerFuture<>();
		future.setFuture(threadPoolScheduled.scheduleWithFixedDelay(() -> {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			future.lock();
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				//System.out.println(action);
				if (future.isCancelled())
					return;
				action.run();
			} catch (Throwable e) { // logger.error
				logger.error("schedule", e);
			} finally {
				future.unlock();
				//noinspection ConstantValue
				if (PerfCounter.ENABLE_PERF && action != null)
					PerfCounter.instance.addRunInfo(action.getClass(), System.nanoTime() - timeBegin);
			}
		}, initialDelay, period, TimeUnit.MILLISECONDS));
		return future;
	}

	public static void DefaultLogAction(@Nullable Throwable ex, long result, @Nullable Protocol<?> p,
										@NotNull String actionName) {
		// exception -> Error
		// 0 != result -> level from p or Info
		// others -> Trace
		Level level;
		if (ex != null)
			level = Level.ERROR;
		else if (result != 0) {
			Service s;
			Application zeze;
			if (p != null && (s = p.getService()) != null && (zeze = s.getZeze()) != null)
				level = zeze.getConfig().getProcessReturnErrorLogLevel();
			else
				level = Level.INFO;
		} else {
			if (!logger.isTraceEnabled())
				return;
			level = Level.TRACE;
		}
		Object userState;
		String userStateStr = p != null && (userState = p.getUserState()) != null ? " UserState=" + userState : "";

		if (result > 0) {
			logger.log(level, "Action={}{} Return={}@{}:{}", actionName, userStateStr, result,
					IModule.getModuleId(result), IModule.getErrorCode(result), ex);
		} else
			logger.log(level, "Action={}{} Return={}", actionName, userStateStr, result, ex);
	}

	public static void logAndStatistics(long result, @Nullable Protocol<?> p, boolean IsRequestSaved) {
		logAndStatistics(null, result, p, IsRequestSaved, null);
	}

	public static void logAndStatistics(@Nullable Throwable ex, long result, @Nullable Protocol<?> p,
										boolean IsRequestSaved) {
		logAndStatistics(ex, result, p, IsRequestSaved, null);
	}

	public static void logAndStatistics(@Nullable Throwable ex, long result, @Nullable Protocol<?> p,
										boolean IsRequestSaved, @Nullable String aName) {
		var protocolName = p != null ? p.getClass().getName() : "?";
		var actionName = null != aName ? aName : IsRequestSaved ? protocolName : protocolName + ":Response";
		var tmpVolatile = logAction;
		if (tmpVolatile != null) {
			try {
				tmpVolatile.run(ex, result, p, actionName);
			} catch (Exception e) {
				logger.error("LogAndStatistics Exception", e);
			}
		}
		if (Macro.enableStatistics)
			ProcedureStatistics.getInstance().getOrAdd(actionName).getOrAdd(result).increment();
	}

	public static long call(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		return call(func, p, null, null);
	}

	public static long call(@NotNull FuncLong func, @Nullable Protocol<?> p,
							@Nullable ProtocolErrorHandle actionWhenError) {
		return call(func, p, actionWhenError, null);
	}

	public static @NotNull Throwable getRootCause(@NotNull Throwable e) {
		for (; ; ) {
			var c = e.getCause();
			if (c == null)
				return e;
			e = c;
		}
	}

	public static long call(@NotNull FuncLong func, @Nullable Protocol<?> p,
							@Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName) {
		var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
		boolean isRequestSaved = p != null && p.isRequest(); // 记住这个，以后可能会被改变。
		try {
			var result = func.call();
			if (result != 0 && isRequestSaved && actionWhenError != null)
				actionWhenError.handle(p, result);
			logAndStatistics(null, result, p, p == null || isRequestSaved, aName);
			return result;
		} catch (Exception ex) {
			long errorCode;
			var rootEx = getRootCause(ex);
			if (rootEx instanceof TaskCanceledException)
				errorCode = Procedure.CancelException;
			else if (rootEx instanceof RaftRetryException)
				errorCode = Procedure.RaftRetry;
			else
				errorCode = Procedure.Exception;

			if (isRequestSaved && actionWhenError != null) {
				try {
					actionWhenError.handle(p, errorCode);
				} catch (Exception e) {
					logger.error("{}", aName != null ? aName : p.getClass().getName(), e);
				}
			}
			logAndStatistics(ex, errorCode, p, p == null || isRequestSaved, aName);
			return errorCode;
		} finally {
			//noinspection ConstantValue
			if (PerfCounter.ENABLE_PERF && func != null) {
				PerfCounter.instance.addRunInfo(aName != null ? aName : (p != null ? p : func).getClass(),
						System.nanoTime() - timeBegin);
			}
		}
	}

	public static void run(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			t.runWhileCommit(() -> executeUnsafe(func, p));
		else
			executeUnsafe(func, p);
	}

	public static void run(@NotNull FuncLong func, @Nullable Protocol<?> p,
						   @Nullable ProtocolErrorHandle actionWhenError) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			t.runWhileCommit(() -> executeUnsafe(func, p, actionWhenError));
		else
			executeUnsafe(func, p, actionWhenError);
	}

	public static void run(@NotNull FuncLong func, @Nullable Protocol<?> p,
						   @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			t.runWhileCommit(() -> executeUnsafe(func, p, actionWhenError, aName));
		else
			executeUnsafe(func, p, actionWhenError, aName);
	}

	public static void run(@NotNull FuncLong func, @Nullable Protocol<?> p,
						   @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
						   @Nullable DispatchMode mode) {
		Transaction t;
		if (mode != DispatchMode.Direct && (t = Transaction.getCurrent()) != null && t.isRunning())
			t.runWhileCommit(() -> executeUnsafe(func, p, actionWhenError, aName, mode));
		else
			executeUnsafe(func, p, actionWhenError, aName, mode);
	}

	public static @NotNull Future<Long> runUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		return runUnsafe(func, p, null, null, DispatchMode.Normal);
	}

	public static @NotNull Future<Long> runUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
												  @Nullable ProtocolErrorHandle actionWhenError) {
		return runUnsafe(func, p, actionWhenError, null, DispatchMode.Normal);
	}

	public static @NotNull Future<Long> runUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
												  @Nullable ProtocolErrorHandle actionWhenError, String aName) {
		return runUnsafe(func, p, actionWhenError, aName, DispatchMode.Normal);
	}

	public static @NotNull Future<Long> runUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
												  @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
												  @Nullable DispatchMode mode) {
		return runUnsafe(func, p, actionWhenError, aName, mode, defaultTimeout);
	}

	public static @NotNull Future<Long> runUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
												  @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
												  @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			var future = new TaskCompletionSource<Long>();
			future.setResult(call(func, p, actionWhenError, aName));
			return future;
		}

		return (mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).submit(() -> {
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				return call(func, p, actionWhenError, aName);
			} catch (Throwable e) { // logger.error
				logger.error("{}", aName != null ? aName : (p != null ? p.getClass().getName() : null), e);
				return Procedure.Exception;
			}
		});
	}

	public static void executeUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		executeUnsafe(func, p, null, null, DispatchMode.Normal);
	}

	public static void executeUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
									 @Nullable ProtocolErrorHandle actionWhenError) {
		executeUnsafe(func, p, actionWhenError, null, DispatchMode.Normal);
	}

	public static void executeUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
									 @Nullable ProtocolErrorHandle actionWhenError, String aName) {
		executeUnsafe(func, p, actionWhenError, aName, DispatchMode.Normal);
	}

	public static void executeUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
									 @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
									 @Nullable DispatchMode mode) {
		executeUnsafe(func, p, actionWhenError, aName, mode, defaultTimeout);
	}

	public static void executeUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
									 @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
									 @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			call(func, p, actionWhenError, aName);
			return;
		}

		(mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).execute(() -> {
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				call(func, p, actionWhenError, aName);
			} catch (Throwable e) { // logger.error
				logger.error("{}", aName != null ? aName : (p != null ? p.getClass().getName() : null), e);
			}
		});
	}

	public static long call(@NotNull Procedure procedure) {
		return call(procedure, (Protocol<?>)null, null);
	}

	public static long call(@NotNull Procedure procedure, @Nullable Protocol<?> from) {
		return call(procedure, from, null);
	}

	public static long call(@NotNull Procedure procedure, @Nullable Protocol<?> from,
							@Nullable Action2<Protocol<?>, Long> actionWhenError) {
		boolean isRequestSaved = from != null && from.isRequest();
		try {
			// 日志在Call里面记录。因为要支持嵌套。
			// 统计在Call里面实现。
			long result = procedure.call();
			if (result != 0 && isRequestSaved && actionWhenError != null)
				actionWhenError.run(from, result);
			logAndStatistics(null, result, from, from == null || isRequestSaved, procedure.getActionName());
			return result;
		} catch (Exception ex) {
			// Procedure.Call处理了所有错误。应该不会到这里。除非内部错误。
			if (isRequestSaved && actionWhenError != null) {
				try {
					actionWhenError.run(from, Procedure.Exception);
				} catch (Exception e) {
					logger.error("ActionWhenError Exception", e);
				}
			}
			logger.error("{}", procedure, ex);
			return Procedure.Exception;
		}
	}

	public static long call(@NotNull Procedure procedure, @NotNull OutObject<Protocol<?>> outProtocol,
							@Nullable Action2<Protocol<?>, Long> actionWhenError) {
		Protocol<?> from = null;
		try {
			// 日志在Call里面记录。因为要支持嵌套。
			// 统计在Call里面实现。
			long result = procedure.call();
			from = outProtocol.value;
			if (result != 0 && from != null && from.isRequest() && actionWhenError != null)
				actionWhenError.run(from, result);
			logAndStatistics(null, result, from, from == null || from.isRequest(), procedure.getActionName());
			return result;
		} catch (Exception ex) {
			// Procedure.Call处理了所有错误。应该不会到这里。除非内部错误。
			if (from != null && from.isRequest() && actionWhenError != null) {
				try {
					actionWhenError.run(from, Procedure.Exception);
				} catch (Exception e) {
					logger.error("ActionWhenError Exception", e);
				}
			}
			logger.error("{}", procedure, ex);
			return Procedure.Exception;
		}
	}

	public static void run(@NotNull Procedure procedure) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			t.runWhileCommit(() -> executeUnsafe(procedure));
		else
			executeUnsafe(procedure);
	}

	public static void run(@NotNull Procedure procedure, @Nullable Protocol<?> from) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			t.runWhileCommit(() -> executeUnsafe(procedure, from));
		else
			executeUnsafe(procedure, from);
	}

	public static void run(@NotNull Procedure procedure, @Nullable Protocol<?> from,
						   @Nullable Action2<Protocol<?>, Long> actionWhenError) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			t.runWhileCommit(() -> executeUnsafe(procedure, from, actionWhenError));
		else
			executeUnsafe(procedure, from, actionWhenError);
	}

	public static void run(@NotNull Procedure procedure, @Nullable Protocol<?> from,
						   @Nullable Action2<Protocol<?>, Long> actionWhenError, @Nullable DispatchMode mode) {
		Transaction t;
		if (mode != DispatchMode.Direct && (t = Transaction.getCurrent()) != null && t.isRunning())
			t.runWhileCommit(() -> executeUnsafe(procedure, from, actionWhenError, mode));
		else
			executeUnsafe(procedure, from, actionWhenError, mode);
	}

	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure) {
		return runUnsafe(procedure, DispatchMode.Normal);
	}

	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from) {
		return runUnsafe(procedure, from, null, DispatchMode.Normal);
	}

	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
												  @Nullable Action2<Protocol<?>, Long> actionWhenError) {
		return runUnsafe(procedure, from, actionWhenError, DispatchMode.Normal);
	}

	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure, @Nullable DispatchMode mode) {
		return runUnsafe(procedure, (Protocol<?>)null, null, mode);
	}

	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
												  @Nullable Action2<Protocol<?>, Long> actionWhenError,
												  @Nullable DispatchMode mode) {
		return runUnsafe(procedure, from, actionWhenError, mode, defaultTimeout);
	}

	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
												  @Nullable Action2<Protocol<?>, Long> actionWhenError,
												  @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			var future = new TaskCompletionSource<Long>();
			future.setResult(call(procedure, from, actionWhenError));
			return future;
		}

		return (mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).submit(() -> {
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				return call(procedure, from, actionWhenError);
			} catch (Throwable e) { // logger.error
				logger.error("{}", procedure, e);
				return Procedure.Exception;
			}
		});
	}

	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure,
												  @NotNull OutObject<Protocol<?>> outProtocol,
												  @Nullable Action2<Protocol<?>, Long> actionWhenError,
												  @Nullable DispatchMode mode) {
		return runUnsafe(procedure, outProtocol, actionWhenError, mode, defaultTimeout);
	}

	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure,
												  @NotNull OutObject<Protocol<?>> outProtocol,
												  @Nullable Action2<Protocol<?>, Long> actionWhenError,
												  @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			var future = new TaskCompletionSource<Long>();
			future.setResult(call(procedure, outProtocol, actionWhenError));
			return future;
		}

		return (mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).submit(() -> {
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				return call(procedure, outProtocol, actionWhenError);
			} catch (Throwable e) { // logger.error
				logger.error("{}", procedure, e);
				return Procedure.Exception;
			}
		});
	}

	public static void executeUnsafe(@NotNull Procedure procedure) {
		executeUnsafe(procedure, DispatchMode.Normal);
	}

	public static void executeUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from) {
		executeUnsafe(procedure, from, null, DispatchMode.Normal);
	}

	public static void executeUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
									 @Nullable Action2<Protocol<?>, Long> actionWhenError) {
		executeUnsafe(procedure, from, actionWhenError, DispatchMode.Normal);
	}

	public static void executeUnsafe(@NotNull Procedure procedure, @Nullable DispatchMode mode) {
		executeUnsafe(procedure, (Protocol<?>)null, null, mode);
	}

	public static void executeUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
									 @Nullable Action2<Protocol<?>, Long> actionWhenError,
									 @Nullable DispatchMode mode) {
		executeUnsafe(procedure, from, actionWhenError, mode, defaultTimeout);
	}

	public static void executeUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
									 @Nullable Action2<Protocol<?>, Long> actionWhenError,
									 @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			call(procedure, from, actionWhenError);
			return;
		}

		(mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).execute(() -> {
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				call(procedure, from, actionWhenError);
			} catch (Throwable e) { // logger.error
				logger.error("{}", procedure, e);
			}
		});
	}

	public static void executeUnsafe(@NotNull Procedure procedure, @NotNull OutObject<Protocol<?>> outProtocol,
									 @Nullable Action2<Protocol<?>, Long> actionWhenError,
									 @Nullable DispatchMode mode) {
		executeUnsafe(procedure, outProtocol, actionWhenError, mode, defaultTimeout);
	}

	public static void executeUnsafe(@NotNull Procedure procedure, @NotNull OutObject<Protocol<?>> outProtocol,
									 @Nullable Action2<Protocol<?>, Long> actionWhenError,
									 @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			call(procedure, outProtocol, actionWhenError);
			return;
		}

		(mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).execute(() -> {
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				call(procedure, outProtocol, actionWhenError);
			} catch (Throwable e) { // logger.error
				logger.error("{}", procedure, e);
			}
		});
	}

	public static void runRpcResponse(@NotNull Procedure procedure) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			t.runWhileCommit(() -> executeRpcResponseUnsafe(procedure));
		else
			executeRpcResponseUnsafe(procedure);
	}

	public static void runRpcResponse(@NotNull Procedure procedure, @Nullable DispatchMode mode) {
		Transaction t;
		if (mode != DispatchMode.Direct && (t = Transaction.getCurrent()) != null && t.isRunning())
			t.runWhileCommit(() -> executeRpcResponseUnsafe(procedure, mode));
		else
			executeRpcResponseUnsafe(procedure, mode);
	}

	public static void runRpcResponse(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			t.runWhileCommit(() -> executeRpcResponseUnsafe(func, p));
		else
			executeRpcResponseUnsafe(func, p);
	}

	public static void runRpcResponse(@NotNull FuncLong func, @Nullable Protocol<?> p, @Nullable DispatchMode mode) {
		Transaction t;
		if (mode != DispatchMode.Direct && (t = Transaction.getCurrent()) != null && t.isRunning())
			t.runWhileCommit(() -> executeRpcResponseUnsafe(func, p, mode));
		else
			executeRpcResponseUnsafe(func, p, mode);
	}

	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull Procedure procedure) {
		return runRpcResponseUnsafe(procedure, DispatchMode.Normal);
	}

	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull Procedure procedure,
															 @Nullable DispatchMode mode) {
		return runRpcResponseUnsafe(procedure, mode, defaultTimeout);
	}

	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull Procedure procedure,
															 @Nullable DispatchMode mode,
															 long timeout) {
		if (mode == DispatchMode.Direct) {
			var future = new TaskCompletionSource<Long>();
			future.setResult(call(procedure));
			return future;
		}

		return (mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).submit(() -> { // rpcResponseThreadPool
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				return call(procedure);
			} catch (Throwable e) { // logger.error
				logger.error("{}", procedure, e);
				return Procedure.Exception;
			}
		});
	}

	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		return runRpcResponseUnsafe(func, p, DispatchMode.Normal);
	}

	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
															 @Nullable DispatchMode mode) {
		return runRpcResponseUnsafe(func, p, mode, defaultTimeout);
	}

	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
															 @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			var future = new TaskCompletionSource<Long>();
			future.setResult(call(func, p));
			return future;
		}

		return (mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).submit(() -> { // rpcResponseThreadPool
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				return call(func, p);
			} catch (Throwable e) { // logger.error
				logger.error("{}", p != null ? p.getClass().getName() : null, e);
				return Procedure.Exception;
			}
		});
	}

	public static void executeRpcResponseUnsafe(@NotNull Procedure procedure) {
		executeRpcResponseUnsafe(procedure, DispatchMode.Normal);
	}

	public static void executeRpcResponseUnsafe(@NotNull Procedure procedure,
												@Nullable DispatchMode mode) {
		executeRpcResponseUnsafe(procedure, mode, defaultTimeout);
	}

	public static void executeRpcResponseUnsafe(@NotNull Procedure procedure,
												@Nullable DispatchMode mode,
												long timeout) {
		if (mode == DispatchMode.Direct) {
			call(procedure);
			return;
		}

		(mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).execute(() -> { // rpcResponseThreadPool
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				call(procedure);
			} catch (Throwable e) { // logger.error
				logger.error("{}", procedure, e);
			}
		});
	}

	public static void executeRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		executeRpcResponseUnsafe(func, p, DispatchMode.Normal);
	}

	public static void executeRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
												@Nullable DispatchMode mode) {
		executeRpcResponseUnsafe(func, p, mode, defaultTimeout);
	}

	public static void executeRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
												@Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			call(func, p);
			return;
		}

		(mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).execute(() -> { // rpcResponseThreadPool
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				call(func, p);
			} catch (Throwable e) { // logger.error
				logger.error("{}", p != null ? p.getClass().getName() : null, e);
			}
		});
	}

	public static void waitAll(@NotNull Collection<Future<?>> tasks) {
		for (var task : tasks) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				forceThrow(e);
			}
		}
	}

	public static void waitAll(Future<?> @NotNull [] tasks) {
		for (var task : tasks) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				forceThrow(e);
			}
		}
	}

	// 利用编译器的漏洞(?)强制抛出任何异常,调用者不必声明throws或包装成RuntimeException,建议只在必要时使用
	@SuppressWarnings("unchecked")
	public static <E extends Throwable> void forceThrow(Throwable e) throws E {
		throw (E)e;
	}

	private Task() {
	}
}
