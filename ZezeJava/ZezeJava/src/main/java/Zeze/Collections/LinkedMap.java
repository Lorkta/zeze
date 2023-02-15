package Zeze.Collections;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Collections.LinkedMap.BClearJobState;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMap;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeValue;
import Zeze.Component.DelayRemove;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.ChangeListener;
import Zeze.Transaction.Changes;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Util.OutLong;

public class LinkedMap<V extends Bean> {
	public static final BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(Bean bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public static class Module extends AbstractLinkedMap {
		private final ConcurrentHashMap<String, LinkedMap<?>> LinkedMaps = new ConcurrentHashMap<>();
		public final Zeze.Application zeze;
		public static final String eClearJobHandleName = "Zeze.Collections.LinkedMap.Clear";

		public Module(Zeze.Application zeze) {
			this.zeze = zeze;
			RegisterZezeTables(zeze);

			// 总是监听，但不直接开放。
			// 监听回调按LinkedMap.Name的后缀名进行回调，不支持广播。
			_tLinkedMapNodes.getChangeListenerMap().addListener(this::OnLinkedMapNodeChange);
			_tLinkedMaps.getChangeListenerMap().addListener(this::OnLinkedMapRootChange);

			this.zeze.getDelayRemove().register(eClearJobHandleName, this::delayClearJob);
		}

		public ByteBuffer encodeChangeListenerWithSpecialName(String specialName, Object key, Changes.Record r) {
			return _tLinkedMapNodes.encodeChangeListenerWithSpecialName(specialName, key, r);
		}

		private void OnLinkedMapNodeChange(Object key, Changes.Record r) {
			var nodeKey = (BLinkedMapNodeKey)key;
			var indexOf = nodeKey.getName().lastIndexOf('@');
			if (indexOf >= 0) {
				var endsWith = nodeKey.getName().substring(indexOf);
				var listener = NodeListeners.get(endsWith);
				if (null != listener)
					listener.OnChanged(key, r);
			}
		}

		private void OnLinkedMapRootChange(Object key, Changes.Record r) {
			var name = (String)key;
			var indexOf = name.lastIndexOf('@');
			if (indexOf >= 0) {
				var endsWith = name.substring(indexOf);
				var listener = RootListeners.get(endsWith);
				if (null != listener)
					listener.OnChanged(key, r);
			}
		}

		@Override
		public void UnRegister() {
			UnRegisterZezeTables(zeze);
		}

		@SuppressWarnings("unchecked")
		public <T extends Bean> LinkedMap<T> open(String name, Class<T> valueClass, int nodeSize) {
			return (LinkedMap<T>)LinkedMaps.computeIfAbsent(name, k -> new LinkedMap<>(this, k, valueClass, nodeSize));
		}

		@SuppressWarnings("unchecked")
		public <T extends Bean> LinkedMap<T> open(String name, Class<T> valueClass) {
			return (LinkedMap<T>)LinkedMaps.computeIfAbsent(name, k -> new LinkedMap<>(this, k, valueClass, 100));
		}

		public final ConcurrentHashMap<String, ChangeListener> NodeListeners = new ConcurrentHashMap<>();
		public final ConcurrentHashMap<String, ChangeListener> RootListeners = new ConcurrentHashMap<>();

		private void delayClearJob(DelayRemove delayRemove, String jobId, Binary jobState) {
			var state = new BClearJobState();
			state.decode(ByteBuffer.Wrap(jobState));
			while (state.getHeadNodeId() != 0) {
				zeze.newProcedure(() -> {
					var node = _tLinkedMapNodes.get(new BLinkedMapNodeKey(state.getLinkedMapName(), state.getHeadNodeId()));
					if (null == node) {
						state.setHeadNodeId(0);
						delayRemove.setJobState(jobId, null); // remove job
						return 0;
					}

					// removeNode 必须另写，不能直接使用LinkedMap.removeNode。
					for (var e : node.getValues())
						_tValueIdToNodeId.remove(new BLinkedMapKey(state.getLinkedMapName(), e.getId()));
					node.getValues().clear(); // gc
					// clear中的删除节点，马上删除，不需要delayRemove。
					_tLinkedMapNodes.remove(new BLinkedMapNodeKey(state.getLinkedMapName(), state.getHeadNodeId()));

					// save state in this procedure
					state.setHeadNodeId(node.getNextNodeId());
					delayRemove.setJobState(jobId, state);
					return 0;
				}, "clear").call();
			}
		}
	}

	private final Module module;
	private final String name;
	private final int nodeSize;
	private final MethodHandle valueConstructor;

	private LinkedMap(Module module, String name, Class<V> valueClass, int nodeSize) {
		this.module = module;
		this.name = name;
		this.nodeSize = nodeSize;
		this.valueConstructor = beanFactory.register(valueClass);
	}

	public String getName() {
		return name;
	}

	// list
	public BLinkedMap getRoot() {
		return module._tLinkedMaps.get(name);
	}

	public BLinkedMapNode getNode(long nodeId) {
		return module._tLinkedMapNodes.get(new BLinkedMapNodeKey(name, nodeId));
	}

	public BLinkedMapNode getFirstNode(OutLong nodeId) {
		var root = getRoot();
		if (null != root) {
			nodeId.value = root.getHeadNodeId();
			return getNode(root.getHeadNodeId());
		}
		return null;
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public long size() {
		return getRoot().getCount();
	}

	public long moveAhead(String id) {
		return move(id, true);
	}

	public long moveTail(String id) {
		return move(id, false);
	}

	private long move(String id, boolean ahead) {
		var nodeId = module._tValueIdToNodeId.get(new BLinkedMapKey(name, id));
		if (nodeId == null)
			return 0;

		var nodeIdLong = nodeId.getNodeId();
		var node = getNode(nodeIdLong);
		var values = node.getValues();

		// activate。优化：这个操作比较多，已经在目标位置，不调整。
		if (ahead) {
			if (values.get(0).getId().equals(id) && getRoot().getHeadNodeId() == nodeIdLong) // HeadNode && List.Last
				return nodeIdLong;
		} else {
			if (values.get(values.size() - 1).getId().equals(id) && getRoot().getTailNodeId() == nodeIdLong) // TailNode && List.Last
				return nodeIdLong;
		}

		for (int i = 0; i < values.size(); i++) {
			var e = values.get(i);
			if (e.getId().equals(id)) {
				values.remove(i);
				if (values.isEmpty())
					removeNodeUnsafe(nodeId.getNodeId(), node);
				return ahead ? addHeadUnsafe(e.copy()) : addTailUnsafe(e.copy());
			}
		}
		throw new IllegalStateException("Node Exist But Value Not Found.");
	}

	// map
	public V getOrAdd(String id) {
		var value = get(id);
		if (null != value)
			return value;
		value = BeanFactory.invoke(valueConstructor);
		put(id, value);
		return value;
	}

	public V put(long id, V value) {
		return put(String.valueOf(id), value, true);
	}

	public V put(String id, V value) {
		return put(id, value, true);
	}

	public V put(String id, V value, boolean ahead) {
		var nodeIdKey = new BLinkedMapKey(name, id);
		var nodeId = module._tValueIdToNodeId.get(nodeIdKey);
		if (nodeId == null) {
			var newNodeValue = new BLinkedMapNodeValue();
			newNodeValue.setId(id);
			newNodeValue.getValue().setBean(value);
			nodeId = new BLinkedMapNodeId();
			nodeId.setNodeId(ahead ? addHeadUnsafe(newNodeValue) : addTailUnsafe(newNodeValue));
			module._tValueIdToNodeId.insert(nodeIdKey, nodeId);
			var root = getRoot();
			root.setCount(root.getCount() + 1);
			return null;
		}
		var node = getNode(nodeId.getNodeId());
		for (var e : node.getValues()) {
			if (e.getId().equals(id)) {
				@SuppressWarnings("unchecked")
				var old = (V)e.getValue().getBean();
				e.getValue().setBean(value);
				return old;
			}
		}
		throw new IllegalStateException("NodeId Exist. But Value Not Found.");
	}

	public Long getNodeId(String id) {
		var nodeId = module._tValueIdToNodeId.get(new BLinkedMapKey(name, id));
		if (nodeId == null)
			return null;
		return nodeId.getNodeId();
	}

	public Long getNodeId(long id) {
		return getNodeId(String.valueOf(id));
	}

	public BLinkedMapNode getNodeById(String id) {
		var nodeId = module._tValueIdToNodeId.get(new BLinkedMapKey(name, id));
		if (nodeId == null)
			return null;
		return getNode(nodeId.getNodeId());
	}

	public BLinkedMapNode getNodeById(long id) {
		return getNodeById(String.valueOf(id));
	}

	public V get(long id) {
		return get(String.valueOf(id));
	}

	public V get(String id) {
		var nodeId = module._tValueIdToNodeId.get(new BLinkedMapKey(name, id));
		if (nodeId == null)
			return null;

		var node = getNode(nodeId.getNodeId());
		for (var e : node.getValues()) {
			if (e.getId().equals(id)) {
				@SuppressWarnings("unchecked")
				var value = (V)e.getValue().getBean();
				return value;
			}
		}
		return null;
	}

	public V remove(long id) {
		return remove(String.valueOf(id));
	}

	@SuppressWarnings("unchecked")
	public V remove(String id) {
		var nodeKey = new BLinkedMapKey(name, id);
		var nodeId = module._tValueIdToNodeId.get(nodeKey);
		if (nodeId == null)
			return null;

		var node = getNode(nodeId.getNodeId());
		var values = node.getValues();
		for (int i = 0, n = values.size(); i < n; i++) {
			var e = values.get(i);
			if (e.getId().equals(id)) {
				values.remove(i);
				module._tValueIdToNodeId.remove(nodeKey);
				var root = getRoot();
				root.setCount(root.getCount() - 1);
				if (values.isEmpty())
					removeNodeUnsafe(nodeId.getNodeId(), node);
				return (V)e.getValue().getBean();
			}
		}
		throw new IllegalStateException("NodeId Exist. But Value Not Found.");
	}

	public void removeNode(long nodeId) {
		BLinkedMapNode node = getNode(nodeId);
		for (var e : node.getValues())
			module._tValueIdToNodeId.remove(new BLinkedMapKey(name, e.getId()));
		var root = getRoot();
		root.setCount(root.getCount() - node.getValues().size());
		node.getValues().clear();
		removeNodeUnsafe(nodeId, node);
	}

	// foreach
	public void clear() {
		var root = module._tLinkedMaps.get(name);
		if (null != root) {
			var headerNodeId = root.getHeadNodeId();
			var tailNodeId = root.getTailNodeId();
			root.setHeadNodeId(0);
			root.setTailNodeId(0);
			root.setCount(0);
			module.zeze.getDelayRemove().addJob(Module.eClearJobHandleName, new BClearJobState(headerNodeId, tailNodeId, name));
		}
	}

	@SuppressWarnings("unchecked")
	public long walk(TableWalkHandle<String, V> func) {
		long count = 0L;
		var root = module._tLinkedMaps.selectDirty(name);
		if (null == root)
			return count;

		var nodeId = root.getHeadNodeId();
		while (nodeId != 0) {
			var node = module._tLinkedMapNodes.selectDirty(new BLinkedMapNodeKey(name, nodeId));
			if (null == node)
				return count; // error
			for (var value : node.getValues()) {
				++count;
				if (!func.handle(value.getId(), (V)value.getValue().getBean()))
					return count;
			}
			nodeId = node.getNextNodeId();
		}
		return count;
	}

	// inner
	private long addHeadUnsafe(BLinkedMapNodeValue nodeValue) {
		var root = module._tLinkedMaps.getOrAdd(name);
		var headNodeId = root.getHeadNodeId();
		var head = headNodeId != 0 ? getNode(headNodeId) : null;
		if (head != null && head.getValues().size() < nodeSize) {
			// head is null means empty
			head.getValues().add(0, nodeValue);
			return headNodeId;
		}
		var newNode = new BLinkedMapNode();
		if (headNodeId != 0)
			newNode.setNextNodeId(headNodeId); // 这里包含了empty
		newNode.getValues().add(0, nodeValue);
		var newNodeId = root.getLastNodeId() + 1;
		root.setLastNodeId(newNodeId);
		root.setHeadNodeId(newNodeId);
		module._tLinkedMapNodes.insert(new BLinkedMapNodeKey(name, newNodeId), newNode);
		if (head != null)
			head.setPrevNodeId(newNodeId);
		else // isEmpty.
			root.setTailNodeId(newNodeId);
		return newNodeId;
	}

	private long addTailUnsafe(BLinkedMapNodeValue nodeValue) {
		var root = module._tLinkedMaps.getOrAdd(name);
		var tailNodeId = root.getTailNodeId();
		var tail = tailNodeId != 0 ? getNode(tailNodeId) : null;
		if (tail != null && tail.getValues().size() < nodeSize) { // tail is null means empty
			tail.getValues().add(nodeValue);
			return tailNodeId;
		}
		var newNode = new BLinkedMapNode();
		if (tailNodeId != 0)
			newNode.setPrevNodeId(tailNodeId); // 这里包含了empty
		newNode.getValues().add(nodeValue);
		var newNodeId = root.getLastNodeId() + 1;
		root.setLastNodeId(newNodeId);
		root.setTailNodeId(newNodeId);
		module._tLinkedMapNodes.insert(new BLinkedMapNodeKey(name, newNodeId), newNode);
		if (tail != null)
			tail.setNextNodeId(newNodeId);
		else // isEmpty.
			root.setHeadNodeId(newNodeId);
		return newNodeId;
	}

	private void removeNodeUnsafe(long nodeId, BLinkedMapNode node) {
		var root = getRoot();
		var prevNodeId = node.getPrevNodeId();
		var nextNodeId = node.getNextNodeId();

		if (prevNodeId == 0) // is head
			root.setHeadNodeId(nextNodeId);
		else
			getNode(prevNodeId).setNextNodeId(nextNodeId);

		if (nextNodeId == 0) // is tail
			root.setTailNodeId(prevNodeId);
		else
			getNode(nextNodeId).setPrevNodeId(prevNodeId);

		// 没有马上删除，启动gc延迟删除。
		module._tLinkedMapNodes.delayRemove(new BLinkedMapNodeKey(name, nodeId));
	}
}
