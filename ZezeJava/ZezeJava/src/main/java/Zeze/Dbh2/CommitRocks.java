package Zeze.Dbh2;

import java.util.ArrayList;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.Commit.BPrepareBatches;
import Zeze.Builtin.Dbh2.Commit.BTransactionState;
import Zeze.Net.Binary;
import Zeze.Raft.RaftRpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.RocksDatabase;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

public class CommitRocks {
	private static final Logger logger = LogManager.getLogger(CommitRocks.class);

	private final Dbh2AgentManager manager;
	private final RocksDatabase database;
	private final RocksDatabase.Table commitPoint;
	private final RocksDatabase.Table commitIndex;
	private WriteOptions writeOptions = RocksDatabase.getDefaultWriteOptions();

	public CommitRocks(Dbh2AgentManager manager) throws RocksDBException {
		this.manager = manager;
		database = new RocksDatabase("CommitRocks");
		commitPoint = database.openTable("CommitPoint");
		commitIndex = database.openTable("CommitIndex");
	}

	public Dbh2AgentManager getManager() {
		return manager;
	}

	public void close() {
		database.close();
	}

	public void setWriteOptions(WriteOptions writeOptions) {
		this.writeOptions = writeOptions;
	}

	public WriteOptions getWriteOptions() {
		return writeOptions;
	}

	public RocksDatabase.Table getCommitPoint() {
		return commitPoint;
	}

	public BTransactionState.Data query(Binary tid) throws RocksDBException {
		var value = commitPoint.get(tid.bytesUnsafe(), tid.getOffset(), tid.size());
		if (null == value)
			return null;
		var state = new BTransactionState.Data();
		state.decode(ByteBuffer.Wrap(value));
		return state;
	}

	private void undo(BPrepareBatches.Data batches) {
		// undo
		var futures = new ArrayList<TaskCompletionSource<?>>();
		for (var e : batches.getDatas().entrySet()) {
			var tid2 = e.getValue().getBatch().getTid();
			if (tid2.size() == 0)
				continue; // not prepared
			futures.add(manager.openBucket(e.getKey()).undoBatch(tid2));
		}
		for (var e : futures)
			e.await();
	}

	public Binary prepare(String queryHost, int queryPort, BPrepareBatches.Data batches) {
		var tid = manager.nextTransactionId();
		var prepareTime = System.currentTimeMillis();
		try {
			// prepare
			saveCommitPoint(tid, batches, Commit.ePreparing);
			var futures = new ArrayList<TaskCompletionSource<RaftRpc<BPrepareBatch.Data, EmptyBean.Data>>>();
			for (var e : batches.getDatas().entrySet()) {
				var batch = e.getValue();
				batch.getBatch().setQueryIp(queryHost);
				batch.getBatch().setQueryPort(queryPort);
				batch.getBatch().setTid(tid);
				futures.add(manager.openBucket(e.getKey()).prepareBatch(batch));
			}
			for (var e : futures) {
				e.await();
			}
		} catch (Throwable ex) {
			undo(batches);
			removeCommitIndex(tid);
			throw new RuntimeException(ex);
		}

		// todo config
		if (System.currentTimeMillis() - prepareTime > 10_000) {
			undo(batches);
			removeCommitIndex(tid);
			throw new RuntimeException("max prepare time exceed.");
		}
		return tid;
	}

	public void commit(String queryHost, int queryPort, BPrepareBatches.Data batches) {
		var tid = prepare(queryHost, queryPort, batches);

		try {
			// 保存 commit-point，如果失败，则 undo。
			saveCommitPoint(tid, batches, Commit.eCommitting);
		} catch (Throwable ex) {
			undo(batches);
			removeCommitIndex(tid);
			throw new RuntimeException(ex);
		}

		// commit
		try {
			var futures = new ArrayList<TaskCompletionSource<?>>();
			for (var e : batches.getDatas().entrySet()) {
				futures.add(manager.openBucket(e.getKey()).commitBatch(e.getValue().getBatch().getTid()));
			}
			for (var e : futures)
				e.await();
			removeCommitIndex(tid);
		} catch (Throwable ex) {
			// todo timer will redo
		}
	}

	private void saveCommitPoint(Binary tid, BPrepareBatches.Data batches, int state) throws RocksDBException {
		var batch = database.newBatch();

		var bState = new BTransactionState.Data();
		bState.setState(state);
		for (var e : batches.getDatas().entrySet()) {
			bState.getBuckets().add(e.getKey());
		}
		var bb = ByteBuffer.Allocate();
		bState.encode(bb);
		commitPoint.put(batch, tid.bytesUnsafe(), tid.getOffset(), tid.size(), bb.Bytes, bb.ReadIndex, bb.size());

		var bbIndex = ByteBuffer.Allocate();
		bbIndex.WriteInt(state);
		commitIndex.put(batch, tid.bytesUnsafe(), tid.getOffset(), tid.size(), bbIndex.Bytes, bbIndex.ReadIndex, bbIndex.size());

		batch.commit(writeOptions);
	}

	private void removeCommitIndex(Binary tid) {
		try {
			commitIndex.delete(tid.bytesUnsafe(), tid.getOffset(), tid.size());
		} catch (RocksDBException e) {
			// 这个错误仅仅记录日志，所有没有删除的index，以后重启和Timer会尝试重做。
			logger.error("", e);
		}
	}
}
