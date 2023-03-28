package Zeze.Transaction.Collections;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Util.Reflect;
import org.jetbrains.annotations.NotNull;

public final class Meta2<K, V> {
	private static final long map1HeadHash = Bean.hash64("Zeze.Transaction.Collections.LogMap1<");
	private static final long map2HeadHash = Bean.hash64("Zeze.Transaction.Collections.LogMap2<");
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, Meta2<?, ?>>> map1Metas = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, Meta2<?, ?>>> map2Metas = new ConcurrentHashMap<>();

	final int logTypeId;
	final BiConsumer<ByteBuffer, K> keyEncoder;
	final Function<ByteBuffer, K> keyDecoder;
	final BiConsumer<ByteBuffer, V> valueEncoder; // 只用于非Bean类型
	final Function<ByteBuffer, V> valueDecoder; // 只用于非Bean类型
	final MethodHandle valueFactory; // 只用于Bean类型

	private Meta2(long headHash, @NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		logTypeId = Bean.hashLog(headHash, valueClass);
		var keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		var valueCodecFuncs = SerializeHelper.createCodec(valueClass);
		keyEncoder = keyCodecFuncs.encoder;
		keyDecoder = keyCodecFuncs.decoder;
		valueEncoder = valueCodecFuncs.encoder;
		valueDecoder = valueCodecFuncs.decoder;
		valueFactory = Bean.class.isAssignableFrom(valueClass) ? Reflect.getDefaultConstructor(valueClass) : null;
	}

	private Meta2(@NotNull Class<K> keyClass, @NotNull ToLongFunction<Bean> get, @NotNull LongFunction<Bean> create) {
		logTypeId = Bean.hashLog(map2HeadHash, DynamicBean.class);
		var keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		keyEncoder = keyCodecFuncs.encoder;
		keyDecoder = keyCodecFuncs.decoder;
		valueEncoder = null;
		valueDecoder = null;
		valueFactory = SerializeHelper.createDynamicFactory(get, create);
	}

	@SuppressWarnings("unchecked")
	static <K, V> @NotNull Meta2<K, V> getMap1Meta(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		var map = map1Metas.computeIfAbsent(keyClass, __ -> new ConcurrentHashMap<>());
		var r = map.get(keyClass);
		if (r != null)
			return (Meta2<K, V>)r;
		return (Meta2<K, V>)map.computeIfAbsent(valueClass, vc -> new Meta2<>(map1HeadHash, keyClass, (Class<V>)vc));
	}

	@SuppressWarnings("unchecked")
	static <K, V extends Bean> @NotNull Meta2<K, V> getMap2Meta(@NotNull Class<K> keyClass,
																@NotNull Class<V> valueClass) {
		var map = map2Metas.computeIfAbsent(keyClass, __ -> new ConcurrentHashMap<>());
		var r = map.get(keyClass);
		if (r != null)
			return (Meta2<K, V>)r;
		return (Meta2<K, V>)map.computeIfAbsent(valueClass, vc -> new Meta2<>(map2HeadHash, keyClass, (Class<V>)vc));
	}

	static <K, V extends Bean> @NotNull Meta2<K, V> createDynamicMapMeta(@NotNull Class<K> keyClass,
																		 @NotNull ToLongFunction<Bean> get,
																		 @NotNull LongFunction<Bean> create) {
		return new Meta2<>(keyClass, get, create);
	}
}
