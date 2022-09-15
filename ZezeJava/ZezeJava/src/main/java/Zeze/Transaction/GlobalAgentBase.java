package Zeze.Transaction;

import java.util.ArrayList;
import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Services.AchillesHeelConfig;
import Zeze.Util.Task;

public abstract class GlobalAgentBase {
	public final Zeze.Application zeze;
	private AchillesHeelConfig config;
	private volatile long activeTime = System.currentTimeMillis();
	protected int globalCacheManagerHashIndex;
	private volatile Releaser releaser;

	public GlobalAgentBase(Zeze.Application zeze) {
		this.zeze = zeze;
		config = new AchillesHeelConfig(1500, 1000, 10 * 1000);
	}

	public final AchillesHeelConfig getConfig() {
		return config;
	}

	public final long getActiveTime() {
		return activeTime;
	}

	public final void setActiveTime(long value) {
		activeTime = value;
		zeze.getAchillesHeelDaemon().setProcessDaemonActiveTime(this, value);
	}

	public boolean isReleasing() {
		return releaser != null;
	}

	public final void initialize(int maxNetPing, int serverProcessTime, int serverReleaseTimeout) {
		config = new AchillesHeelConfig(maxNetPing, serverProcessTime, serverReleaseTimeout);
		zeze.getAchillesHeelDaemon().onInitialize(this);
	}

	public enum CheckReleaseResult {
		NoRelease,
		Releasing,
		Timeout,
	}

	public CheckReleaseResult checkReleaseTimeout(long now, int timeout) {
		var r = releaser;
		if (r == null)
			return CheckReleaseResult.NoRelease;

		if (r.isCompletedSuccessfully()) {
			releaser = null;
			// 每次成功Release，设置一次活动时间，阻止AchillesHeelDaemon马上再次触发Release。
			setActiveTime(System.currentTimeMillis());
			return CheckReleaseResult.NoRelease;
		}

		return now - r.startTime > timeout ? CheckReleaseResult.Timeout : CheckReleaseResult.Releasing;
	}

	public static class Releaser {
		public final int globalIndex;
		public final long startTime = System.currentTimeMillis();
		public final ArrayList<Future<Boolean>> tasks = new ArrayList<>();
		public final Runnable endAction;

		public final boolean isCompletedSuccessfully() {
			try {
				for (var task : tasks) {
					if (!task.isDone() || !task.get())
						return false;
				}
				if (endAction != null)
					endAction.run();
				return true;
			} catch (Exception e) {
				return false;
			}
		}

		public Releaser(Application zeze, int index, Runnable endAction) {
			this.endAction = endAction;
			globalIndex = index;
			for (var database : zeze.getDatabases().values()) {
				for (var table : database.getTables()) {
					if (!table.isMemory()) {
						tasks.add(Task.getCriticalThreadPool().submit(() -> {
							table.reduceInvalidAllLocalOnly(index);
							return true;
						}));
					}
				}
			}
		}
	}

	// 开始释放本地锁。
	// 1.【要并发，要快】启动线程池来执行，释放锁除了需要和应用互斥，没有其他IO操作，基本上都是cpu。
	// 2. 超时没有释放完成，程序中止。see tryHalt。
	// 3. 每个Global服务一个Releaser.
	public void startRelease(Application zeze, Runnable endAction) {
		synchronized (this) {
			releaser = new Releaser(zeze, globalCacheManagerHashIndex, endAction);
		}
		cancelPending();
	}

	protected abstract void cancelPending();

	public abstract void keepAlive();
}
