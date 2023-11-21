package Zeze.Services;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.FamilyClass;
import Zeze.Net.Protocol;
import Zeze.Net.Selectors;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import Zeze.Util.ThreadFactoryWithName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BinLogger {
	private static final Logger logger = LogManager.getLogger(BinLogger.class);
	private static final int timeZoneOffset = TimeZone.getDefault().getRawOffset(); // 北京时间(+8): 28800_000
	private static final int DEFAULT_PORT = 5004; // 服务的默认端口号
	private static final int MAX_LOG_SIZE = 0xfffff; // 1M-1, 单条日志数据的最大长度(涉及文件格式设计,不能改动)
	private static final int QUEUE_COUNT_LIMIT = 1024 * 1024; // 1M, 日志队列的数量限制
	private static final int QUEUE_SIZE_LIMIT = Math.max(256 * 1024 * 1024, MAX_LOG_SIZE); // 256M, 日志队列的数据总长度限制,不能小于MAX_LOG_SIZE
	private static final int BIN_BUFFER = 1024 * 1024; // bin类型文件的写缓冲区大小,积累到一定量或定期flush给OS
	private static final int OTHER_BUFFER = 64 * 1024; // 同上,用于其它类型文件
	private static final int FLUSH_PERIOD = 1_000; // flush日志文件的时间间隔(毫秒)
	private static final int WRITE_THREAD_IDLE_SLEEP = 100; // 输出日志线程空闲时的sleep时长(毫秒)

	public static final class LogData extends Protocol<LogData> {
		public static final int protocolId = Bean.hash32(LogData.class.getName()); // 117415474
		public static final long typeId = makeTypeId(0, protocolId); // 117415474

		static {
			register(typeId, LogData.class);
		}

		public long roleId;
		public long dataType;
		public ByteBuffer data;

		public LogData() {
		}

		public LogData(long roleId, long dataType, @NotNull ByteBuffer data) {
			this.roleId = roleId;
			this.dataType = dataType;
			this.data = data;
		}

		public LogData(long roleId, @NotNull Serializable s) {
			this(roleId, s.typeId(), ByteBuffer.encode(s));
		}

		@Override
		public int getModuleId() {
			return 0;
		}

		@Override
		public int getProtocolId() {
			return protocolId;
		}

		@Override
		public long getTypeId() {
			return typeId;
		}

		@Override
		public int preAllocSize() {
			var dataSize = data.size();
			return 1 + 1 + ByteBuffer.WriteLongSize(roleId) + 8 + ByteBuffer.WriteUIntSize(dataSize) + dataSize;
		}

		@Override
		public void preAllocSize(int size) {
		}

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteByte(FamilyClass.Protocol);
			bb.WriteByte(1); // version
			bb.WriteLong(roleId);
			bb.WriteLong8(dataType);
			bb.WriteBytes(data.Bytes, data.ReadIndex, data.size());
		}

		@Override
		public void decode(@NotNull ByteBuffer bb) {
			var header = bb.ReadInt();
			if ((header & FamilyClass.FamilyClassMask) != FamilyClass.Protocol) {
				throw new IllegalStateException("invalid header(" + header + ") for decoding protocol "
						+ getClass().getName());
			}
			if ((header & FamilyClass.BitResultCode) != 0)
				bb.SkipLong(); // resultCode
			int version = bb.ReadByte();
			if (version != 1)
				throw new UnsupportedOperationException("version=" + version);
			roleId = bb.ReadLong();
			dataType = bb.ReadLong8();
			data = ByteBuffer.Wrap(bb.ReadBytes());
		}

		@Override
		public @NotNull String toString() {
			return "{" + roleId + ',' + dataType + ',' + data.toString() + '}';
		}
	}

	public static final class BinLoggerAgent extends Service {
		private Connector connector;

		public BinLoggerAgent() {
			this(null);
		}

		public BinLoggerAgent(@Nullable Config config) {
			super("BinLoggerAgent", config);

			var opt = getConfig().getHandshakeOptions();
			if (opt.getKeepCheckPeriod() == 0)
				opt.setKeepCheckPeriod(5);
			if (opt.getKeepRecvTimeout() == 0)
				opt.setKeepRecvTimeout(60);
			if (opt.getKeepSendTimeout() == 0)
				opt.setKeepSendTimeout(30);
		}

		@Override
		public synchronized void start() throws Exception {
			if (connector != null)
				stop();
			var cfg = getConfig();
			int n = cfg.connectorCount();
			if (n != 1)
				throw new IllegalStateException("connectorCount = " + n + " != 1");
			cfg.forEachConnector(c -> this.connector = c);
			super.start();
		}

		public synchronized @NotNull BinLoggerAgent start(@NotNull String host, int port) throws Exception {
			if (connector != null)
				stop();
			connector = new Connector(host, port, true);
			connector.SetService(this);
			connector.setAutoReconnect(true);
			connector.start();
			return this;
		}

		@Override
		public synchronized void stop() throws Exception {
			if (connector != null) {
				connector.stop();
				connector = null;
			}
			super.stop();
		}

		public void waitReady() {
			connector.WaitReady();
		}

		public boolean sendLog(long roleId, @NotNull Serializable log) {
			var so = connector.getSocket();
			return so != null && so.Send(new LogData(roleId, log));
		}

		@Override
		public void dispatchProtocol(long typeId, @NotNull ByteBuffer bb,
									 @NotNull ProtocolFactoryHandle<?> factoryHandle,
									 @NotNull AsyncSocket so) {
			try {
				decodeProtocol(typeId, bb, factoryHandle, so).handle(this, factoryHandle); // 所有协议处理几乎无阻塞,可放心直接跑在IO线程上
			} catch (Throwable e) { // logger.error
				logger.error("BinLoggerAgent.dispatchProtocol exception:", e);
			}
		}
	}

	public static final class BinLoggerService extends Service {
		private final @NotNull String logPath; // 日志保存的路径,以"/"结尾,日志文件名(除后缀名)是8位日期数字
		private @Nullable RandomAccessFile lockFile; // 以"LOCK"命名的文件,用于BinLogger对象独占日志写入权限
		private BufferedOutputStream binFile; // Bean(Data)结构日志经过二进制序列化紧凑连续保存的文件
		private BufferedOutputStream posFile; // 每条日志在bin文件中的位置和大小,小端保存为8字节整数,其中位置占高44位(最大支持16T),大小占低20位(最大支持1M-1)
		private BufferedOutputStream tsFile; // 每条日志的时间戳,小端保存为8字节整数,其中高44位是UTC毫秒时间戳,低20位是该时间戳的日志序号(从0开始)
		private BufferedOutputStream dtFile; // 每条日志的Bean类型,小端保存为8字节整数
		private BufferedOutputStream idFile; // 每条日志的所属ID,小端保存为8字节整数,通常为角色ID
		private final @NotNull ReentrantLock queueLock = new ReentrantLock(); // 写日志队列的锁
		private final @NotNull Condition queueLockCond = queueLock.newCondition(); // 写日志队列的锁等待条件
		private Thread writeLogThread; // 处理并输出日志文件的线程
		private ArrayList<LogData> writeLogQueue; // 写日志队列
		private long writeLogQueueSize; // 写日志队列已写入的数据总大小
		private long binFileSize; // 当前bin文件大小
		private long lastFlushMs; // 上次flush文件的毫秒时间戳
		private int curDayStamp; // 当前的日期戳
		private boolean started; // 是否已经开始服务
		private boolean waitingQueue; // 写日志队列是否已满导致等待

		public BinLoggerService(@Nullable String logPath) {
			this(null, logPath);
		}

		public BinLoggerService(@Nullable Config config, @Nullable String logPath) {
			super("BinLoggerService", config != null ? config : new Config().loadAndParse());
			if (logPath == null)
				logPath = "";
			else {
				logPath = logPath.trim().replace('\\', '/');
				if (!logPath.endsWith("/"))
					logPath += '/';
			}
			this.logPath = logPath;

			var opt = getConfig().getHandshakeOptions();
			if (opt.getKeepCheckPeriod() == 0)
				opt.setKeepCheckPeriod(5);
			if (opt.getKeepRecvTimeout() == 0)
				opt.setKeepRecvTimeout(60);
			if (opt.getKeepSendTimeout() == 0)
				opt.setKeepSendTimeout(30);

			AddFactoryHandle(LogData.typeId, new ProtocolFactoryHandle<>(LogData::new, this::processLogData,
					TransactionLevel.None, DispatchMode.Direct));
			ShutdownHook.add(this::stop);
		}

		@Override
		public void start() throws Exception {
			start(null, DEFAULT_PORT);
		}

		// 参数host,port优先; 如果传null/<=0则以config为准; 如果config也没配置则用默认值null/DEFAULT_PORT
		public synchronized void start(@Nullable String host, int port) throws Exception {
			if (started)
				stop();
			started = true;
			logger.info("BinLoggerService starting ...");
			var sc = getConfig();
			if (sc.acceptorCount() == 0)
				sc.addAcceptor(new Acceptor(port > 0 ? port : DEFAULT_PORT, host));
			else {
				sc.forEachAcceptor2(acceptor -> {
					if (host != null)
						acceptor.setIp(host);
					if (port > 0)
						acceptor.setPort(port);
					return false;
				});
			}
			startLogger();
			super.start();
		}

		@Override
		public synchronized void stop() throws Exception {
			if (!started)
				return;
			started = false;
			logger.info("BinLoggerService stopping ...");
			try {
				super.stop();
			} finally {
				stopLogger();
			}
		}

		private static final class RecoveryFile implements Closeable {
			final @NotNull RandomAccessFile raf;
			final @NotNull FileChannel fc;
			final long size;

			private RecoveryFile(@NotNull String fileName) throws IOException {
				RandomAccessFile f = null;
				try {
					raf = f = new RandomAccessFile(fileName, "rw");
					fc = f.getChannel();
					size = fc.size();
				} catch (Throwable e) {
					if (f != null)
						forceClose(f);
					throw e;
				}
			}

			void tryTruncate(long size) throws IOException {
				if (this.size != size)
					fc.truncate(size);
			}

			@Override
			public void close() {
				forceClose(raf);
			}
		}

		private static void forceClose(@NotNull Closeable f) {
			try {
				f.close();
			} catch (Exception ignored) {
			}
		}

		private static int toDayStamp(long utcMs) {
			return (int)((utcMs + timeZoneOffset) / 86400_000);
		}

		@SuppressWarnings("deprecation")
		private static @NotNull String toDayStr(int dayStamp) { // 20231117
			var date = new Date(dayStamp * 86400_000L - timeZoneOffset);
			return String.format("%4d%2d%2d", date.getYear() + 1900, date.getMonth() + 1, date.getDate());
		}

		private void startLogger() throws Exception {
			logger.info("lock logPath: '{}'", logPath);
			try {
				// 1.目录上锁
				Files.createDirectories(Path.of(logPath));
				lockFile = new RandomAccessFile(logPath + "LOCK", "rw");
				if (lockFile.getChannel().tryLock() == null)
					throw new IOException("tryLock LOCK file failed");
				// 2.修复并打开当天的所有日志和索引文件
				curDayStamp = toDayStamp(System.currentTimeMillis());
				var fnPrefix = logPath + toDayStr(curDayStamp);
				try (var binF = new RecoveryFile(fnPrefix + ".bin");
					 var posF = new RecoveryFile(fnPrefix + ".pos");
					 var tsF = new RecoveryFile(fnPrefix + ".ts");
					 var dtF = new RecoveryFile(fnPrefix + ".dt");
					 var idF = new RecoveryFile(fnPrefix + ".id")) {
					binFileSize = binF.size;
					logger.info("recovery: bin,pos,ts,dt,id.size={},{},{},{},{}; fileNamePrefix='{}'",
							binFileSize, posF.size, tsF.size, dtF.size, idF.size, fnPrefix);
					var otherTruncateSize = Math.min(Math.min(Math.min(posF.size, tsF.size), dtF.size), idF.size) & ~7;
					var buf = new byte[8];
					for (; otherTruncateSize >= 8; otherTruncateSize -= 8) {
						posF.raf.seek(otherTruncateSize - 8);
						posF.raf.read(buf);
						var posLen = ByteBuffer.ToLong(buf, 0);
						if ((posLen >>> 20) + (posLen & 0xfffff) <= binFileSize)
							break;
					}
					logger.info("recovery: truncate size={}", otherTruncateSize);
					posF.tryTruncate(otherTruncateSize);
					tsF.tryTruncate(otherTruncateSize);
					dtF.tryTruncate(otherTruncateSize);
					idF.tryTruncate(otherTruncateSize);
					binFile = new BufferedOutputStream(new FileOutputStream(fnPrefix + ".bin", true), BIN_BUFFER);
					posFile = new BufferedOutputStream(new FileOutputStream(fnPrefix + ".pos", true), OTHER_BUFFER);
					tsFile = new BufferedOutputStream(new FileOutputStream(fnPrefix + ".ts", true), OTHER_BUFFER);
					dtFile = new BufferedOutputStream(new FileOutputStream(fnPrefix + ".dt", true), OTHER_BUFFER);
					idFile = new BufferedOutputStream(new FileOutputStream(fnPrefix + ".id", true), OTHER_BUFFER);
					lastFlushMs = System.currentTimeMillis();
				}
				// 3.开启输出日志线程
				writeLogQueue = new ArrayList<>();
				writeLogQueueSize = 0;
				waitingQueue = false;
				writeLogThread = new Thread(this::writeLogThread, "WriteLogThread");
				writeLogThread.setPriority(Thread.NORM_PRIORITY + 2); // 稍调高点优先级,确保输出日志吞吐性能
				writeLogThread.start();
			} catch (Throwable e) {
				started = false;
				stopLogger();
				throw e;
			}
		}

		private void stopLogger() throws Exception {
			if (writeLogThread != null) {
				logger.info("waiting for writeLogThread ...");
				writeLogThread.join(); // 线程还没开始时也不会等待
				writeLogThread = null;
			}
			if (idFile != null) {
				forceClose(idFile);
				idFile = null;
			}
			if (dtFile != null) {
				forceClose(dtFile);
				dtFile = null;
			}
			if (tsFile != null) {
				forceClose(tsFile);
				tsFile = null;
			}
			if (posFile != null) {
				forceClose(posFile);
				posFile = null;
			}
			if (binFile != null) {
				forceClose(binFile);
				binFile = null;
			}
			if (lockFile != null) {
				forceClose(lockFile);
				lockFile = null;
			}
			logger.info("unlock logPath: '{}'", logPath);
		}

		@Override
		public void dispatchProtocol(long typeId, @NotNull ByteBuffer bb,
									 @NotNull ProtocolFactoryHandle<?> factoryHandle,
									 @NotNull AsyncSocket so) {
			try {
				decodeProtocol(typeId, bb, factoryHandle, so).handle(this, factoryHandle); // 所有协议处理几乎无阻塞,可放心直接跑在IO线程上
			} catch (Throwable e) { // logger.error
				logger.error("BinLoggerService.dispatchProtocol exception:", e);
			}
		}

		private long processLogData(@NotNull LogData p) throws InterruptedException {
			int dataSize = p.data.size();
			if (dataSize > MAX_LOG_SIZE) {
				logger.warn("too long size of LogData: roleId={}, type={}, size={}, sender={}",
						p.roleId, p.dataType, dataSize, p.getSender());
			} else {
				queueLock.lock();
				try {
					for (; ; ) {
						var wlq = writeLogQueue;
						if (wlq == null)
							break;
						var newQueueSize = writeLogQueueSize + dataSize;
						if (newQueueSize <= QUEUE_SIZE_LIMIT && wlq.size() < QUEUE_COUNT_LIMIT) {
							writeLogQueueSize = newQueueSize;
							wlq.add(p);
							return 0;
						}
						waitingQueue = true;
						queueLockCond.await();
					}
				} finally {
					queueLock.unlock();
				}
				logger.info("drop LogData: roleId={}, type={}, size={}, sender={}",
						p.roleId, p.dataType, dataSize, p.getSender());
			}
			return 0;
		}

		private void writeLogThread() {
			var readLogQueue = new ArrayList<LogData>(); // 读日志队列
			var lastTs = 0L;
			var buf = new byte[8];
			for (int queueSize; ; ) {
				try {
					queueLock.lock();
					try {
						var wlq = writeLogQueue;
						if (wlq == null) // 阻止了写队列后,也处理完读队列,可以退出了
							break;
						queueSize = wlq.size();
						if (queueSize > 0) {
							writeLogQueue = readLogQueue;
							readLogQueue = wlq;
							writeLogQueueSize = 0;
							if (waitingQueue) {
								waitingQueue = false;
								queueLockCond.signalAll();
							}
						}
						if (!started)
							writeLogQueue = null; // 阻止日志再进入队列
					} finally {
						queueLock.unlock();
					}
					if (queueSize > 0) {
						var curMs = System.currentTimeMillis();
						if (curMs != lastTs >>> 20)
							lastTs = curMs << 20;
						var dayStamp = toDayStamp(curMs);
						if (dayStamp != curDayStamp) {
							curDayStamp = dayStamp;
							var fnPrefix = logPath + toDayStr(curDayStamp);
							forceClose(binFile);
							forceClose(posFile);
							forceClose(tsFile);
							forceClose(dtFile);
							forceClose(idFile);
							binFile = new BufferedOutputStream(new FileOutputStream(fnPrefix + ".bin"), BIN_BUFFER);
							posFile = new BufferedOutputStream(new FileOutputStream(fnPrefix + ".pos"), OTHER_BUFFER);
							tsFile = new BufferedOutputStream(new FileOutputStream(fnPrefix + ".ts"), OTHER_BUFFER);
							dtFile = new BufferedOutputStream(new FileOutputStream(fnPrefix + ".dt"), OTHER_BUFFER);
							idFile = new BufferedOutputStream(new FileOutputStream(fnPrefix + ".id"), OTHER_BUFFER);
							lastFlushMs = System.currentTimeMillis();
						}
						for (int i = 0; i < queueSize; i++) {
							var logData = readLogQueue.get(i);
							var data = logData.data;
							var dataSize = data.size();
							binFile.write(data.Bytes, data.ReadIndex, dataSize);
							ByteBuffer.longLeHandler.set(buf, 0, (binFileSize << 20) + dataSize);
							posFile.write(buf);
							binFileSize += dataSize;
							ByteBuffer.longLeHandler.set(buf, 0, lastTs++);
							tsFile.write(buf);
							ByteBuffer.longLeHandler.set(buf, 0, logData.dataType);
							dtFile.write(buf);
							ByteBuffer.longLeHandler.set(buf, 0, logData.roleId);
							idFile.write(buf);
						}
						readLogQueue.clear();
						if (curMs - lastFlushMs >= FLUSH_PERIOD) { // 最多一秒刷新一次
							lastFlushMs = curMs;
							binFile.flush();
							posFile.flush();
							tsFile.flush();
							dtFile.flush();
							idFile.flush();
						}
					} else if (started) {
						//noinspection BusyWait
						Thread.sleep(WRITE_THREAD_IDLE_SLEEP);
					}
				} catch (Throwable e) {
					logger.error("writeLogThread exception:", e);
					try {
						//noinspection BusyWait
						Thread.sleep(WRITE_THREAD_IDLE_SLEEP);
					} catch (InterruptedException ex) {
						Task.forceThrow(ex);
					}
				}
			}
		}
	}

	public static void main(String @NotNull [] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
			logger.error("uncaught exception in {}:", t, e);
		});

		String host = null;
		int port = 0;
		int threadCount = 0;
		String path = "binlog";

		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-host":
				host = args[++i];
				if (host.isBlank())
					host = null;
				break;
			case "-port":
				port = Integer.parseInt(args[++i]);
				break;
			case "-threads":
				threadCount = Integer.parseInt(args[++i]);
				break;
			case "-path":
				path = args[++i].trim();
				break;
			default:
				throw new IllegalArgumentException("unknown argument: " + args[i]);
			}
		}

		if (threadCount < 1)
			threadCount = Runtime.getRuntime().availableProcessors();
		Task.initThreadPool(Task.newCriticalThreadPool("ZezeTaskPool"),
				Executors.newSingleThreadScheduledExecutor(new ThreadFactoryWithName("ZezeScheduledPool")));
		if (Selectors.getInstance().getCount() < threadCount)
			Selectors.getInstance().add(threadCount - Selectors.getInstance().getCount());

		new BinLoggerService(path).start(host, port);
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}