package Zeze.Component;

import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Application;
import Zeze.Builtin.AutoKey.BSeedKey;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.OutLong;
import Zeze.Util.OutObject;
import Zeze.Util.Task;

public class AutoKey {
	public static class Module extends AbstractAutoKey {
		private final ConcurrentHashMap<String, AutoKey> map = new ConcurrentHashMap<>();
		public final Application zeze;

		// 这个组件Zeze.Application会自动初始化，不需要应用初始化。
		public Module(Application zeze) {
			this.zeze = zeze;
			RegisterZezeTables(zeze);
		}

		@Override
		public void UnRegister() {
			UnRegisterZezeTables(zeze);
		}

		/**
		 * 这个返回值，可以在自己模块内保存下来，效率高一些。
		 */
		public AutoKey getOrAdd(String name) {
			return map.computeIfAbsent(name, name2 -> new AutoKey(this, name2));
		}
	}

	private static final int ALLOCATE_COUNT_MIN = 64;
	private static final int ALLOCATE_COUNT_MAX = 1024 * 1024;

	private final Module module;
	private final String name;
	private volatile Range range;

	private int allocateCount = ALLOCATE_COUNT_MIN;
	private long lastAllocateTime = System.currentTimeMillis();

	private AutoKey(Module module, String name) {
		this.module = module;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int getAllocateCount() {
		return allocateCount;
	}

	public long nextId() {
		var bb = nextByteBuffer();
		if (bb.Size() > 8) {
			throw new IllegalStateException("AutoKey.nextId overflow: serverId="
					+ module.zeze.getConfig().getServerId() + ", nextId=" + bb);
		}
		return ByteBuffer.ToLongBE(bb.Bytes, 0, bb.WriteIndex); // 这里用BE(大端)是为了保证返回值一定为正,且保证ID值随seed的增长而增长
	}

	public byte[] nextBytes() {
		return nextByteBuffer().Bytes; // nextByteBuffer的Bytes一定是正好大小的数组
	}

	public Binary nextBinary() {
		return new Binary(nextByteBuffer());
	}

	/**
	 * @return base64编码的ID
	 */
	public String nextString() {
		return Base64.getEncoder().encodeToString(nextBytes());
	}

	public ByteBuffer nextByteBuffer() {
		int serverId = module.zeze.getConfig().getServerId();
		if (serverId < 0) // serverId不应该<0,因为会导致nextId返回负值
			throw new IllegalStateException("AutoKey.nextByteBuffer: serverId(" + serverId + ") < 0");
		var seed = nextSeed();
		int size = ByteBuffer.writeULongSize(seed);
		if (serverId > 0)
			size += ByteBuffer.writeUIntSize(serverId);
		var bb = ByteBuffer.Allocate(size);
		if (serverId > 0) // 如果serverId==0,写1个字节0不会影响ToLongBE的结果,但会多占1个字节,所以只在serverId>0时写ByteBuffer
			bb.WriteUInt(serverId);
		bb.WriteULong(seed);
		return bb;
	}

	/**
	 * 设置最小的ID值, 使下次nextId()的结果不小于此值
	 */
	public boolean setMinId(long minId) {
		int serverId = module.zeze.getConfig().getServerId();
		if (serverId < 0) // serverId不应该<0,因为会导致nextId返回负值
			throw new IllegalStateException("AutoKey.setMinId: serverId(" + serverId + ") < 0");
		if (serverId == 0)
			return setSeed(minId); // WriteULong(minId)再ToLongBE得到的值一定不小于minId,其实还能再选出符合条件的更小seed值,但serverId极少=0,所以不考虑那么多了
		var bb = ByteBuffer.Allocate(8);
		bb.WriteUInt(serverId);
		bb.WriteULong(0);
		long id = ByteBuffer.ToLongBE(bb.Bytes, 0, bb.WriteIndex);
		long seed = 0;
		while (id < minId) {
			if ((id & 0xff80_0000_0000_0000L) != 0) {
				throw new IllegalStateException("AutoKey.setMinId: minId(" + minId
						+ ") is too large for serverId(" + serverId + ')');
			}
			id <<= 8;
			seed = seed == 0 ? 0x80 : seed << 7; // 每多7位,WriteULong序列化就要多1字节
		}
		return setSeed(seed);
	}

	/**
	 * 设置当前serverId的种子，新种子必须比当前值大。
	 *
	 * @param seed new seed.
	 * @return true if success.
	 */
	public boolean setSeed(long seed) {
		try {
			return Procedure.Success == Task.runUnsafe(module.zeze.newProcedure(() -> {
				var seedKey = new BSeedKey(module.zeze.getConfig().getServerId(), name);
				var bAutoKey = module._tAutoKeys.getOrAdd(seedKey);
				if (seed > bAutoKey.getNextId()) {
					bAutoKey.setNextId(seed);
					return 0;
				}
				return Procedure.LogicError;
			}, "AutoKey.setSeed")).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 增加当前serverId的种子。只能增加，如果溢出，返回失败。
	 *
	 * @param delta delta
	 * @return true if success.
	 */
	public boolean increaseSeed(long delta) {
		if (delta <= 0)
			return false;
		try {
			return Procedure.Success == Task.runUnsafe(module.zeze.newProcedure(() -> {
				var seedKey = new BSeedKey(module.zeze.getConfig().getServerId(), name);
				var bAutoKey = module._tAutoKeys.getOrAdd(seedKey);
				var newSeed = bAutoKey.getNextId() + delta;
				if (newSeed > 0) {
					bAutoKey.setNextId(newSeed);
					return 0;
				}
				// 溢出
				return Procedure.LogicError;
			}, "AutoKey.increaseSeed")).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 返回当前serverId的种子。
	 *
	 * @return seed
	 */
	public long getSeed() {
		long ret;
		try {
			var result = new OutLong();
			ret = Task.runUnsafe(module.zeze.newProcedure(() -> {
				var seedKey = new BSeedKey(module.zeze.getConfig().getServerId(), name);
				var bAutoKey = module._tAutoKeys.getOrAdd(seedKey);
				result.value = bAutoKey.getNextId();
				return 0;
			}, "AutoKey.getSeed")).get();
			if (ret == Procedure.Success)
				return result.value;
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
		throw new RuntimeException("AutoKey.getSeed failed: " + ret);
	}

	private long nextSeed() {
		while (true) {
			var localRange = range;
			if (localRange != null) {
				var next = localRange.tryNextId();
				if (next != 0)
					return next; // allocate in range success
			}

			synchronized (this) {
				//noinspection NumberEquality
				if (range != localRange)
					continue;
				long ret;
				try {
					var newRange = new OutObject<Range>();
					ret = Task.runUnsafe(module.zeze.newProcedure(() -> {
						Transaction.whileCommit(() -> {
							// 不能在重做时重复计算，一次事务重新计算一次，下一次生效。
							// 这里可能有并发问题, 不过影响可以忽略
							var now = System.currentTimeMillis();
							var diff = now - lastAllocateTime;
							lastAllocateTime = now;
							long newCount = allocateCount;
							if (diff < 30 * 1000) // 30 seconds
								newCount <<= 1;
							else if (diff > 120 * 1000) // 120 seconds
								newCount >>= 1;
							else
								return;
							allocateCount = (int)Math.min(Math.max(newCount, ALLOCATE_COUNT_MIN), ALLOCATE_COUNT_MAX);
						});

						var seedKey = new BSeedKey(module.zeze.getConfig().getServerId(), name);
						var key = module._tAutoKeys.getOrAdd(seedKey);
						var start = key.getNextId();
						var end = start + allocateCount; // allocateCount == 0 会死循环。
						key.setNextId(end);
						newRange.value = new Range(start, end);
						return 0;
					}, "AutoKey.allocateSeeds")).get();
					if (ret == Procedure.Success) {
						range = newRange.value;
						continue;
					}
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
				throw new RuntimeException("AutoKey.nextSeed failed: " + ret);
			}
		}
	}

	private static final class Range extends AtomicLong {
		private final long max;

		public long tryNextId() {
			var nextId = incrementAndGet(); // 可能会超过max,但通常不会超出很多,更不可能溢出long最大值
			return nextId <= max ? nextId : 0;
		}

		// 分配范围: [start+1,end]
		public Range(long start, long end) {
			super(start);
			max = end;
		}
	}
}
