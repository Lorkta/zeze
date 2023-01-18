package Zeze.Net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Selector extends Thread implements ByteBufferAllocator {
	private static final Logger logger = LogManager.getLogger(Selector.class);

	// 以下常量为临时兼容,以后会去掉,应该改用Selectors里的
	public static final int DEFAULT_BUFFER_SIZE = 32 * 1024; // 单个buffer的字节容量
	public static final int DEFAULT_BBPOOL_LOCAL_CAPACITY = 1000; // 本地池的最大保留buffer数量
	public static final int DEFAULT_BBPOOL_MOVE_COUNT = 1000; // 本地池和全局池之间移动一次的buffer数量
	public static final int DEFAULT_BBPOOL_GLOBAL_CAPACITY = 100 * DEFAULT_BBPOOL_MOVE_COUNT; // 全局池的最大buffer数量
	public static final int DEFAULT_SELECT_TIMEOUT = 0; // 0表示无超时,>0表示每次select的超时毫秒数

	private final Selectors selectors;
	private final java.nio.channels.Selector selector;
	private final ByteBuffer readBuffer; // 此线程共享的buffer,只能临时使用
	private final AtomicInteger wakeupNotified = new AtomicInteger();
	private final ArrayList<ByteBuffer> bbPool = new ArrayList<>();
	private boolean firstAction;
	private volatile boolean running = true;

//	public final AtomicLong wakeupCount0 = new AtomicLong();
//	public final AtomicLong wakeupCount1 = new AtomicLong();
//	public final AtomicLong wakeupTime = new AtomicLong();
//	public long lastTime;

	public Selector(Selectors selectors, String threadName) throws IOException {
		super(threadName);
		setDaemon(true);
		this.selectors = selectors;
		selector = java.nio.channels.Selector.open();
		readBuffer = ByteBuffer.allocate(selectors.getReadBufferSize());
	}

	public Selectors getSelectors() {
		return selectors;
	}

	ByteBuffer getReadBuffer() {
		return readBuffer;
	}

	@Override
	public ByteBuffer alloc() {
		int n = bbPool.size();
		if (n <= 0) {
			var bbPoolGlobalCapacity = selectors.getBbPoolGlobalCapacity();
			if (bbPoolGlobalCapacity > 0) {
				var bbGlobalPoolLock = selectors.getBbGlobalPoolLock();
				bbGlobalPoolLock.lock();
				try {
					var bbGlobalPool = selectors.getBbGlobalPool();
					int bbPoolMoveCount = selectors.getBbPoolMoveCount();
					int gn = bbGlobalPool.size();
					if (gn >= bbPoolMoveCount) {
						var bbMoves = bbGlobalPool.subList(gn - bbPoolMoveCount, gn);
						bbPool.addAll(bbMoves);
						bbMoves.clear();
					}
				} finally {
					bbGlobalPoolLock.unlock();
				}
				n = bbPool.size();
			}
		}
		return n > 0 ? bbPool.remove(n - 1) : ByteBuffer.allocateDirect(selectors.getBbPoolBlockSize());
	}

	@Override
	public void free(ByteBuffer bb) {
		int bbPoolLocalCapacity = selectors.getBbPoolLocalCapacity();
		int bbPoolMoveCount = selectors.getBbPoolMoveCount();
		int n = bbPool.size();
		if (n >= bbPoolLocalCapacity + bbPoolMoveCount) { // 可以释放一批
			var bbPoolGlobalCapacity = selectors.getBbPoolGlobalCapacity();
			if (bbPoolGlobalCapacity > 0) {
				var bbGlobalPool = selectors.getBbGlobalPool();
				var bbMoves = bbPool.subList(n - bbPoolMoveCount, n);
				var bbGlobalPoolLock = selectors.getBbGlobalPoolLock();
				bbGlobalPoolLock.lock();
				try {
					if (bbGlobalPool.size() < bbPoolGlobalCapacity) // 全局池也放不下就丢弃这次搬移的buffers
						bbGlobalPool.addAll(bbMoves);
				} finally {
					bbGlobalPoolLock.unlock();
					bbMoves.clear();
				}
			}
		}
		bb.position(0);
		bb.limit(bb.capacity());
		bbPool.add(bb);
	}

	SelectionKey register(SelectableChannel sc, int ops, SelectorHandle handle) {
		try {
			SelectionKey key = sc.register(selector, ops, handle);
			// 当引擎线程执行register时，wakeup会导致一次多余唤醒。
			// 这在连接建立不是很繁忙的应用中问题不大。
			// 下面通过判断是否本线程来决定是否调用wakeup。
			if (Thread.currentThread() != this)
				selector.wakeup(); // 不会丢失。
			return key;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() {
		running = false;
		selector.wakeup();

		// join
		while (true) {
			try {
				join();
				break;
			} catch (Exception ex) {
				logger.error("{} close skip.", getClass().getName(), ex);
			}
		}

		try {
			selector.close();
		} catch (Exception e) {
			logger.error("{} selector.close skip.", getClass().getName(), e);
		}
	}

	private static final class WakeupThread {
		private static final ArrayBlockingQueue<java.nio.channels.Selector> wakeupQueue = new ArrayBlockingQueue<>(256);

		static {
			var thread = new Thread("WakeupThread") {
				@Override
				public void run() {
					try {
						//noinspection InfiniteLoopStatement
						for (; ; )
							wakeupQueue.take().wakeup();
					} catch (InterruptedException e) {
						logger.error("WakeupThread interrupted:", e);
					}
				}
			};
			thread.setDaemon(true);
			thread.setUncaughtExceptionHandler((__, e) -> logger.error("uncaught exception", e));
			thread.start();
		}

		public static void postWakeup(java.nio.channels.Selector selector) {
			if (!wakeupQueue.offer(selector))
				selector.wakeup();
		}
	}

	public void wakeup() {
		int selectTimeout = selectors.getSelectTimeout();
		if (selectTimeout <= 0 && Thread.currentThread() != this && wakeupNotified.compareAndSet(0, 1)) {
//			wakeupCount1.incrementAndGet();
//			long t = System.nanoTime();
			if (selectTimeout == 0)
				selector.wakeup();
			else
				WakeupThread.postWakeup(selector);
//			wakeupTime.addAndGet(System.nanoTime() - t);
		}// else
//			wakeupCount0.incrementAndGet();
	}

	@Override
	public void run() {
//		lastTime = System.nanoTime();
		int selectTimeout = Math.max(selectors.getSelectTimeout(), 0);
		while (running) {
//			var t = System.nanoTime();
//			if (t - lastTime >= 1_000_000_000L) {
//				long time = t - lastTime;
//				lastTime = t;
//				long count0 = wakeupCount0.getAndSet(0);
//				long count1 = wakeupCount1.getAndSet(0);
//				long wTime = wakeupTime.getAndSet(0);
//				logger.info("wakeup: {}, {}, {} ns, {} ms", count0, count1, count1 > 0 ? wTime / count1 : -1,
//						time / 1_000_000);
//			}
			try {
				// 如果在这个时间窗口 wakeup，下面的 select 会马上返回。wakeup 不会丢失。
				if (selectTimeout == 0) {
					firstAction = true;
					wakeupNotified.set(0);
				}
				selector.select(key -> {
					if (firstAction) {
						firstAction = false;
						wakeupNotified.set(1);
					}
					if (!key.isValid())
						return; // key maybe cancel in loop
					SelectorHandle handle = null;
					try {
						handle = (SelectorHandle)key.attachment();
						handle.doHandle(key);
					} catch (Throwable e) { // Run Handle. 必须捕捉所有异常。
						if (handle != null) {
							try {
								handle.doException(key, e);
							} catch (Throwable e3) { // Skip. 必须捕捉所有异常。
								logger.error("Selector.run", e);
								logger.error("SelectorHandle.doException: {}", e, e3);
							}
						} else
							logger.error("Selector.run", e);
						try {
							key.channel().close();
						} catch (Throwable e2) { // Skip. 必须捕捉所有异常。
							logger.error("SocketChannel.close", e2);
						}
					}
				}, selectTimeout);
			} catch (Throwable e) { // ??? 必须捕捉所有异常。
				logger.error("Selector.run", e);
			}
		}
	}
}
