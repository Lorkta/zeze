package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 同TaskOneByOneByKey,只是用ConcurrentLinkedQueue代替ArrayDeque和锁.
 * 另外由于不用临界区,shutdown和加任务的并发很难做,所以暂时不支持shutdown,也不支持cancel了.
 */
public final class TaskOneByOneByKey2 {
	private static final Logger logger = LogManager.getLogger(TaskOneByOneByKey2.class);
	private static final VarHandle vhSubmitted;

	static {
		try {
			vhSubmitted = MethodHandles.lookup().findVarHandle(TaskOneByOne.class, "submitted", boolean.class);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private final TaskOneByOne[] concurrency;
	private final int hashMask;
	private final Executor executor;

	public TaskOneByOneByKey2(Executor executor) {
		this(1024, executor);
	}

	public TaskOneByOneByKey2() {
		this(1024, null);
	}

	public TaskOneByOneByKey2(int concurrencyLevel) {
		this(concurrencyLevel, null);
	}

	public TaskOneByOneByKey2(int concurrencyLevel, Executor executor) {
		this.executor = executor;
		if (concurrencyLevel < 1 || concurrencyLevel > 0x4000_0000)
			throw new IllegalArgumentException("Illegal concurrencyLevel: " + concurrencyLevel);

		int capacity = 1;
		while (capacity < concurrencyLevel)
			capacity <<= 1;
		concurrency = new TaskOneByOne[capacity];
		for (int i = 0; i < concurrency.length; i++)
			concurrency[i] = new TaskOneByOne();
		hashMask = capacity - 1;
	}

	public int getConcurrencyLevel() {
		return concurrency.length;
	}

	public int getQueueSize(int index) {
		return index >= 0 && index < concurrency.length ? concurrency[index].queue.size() : -1; // 可能有并发问题导致结果不准确,但通常问题不大
	}

	static abstract class Barrier {
		private static final VarHandle vhCount;

		static {
			try {
				vhCount = MethodHandles.lookup().findVarHandle(Barrier.class, "count", int.class);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}

		private final ConcurrentHashSet<TaskOneByOne> reached = new ConcurrentHashSet<>();
		@SuppressWarnings("FieldMayBeFinal")
		private volatile int count;

		Barrier(int count) {
			this.count = count;
		}

		abstract String getName();

		abstract void run() throws Exception;

		private void reachedRunNext() {
			for (var taskOneByOne : reached)
				taskOneByOne.runNext();
		}

		void reach(TaskOneByOne taskOneByOne, int sum) {
			reached.add(taskOneByOne);
			if ((int)vhCount.getAndAdd(this, -sum) > sum)
				return;

			try {
				run();
			} catch (Exception ex) {
				logger.error("{} run exception:", getName(), ex);
			} finally {
				// 成功执行
				// 1. 触发所有桶的runNext，
				// 2. 自己也返回false，不再继续runNext。
				reachedRunNext();
			}
		}
	}

	static final class BarrierProcedure extends Barrier {
		private final Procedure procedure;

		BarrierProcedure(Procedure procedure, int count) {
			super(count);
			this.procedure = procedure;
		}

		@Override
		String getName() {
			return procedure.getActionName();
		}

		@Override
		void run() {
			procedure.call();
		}
	}

	static final class BarrierAction extends Barrier {
		private final Action0 action;
		private final String actionName;

		BarrierAction(String actionName, Action0 action, int count) {
			super(count);
			this.action = action;
			this.actionName = actionName;
		}

		@Override
		String getName() {
			return actionName;
		}

		@Override
		void run() throws Exception {
			action.run();
		}
	}

	public synchronized <T> void executeCyclicBarrier(Collection<T> keys, Procedure procedure, DispatchMode mode) {
		if (keys.isEmpty())
			throw new IllegalArgumentException("CyclicBarrier keys is empty.");

		var group = new HashMap<TaskOneByOne, OutInt>();
		int count = 0;
		for (var key : keys) {
			group.computeIfAbsent(bucket(key), __ -> new OutInt()).value++;
			count++;
		}
		var barrier = new BarrierProcedure(procedure, count);
		for (var e : group.entrySet()) {
			var sum = e.getValue().value;
			e.getKey().executeBarrier(barrier, sum, mode);
		}
	}

	public synchronized <T> void executeCyclicBarrier(Collection<T> keys, String actionName, Action0 action,
													  DispatchMode mode) {
		if (keys.isEmpty())
			throw new IllegalArgumentException("CyclicBarrier keys is empty.");

		var group = new HashMap<TaskOneByOne, OutInt>();
		int count = 0;
		for (var key : keys) {
			group.computeIfAbsent(bucket(key), __ -> new OutInt()).value++;
			count++;
		}
		var barrier = new BarrierAction(actionName, action, count);
		for (var e : group.entrySet()) {
			var sum = e.getValue().value;
			e.getKey().executeBarrier(barrier, sum, mode);
		}
	}

	public static class Batch<T> {
		private final AtomicInteger keysCount;
		private final Action1<T> action;
		private final Action0 batchEnd;

		public Batch(int keysSize, Action1<T> action, Action0 batchEnd) {
			this.keysCount = new AtomicInteger(keysSize);
			this.action = action;
			this.batchEnd = batchEnd;
		}

		public void run(T key) throws Exception {
			action.run(key);
			if (keysCount.decrementAndGet() == 0)
				batchEnd.run();
		}
	}

	public <T> void executeBatch(Collection<T> keys, Action1<T> action, Action0 batchEnd, DispatchMode mode) {
		var batch = new Batch<>(keys.size(), action, batchEnd);
		for (var key : keys)
			Execute(key, () -> batch.run(key), mode);
	}

	public void executeBatch(LongList keys, Action1<Long> action, Action0 batchEnd, DispatchMode mode) {
		var batch = new Batch<>(keys.size(), action, batchEnd);
		keys.foreach((key) -> Execute(key, () -> batch.run(key), mode));
	}

	public void Execute(Object key, Action0 action) {
		Execute(key.hashCode(), action, DispatchMode.Normal);
	}

	public void Execute(Object key, Action0 action, DispatchMode mode) {
		Execute(key.hashCode(), action, mode);
	}

	public void Execute(Object key, Action0 action, String name) {
		Execute(key.hashCode(), action, name, DispatchMode.Normal);
	}

	public void Execute(Object key, Action0 action, String name, DispatchMode mode) {
		Execute(key.hashCode(), action, name, mode);
	}

	public void Execute(Object key, Func0<?> func) {
		Execute(key.hashCode(), func, DispatchMode.Normal);
	}

	public void Execute(Object key, Func0<?> func, DispatchMode mode) {
		Execute(key.hashCode(), func, mode);
	}

	public void Execute(Object key, Func0<?> func, String name) {
		Execute(key.hashCode(), func, name, DispatchMode.Normal);
	}

	public void Execute(Object key, Func0<?> func, String name, DispatchMode mode) {
		Execute(key.hashCode(), func, name, mode);
	}

	public void Execute(Object key, Procedure procedure) {
		Execute(key.hashCode(), procedure, DispatchMode.Normal);
	}

	public void Execute(Object key, Procedure procedure, DispatchMode mode) {
		Execute(key.hashCode(), procedure, mode);
	}

	public void Execute(int key, Action0 action) {
		Execute(key, action, null, DispatchMode.Normal);
	}

	public void Execute(int key, Action0 action, DispatchMode mode) {
		Execute(key, action, null, mode);
	}

	public void Execute(int key, Action0 action, String name) {
		Execute(key, action, name, DispatchMode.Normal);
	}

	public void Execute(int key, Action0 action, String name, DispatchMode mode) {
		if (action == null)
			throw new IllegalArgumentException("null action");
		concurrency[hash(key) & hashMask].execute(action, name, mode);
	}

	private TaskOneByOne bucket(Object key) {
		return concurrency[hash(key.hashCode()) & hashMask];
	}

	public void Execute(int key, Func0<?> func) {
		Execute(key, func, null, DispatchMode.Normal);
	}

	public void Execute(int key, Func0<?> func, DispatchMode mode) {
		Execute(key, func, null, mode);
	}

	public void Execute(int key, Func0<?> func, String name) {
		Execute(key, func, name, DispatchMode.Normal);
	}

	public void Execute(int key, Func0<?> func, String name, DispatchMode mode) {
		if (func == null)
			throw new IllegalArgumentException("null func");
		concurrency[hash(key) & hashMask].execute(func, name, mode);
	}

	public void Execute(int key, Procedure procedure) {
		Execute(key, procedure, DispatchMode.Normal);
	}

	public void Execute(int key, Procedure procedure, DispatchMode mode) {
		concurrency[hash(key) & hashMask].execute(procedure::call, procedure.getActionName(), mode);
	}

	public void Execute(long key, Action0 action) {
		Execute(Long.hashCode(key), action, DispatchMode.Normal);
	}

	public void Execute(long key, Action0 action, DispatchMode mode) {
		Execute(Long.hashCode(key), action, mode);
	}

	public void Execute(long key, Action0 action, String name) {
		Execute(Long.hashCode(key), action, name, DispatchMode.Normal);
	}

	public void Execute(long key, Action0 action, String name, DispatchMode mode) {
		Execute(Long.hashCode(key), action, name, mode);
	}

	public void Execute(long key, Func0<?> func) {
		Execute(Long.hashCode(key), func, DispatchMode.Normal);
	}

	public void Execute(long key, Func0<?> func, DispatchMode mode) {
		Execute(Long.hashCode(key), func, mode);
	}

	public void Execute(long key, Func0<?> func, String name) {
		Execute(Long.hashCode(key), func, name, DispatchMode.Normal);
	}

	public void Execute(long key, Func0<?> func, String name, DispatchMode mode) {
		Execute(Long.hashCode(key), func, name, mode);
	}

	public void Execute(long key, Procedure procedure) {
		Execute(Long.hashCode(key), procedure, DispatchMode.Normal);
	}

	public void Execute(long key, Procedure procedure, DispatchMode mode) {
		Execute(Long.hashCode(key), procedure, mode);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		for (int i = 0; i < concurrency.length; i++) {
			var s = concurrency[i].toString();
			if (s.length() > 2)
				sb.append(i).append(": ").append(s).append('\n');
		}
		return sb.toString();
	}

	/**
	 * Applies a supplemental hash function to a given hashCode, which defends
	 * against poor quality hash functions. This is critical because HashMap uses
	 * power-of-two length hash tables, that otherwise encounter collisions for
	 * hashCodes that do not differ in lower bits. Note: Null keys always map to
	 * hash 0, thus index 0.
	 *
	 * @see HashMap
	 */
	private static int hash(int _h) {
		int h = _h;
		h ^= (h >>> 20) ^ (h >>> 12);
		return (h ^ (h >>> 7) ^ (h >>> 4));
	}

	private Executor getExecutor(DispatchMode mode) {
		return executor != null ? executor : (mode == DispatchMode.Critical
				? Zeze.Util.Task.getCriticalThreadPool()
				: Zeze.Util.Task.getThreadPool());
	}

	final class TaskOneByOne implements Runnable {
		abstract class Task {
			final String name;
			final DispatchMode mode;

			Task(String name, DispatchMode mode) {
				this.name = name;
				this.mode = mode;
			}

			abstract boolean process();
		}

		final class TaskAction extends Task {
			private final Action0 action;

			TaskAction(Action0 action, String name, DispatchMode mode) {
				super(name != null ? name : action.getClass().getName(), mode);
				this.action = action;
			}

			@Override
			boolean process() {
				try {
					action.run();
				} catch (Exception e) {
					logger.error("TaskAction run exception: {}", name, e);
				}
				return true;
			}
		}

		final class TaskFunc extends Task {
			private final Func0<?> func;

			TaskFunc(Func0<?> func, String name, DispatchMode mode) {
				super(name, mode);
				this.func = func;
			}

			@Override
			boolean process() {
				try {
					func.call();
				} catch (Exception e) {
					logger.error("TaskFunc run exception: {}", name, e);
				}
				return true;
			}
		}

		final class TaskBarrierProcedure extends Task {
			private final BarrierProcedure barrier;
			private final int sum;

			TaskBarrierProcedure(BarrierProcedure barrier, int sum, DispatchMode mode) {
				super(barrier.getName(), mode);
				this.barrier = barrier;
				this.sum = sum;
			}

			@Override
			boolean process() {
				barrier.reach(TaskOneByOne.this, sum);
				return false;
			}
		}

		final class TaskBarrierAction extends Task {
			private final BarrierAction barrier;
			private final int sum;

			TaskBarrierAction(BarrierAction barrier, int sum, DispatchMode mode) {
				super(barrier.actionName, mode);
				this.barrier = barrier;
				this.sum = sum;
			}

			@Override
			boolean process() {
				barrier.reach(TaskOneByOne.this, sum);
				return false;
			}
		}

		private final ConcurrentLinkedQueue<Task> queue = new ConcurrentLinkedQueue<>();
		private volatile boolean submitted;

		void execute(Action0 action, String name, DispatchMode mode) {
			submit(new TaskAction(action, name, mode));
		}

		void execute(Func0<?> func, String name, DispatchMode mode) {
			submit(new TaskFunc(func, name, mode));
		}

		void executeBarrier(BarrierProcedure barrier, int sum, DispatchMode mode) {
			submit(new TaskBarrierProcedure(barrier, sum, mode));
		}

		void executeBarrier(BarrierAction barrier, int sum, DispatchMode mode) {
			submit(new TaskBarrierAction(barrier, sum, mode));
		}

		private void submit(Task task) {
			queue.offer(task);
			if ((boolean)vhSubmitted.compareAndSet(this, false, true))
				runNext();
		}

		private Task pollTask() {
			for (; ; ) {
				var task = queue.poll();
				if (task != null)
					return task;
				submitted = false;
				task = queue.peek();
				if (task == null || !(boolean)vhSubmitted.compareAndSet(this, false, true))
					return null;
			}
		}

		private Task peekTask() {
			for (; ; ) {
				var task = queue.peek();
				if (task != null)
					return task;
				submitted = false;
				task = queue.peek();
				if (task == null || !(boolean)vhSubmitted.compareAndSet(this, false, true))
					return null;
			}
		}

		void runNext() {
			var task = peekTask();
			if (task != null)
				getExecutor(task.mode).execute(this);
		}

		@Override
		public void run() {
			var task = pollTask();
			if (task == null)
				return;
			for (var mode = task.mode; ; queue.poll()) {
				if (!task.process())
					return; // 稍后通过runNext继续执行
				task = peekTask();
				if (task == null)
					return;
				if (task.mode != mode) {
					getExecutor(task.mode).execute(this);
					return;
				}
			}
		}

		@Override
		public String toString() {
			var sb = new StringBuilder().append('[');
			for (var task : queue)
				sb.append(task.name).append(',');
			int n = sb.length();
			if (n > 1)
				sb.setLength(n - 1);
			return sb.append(']').toString();
		}
	}
}