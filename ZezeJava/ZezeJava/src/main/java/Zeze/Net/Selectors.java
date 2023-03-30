package Zeze.Net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Selectors {
	public static final class InstanceHolder { // for lazy-init
		private static final Selectors instance = new Selectors("Selector");
	}

	public static @NotNull Selectors getInstance() {
		return InstanceHolder.instance;
	}

	@SuppressWarnings("CanBeFinal")
	public static final class Config {
		public int bbPoolBlockSize = 64 * 1024; // 单个buffer的字节容量,推荐等于Socket.getSendBufferSize()
		public int bbPoolLocalCapacity = 1024; // 本地池的最大保留buffer数量
		public int bbPoolMoveCount = 1024; // 本地池和全局池之间移动一次的buffer数量
		public int bbPoolGlobalCapacity = 64 * bbPoolMoveCount; // 全局池的最大buffer数量
		public int selectTimeout; // 0表示无超时,>0表示每次select的超时毫秒数,-1表示无超时且异步wakeup
		public int readBufferSize = 64 * 1024; // 读buffer的字节容量

		public void check() {
			if (bbPoolBlockSize <= 0)
				throw new IllegalArgumentException("bbPoolBlockSize <= 0: " + bbPoolBlockSize);
			if (bbPoolLocalCapacity < 0)
				throw new IllegalArgumentException("bbPoolLocalCapacity < 0: " + bbPoolLocalCapacity);
			if (bbPoolMoveCount <= 0)
				throw new IllegalArgumentException("bbPoolMoveCount <= 0: " + bbPoolMoveCount);
			if (bbPoolGlobalCapacity < 0)
				throw new IllegalArgumentException("bbPoolGlobalCapacity < 0: " + bbPoolGlobalCapacity);
			if (selectTimeout < -1)
				throw new IllegalArgumentException("selectTimeout < -1: " + selectTimeout);
			if (readBufferSize <= 0)
				throw new IllegalArgumentException("readBufferSize <= 0: " + readBufferSize);
		}
	}

	private final @NotNull String name;
	private final int bbPoolBlockSize;
	private final int bbPoolLocalCapacity;
	private final int bbPoolMoveCount;
	private final int bbPoolGlobalCapacity;
	private final int selectTimeout;
	private final int readBufferSize;
	private final ArrayList<ByteBuffer> bbGlobalPool = new ArrayList<>(); // 全局池
	private final Lock bbGlobalPoolLock = new ReentrantLock(); // 全局池的锁
	private volatile @Nullable Selector[] selectorList;
	private final AtomicLong choiceCount = new AtomicLong();

	public Selectors(@NotNull String name) {
		this(name, 1, null);
	}

	public Selectors(@NotNull String name, int count) {
		this(name, count, null);
	}

	public Selectors(@NotNull String name, int count, @Nullable Config config) {
		this.name = name;
		if (config == null)
			config = new Config();
		config.check();
		bbPoolBlockSize = config.bbPoolBlockSize;
		bbPoolLocalCapacity = config.bbPoolLocalCapacity;
		bbPoolMoveCount = config.bbPoolMoveCount;
		bbPoolGlobalCapacity = config.bbPoolGlobalCapacity;
		selectTimeout = config.selectTimeout;
		readBufferSize = config.readBufferSize;
		add(count);
	}

	public int getBbPoolBlockSize() {
		return bbPoolBlockSize;
	}

	public int getBbPoolLocalCapacity() {
		return bbPoolLocalCapacity;
	}

	public int getBbPoolMoveCount() {
		return bbPoolMoveCount;
	}

	public int getBbPoolGlobalCapacity() {
		return bbPoolGlobalCapacity;
	}

	public int getSelectTimeout() {
		return selectTimeout;
	}

	public int getReadBufferSize() {
		return readBufferSize;
	}

	@NotNull ArrayList<ByteBuffer> getBbGlobalPool() {
		return bbGlobalPool;
	}

	@NotNull Lock getBbGlobalPoolLock() {
		return bbGlobalPoolLock;
	}

	public int getCount() {
		return selectorList.length;
	}

	public @NotNull Selectors add(int count) {
		try {
			Selector[] tmp = selectorList;
			tmp = tmp == null ? new Selector[count = Math.max(count, 0)] : Arrays.copyOf(tmp, tmp.length + count);
			for (int i = tmp.length - count; i < tmp.length; i++) {
				tmp[i] = new Selector(this, name + '-' + i);
				tmp[i].start();
			}
			selectorList = tmp;
			return this;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public @Nullable Selector choice() {
		Selector[] tmp = selectorList; // thread safe
		if (tmp == null)
			return null;

		long count = choiceCount.getAndIncrement();
		int index = (int)((count & Long.MAX_VALUE) % tmp.length);
		return tmp[index];
	}

	public synchronized void close() {
		Selector[] tmp = selectorList;
		if (tmp != null) {
			selectorList = null;
			for (Selector s : tmp)
				s.close();
		}
	}
}
