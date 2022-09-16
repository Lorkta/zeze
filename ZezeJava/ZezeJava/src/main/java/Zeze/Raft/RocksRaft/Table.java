package Zeze.Raft.RocksRaft;

import java.lang.invoke.MethodHandle;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.DatabaseRocksDb;
import Zeze.Util.ConcurrentLruLike;
import Zeze.Util.Func1;
import Zeze.Util.Func2;
import Zeze.Util.Reflect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;

public final class Table<K, V extends Bean> {
	private static final Logger logger = LogManager.getLogger(Table.class);

	private final Rocks rocks;
	private final String templateName;
	private final int templateId;
	private final String name;
	private final BiConsumer<ByteBuffer, K> keyEncodeFunc;
	private final Function<ByteBuffer, K> keyDecodeFunc;
	private final MethodHandle valueFactory;
	private int cacheCapacity = 10000;
	private ColumnFamilyHandle columnFamily;
	private ConcurrentLruLike<K, Record<K>> lruCache;
	private BiPredicate<K, Record<K>> lruTryRemoveCallback;

	public ConcurrentLruLike<K, Record<K>> getLruCache() {
		return lruCache;
	}

	public Table(Rocks rocks, String templateName, int templateId, Class<K> keyClass, Class<V> valueClass, BiPredicate<K, Record<K>> callback) {
		this.rocks = rocks;
		this.templateName = templateName;
		this.templateId = templateId;
		name = String.format("%s#%d", this.templateName, this.templateId);
		keyEncodeFunc = SerializeHelper.createEncodeFunc(keyClass);
		keyDecodeFunc = SerializeHelper.createDecodeFunc(keyClass);
		valueFactory = Reflect.getDefaultConstructor(valueClass);
		lruTryRemoveCallback = callback;
		open();
	}

	public void open() {
		columnFamily = rocks.openFamily(name);
		lruCache = new ConcurrentLruLike<>(name, cacheCapacity, lruTryRemoveCallback, 200, 2000, 1024);
	}

	public Rocks getRocks() {
		return rocks;
	}

	public String getTemplateName() {
		return templateName;
	}

	public int getTemplateId() {
		return templateId;
	}

	public String getName() {
		return name;
	}

	public int getCacheCapacity() {
		return cacheCapacity;
	}

	public void setCacheCapacity(int value) {
		cacheCapacity = value;
	}

	public ColumnFamilyHandle getColumnFamily() {
		return columnFamily;
	}

	public BiPredicate<K, Record<K>> getLruTryRemoveCallback() {
		return lruTryRemoveCallback;
	}

	public void setLruTryRemoveCallback(BiPredicate<K, Record<K>> value) {
		lruTryRemoveCallback = value;
	}

	public void encodeKey(ByteBuffer bb, K key) {
		keyEncodeFunc.accept(bb, key);
	}

	public K decodeKey(ByteBuffer bb) {
		return keyDecodeFunc.apply(bb);
	}

	@SuppressWarnings("unchecked")
	public V newValue() {
		try {
			return (V)valueFactory.invoke();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public V get(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(name, key);

		var cr = currentT.getRecordAccessed(tkey);
		if (cr != null)
			return (V)cr.newestValue();

		Record<K> r = getOrLoad(key);
		currentT.addRecordAccessed(r.createRootInfoIfNeed(tkey), new Transaction.RecordAccessed(r));
		return (V)r.getValue();
	}

	@SuppressWarnings("unchecked")
	public V getOrAdd(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(name, key);

		var cr = currentT.getRecordAccessed(tkey);
		if (cr != null) {
			V crv = (V)cr.newestValue();
			if (crv != null)
				return crv;
			// add
		} else {
			Record<K> r = getOrLoad(key);
			cr = new Transaction.RecordAccessed(r);
			currentT.addRecordAccessed(r.createRootInfoIfNeed(tkey), cr);
			if (r.getValue() != null)
				return (V)r.getValue();
			// add
		}

		V add = newValue();
		add.initRootInfo(cr.getOrigin().createRootInfoIfNeed(tkey), null);
		cr.put(currentT, add);
		return add;
	}

	private Record<K> getOrLoad(K key) {
		return getOrLoad(key, null);
	}

	private Record<K> getOrLoad(K key, Bean putValue) {
		TableKey tkey = new TableKey(name, key);
		while (true) {
			var r = lruCache.getOrAdd(key, () -> {
				var newR = new Record<>(keyEncodeFunc);
				newR.setTable(this);
				newR.setKey(key);
				return newR;
			});
			r.mutex.lock();
			try {
				if (r.getRemoved())
					continue;

				if (putValue != null) {
					// from followerApply
					r.setValue(putValue);
					r.getValue().initRootInfo(r.createRootInfoIfNeed(tkey), null);
					r.setTimestamp(Record.getNextTimestamp());
					r.setState(Record.StateLoad);
				} else if (r.getState() == Record.StateNew) {
					// fresh record
					r.setValue(storageLoad(key));
					if (r.getValue() != null)
						r.getValue().initRootInfo(r.createRootInfoIfNeed(tkey), null);
					r.setTimestamp(Record.getNextTimestamp());
					r.setState(Record.StateLoad);
				}
				// else in cache
				return r;
			} finally {
				r.mutex.unlock();
			}
		}
	}

	private V storageLoad(K key) {
		var keyBB = ByteBuffer.Allocate();
		keyEncodeFunc.accept(keyBB, key);
		byte[] valueBytes;
		try {
			valueBytes = rocks.getStorage().get(columnFamily, DatabaseRocksDb.getDefaultReadOptions(),
					keyBB.Bytes, 0, keyBB.WriteIndex);
		} catch (RocksDBException e) {
			throw new RuntimeException(e);
		}
		if (valueBytes == null)
			return null;
		var valueBB = ByteBuffer.Wrap(valueBytes);
		var value = newValue();
		value.decode(valueBB);
		return value;
	}

	public boolean tryAdd(K key, V value) {
		if (get(key) != null)
			return false;

		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(name, key);
		var cr = currentT.getRecordAccessed(tkey);
		value.initRootInfo(cr.getOrigin().createRootInfoIfNeed(tkey), null);
		cr.put(currentT, value);
		return true;
	}

	public void insert(K key, V value) {
		if (!tryAdd(key, value))
			throw new IllegalArgumentException(String.format("table:%s insert key:%s exists", getClass().getName(), key));
	}

	public void put(K key, V value) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(name, key);

		var cr = currentT.getRecordAccessed(tkey);
		if (cr == null) {
			var r = getOrLoad(key);
			cr = new Transaction.RecordAccessed(r);
			currentT.addRecordAccessed(r.createRootInfoIfNeed(tkey), cr);
		}
		value.initRootInfo(cr.getOrigin().createRootInfoIfNeed(tkey), null);
		cr.put(currentT, value);
	}

	// 几乎和Put一样，还是独立开吧。
	public void remove(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(name, key);

		var cr = currentT.getRecordAccessed(tkey);
		if (cr != null) {
			cr.put(currentT, null);
			return;
		}

		Record<K> r = getOrLoad(key);
		cr = new Transaction.RecordAccessed(r);
		cr.put(currentT, null);
		currentT.addRecordAccessed(r.createRootInfoIfNeed(tkey), cr);
	}

	public boolean walk(Func2<K, V, Boolean> callback) throws Throwable {
		try (var it = rocks.getStorage().newIterator(columnFamily, DatabaseRocksDb.getDefaultReadOptions())) {
			for (it.seekToFirst(); it.isValid(); it.next()) {
				var key = keyDecodeFunc.apply(ByteBuffer.Wrap(it.key()));
				var value = newValue();
				value.decode(ByteBuffer.Wrap(it.value()));
				if (!callback.call(key, value))
					return false;
			}
			return true;
		}
	}

	public boolean walkKey(Func1<K, Boolean> callback) throws Throwable {
		try (var it = rocks.getStorage().newIterator(columnFamily, DatabaseRocksDb.getDefaultReadOptions())) {
			for (it.seekToFirst(); it.isValid(); it.next()) {
				var key = keyDecodeFunc.apply(ByteBuffer.Wrap(it.key()));
				if (!callback.call(key))
					return false;
			}
			return true;
		}
	}

	public Record<K> followerApply(K key, Changes.Record rLog) {
		Record<K> r;
		switch (rLog.getState()) {
		case Changes.Record.Remove:
			r = getOrLoad(key);
			r.setValue(null);
			r.setTimestamp(Record.getNextTimestamp());
			break;

		case Changes.Record.Put:
			r = getOrLoad(key, rLog.getPutValue());
			break;

		case Changes.Record.Edit:
			r = getOrLoad(key);
			if (r.getValue() == null) {
				logger.fatal("editing bug record not exist. table={} key={} state={}",
						name, key, r.getState(), new Exception());
				rocks.getRaft().fatalKill();
			}
			for (var log : rLog.getLogBean())
				r.getValue().followerApply(log); // 最多一个。
			break;

		default:
			logger.fatal("unknown Changes.Record.State. table={} key={} state={}",
					name, key, rLog.getState(), new Exception());
			rocks.getRaft().fatalKill();
			return null;
		}
		return r;
	}
}
