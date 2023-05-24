package Zeze.Dbh2;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BSplitPut;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Raft.Raft;
import Zeze.Util.Random;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

public class Dbh2StateMachine extends Zeze.Raft.StateMachine {
	private static final Logger logger = LogManager.getLogger(Dbh2StateMachine.class);
	private Bucket bucket;
	private TidAllocator tidAllocator;
	private final ConcurrentHashMap<Binary, Dbh2Transaction> transactions = new ConcurrentHashMap<>();
	private Future<?> timer;
	private CommitAgent commitAgent;
	private final Dbh2 dbh2;
	private Runnable noTransactionHandle;

	final AtomicLong counterGet = new AtomicLong();
	private final AtomicLong counterPut = new AtomicLong();
	final AtomicLong sizeGet = new AtomicLong();
	private final AtomicLong sizePut = new AtomicLong();
	private final AtomicLong counterDelete = new AtomicLong();
	private final AtomicLong counterPrepareBatch = new AtomicLong();
	private final AtomicLong counterCommitBatch = new AtomicLong();
	private final AtomicLong counterUndoBatch = new AtomicLong();

	private long lastGet;
	private long lastPut;
	private long lastSizeGet;
	private long lastSizePut;
	private long lastDelete;
	private long lastPrepareBatch;
	private long lastCommitBatch;
	private long lastUndoBatch;
	private long lastReportTime = System.currentTimeMillis();

	public double load() {
		var now = System.currentTimeMillis();
		var elapse = (now - lastReportTime) / 1000.0f;
		lastReportTime = now;

		var nowGet = counterGet.get();
		var nowPut = counterPut.get();
		var nowSizeGet = sizeGet.get();
		var nowSizePut = sizePut.get();
		var nowDelete = counterDelete.get();
		var nowPrepareBatch = counterPrepareBatch.get();
		var nowCommitBatch = counterCommitBatch.get();
		var nowUndoBatch = counterUndoBatch.get();

		var diffGet = nowGet - lastGet;
		var diffPut = nowPut - lastPut;
		var diffSizeGet = nowSizeGet - lastSizeGet;
		var diffSizePut = nowSizePut - lastSizePut;
		var diffDelete = nowDelete - lastDelete;
		var diffPrepareBatch = nowPrepareBatch - lastPrepareBatch;
		var diffCommitBatch = nowCommitBatch - lastCommitBatch;
		var diffUndoBatch = nowUndoBatch - lastUndoBatch;

		if (diffGet > 0 || diffPut > 0 || diffDelete > 0 || diffSizeGet > 0 || diffSizePut > 0
				|| diffPrepareBatch > 0 || diffCommitBatch > 0 || diffUndoBatch > 0) {
			lastGet = nowGet;
			lastPut = nowPut;
			lastSizeGet = nowSizeGet;
			lastSizePut = nowSizePut;
			lastDelete = nowDelete;
			lastPrepareBatch = nowPrepareBatch;
			lastCommitBatch = nowCommitBatch;
			lastUndoBatch = nowUndoBatch;

			var avgGet = diffGet / elapse;
			var avgPut = diffPut / elapse;
			var avgDelete = diffDelete / elapse;

			//noinspection StringBufferReplaceableByString
			var sb = new StringBuilder();
			sb.append("load: ");
			sb.append(Dbh2.formatMeta(getBucket().getMeta()));
			sb.append(" get=").append(avgGet);
			sb.append(" put=").append(avgPut);
			sb.append(" getSize=").append(diffSizeGet / elapse);
			sb.append(" putSize=").append(diffSizePut / elapse);
			sb.append(" delete=").append(avgDelete);
			sb.append(" prepare=").append(diffPrepareBatch / elapse);
			sb.append(" commit=").append(diffCommitBatch / elapse);
			sb.append(" undo=").append(diffUndoBatch / elapse);

			logger.info(sb.toString());

			// 负载，put，delete全算，get算1%。
			return (avgPut + avgDelete) + avgGet * 0.01;
		}
		return 0.0;
	}

	public Dbh2StateMachine(Dbh2 dbh2) {
		this.dbh2 = dbh2;

		super.addFactory(LogPrepareBatch.TypeId_, LogPrepareBatch::new);
		super.addFactory(LogCommitBatch.TypeId_, LogCommitBatch::new);
		super.addFactory(LogUndoBatch.TypeId_, LogUndoBatch::new);
		super.addFactory(LogSetBucketMeta.TypeId_, LogSetBucketMeta::new);
		super.addFactory(LogAllocateTid.TypeId_, LogAllocateTid::new);

		super.addFactory(LogEndSplit.TypeId_, LogEndSplit::new);
		super.addFactory(LogSetSplittingMeta.TypeId_, LogSetSplittingMeta::new);
		super.addFactory(LogSplitPut.TypeId_, LogSplitPut::new);
	}

	public void setupHandleIfNoTransaction(Runnable handle) {
		if (transactions.isEmpty())
			handle.run();
		else
			noTransactionHandle = handle;
	}

	private void triggerNoTransactionIf() {
		if (transactions.isEmpty() && null != noTransactionHandle) {
			noTransactionHandle.run();
			noTransactionHandle = null;
		}
	}

	public Bucket getBucket() {
		return bucket;
	}

	public TidAllocator getTidAllocator() {
		return tidAllocator;
	}

	public ConcurrentHashMap<Binary, Dbh2Transaction> getTransactions() {
		return transactions;
	}

	public void openBucket() {
		if (bucket != null)
			return;
		bucket = new Bucket(getRaft().getRaftConfig());
		tidAllocator = new TidAllocator();

		if (null == timer) {
			var period = getRaft().getRaftConfig().getAppendEntriesTimeout() + 200;
			var delay = Random.getInstance().nextLong(period);
			timer = Task.scheduleUnsafe(delay, period, this::onTimer);
		}

		if (null == commitAgent)
			commitAgent = new CommitAgent();
	}

	private void onTimer() {
		if (!getRaft().isLeader())
			return;

		var now = System.currentTimeMillis();
		for (var e : transactions.entrySet()) {
			var t = e.getValue();
			if (now - t.getCreateTime() < dbh2.getConfig().getBucketMaxTime())
				continue;
			var tid = e.getKey();
			var state = commitAgent.query(t.getQueryIp(), t.getQueryPort(), tid);
			if (Commit.eCommitNotExist == state.getState()
					|| Commit.ePreparing == state.getState()) {
				logger.warn("timeout undo tid=" + tid + " state=" + state);
				getRaft().appendLog(new LogUndoBatch(tid));
			}
		}
	}

	public void setBucketMeta(BBucketMeta.Data argument) {
		try {
			bucket.setMeta(argument);
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	public void setSplittingMeta(BBucketMeta.Data argument) {
		try {
			bucket.setSplittingMeta(argument);
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	public void endSplit(BBucketMeta.Data from, BBucketMeta.Data to) {
		try (var it = bucket.getTData().iterator()) {
			it.seek(from.getKeyLast().copyIf());
			if (!it.isValid())
				return;
			var first = it.key();

			it.seekToLast();
			if (!it.isValid())
				return;
			var last = it.key();

			// deleteRange 不包含last，需要制造一个比当前last后面的key。
			last = Arrays.copyOf(last, last.length + 1);
			bucket.getTData().deleteRange(first, last);

			bucket.setMeta(from);
			bucket.addSplitMetaHistory(from, to);
			bucket.deleteSplittingMeta();
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	/////////////////////////////////////////////////////////////////////
	// 下面这些方法用于Log.apply，不能失败，失败将停止程序。
	public void allocateTid(long range) {
		try {
			var start = bucket.getTid();
			var end = start + range;
			bucket.setTid(end);
			tidAllocator.setRange(start, end);
		} catch (RocksDBException ex) {
			logger.error("", ex);
			getRaft().fatalKill();
		}
	}

	public void prepareBatch(BBatch.Data bBatch) {
		try {
			counterPrepareBatch.incrementAndGet();
			var txn = transactions.computeIfAbsent(bBatch.getTid(), _tid -> new Dbh2Transaction(bBatch));
			counterPut.addAndGet(txn.getBatch().getPuts().size());
			var totalPutValueSize = 0;
			for (var e : txn.getBatch().getPuts().entrySet()) {
				totalPutValueSize += e.getKey().size();
				totalPutValueSize += e.getValue().size();
			}
			sizePut.addAndGet(totalPutValueSize);
			counterDelete.addAndGet(txn.getBatch().getDeletes().size());
			txn.prepareBatch(bucket, bBatch);
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	public void commitBatch(Binary tid) {
		try (var txn = transactions.remove(tid)) {
			counterCommitBatch.incrementAndGet();
			if (null != txn)
				dbh2.onCommitBatch(txn);
			triggerNoTransactionIf();
		}
	}

	public void undoBatch(Binary tid) {
		try (var txn = transactions.remove(tid)) {
			counterUndoBatch.incrementAndGet();
			if (null != txn)
				txn.undoBatch(bucket);
			triggerNoTransactionIf();
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	////////////////////////////////////////////////////////////
	// raft implement
	public String getDbHome() {
		return getRaft().getRaftConfig().getDbHome();
	}

	@Override
	public SnapshotResult snapshot(String path) throws Exception {
		long t0 = System.nanoTime();
		SnapshotResult result = new SnapshotResult();
		var cpHome = checkpoint(result);

		long t1 = System.nanoTime();
		var backupDir = Paths.get(getDbHome(), "backup").toString();
		var backupFile = new File(backupDir);
		if (!backupFile.isDirectory() && !backupFile.mkdirs())
			logger.error("create backup directory failed: {}", backupDir);
		RocksDatabase.backup(cpHome, backupDir);

		long t2 = System.nanoTime();
		LogSequence.deleteDirectory(new File(cpHome));
		Zeze.Raft.RocksRaft.Rocks.createZipFromDirectory(backupDir, path);

		long t3 = System.nanoTime();
		getRaft().getLogSequence().commitSnapshot(path, result.lastIncludedIndex);

		result.success = true;
		result.checkPointNanoTime = t1 - t0;
		result.backupNanoTime = t2 - t1;
		result.zipNanoTime = t3 - t2;
		result.totalNanoTime = System.nanoTime() - t0;
		return result;
	}

	public void close() throws Exception {
		for (var tran : transactions.values())
			tran.close();
		transactions.clear();

		if (bucket != null) {
			bucket.close();
			bucket = null;
		}

		if (null != timer) {
			timer.cancel(true);
			timer = null;
		}

		if (null != commitAgent) {
			commitAgent.stop();
			commitAgent = null;
		}
	}

	@Override
	public void reset() {
		var path = Path.of(getDbHome(), "statemachine").toAbsolutePath().toFile();
		LogSequence.deletedDirectoryAndCheck(path, 100);
	}

	@Override
	public void loadSnapshot(String path) throws Exception {
		var backupDir = Paths.get(getDbHome(), "backup").toString();
		var backupFile = new File(backupDir);
		if (!backupFile.isDirectory() || new File(path).lastModified() > backupFile.lastModified()) {
			LogSequence.deletedDirectoryAndCheck(backupFile, 100);
			Zeze.Raft.RocksRaft.Rocks.extractZipToDirectory(path, backupDir);
		}
		restore(backupDir);
	}

	public String checkpoint(SnapshotResult result) throws RocksDBException {
		var checkpointDir = Paths.get(getDbHome(), "checkpoint_" + System.currentTimeMillis()).toString();

		// fast checkpoint, will stop application apply.
		Raft raft = getRaft();
		raft.lock();
		try {
			var lastAppliedLog = raft.getLogSequence().lastAppliedLogTermIndex();
			result.lastIncludedIndex = lastAppliedLog.getIndex();
			result.lastIncludedTerm = lastAppliedLog.getTerm();

			try (var cp = bucket.getDb().newCheckpoint()) {
				cp.createCheckpoint(checkpointDir);
			}
		} finally {
			raft.unlock();
		}
		return checkpointDir;
	}

	public void restore(String backupDir) throws Exception {
		getRaft().lock();
		try {
			close();
			var dbName = Paths.get(getDbHome(), "statemachine").toString();
			RocksDatabase.restore(backupDir, dbName);
			openBucket(); // reopen
		} finally {
			getRaft().unlock();
		}
	}

	public void applySplitPut(BSplitPut.Data puts) {
		try {
			var table = bucket.getTData();
			if (puts.isFromTransaction()) {
				// 事务同步流程
				for (var e : puts.getPuts().entrySet()) {
					var key = e.getKey();
					var value = e.getValue();

					// replace
					table.put(key.bytesUnsafe(), key.getOffset(), key.size(),
							value.bytesUnsafe(), value.getOffset(), value.size());
				}
				return; // done;
			}

			// 数据复制流程
			for (var e : puts.getPuts().entrySet()) {
				var key = e.getKey();
				var value = e.getValue();

				// putIfAbsent
				if (table.get(key.bytesUnsafe(), key.getOffset(), key.size()) == null) {
					table.put(key.bytesUnsafe(), key.getOffset(), key.size(),
							value.bytesUnsafe(), value.getOffset(), value.size());
				}
			}
		} catch (RocksDBException ex) {
			logger.error("", ex);
			getRaft().fatalKill();
		}
	}
}
