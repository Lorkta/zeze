package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Beans.Provider.BModule;
import Zeze.Beans.ProviderDirect.AnnounceProviderInfo;
import Zeze.Beans.ProviderDirect.ModuleRedirect;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllResult;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.ServiceInfo;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provider之间直连网络管理服务。
 */
public class ProviderDirectService extends Zeze.Services.HandshakeBoth {
	private static final Logger logger = LogManager.getLogger(ProviderDirectService.class);
	public ProviderApp ProviderApp;

	public ProviderDirectService(String name, Zeze.Application zeze) throws Throwable {
		super(name, zeze);
	}

	public synchronized void TryConnectAndSetReady(Agent.SubscribeState ss) {
		var infos = ss.getServiceInfosPending();
		if (null == infos)
			return;

		for (var pm : infos.getServiceInfoListSortedByIdentity()) {
			var connName = pm.getPassiveIp() + ":" + pm.getPassivePort();
			var ps = ProviderSessions.get(connName);
			if (null != ps) {
				// connection has ready.
				System.out.println("TryConnectAndSetReady " + getZeze().getConfig().getServerId() + " identity=" + pm.getServiceIdentity());
				var mid = Integer.parseInt(infos.getServiceName().split("#")[1]);
				var m = ProviderApp.Modules.get(mid);
				SetReady(ss, pm, ps, mid, m);
				continue;
			}
			var serverId = Integer.parseInt(pm.getServiceIdentity());
			if (serverId < getZeze().getConfig().getServerId())
				continue;
			if (serverId == getZeze().getConfig().getServerId()) {
				SetRelativeServiceReady(new ProviderSession(0), ProviderApp.DirectIp, ProviderApp.DirectPort);
				continue;
			}
			var out = new OutObject<Connector>();
			if (getConfig().TryGetOrAddConnector(pm.getPassiveIp(), pm.getPassivePort(), true, out)) {
				// 新建的Connector。开始连接。
				System.out.println("Connect To " + out.Value.getName());
				out.Value.Start();
			}
		}
	}

	@Override
	public void OnHandshakeDone(AsyncSocket socket) throws Throwable {
		super.OnHandshakeDone(socket);

		var ps = new ProviderSession(socket.getSessionId());
		socket.setUserState(ps);
		var c = socket.getConnector();
		if (c != null) {
			// 主动连接。
			System.out.println("OnHandshakeDone " + c.getName() + " serverId=" + getZeze().getConfig().getServerId()
				+ " ss=" + ProviderSessions);
			SetRelativeServiceReady(ps, c.getHostNameOrAddress(), c.getPort());
			System.out.println("OnHandshakeDone ++++ " + c.getName() + " serverId=" + getZeze().getConfig().getServerId()
					+ " ss=" + ProviderSessions);
			var r = new AnnounceProviderInfo();
			r.Argument.setIp(ProviderApp.DirectIp);
			r.Argument.setPort(ProviderApp.DirectPort);
			r.Send(socket, (_r) -> 0L); // skip result
		}
		// 被动连接等待对方报告信息时再处理。
		// call base
	}

	public ConcurrentHashMap<String, ProviderSession> ProviderSessions = new ConcurrentHashMap<>();

	synchronized void SetRelativeServiceReady(ProviderSession ps, String ip, int port) {
		ps.ServerLoadIp = ip;
		ps.ServerLoadPort = port;
		// 本机的连接可能设置多次。此时使用已经存在的，忽略后面的。
		if (null != ProviderSessions.putIfAbsent(ps.getServerLoadName(), ps))
			return;

		// 需要把所有符合当前连接目标的Provider相关的服务信息都更新到当前连接的状态。
		for (var ss : getZeze().getServiceManagerAgent().getSubscribeStates().values()) {
			if (ss.getServiceName().startsWith(ProviderApp.ServerServiceNamePrefix)) {
				var infos = ss.getServiceInfosPending();
				if (null == infos)
					continue;
				var mid = Integer.parseInt(ss.getServiceName().split("#")[1]);
				var m = ProviderApp.Modules.get(mid);
				for (var server : infos.getServiceInfoListSortedByIdentity()) {
					// 符合当前连接目标。每个Identity标识的服务的(ip,port)必须不一样。
					if (server.getPassiveIp().equals(ip) && server.getPassivePort() == port) {
						SetReady(ss, server, ps, mid, m);
					}
				}
			}
		}
	}

	private void SetReady(Agent.SubscribeState ss, ServiceInfo server, ProviderSession ps, int mid, BModule m) {
		var pms = new ProviderModuleState(ps.getSessionId(), mid, m.getChoiceType(), m.getConfigType());
		ps.GetOrAddServiceReadyState(ss.getServiceName()).put(server.getServiceIdentity(), pms);
		System.out.println("SetReady " + getZeze().getConfig().getServerId() + " " + ss.getServiceName() + ":" + server.getServiceIdentity());
		ss.SetServiceIdentityReadyState(server.getServiceIdentity(), pms);
	}

	@Override
	public void OnSocketClose(AsyncSocket socket, Throwable ex) throws Throwable {
		var ps = (ProviderSession)socket.getUserState();
		if (ps != null) {
			for (var service : ps.ServiceReadyStates.entrySet()) {
				var subs = getZeze().getServiceManagerAgent().getSubscribeStates().get(service.getKey());
				for (var identity : service.getValue().keySet()) {
					subs.SetServiceIdentityReadyState(identity, null);
				}
			}
			ProviderSessions.remove(ps.getServerLoadName());
		}
		super.OnSocketClose(socket, ex);
	}

	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) throws Throwable {
		// 防止Client不进入加密，直接发送用户协议。
		if (!IsHandshakeProtocol(p.getTypeId())) {
			p.getSender().VerifySecurity();
		}

		if (p.getTypeId() == ModuleRedirect.TypeId_) {
			if (null != factoryHandle.Handle) {
				var redirect = (ModuleRedirect)p;
				// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
				getZeze().getTaskOneByOneByKey().Execute(redirect.Argument.getHashCode(),
						() -> Zeze.Util.Task.Call(() -> factoryHandle.Handle.handle(p), p, Protocol::SendResultCode));
			} else
				logger.warn("Protocol Handle Not Found: {}", p);
			return;
		}
		if (p.getTypeId() == ModuleRedirectAllResult.TypeId_) {
			if (null != factoryHandle.Handle) {
				var r = (ModuleRedirectAllResult)p;
				// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
				Zeze.Util.Task.Call(() -> factoryHandle.Handle.handle(p), p, Protocol::SendResultCode, r.Argument.getMethodFullName());
			} else
				logger.warn("Protocol Handle Not Found: {}", p);
			return;
		}
		// 所有的Direct都不启用存储过程。
		Zeze.Util.Task.Call(() -> factoryHandle.Handle.handle(p), p, Protocol::SendResultCode);
		//super.DispatchProtocol(p, factoryHandle);
	}

	@Override
	public <P extends Protocol<?>> void DispatchRpcResponse(
			P rpc, ProtocolHandle<P> responseHandle, ProtocolFactoryHandle<?> factoryHandle) throws Throwable {

		if (rpc.getTypeId() == ModuleRedirect.TypeId_) {
			var redirect = (ModuleRedirect)rpc;
			// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
			getZeze().getTaskOneByOneByKey().Execute(redirect.Argument.getHashCode(),
					() -> Zeze.Util.Task.Call(() -> responseHandle.handle(rpc), rpc));
			return;
		}

		// no procedure.
		Task.run(() -> Task.Call(() -> responseHandle.handle(rpc), rpc), "ProviderDirectService.DispatchRpcResponse");
		//super.DispatchRpcResponse(rpc, responseHandle, factoryHandle);
	}
}
