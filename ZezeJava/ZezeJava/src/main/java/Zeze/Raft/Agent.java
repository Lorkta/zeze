package Zeze.Raft;

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.ToLongFunction;
import Zeze.Application;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Services.HandshakeClient;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Action1;
import Zeze.Util.Func3;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutObject;
import Zeze.Util.PersistentAtomicLong;
import Zeze.Util.Random;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public final class Agent {
	private static final Logger logger = LogManager.getLogger(Agent.class);
	private static final boolean isDebugEnabled = logger.isDebugEnabled();

	// 保证在Raft-Server检查UniqueRequestId唯一性过期前唯一即可。
	// 使用持久化是为了避免短时间重启，Id重复。
	private final PersistentAtomicLong uniqueRequestIdGenerator;
	private RaftConfig raftConfig;
	private NetClient client;
	private volatile ConnectorEx leader;
	private final LongConcurrentHashMap<RaftRpc<?, ?>> pending = new LongConcurrentHashMap<>();
	private long term;
	public boolean dispatchProtocolToInternalThreadPool;
	private int pendingLimit = 5000; // -1 no limit
	private Future<?> resendTask;

	// 加急请求ReSend时优先发送，多个请求不保证顺序。这个应该仅用于Login之类的特殊协议，一般来说只有一个。
	private final LongConcurrentHashMap<RaftRpc<?, ?>> urgentPending = new LongConcurrentHashMap<>();
	private Action1<Agent> onSetLeader;
	private final Lock mutex = new ReentrantLock();

	public RaftConfig getRaftConfig() {
		return raftConfig;
	}

	public int getPendingLimit() {
		return pendingLimit;
	}

	public void setPendingLimit(int value) {
		pendingLimit = value;
	}

	public NetClient getClient() {
		return client;
	}

	public String getName() {
		return client.getName();
	}

	public ConnectorEx getLeader() {
		return leader;
	}

	public long getTerm() {
		return term;
	}

	public Action1<Agent> getOnSetLeader() {
		return onSetLeader;
	}

	public void setOnSetLeader(Action1<Agent> onSetLeader) {
		this.onSetLeader = onSetLeader;
	}

	public <TArgument extends Serializable, TResult extends Serializable> void send(RaftRpc<TArgument, TResult> rpc,
																					ToLongFunction<Protocol<?>> handle) {
		send(rpc, handle, false);
	}

	/**
	 * 发送Rpc请求。
	 */
	public <TArgument extends Serializable, TResult extends Serializable> void send(RaftRpc<TArgument, TResult> rpc,
																					ToLongFunction<Protocol<?>> handle,
																					boolean urgent) {
		if (handle == null)
			throw new IllegalArgumentException("null handle");
		if (pendingLimit > 0 && pending.size() > pendingLimit) // UrgentPending不限制。
			throw new IllegalStateException("too many pending");

		// 由于interface不能把setter弄成保护的，实际上外面可以修改。
		// 简单检查一下吧。
		if (rpc.getUnique().getRequestId() != 0)
			throw new IllegalStateException("RaftRpc.UniqueRequestId != 0. Need A Fresh RaftRpc");

		rpc.getUnique().setRequestId(uniqueRequestIdGenerator.next());
		// 外面可以设置clientId，默认使用Generator.getName();
		if (rpc.getUnique().getClientId().isEmpty())
			rpc.getUnique().setClientId(uniqueRequestIdGenerator.getName());
		rpc.setCreateTime(System.currentTimeMillis());
		rpc.setSendTime(rpc.getCreateTime());
		if (rpc.getTimeout() == 0) // set default timeout
			rpc.setTimeout(raftConfig.getAgentTimeout());

		rpc.setUrgent(urgent);
		var pending = urgent ? urgentPending : this.pending;
		rpc.handle = handle;
		if (pending.putIfAbsent(rpc.getUnique().getRequestId(), rpc) != null)
			throw new IllegalStateException("duplicate requestId rpc=" + rpc);

		rpc.setResponseHandle(p -> sendHandle(p, rpc));
		ConnectorEx leader = this.leader;
		if (!rpc.Send(leader != null ? leader.TryGetReadySocket() : null))
			logger.debug("Send failed: leader={}, rpc={}", leader, rpc);
	}

	private <TArgument extends Serializable, TResult extends Serializable> long sendHandle(Rpc<TArgument, TResult> p,
																						   RaftRpc<TArgument, TResult> rpc) {
		var net = (RaftRpc<TArgument, TResult>)p;
		if (net.isTimeout() || isRetryError(net.getResultCode()))
			return Procedure.Success; // Pending Will Resend.

		long requestId = rpc.getUnique().getRequestId();
		if (pending.remove(requestId) != null || urgentPending.remove(requestId) != null) {
			rpc.setRequest(net.isRequest());
			rpc.Result = net.Result;
			rpc.setSender(net.getSender());
			rpc.setResultCode(net.getResultCode());

			if (rpc.getResultCode() == Procedure.RaftApplied)
				rpc.setIsTimeout(false);
			if (isDebugEnabled)
				logger.debug("Agent Rpc={} RequestId={} ResultCode={} Sender={}",
						rpc.getClass().getSimpleName(), requestId, rpc.getResultCode(), rpc.getSender());
			return rpc.handle.applyAsLong(rpc);
		}
		return Procedure.Success;
	}

	private static boolean isRetryError(long error) {
		return error == Procedure.CancelException ||
				error == Procedure.RaftRetry ||
				error == Procedure.DuplicateRequest;
	}

	@SuppressWarnings("SameReturnValue")
	private <TArgument extends Serializable, TResult extends Serializable> long sendForWaitHandle(Rpc<TArgument, TResult> p,
																								  RaftRpc<TArgument, TResult> rpc) {
		var net = (RaftRpc<TArgument, TResult>)p;
		if (net.isTimeout() || isRetryError(net.getResultCode()))
			return Procedure.Success; // Pending Will Resend.

		long requestId = rpc.getUnique().getRequestId();
		if (pending.remove(requestId) != null || urgentPending.remove(requestId) != null) {
			rpc.setRequest(net.isRequest());
			rpc.Result = net.Result;
			rpc.setSender(net.getSender());
			rpc.setResultCode(net.getResultCode());

			if (rpc.getResultCode() == Procedure.RaftApplied)
				rpc.setIsTimeout(false);
			if (isDebugEnabled)
				logger.debug("Agent Rpc={} RequestId={} ResultCode={} Sender={}",
						rpc.getClass().getSimpleName(), requestId, rpc.getResultCode(), rpc.getSender());
			rpc.future.setResult(rpc);
		}
		return Procedure.Success;
	}

	public <TArgument extends Serializable, TResult extends Serializable> TaskCompletionSource<RaftRpc<TArgument, TResult>> sendForWait(
			RaftRpc<TArgument, TResult> rpc) {
		return sendForWait(rpc, false);
	}

	public <TArgument extends Serializable, TResult extends Serializable> TaskCompletionSource<RaftRpc<TArgument, TResult>> sendForWait(
			RaftRpc<TArgument, TResult> rpc, boolean urgent) {
		if (pendingLimit > 0 && pending.size() > pendingLimit) // UrgentPending不限制。
			throw new IllegalStateException("too many pending");
		// 由于interface不能把setter弄成保护的，实际上外面可以修改。
		// 简单检查一下吧。
		if (rpc.getUnique().getRequestId() != 0)
			throw new IllegalStateException("RaftRpc.UniqueRequestId != 0. Need A Fresh RaftRpc");

		rpc.getUnique().setRequestId(uniqueRequestIdGenerator.next());
		// 外面在发送前可以设置clientId
		if (rpc.getUnique().getClientId().isEmpty())
			rpc.getUnique().setClientId(uniqueRequestIdGenerator.getName());
		rpc.setCreateTime(System.currentTimeMillis());
		rpc.setSendTime(rpc.getCreateTime());
		if (rpc.getTimeout() == 0) // set default timeout
			rpc.setTimeout(raftConfig.getAgentTimeout());

		var future = new TaskCompletionSource<RaftRpc<TArgument, TResult>>();
		rpc.setUrgent(urgent);
		var pending = urgent ? urgentPending : this.pending;
		rpc.future = future;
		if (pending.putIfAbsent(rpc.getUnique().getRequestId(), rpc) != null)
			throw new IllegalStateException("duplicate requestId rpc=" + rpc);

		rpc.setResponseHandle(p -> sendForWaitHandle(p, rpc));
		ConnectorEx leader = this.leader;
		if (!rpc.Send(leader != null ? leader.TryGetReadySocket() : null))
			logger.debug("Send failed: leader={}, rpc={}", leader, rpc);
		return future;
	}

	public static class ConnectorEx extends Connector {
		public ConnectorEx(String host) {
			this(host, 0);
		}

		public ConnectorEx(String host, int port) {
			super(host, port);
		}
	}

	public void stop() throws Exception {
		mutex.lock();
		try {
			if (resendTask != null) {
				resendTask.cancel(true);
				resendTask = null;
			}
			if (client == null)
				return;

			client.stop();
			client = null;

			leader = null;
			pending.clear();
			urgentPending.clear();
		} finally {
			mutex.unlock();
		}
	}

	public Agent(String name, Application zeze) throws Exception {
		this(name, zeze, null);
	}

	public Agent(String name, Application zeze, RaftConfig raftConf) throws Exception {
		uniqueRequestIdGenerator = PersistentAtomicLong.getOrAdd(name + '.' + zeze.getConfig().getServerId());
		init(new NetClient(this, name, zeze), raftConf);
	}

	public Agent(String name, Application zeze, RaftConfig raftConf,
				 Func3<Agent, String, Application, NetClient> netClientFactory) throws Exception {
		uniqueRequestIdGenerator = PersistentAtomicLong.getOrAdd(name + '.' + zeze.getConfig().getServerId());
		init(netClientFactory.call(this, name, zeze), raftConf);
	}

	public Agent(String name, RaftConfig raftConf) throws Exception {
		this(name, raftConf, null);
	}

	public Agent(String name, RaftConfig raftConf, Config config) throws Exception {
		if (config == null)
			config = Config.load();

		uniqueRequestIdGenerator = PersistentAtomicLong.getOrAdd(name + ',' + config.getServerId());
		init(new NetClient(this, name, config), raftConf);
	}

	public Agent(String name, RaftConfig raftConf, Config config,
				 Func3<Agent, String, Config, NetClient> netClientFactory) throws Exception {
		if (config == null)
			config = Config.load();

		uniqueRequestIdGenerator = PersistentAtomicLong.getOrAdd(name + ',' + config.getServerId());
		init(netClientFactory.call(this, name, config), raftConf);
	}

	private void init(NetClient client, RaftConfig raftConf) throws Exception {
		if (raftConf == null)
			raftConf = RaftConfig.load();

		raftConfig = raftConf;
		this.client = client;

		if (this.client.getConfig().acceptorCount() != 0)
			throw new IllegalStateException("Acceptor Found!");
		if (this.client.getConfig().connectorCount() != 0)
			throw new IllegalStateException("Connector Found!");

		for (var node : raftConfig.getNodes().values())
			this.client.getConfig().addConnector(new ConnectorEx(node.getHost(), node.getPort()));

		this.client.AddFactoryHandle(LeaderIs.TypeId_, new Service.ProtocolFactoryHandle<>(
				LeaderIs::new, this::processLeaderIs, TransactionLevel.Serializable, DispatchMode.Normal));

		// ugly
		resendTask = Task.scheduleUnsafe(1000, 1000, this::resend);
	}

	private Connector getRandomConnector(Connector except) {
		var notMe = new ArrayList<Connector>(client.getConfig().connectorCount());
		client.getConfig().forEachConnector(c -> {
			if (c != except)
				notMe.add(c);
		});
		return notMe.isEmpty() ? null : notMe.get(Random.getInstance().nextInt(notMe.size()));
	}

	private long processLeaderIs(LeaderIs r) throws Exception {
		ConnectorEx leader = this.leader;
		logger.info("=============== LEADERIS Old={} New={} From={}",
				leader != null ? leader.getName() : null, r.Argument.getLeaderId(), r.getSender());

		var node = client.getConfig().findConnector(r.Argument.getLeaderId());
		if (node == null) {
			// 当前 Agent 没有 Leader 的配置，创建一个。
			// 由于 Agent 在新增 node 时也会得到新配置广播，
			// 一般不会发生这种情况。
			var address = r.Argument.getLeaderId().split(":");
			if (address.length != 2)
				return 0;

			OutObject<Connector> outNode = new OutObject<>();
			if (client.getConfig().tryGetOrAddConnector(address[0], Integer.parseInt(address[1]), true, outNode))
				outNode.value.start();
		} else {
			//noinspection DataFlowIssue
			if (!r.Argument.isLeader() && r.Argument.getLeaderId().equals(r.getSender().getConnector().getName())) {
				// 【错误处理】用来观察。
				logger.warn("New Leader Is Not A Leader.");
				// 发送者不是Leader，但它的发送的LeaderId又是自己，【尝试选择另外一个Node】。
				node = getRandomConnector(node);
			}
		}

		if (setLeader(r, node instanceof ConnectorEx ? (ConnectorEx)node : null))
			resend(true);
		// OnLeaderChanged?.Invoke(this);
		r.SendResultCode(0);
		return Procedure.Success;
	}

	private void resend() {
		resend(false);
	}

	public void cancelPending() {
		// 不包括UrgentPending
		if (pending.size() == 0)
			return;

		var removed = new ArrayList<RaftRpc<?, ?>>();
		// Pending存在并发访问，这样写更可靠。
		for (var rpc : pending) {
			var r = pending.remove(rpc.getUnique().getRequestId());
			if (null != r)
				removed.add(r);
		}
		if (isDebugEnabled)
			logger.debug("Found {} RaftRpc cancel", removed.size());
		Task.getCriticalThreadPool().execute(() -> trigger(removed, "Cancel"));
	}

	private void resend(boolean immediately) {
		ConnectorEx leader = this.leader;
		if (leader != null)
			leader.start();
		// ReSendPendingRpc
		var leaderSocket = leader != null ? leader.TryGetReadySocket() : null;
		ArrayList<RaftRpc<?, ?>> removed = null;
		long now = System.currentTimeMillis();
		long timeout = raftConfig.getAppendEntriesTimeout() + 200; // 比一次raft-rpc超时大一些。
		for (var rpc : urgentPending) {
			if (rpc.getTimeout() > 0 && now - rpc.getCreateTime() > rpc.getTimeout()) {
				rpc = urgentPending.remove(rpc.getUnique().getRequestId());
				if (rpc != null) {
					if (removed == null)
						removed = new ArrayList<>();
					removed.add(rpc);
				}
				continue;
			}
			if ((immediately && now - rpc.getCreateTime() > timeout)
					|| now - rpc.getSendTime() > timeout) {
				if (isDebugEnabled)
					logger.debug("ReSendU {}/{} {}", urgentPending.size(), leaderSocket, rpc);
				rpc.setSendTime(now);
				if (!rpc.Send(leaderSocket)) {
					logger.info("SendRequest failed {}", rpc);
					break;
				}
			}
		}
		for (var rpc : pending) {
			if (rpc.getTimeout() > 0 && now - rpc.getCreateTime() > rpc.getTimeout()) {
				rpc = pending.remove(rpc.getUnique().getRequestId());
				if (rpc != null) {
					if (removed == null)
						removed = new ArrayList<>();
					removed.add(rpc);
				}
				continue;
			}
			if ((immediately && now - rpc.getCreateTime() > timeout)
					|| now - rpc.getSendTime() > timeout) {
				if (isDebugEnabled)
					logger.debug("ReSend {}/{} {}", pending.size(), leaderSocket, rpc);
				rpc.setSendTime(now);
				if (!rpc.Send(leaderSocket)) {
					logger.info("SendRequest failed {}", rpc);
					break;
				}
			}
		}
		if (removed != null) {
			if (isDebugEnabled)
				logger.debug("Found {} RaftRpc timeout", removed.size());
			var removed0 = removed;
			Task.getCriticalThreadPool().execute(() -> trigger(removed0));
		}
	}

	private static void trigger(ArrayList<RaftRpc<?, ?>> removed) {
		trigger(removed, "Timeout");
	}

	private static void trigger(ArrayList<RaftRpc<?, ?>> removed, String reason) {
		for (var r : removed) {
			if (null == r)
				continue;
			r.setIsTimeout(true);
			if (null != r.future) {
				r.future.setException(new RuntimeException(reason));
			} else {
				try {
					r.handle.applyAsLong(r);
				} catch (Throwable e) { // run handle. 必须捕捉所有异常。logger.error
					logger.error("", e);
				}
			}
		}
	}

	public boolean setLeader(LeaderIs r, ConnectorEx newLeader) throws Exception {
		mutex.lock();
		try {
			if (r.Argument.getTerm() < term) {
				logger.warn("Skip LeaderIs {} {}", newLeader.getName(), r);
				return false;
			}

			leader = newLeader; // change current Leader
			term = r.Argument.getTerm();
			if (newLeader != null)
				newLeader.start(); // try connect immediately
			Action1<Agent> onSetLeader = this.onSetLeader;
			if (onSetLeader != null)
				onSetLeader.run(this);
			return true;
		} finally {
			mutex.unlock();
		}
	}

	public static class NetClient extends HandshakeClient {
		private final Agent agent;

		public NetClient(Agent agent, String name, Application zeze) {
			super(name, zeze);
			this.agent = agent;
		}

		public NetClient(Agent agent, String name, Config config) {
			super(name, config);
			this.agent = agent;
		}

		public Agent getAgent() {
			return agent;
		}

		@Override
		public <P extends Protocol<?>> void dispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
																ProtocolFactoryHandle<?> factoryHandle) {
			// Raft RPC 的回复处理应该都不是block的,直接在IO线程处理,避免线程池堆满等待又无法唤醒导致死锁
			try {
				responseHandle.handle(rpc);
			} catch (Throwable e) { // run handle. 必须捕捉所有异常。logger.error
				logger.error("Agent.NetClient.dispatchRpcResponse", e);
			}
		}

		@Override
		public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) throws Exception {
			// 不支持事务
			var p = decodeProtocol(typeId, bb, factoryHandle, so);
			p.dispatch(this, factoryHandle);
		}

		@Override
		public void dispatchProtocol(@NotNull Protocol<?> p, @NotNull ProtocolFactoryHandle<?> factoryHandle) throws Exception {
			// 虚拟线程创建太多Critical线程反而容易卡,以后考虑跑另个虚拟线程池里
			if (p.getTypeId() == LeaderIs.TypeId_ || isHandshakeProtocol(p.getTypeId()) || agent.dispatchProtocolToInternalThreadPool) {
				Task.getCriticalThreadPool().execute(() -> Task.call(() -> p.handle(this, factoryHandle), "InternalRequest"));
			} else
				Task.runUnsafe(() -> p.handle(this, factoryHandle),
						p, Protocol::trySendResultCode, null, factoryHandle.Mode);
		}
	}
}
