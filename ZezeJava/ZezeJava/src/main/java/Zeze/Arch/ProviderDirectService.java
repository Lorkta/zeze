package Zeze.Arch;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Provider.BModule;
import Zeze.Builtin.ProviderDirect.AnnounceProviderInfo;
import Zeze.Builtin.ProviderDirect.ModuleRedirect;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfos;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Util.Action0;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provider之间直连网络管理服务。
 */
public class ProviderDirectService extends Zeze.Services.HandshakeBoth {
	private static final Logger logger = LogManager.getLogger(ProviderDirectService.class);

	protected ProviderApp providerApp;
	public final ConcurrentHashMap<String, ProviderSession> providerByLoadName = new ConcurrentHashMap<>();
	public final LongConcurrentHashMap<ProviderSession> providerByServerId = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<ConcurrentHashSet<Action0>> serverReadyEvents = new LongConcurrentHashMap<>();

	public ProviderDirectService(String name, Zeze.Application zeze) {
		super(name, zeze);
	}

	public synchronized void removeServer(Agent.SubscribeState ss, BServiceInfo pm) {
		var connName = pm.getPassiveIp() + ":" + pm.getPassivePort();
		var conn = getConfig().findConnector(connName);
		if (conn != null) {
			conn.stop();
			providerByLoadName.remove(connName);
			var serverId = Integer.parseInt(pm.getServiceIdentity());
			providerByServerId.remove(serverId);
			ss.setServiceIdentityReadyState(pm.getServiceIdentity(), null);
			getConfig().removeConnector(conn);
		}
	}

	public synchronized void addServer(Agent.SubscribeState ss, BServiceInfo pm) {
		var connName = pm.getPassiveIp() + ":" + pm.getPassivePort();
		var ps = providerByLoadName.get(connName);
		if (ps != null) {
			// connection has ready.
			var mid = Integer.parseInt(pm.getServiceName().split("#")[1]);
			var m = providerApp.modules.get(mid);
			setReady(ss, pm, ps, mid, m);
			return;
		}
		if (pm.getServiceIdentity().startsWith("@")) // from linkd
			return;
		var serverId = Integer.parseInt(pm.getServiceIdentity());
		if (serverId < getZeze().getConfig().getServerId())
			return;
		if (serverId == getZeze().getConfig().getServerId()) {
			setRelativeServiceReady(newSession(serverId), providerApp.directIp, providerApp.directPort);
			return;
		}
		var out = new OutObject<Connector>();
		if (getConfig().tryGetOrAddConnector(pm.getPassiveIp(), pm.getPassivePort(), true, out)) {
			// 新建的Connector。开始连接。
			out.value.userState = newSession(serverId);
			out.value.start();
		}
	}

	public void tryConnectAndSetReady(Agent.SubscribeState ss, BServiceInfos infos) {
		var current = new HashMap<String, BServiceInfo>();
		for (var pm : infos.getServiceInfoListSortedByIdentity()) {
			addServer(ss, pm);
			current.put(pm.getPassiveIp() + ":" + pm.getPassivePort(), pm);
		}
		getConfig().forEachConnector(c -> current.remove(c.getName()));
		for (var pm : current.values()) {
			removeServer(ss, pm);
		}
	}

	@SuppressWarnings("MethodMayBeStatic")
	public ProviderSession newSession(int serverId) {
		var session = new ProviderSession();
		session.serverId = serverId;
		return session;
	}

	@SuppressWarnings("MethodMayBeStatic")
	public ProviderSession newSession(AsyncSocket so) {
		var session = new ProviderSession();
		session.sessionId = so.getSessionId();
		return session;
	}

	@Override
	public void OnSocketAccept(AsyncSocket so) {
		if (so.getConnector() == null) {
			// 被动连接等待对方报告信息时再处理。
			// passive connection continue process in ProviderDirect.ProcessAnnounceProviderInfoRequest.
			so.setUserState(newSession(so)); // acceptor
		}
		super.OnSocketAccept(so);
	}

	@Override
	public void OnHandshakeDone(AsyncSocket so) throws Exception {
		// call base
		super.OnHandshakeDone(so);

		var c = so.getConnector();
		if (c != null) {
			// 主动连接。
			var ps = (ProviderSession)so.getUserState();
			ps.sessionId = so.getSessionId();
			setRelativeServiceReady(ps, c.getHostNameOrAddress(), c.getPort());

			var r = new AnnounceProviderInfo();
			r.Argument.setIp(providerApp.directIp);
			r.Argument.setPort(providerApp.directPort);
			r.Argument.setServerId(providerApp.zeze.getConfig().getServerId());
			r.Send(so, (_r) -> 0L); // skip result
		}
	}

	public void waitDirectServerReady(int serverId) {
		waitDirectServerReady(serverId, 3000);
	}

	public void waitDirectServerReady(int serverId, long timeout) {
		var future = new TaskCompletionSource<Long>();
		Action0 callback = () -> future.setResult(0L);
		try {
			waitDirectServerReady(serverId, callback);
			future.await(timeout);
		} finally {
			serverReadyEvents.computeIfAbsent(serverId, __ -> new ConcurrentHashSet<>()).remove(callback);
		}
	}

	public void waitDirectServerReady(int serverId, Action0 callback) {
		synchronized (this) {
			if (!providerByServerId.containsKey(serverId)) {
				serverReadyEvents.computeIfAbsent(serverId, __ -> new ConcurrentHashSet<>()).add(callback);
				return;
			}
		}
		try {
			callback.run(); // 锁外回调，避免死锁风险。
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// 由于sm的服务信息是碎片传递给订阅者的，所以本质上得到的快照在启动的时候几乎总是不完整的，
	// 为了得到真正所有的服务器信息，只有sleep。
	// 先不提供waitAllDirectServerReady了。
	/*
	public void waitAllDirectServerReady(Action0 callback) {
		// 得到当前provider服务集合（快照）。
		var servers = new HashSet<Integer>();
		for (var ss : providerApp.zeze.getServiceManager().getSubscribeStates().values()) {
			if (ss.getServiceName().startsWith(providerApp.serverServiceNamePrefix)) {
				for (var info : ss.getServiceInfos().getServiceInfoListSortedByIdentity()) {
					var serverId = Integer.parseInt(info.serviceIdentity);
					servers.add(serverId);
				}
			}
		}
		// 得到没有direct没有好的。
		var pending = new HashSet<Integer>();
		synchronized (this) {
			for (var serverId : servers) {
				if (!providerByServerId.containsKey(serverId))
					pending.add(serverId);
			}
		}
		// 订阅没有准备好的。
		for (var pend : pending) {

		}
	}
	*/

	// under lock
	private void notifyServerReady(int serverId) {
		var watchers = serverReadyEvents.computeIfAbsent(serverId, __ -> new ConcurrentHashSet<>());
		for (var w : watchers) {
			try {
				w.run();
			} catch (Exception ex) {
				logger.error("", ex);
			}
		}
		watchers.clear();
	}

	synchronized void setRelativeServiceReady(ProviderSession ps, String ip, int port) {
		ps.serverLoadIp = ip;
		ps.serverLoadPort = port;
		// 本机的连接可能设置多次。此时使用已经存在的，忽略后面的。
		if (providerByLoadName.putIfAbsent(ps.getServerLoadName(), ps) != null)
			return;
		providerByServerId.put(ps.getServerId(), ps);

		// 需要把所有符合当前连接目标的Provider相关的服务信息都更新到当前连接的状态。
		for (var ss : getZeze().getServiceManager().getSubscribeStates().values()) {
			if (ss.getServiceName().startsWith(providerApp.serverServiceNamePrefix)) {
				var infos = ss.getSubscribeType() == BSubscribeInfo.SubscribeTypeSimple
						? ss.getServiceInfos() : ss.getServiceInfosPending();
				if (infos == null)
					continue;
				var mid = Integer.parseInt(ss.getServiceName().split("#")[1]);
				var m = providerApp.modules.get(mid);
				for (var server : infos.getServiceInfoListSortedByIdentity()) {
					// 符合当前连接目标。每个Identity标识的服务的(ip,port)必须不一样。
					if (server.getPassiveIp().equals(ip) && server.getPassivePort() == port) {
						setReady(ss, server, ps, mid, m);
					}
				}
			}
		}
		// 最后才通知成功。
		notifyServerReady(ps.getServerId());
	}

	private void setReady(Agent.SubscribeState ss, BServiceInfo server, ProviderSession ps, int mid, BModule m) {
		var pms = new ProviderModuleState(ps.getSessionId(), mid, m.getChoiceType(), m.getConfigType());
		ps.getOrAddServiceReadyState(ss.getServiceName()).put(server.getServiceIdentity(), pms);
		ss.setServiceIdentityReadyState(server.getServiceIdentity(), pms);
	}

	@Override
	public void OnSocketClose(AsyncSocket socket, Throwable ex) throws Exception {
		var ps = (ProviderSession)socket.getUserState();
		if (ps != null) {
			for (var service : ps.ServiceReadyStates.entrySet()) {
				var subs = getZeze().getServiceManager().getSubscribeStates().get(service.getKey());
				for (var identity : service.getValue().keySet()) {
					subs.setServiceIdentityReadyState(identity, null);
				}
			}
			providerByLoadName.remove(ps.getServerLoadName());
			providerByServerId.remove(ps.getServerId());
		}
		super.OnSocketClose(socket, ex);
	}

	@Override
	public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) {
		var p = decodeProtocol(typeId, bb, factoryHandle, so);
		if (p.getTypeId() == ModuleRedirect.TypeId_) {
			var r = (ModuleRedirect)p;
			// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
			getZeze().getTaskOneByOneByKey().Execute(r.Argument.getHashCode(),
					() -> Task.call(() -> p.handle(this, factoryHandle), p,
							Protocol::trySendResultCode, r.Argument.getMethodFullName()),
					factoryHandle.Mode);
			return;
		}
		if (p.getTypeId() == ModuleRedirectAllResult.TypeId_) {
			var r = (ModuleRedirectAllResult)p;
			// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
			Task.runUnsafe(() -> p.handle(this, factoryHandle), p, Protocol::trySendResultCode,
					r.Argument.getMethodFullName(), factoryHandle.Mode);
			return;
		}
		// 所有的Direct都不启用存储过程。
		Task.runUnsafe(() -> p.handle(this, factoryHandle), p, Protocol::trySendResultCode, null, factoryHandle.Mode);
		//super.DispatchProtocol(p, factoryHandle);
	}

	@Override
	public <P extends Protocol<?>> void dispatchRpcResponse(
			P rpc, ProtocolHandle<P> responseHandle, ProtocolFactoryHandle<?> factoryHandle) {

		if (rpc.getTypeId() == ModuleRedirect.TypeId_) {
			var redirect = (ModuleRedirect)rpc;
			// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
			getZeze().getTaskOneByOneByKey().Execute(redirect.Argument.getHashCode(),
					() -> Task.call(() -> responseHandle.handle(rpc), rpc), factoryHandle.Mode);
			return;
		}

		// no procedure.
		Task.runUnsafe(() -> Task.call(() -> responseHandle.handle(rpc), rpc),
				"ProviderDirectService.DispatchRpcResponse", factoryHandle.Mode);
		//super.DispatchRpcResponse(rpc, responseHandle, factoryHandle);
	}

	@Override
	public void onServerSocketBind(ServerSocket ss) {
		providerApp.directPort = ss.getLocalPort();
	}
}
