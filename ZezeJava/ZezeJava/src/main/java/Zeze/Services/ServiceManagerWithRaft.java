package Zeze.Services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Builtin.ServiceManagerWithRaft.*;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.CollMap2;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Raft.UniqueRequestId;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action0;
import Zeze.Util.Func0;
import Zeze.Util.KV;
import Zeze.Util.LongHashMap;
import Zeze.Util.LongHashSet;
import Zeze.Util.LongList;
import Zeze.Util.Random;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServiceManagerWithRaft extends AbstractServiceManagerWithRaft {
	private static final Logger logger = LogManager.getLogger(ServiceManagerWithRaft.class);
	private final Rocks rocks;
	private final Table<String, BAutoKey> tableAutoKey;
	private final Table<String, BSession> tableSession;
	private final Table<String, BLoadObservers> tableLoadObservers;
	private final Table<String, BServerState> tableServerState;
	private final ConcurrentHashMap<Integer, Future<?>> offlineNotifyFutures = new ConcurrentHashMap<>();
	// 需要从配置文件中读取，把这个引用加入：Zeze.Config.AddCustomize
	private final ServiceManagerServer.Conf config;

	public ServiceManagerWithRaft(String raftName, RaftConfig raftConf, Zeze.Config config,
								  boolean RocksDbWriteOptionSync) throws Throwable {
		if (null == config)
			config = Zeze.Config.load();
		this.config = config.getCustomize(new ServiceManagerServer.Conf());

		rocks = new Rocks(raftName, RocksMode.Pessimism, raftConf, config, RocksDbWriteOptionSync, SMServer::new);
		RegisterRocksTables(rocks);
		RegisterProtocols(rocks.getRaft().getServer());
		rocks.getRaft().getServer().Start();

		tableAutoKey = rocks.<String, BAutoKey>getTableTemplate("tAutoKey").openTable();
		tableSession = rocks.<String, BSession>getTableTemplate("tSession").openTable();
		tableLoadObservers = rocks.<String, BLoadObservers>getTableTemplate("tLoadObservers").openTable();
		tableServerState = rocks.<String, BServerState>getTableTemplate("tServerState").openTable();
	}

	/**
	 * 所有Raft网络层收到的请求和Rpc的结果，全部加锁，直接运行。
	 * 这样整个程序就单线程化了。
	 */
	public static class SMServer extends Zeze.Raft.Server {
		public SMServer(Zeze.Raft.Raft raft, String name, Zeze.Config config) throws Throwable {
			super(raft, name, config);
		}

		@Override
		public synchronized <P extends Protocol<?>> void dispatchRaftRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
																ProtocolFactoryHandle<?> factoryHandle) throws Throwable {
			Task.runRpcResponseUnsafe(() -> responseHandle.handle(rpc), rpc, DispatchMode.Direct);
		}

		@Override
		public synchronized void dispatchRaftRequest(UniqueRequestId key, Func0<?> func, String name, Action0 cancel, DispatchMode mode) {
			try {
				func.call();
			} catch (Throwable ex) {
				logger.error("impossible!", ex);
			}
		}

		@Override
		public void OnSocketClose(AsyncSocket so, Throwable e) throws Throwable {
			var netSession = (Session)so.getUserState();
			if (null != netSession) {
				synchronized (this) {
					netSession.onClose();
				}
			}
			super.OnSocketClose(so, e);
		}
	}

	public class Session {
		private final String name;
		private final long sessionId;
		private final Future<?> keepAliveTimerTask;
		public static final long eOfflineNotifyDelay = 60 * 1000;

		public Session(String name, long sessionId) {
			this.name = name;
			this.sessionId = sessionId;

			if (config.keepAlivePeriod > 0) {
				keepAliveTimerTask = Task.scheduleUnsafe(
						Random.getInstance().nextInt(config.keepAlivePeriod),
						config.keepAlivePeriod,
						() -> {
							AsyncSocket s = null;
							try {
								s = rocks.getRaft().getServer().GetSocket(sessionId);
								var r = new KeepAlive();
								r.SendAndWaitCheckResultCode(s);
							} catch (Throwable ex) {
								if (s != null)
									s.close();
								logger.error("ServiceManager.KeepAlive", ex);
							}
						});
			} else
				keepAliveTimerTask = null;
		}

		public void onClose() {
			if (keepAliveTimerTask != null)
				keepAliveTimerTask.cancel(false);

			/*
			for (var info : subscribes.values())
				serviceManager.unSubscribeNow(sessionId, info);

			var changed = new HashMap<String, ServerState>(registers.size());
			for (var info : registers) {
				var state = serviceManager.unRegisterNow(sessionId, info);
				if (state != null)
					changed.putIfAbsent(state.serviceName, state);
			}
			changed.values().forEach(ServerState::startReadyCommitNotify);
			 */
			// offline notify，开启一个线程执行，避免互等造成麻烦。
			// 这个操作不能cancel，即使Server重新起来了，通知也会进行下去。
			var session = tableSession.get(name);
			if (null != session) {
				var serverId = session.getOfflineRegisterServerId();
				if (serverId >= 0) {
					if (!offlineNotifyFutures.containsKey(serverId))
						offlineNotifyFutures.put(serverId, Task.scheduleUnsafe(eOfflineNotifyDelay,
								() -> offlineNotify(session,true)));
				} else {
					Task.run(() -> offlineNotify(session, false), "offlineNotifyImmediately");
				}
			}
		}

		private void tryNotifyOffline(OfflineNotify notify, BSession session, BOfflineNotifyRocks notifyId, HashSet<Session> skips) {
			var selected = randomFor(session, notifyId.getNotifyId(), skips);
			if (selected == null)
				return; // 没有找到可用的通知对象，放弃通知。

			notify.Send(selected.getValue(), (This) -> {
				logger.info("OfflineNotify: serverId={} notifyId={} selectSessionId={} resultCode={}",
						session.getOfflineRegisterServerId(), notifyId, selected.getKey().sessionId, notify.getResultCode());
				if (notify.getResultCode() != 0)
					tryNotifyOffline(notify, session, notifyId, skips);
				return 0;
			});

			// 保存这一次通知失败session，下一次尝试选择的时候忽略。
			skips.add(selected.getKey());
		}

		private void offlineNotify(BSession session, boolean delay) {
			var serverId = session.getOfflineRegisterServerId();
			if (delay && null == offlineNotifyFutures.remove(serverId))
				return; // 此serverId的新连接已经连上或者通知已经执行。

			logger.info("OfflineNotify: serverId={} notifyIds={} begin",
					serverId, session.getOfflineRegisterNotifies());
			for (var notifyId : session.getOfflineRegisterNotifies().values()) {
				var skips = new HashSet<Session>();

				var notify = new OfflineNotify();
				var netNotifyId = notify.Argument;
				netNotifyId.setServerId(notifyId.getServerId());
				netNotifyId.setNotifyId(notifyId.getNotifyId());
				netNotifyId.setNotifySerialId(notifyId.getNotifySerialId());
				netNotifyId.setNotifyContext(notifyId.getNotifyContext());

				tryNotifyOffline(notify, session, notifyId, skips);
			}
			logger.info("OfflineNotify: serverId={} end", session.getOfflineRegisterServerId());
		}

		// 从注册了这个notifyId的其他session中随机选择一个。
		private KV<Session, AsyncSocket> randomFor(BSession session, String notifyId, HashSet<Session> skips) {
			var sessions = new ArrayList<KV<Session, AsyncSocket>>();
			try {
				rocks.getRaft().getServer().foreach(socket -> {
					var netSession = (Session)socket.getUserState();
					if (netSession != null && netSession != this && !skips.contains(netSession)) {
						if (session.getOfflineRegisterNotifies().containsKey(notifyId))
							sessions.add(KV.create(netSession, socket));
					}
				});
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
			if (sessions.isEmpty())
				return null;
			return sessions.get(Random.getInstance().nextInt(sessions.size()));
		}
	}

	@Override
	protected long ProcessLoginRequest(Login r) throws Throwable {
		var session = tableSession.getOrAdd(r.Argument.getSessionName());
		r.getSender().setUserState(new Session(r.Argument.getSessionName(), r.getSender().getSessionId()));
		session.setSessionId(r.getSender().getSessionId());
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessAllocateIdRequest(AllocateId r) throws Throwable {
		r.Result.setName(r.Argument.getName());
		var autoKey = tableAutoKey.getOrAdd(r.Argument.getName());

		r.Result.setStartId(autoKey.getCurrent());

		// 随便修正一下分配数量。
		var count = r.Argument.getCount();
		if (count < 256)
			count = 256;
		else if (count > 10000)
			count = 10000;

		long current = autoKey.getCurrent() + count;
		autoKey.setCurrent(current);
		r.Result.setCount(count);

		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessOfflineRegisterRequest(OfflineRegister r) throws Throwable {
		logger.info("{}: OfflineRegister serverId={} notifyId={}",
				r.getSender(), r.Argument.getServerId(), r.Argument.getNotifyId());
		var netSession = (Session)r.getSender().getUserState();
		var session = tableSession.get(netSession.name);
		// 允许重复注册：简化server注册逻辑。
		session.setOfflineRegisterServerId(r.Argument.getServerId());

		var bOfflineNotifyRocks = new BOfflineNotifyRocks();
		bOfflineNotifyRocks.setServerId(r.Argument.getServerId());
		bOfflineNotifyRocks.setNotifyContext(r.Argument.getNotifyContext());
		bOfflineNotifyRocks.setNotifyId(r.Argument.getNotifyId());
		bOfflineNotifyRocks.setNotifySerialId(r.Argument.getNotifySerialId());
		session.getOfflineRegisterNotifies().put(r.Argument.getNotifyId(), bOfflineNotifyRocks);

		var future = offlineNotifyFutures.remove(r.Argument.getServerId());
		if (null != future)
			future.cancel(true);

		r.SendResult();
		return 0;
	}

	// todo subscribeAndSend 的时候注册
	//  for (var info : serviceInfos.values())
	//     serviceManager.addLoadObserver(info.getPassiveIp(), info.getPassivePort(), r.getSender());
	private void addLoadObserver(String ip, int port, AsyncSocket sender) {
		if (!ip.isEmpty() && port != 0) {
			var loadObservers = tableLoadObservers.getOrAdd(ip + ":" + port);
			loadObservers.getObservers().add(((Session)sender.getUserState()).name);
		}
	}

	@Override
	protected long ProcessSetServerLoadRequest(SetServerLoad r) throws Throwable {
		var loadObservers = tableLoadObservers.getOrAdd(r.Argument.getIp() + ":" + r.Argument.getPort());
		var observers = loadObservers.getObservers();

		var set = new SetServerLoad();
		set.Argument = r.Argument;

		ArrayList<String> removed = null;
		for (var observer : observers) {
			try {
				var session = tableSession.get(observer);
				if (null != session && set.Send(rocks.getRaft().getServer().GetSocket(session.getSessionId())))
					continue;
			} catch (Throwable ignored) {
			}
			if (removed == null)
				removed = new ArrayList<>();
			removed.add(observer);
		}
		if (removed != null) {
			for (var remove : removed)
				observers.remove(remove);
		}
		return 0;
	}

	private static BServiceInfoRocks toRocks(BServiceInfo serverInfo, String sessionName) {
		return new BServiceInfoRocks(serverInfo.getServiceName(), serverInfo.getServiceIdentity(),
				serverInfo.getPassiveIp(), serverInfo.getPassivePort(),
				serverInfo.getExtraInfo(), serverInfo.getParam(),
				sessionName);
	}

	private static BKeyServiceInfoRocks toRocksKey(BServiceInfo serverInfo) {
		return new BKeyServiceInfoRocks(serverInfo.getServiceName(), serverInfo.getServiceIdentity(),
				serverInfo.getPassiveIp(), serverInfo.getPassivePort(),
				serverInfo.getExtraInfo(), serverInfo.getParam());
	}

	private static BSubscribeInfoRocks toRocks(BSubscribeInfo si) {
		return new BSubscribeInfoRocks(si.getServiceName(), si.getSubscribeType());
	}

	@Override
	protected long ProcessRegisterRequest(Register r) throws Throwable {
		var netSession = (Session)r.getSender().getUserState();
		var session = tableSession.get(netSession.name);
		// 允许重复登录，断线重连Agent不好原子实现重发。
		if (session.getRegisters().add(toRocksKey(r.Argument))) {
			logger.info("{}: Register {} serverId={} ip={} port={}", r.getSender(), r.Argument.getServiceName(),
					r.Argument.getServiceIdentity(), r.Argument.getPassiveIp(), r.Argument.getPassivePort());
		} else {
			logger.info("{}: Already Registered {} serverId={} ip={} port={}", r.getSender(), r.Argument.getServiceName(),
					r.Argument.getServiceIdentity(), r.Argument.getPassiveIp(), r.Argument.getPassivePort());
		}
		var state = tableServerState.getOrAdd(r.Argument.getServiceName());

		// AddOrUpdate，否则重连重新注册很难恢复到正确的状态。
		state.getServiceInfos().put(r.Argument.getServiceIdentity(), toRocks(r.Argument, netSession.name));
		r.SendResultCode(Register.Success);
		startReadyCommitNotify(state);
		notifySimpleOnRegister(state, r.Argument);

		return Procedure.Success;
	}

	private void startReadyCommitNotify(BServerState state) {
		// todo
	}

	private void notifySimpleOnRegister(BServerState state, BServiceInfo info) {
		if (state.getSimple().size() == 0)
			return;
		var sb = new StringBuilder();
		for (var sessionName : state.getSimple().keys()) {
			var session = tableSession.get(sessionName);
			if (null != session && new Register(info).Send(rocks.getRaft().getServer().GetSocket(session.getSessionId()))) {
				sb.append(sessionName).append(',');
				continue;
			}
			logger.warn("NotifySimpleOnRegister {} failed: serverId({}) => sessionId({})",
					info.getServiceName(), info.getServiceIdentity(), sessionName);
		}
		var n = sb.length();
		if (n > 0) {
			sb.setLength(n - 1);
			logger.info("NotifySimpleOnRegister {} serverId({}) => sessionIds({})",
					info.getServiceName(), info.getServiceIdentity(), sb);
		}
	}

	@Override
	protected long ProcessSubscribeRequest(Subscribe r) throws Throwable {
		logger.info("{}: Subscribe {} type={}",
				r.getSender(), r.Argument.getServiceName(), r.Argument.getSubscribeType());
		var netSession = (Session)r.getSender().getUserState();
		var session = tableSession.get(netSession.name);
		session.getSubscribes().put(r.Argument.getServiceName(), toRocks(r.Argument));
		var state = tableServerState.getOrAdd(r.Argument.getServiceName());
		return subscribeAndSend(state, r, netSession.name);
	}

	@Override
	protected long ProcessUnRegisterRequest(UnRegister r) throws Throwable {
		logger.info("{}: UnRegister {} serverId={}",
				r.getSender(), r.Argument.getServiceName(), r.Argument.getServiceIdentity());
		var netSession = (Session)r.getSender().getUserState();
		unRegisterNow(netSession.name, r.Argument); // ignore UnRegisterNow failed
		var session = tableSession.get(netSession.name);
		session.getRegisters().remove(toRocksKey(r.Argument)); // ignore remove failed
		// 注销不存在也返回成功，否则Agent处理比较麻烦。
		r.SendResultCode(Zeze.Services.ServiceManager.UnRegister.Success);
		return Procedure.Success;
	}

	public BServerState unRegisterNow(String ssName, BServiceInfo info) {
		var state = tableServerState.get(info.getServiceName());
		if (state != null) {
			var exist = state.getServiceInfos().get(info.getServiceIdentity());
			state.getServiceInfos().remove(info.getServiceIdentity());
			if (exist != null && exist.getSessionName().equals(ssName)) {
				// 有可能当前连接没有注销，新的注册已经AddOrUpdate，此时忽略当前连接的注销。
				notifySimpleOnUnRegister(state, fromRocks(exist));
				return state;
			}
		}
		return null;
	}

	private static BServiceInfo fromRocks(BServiceInfoRocks rocks) {
		return new BServiceInfo(rocks.getServiceName(), rocks.getServiceIdentity(),
				rocks.getPassiveIp(), rocks.getPassivePort(), rocks.getExtraInfo(), rocks.getParam());
	}

	public void notifySimpleOnUnRegister(BServerState state, BServiceInfo info) {
		if (state.getSimple().size() == 0)
			return;

		var sb = new StringBuilder();
		for (var sessionName : state.getSimple().keys()) {
			var session = tableSession.get(sessionName);
			if (null != session && new UnRegister(info).Send(rocks.getRaft().getServer().GetSocket(session.getSessionId()))) {
				sb.append(sessionName).append(',');
				continue;
			}
			logger.warn("NotifySimpleOnUnRegister {} failed: serverId({}) => sessionId({})",
					info.getServiceName(), info.getServiceIdentity(), sessionName);
		}
		var n = sb.length();
		if (n > 0) {
			sb.setLength(n - 1);
			logger.info("NotifySimpleOnUnRegister {} serverId({}) => sessionIds({})",
					info.getServiceName(), info.getServiceIdentity(), sb);
		}
	}

	@Override
	protected long ProcessUnSubscribeRequest(UnSubscribe r) throws Throwable {
		logger.info("{}: UnSubscribe {} type={}",
				r.getSender(), r.Argument.getServiceName(), r.Argument.getSubscribeType());
		var netSession = (Session)r.getSender().getUserState();
		var session = tableSession.get(netSession.name);
		var sub = session.getSubscribes().get(r.Argument.getServiceName());
		session.getSubscribes().remove(r.Argument.getServiceName());
		if (sub != null) {
			if (r.Argument.getSubscribeType() == sub.getSubscribeType()) {
				var changed = unSubscribeNow(netSession.name, r.Argument);
				if (changed != null) {
					r.setResultCode(UnSubscribe.Success);
					r.SendResult();
					tryCommit(changed);
					return Procedure.Success;
				}
			}
		}
		// 取消订阅不能存在返回成功。否则Agent比较麻烦。
		//r.ResultCode = UnSubscribe.NotExist;
		//r.SendResult();
		//return Procedure.LogicError;
		r.setResultCode(Zeze.Services.ServiceManager.UnRegister.Success);
		r.SendResult();
		return Procedure.Success;
	}

	public BServerState unSubscribeNow(String sessionName, BSubscribeInfo info) {
		var state = tableServerState.get(info.getServiceName());
		if (state != null) {
			CollMap2<String, BSubscribeStateRocks> subState;
			switch (info.getSubscribeType()) {
			case BSubscribeInfo.SubscribeTypeSimple:
				subState = state.getSimple();
				break;
			case BSubscribeInfo.SubscribeTypeReadyCommit:
				subState = state.getReadyCommit();
				break;
			default:
				return null;
			}
			var removed = subState.get(sessionName);
			subState.remove(sessionName);
			if (removed != null)
				return state;
		}
		return null;
	}

	@Override
	protected long ProcessUpdateRequest(Update r) throws Throwable {
		logger.info("{}: Update {} serverId={} ip={} port={}", r.getSender(), r.Argument.getServiceName(),
				r.Argument.getServiceIdentity(), r.Argument.getPassiveIp(), r.Argument.getPassivePort());
		var netSession = (Session)r.getSender().getUserState();
		var session = tableSession.get(netSession.name);
		if (!session.getRegisters().contains(toRocksKey(r.Argument)))
			return Zeze.Services.ServiceManager.Update.ServiceNotRegister;

		var state = tableServerState.get(r.Argument.getServiceName());
		if (state == null)
			return Update.ServerStateError;

		var rc = updateAndNotify(state, r.Argument);
		if (rc != 0)
			return rc;
		r.SendResult();
		return 0;
	}

	public int updateAndNotify(BServerState state, BServiceInfo info) {
		var current = state.getServiceInfos().get(info.getServiceIdentity());
		if (current == null)
			return Update.ServiceIdentityNotExist;

		current.setPassiveIp(info.getPassiveIp());
		current.setPassivePort(info.getPassivePort());
		current.setExtraInfo(info.getExtraInfo());

		// 简单广播。
		var sb = new StringBuilder();
		for (var sessionName : state.getSimple().keys()) {
			var session = tableSession.get(sessionName);
			if (null != session && new Update(fromRocks(current)).Send(rocks.getRaft().getServer().GetSocket(session.getSessionId()))) {
				sb.append(sessionName).append(',');
				continue;
			}

			logger.warn("UpdateAndNotify {} failed: serverId({}) => sessionId({})",
					info.getServiceName(), info.getServiceIdentity(), sessionName);
		}
		var n = sb.length();
		if (n > 0)
			sb.setCharAt(n - 1, ';');
		else
			sb.append(';');

		for (var sessionName : state.getReadyCommit().keys()) {
			var session = tableSession.get(sessionName);
			if (new Update(fromRocks(current)).Send(rocks.getRaft().getServer().GetSocket(session.getSessionId()))) {
				sb.append(sessionName).append(',');
				continue;
			}
			logger.warn("UpdateAndNotify {} failed: serverId({}) => sessionId({})",
					info.getServiceName(), info.getServiceIdentity(), sessionName);
		}
		if (sb.length() > 1)
			logger.info("UpdateAndNotify {} serverId({}) => sessionIds({})",
					info.getServiceName(), info.getServiceIdentity(), sb);
		return 0;
	}

	@Override
	protected long ProcessReadyServiceListRequest(ReadyServiceList r) throws Throwable {
		return 0;
	}

	private long subscribeAndSend(BServerState state, Subscribe r, String ssName) {
		// todo
		return 0;
	}

	private void tryCommit(BServerState state) {

	}

}
