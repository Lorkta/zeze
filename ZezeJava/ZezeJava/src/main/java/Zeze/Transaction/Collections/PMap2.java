package Zeze.Transaction.Collections;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;
import Zeze.Transaction.Log;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.Empty;

@SuppressWarnings("DataFlowIssue")
public class PMap2<K, V extends Bean> extends PMap<K, V> {
	private static final @NotNull Logger logger = LogManager.getLogger(PMap2.class);

	protected final @NotNull Meta2<K, V> meta;

	public PMap2(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		meta = Meta2.getMap2Meta(keyClass, valueClass);
	}

	public PMap2(@NotNull Class<K> keyClass, @NotNull ToLongFunction<Bean> get, @NotNull LongFunction<Bean> create) { // only for DynamicBean value
		meta = Meta2.createDynamicMapMeta(keyClass, get, create);
	}

	private PMap2(@NotNull Meta2<K, V> meta) {
		this.meta = meta;
	}

	@SuppressWarnings("unchecked")
	public @NotNull V createValue() {
		try {
			return (V)meta.valueFactory.invoke();
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) { // MethodHandle.invoke
			throw new RuntimeException(e);
		}
	}

	public @NotNull V getOrAdd(@NotNull K key) {
		var exist = get(key);
		if (exist == null) {
			exist = createValue();
			put(key, exist);
		}
		return exist;
	}

	@Override
	public @Nullable V put(@NotNull K key, @NotNull V value) {
		//noinspection ConstantValue
		if (key == null)
			throw new IllegalArgumentException("null key");
		//noinspection ConstantValue
		if (value == null)
			throw new IllegalArgumentException("null value");

		if (!value.isManaged())
			value.mapKey(key);
		if (isManaged()) {
			value.initRootInfoWithRedo(rootInfo, this);
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap2<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return mapLog.put(key, value);
		}
		var oldV = map.get(key);
		map = map.plus(key, value);
		return oldV;
	}

	@Override
	public void putAll(@NotNull Map<? extends K, ? extends V> m) {
		for (var p : m.entrySet()) {
			var k = p.getKey();
			if (k == null)
				throw new IllegalArgumentException("null key");
			var v = p.getValue();
			if (v == null)
				throw new IllegalArgumentException("null value");
			if (!v.isManaged())
				v.mapKey(k);
		}

		if (isManaged()) {
			for (var v : m.values())
				v.initRootInfoWithRedo(rootInfo, this);
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.putAll(m);
		} else
			map = map.plusAll(m);
	}

	@SuppressWarnings("unchecked")
	@Override
	public @Nullable V remove(@NotNull Object key) {
		if (isManaged()) {
			var mapLog = (LogMap2<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return mapLog.remove((K)key);
		}
		//noinspection SuspiciousMethodCalls
		var exist = map.get(key);
		map = map.minus(key);
		return exist;
	}

	@Override
	public boolean remove(@NotNull Entry<K, V> item) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return mapLog.remove(item.getKey(), item.getValue());
		}
		var old = map;
		var exist = old.get(item.getKey());
		if (null != exist && exist.equals(item.getValue())) {
			map = map.minus(item.getKey());
			return true;
		}
		return false;
	}

	@Override
	public void clear() {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap2<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.clear();
		} else
			map = Empty.map();
	}

	@Override
	public void followerApply(@NotNull Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogMap2<K, V>)_log;
		var tmp = map;
		for (var put : log.getReplaced().values())
			put.initRootInfo(rootInfo, this);
		tmp = tmp.plusAll(log.getReplaced()).minusAll(log.getRemoved());

		// apply changed
		for (var e : log.getChangedWithKey().entrySet()) {
			Bean value = tmp.get(e.getKey());
			if (value != null)
				value.followerApply(e.getValue());
			else
				logger.error("Not Exist! Key={} Value={}", e.getKey(), e.getValue());
		}
		map = tmp;
	}

	@Override
	public @NotNull LogBean createLogBean() {
		var log = new LogMap2<>(meta, map);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		return log;
	}

	@Override
	protected void initChildrenRootInfo(@NotNull Record.RootInfo root) {
		for (var v : map.values())
			v.initRootInfo(root, this);
	}

	@Override
	protected void initChildrenRootInfoWithRedo(@NotNull Record.RootInfo root) {
		for (var v : map.values())
			v.initRootInfoWithRedo(root, this);
	}

	@Override
	public @NotNull PMap2<K, V> copy() {
		var copy = new PMap2<>(meta);
		copy.map = map;
		return copy;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		var tmp = getMap();
		bb.WriteUInt(tmp.size());
		var encoder = meta.keyEncoder;
		for (var e : tmp.entrySet()) {
			encoder.accept(bb, e.getKey());
			e.getValue().encode(bb);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void decode(@NotNull ByteBuffer bb) {
		clear();
		var decoder = meta.keyDecoder;
		try {
			for (int i = bb.ReadUInt(); i > 0; i--) {
				var key = decoder.apply(bb);
				V value = (V)meta.valueFactory.invoke();
				value.decode(bb);
				put(key, value);
			}
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) { // MethodHandle.invoke
			throw new RuntimeException(e);
		}
	}

	public <D extends Data> void putAllData(@NotNull Map<K, D> dataMap) {
		Bean.toBeanMap(dataMap, this);
	}

	public <D extends Data> void toDataMap(@NotNull Map<K, D> dataList) {
		Bean.toDataMap(getMap(), dataList);
	}

	public <D extends Data> @NotNull HashMap<K, D> toDataMap() {
		var beanMap = getMap();
		var dataMap = new HashMap<K, D>(((beanMap.size() + 2) / 3) * 4);
		Bean.toDataMap(beanMap, dataMap);
		return dataMap;
	}
}
