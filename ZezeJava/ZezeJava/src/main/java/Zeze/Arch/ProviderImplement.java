package Zeze.Arch;

import Zeze.Builtin.Provider.AnnounceLinkInfo;
import Zeze.Builtin.Provider.BKick;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Builtin.Provider.Kick;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.OutObject;
import Zeze.Util.PerfCounter;
import Zeze.Util.Task;
import Zeze.Util.TransactionLevelAnnotation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ProviderImplement extends AbstractProviderImplement {
	private static final Logger logger = LogManager.getLogger(ProviderImplement.class);
	private static final ThreadLocal<Dispatch> localDispatch = new ThreadLocal<>();

	protected ProviderApp providerApp;

	void addServer(Agent.SubscribeState ss, BServiceInfo pm) {
		if (ss.getServiceName().equals(providerApp.linkdServiceName))
			providerApp.providerService.apply(pm);
	}

	void applyOnChanged(Agent.SubscribeState subState) {
		if (subState.getServiceName().equals(providerApp.linkdServiceName)) {
			// Linkd info
			providerApp.providerService.apply(subState.getServiceInfos());
		} else if (subState.getServiceName().startsWith(providerApp.serverServiceNamePrefix)) {
			// Provider info
			// 对于 SubscribeTypeSimple 是不需要 SetReady 的，为了能一致处理，就都设置上了。
			// 对于 SubscribeTypeReadyCommit 在 ApplyOnPrepare 中处理。
			if (subState.getSubscribeType() == BSubscribeInfo.SubscribeTypeSimple)
				providerApp.providerDirectService.tryConnectAndSetReady(subState, subState.getServiceInfos());
		}
	}

	void applyOnPrepare(Agent.SubscribeState subState) {
		var pending = subState.getServiceInfosPending();
		if (pending != null && pending.getServiceName().startsWith(providerApp.serverServiceNamePrefix))
			providerApp.providerDirectService.tryConnectAndSetReady(subState, pending);
	}

	public static @Nullable Dispatch localDispatch() {
		return localDispatch.get();
	}

	/**
	 * 注册所有支持的模块服务。
	 * 包括静态动态。
	 * 注册的模块时带上用于Provider之间连接的ip，port。
	 * <p>
	 * 订阅Linkd服务。
	 * Provider主动连接Linkd。
	 */
	public void registerModulesAndSubscribeLinkd() {
		var sm = providerApp.zeze.getServiceManager();
		var identity = String.valueOf(providerApp.zeze.getConfig().getServerId());
		// 注册本provider的静态服务
		for (var it = providerApp.staticBinds.iterator(); it.moveToNext(); ) {
			sm.registerService(providerApp.serverServiceNamePrefix + it.key(), identity,
					providerApp.directIp, providerApp.directPort);
		}
		// 注册本provider的动态服务
		for (var it = providerApp.dynamicModules.iterator(); it.moveToNext(); ) {
			sm.registerService(providerApp.serverServiceNamePrefix + it.key(), identity,
					providerApp.directIp, providerApp.directPort);
		}

		// 订阅provider直连发现服务
		for (var it = providerApp.modules.iterator(); it.moveToNext(); )
			sm.subscribeService(providerApp.serverServiceNamePrefix + it.key(), it.value().getSubscribeType());

		// 订阅linkd发现服务。
		sm.subscribeService(providerApp.linkdServiceName, BSubscribeInfo.SubscribeTypeSimple);
	}

	public static void sendKick(AsyncSocket sender, long linkSid, int code, @NotNull String desc) {
		new Kick(new BKick.Data(linkSid, code, desc)).Send(sender);
	}

	@SuppressWarnings("MethodMayBeStatic")
	public ProviderUserSession newSession(Dispatch p) {
		return new ProviderUserSession(p);
	}

	@TransactionLevelAnnotation(Level = TransactionLevel.None)
	@Override
	protected long ProcessDispatch(Dispatch p) {
		var sender = p.getSender();
		var arg = p.Argument;
		var linkSid = arg.getLinkSid();
		var typeId = arg.getProtocolType();
		Protocol<?> p2 = null;
		try {
			var factoryHandle = providerApp.providerService.findProtocolFactoryHandle(typeId);
			if (factoryHandle == null) {
				sendKick(sender, linkSid, BKick.ErrorProtocolUnknown, "unknown protocol: " + typeId);
				return Procedure.LogicError;
			}
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			localDispatch.set(p);
			int psize = arg.getProtocolData().size();
			var session = newSession(p);
			var zeze = sender.getService().getZeze();
			var txn = Transaction.getCurrent();
			var outRpcContext = new OutObject<Rpc<?, ?>>();
			if (txn == null && zeze != null && factoryHandle.Level != TransactionLevel.None) {
				var outProtocol = new OutObject<Protocol<?>>();
				var r = Task.call(zeze.newProcedure(() -> { // 创建存储过程并且在当前线程中调用。
					var p3 = factoryHandle.Factory.create();
					var t = Transaction.getCurrent();
					var proc = t.getTopProcedure();
					//noinspection DataFlowIssue
					proc.setActionName(p3.getClass().getName());
					p3.decode(ByteBuffer.Wrap(arg.getProtocolData()));
					p3.setSender(sender);
					p3.setUserState(session);
					var isRpcResponse = !p3.isRequest(); // && p3 instanceof Rpc
					if (isRpcResponse)
						proc.setActionName(proc.getActionName() + ":Response");
					if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(p3.getTypeId())
							&& outProtocol.value == null) { // redo后不再输出日志
						var roleId = session.getRoleId();
						if (roleId == null)
							roleId = -arg.getLinkSid();
						AsyncSocket.log("Recv", roleId, arg.getOnlineSetName(), p3);
					}
					outProtocol.value = p3;
					t.runWhileCommit(() -> arg.setProtocolData(Binary.Empty)); // 这个字段不再需要读了,避免ProviderUserSession引用太久,置空
					if (isRpcResponse)
						return processRpcResponse(outRpcContext, p3);
					// protocol or rpc request
					var handler = factoryHandle.Handle;
					return handler != null ? handler.handleProtocol(p3) : Procedure.NotImplement;
				}, null, factoryHandle.Level, session), outProtocol, (p4, code) -> {
					p4.setResultCode(code);
					session.sendResponse(p4);
				});
				if (PerfCounter.ENABLE_PERF) {
					PerfCounter.instance.addRecvInfo(typeId, factoryHandle.Class,
							Protocol.HEADER_SIZE + psize, System.nanoTime() - timeBegin);
				}
				return r;
			}

			p2 = factoryHandle.Factory.create();
			p2.decode(ByteBuffer.Wrap(arg.getProtocolData()));
			p2.setSender(sender);
			p2.setUserState(session);
			if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId)) {
				var roleId = session.getRoleId();
				if (roleId == null)
					roleId = -linkSid;
				AsyncSocket.log("Recv", roleId, arg.getOnlineSetName(), p2);
			}
			var isRpcResponse = !p2.isRequest(); // && p2 instanceof Rpc
			if (txn != null) { // 已经在事务中，嵌入执行。此时忽略p2的NoProcedure配置。
				//noinspection ConstantConditions
				txn.getTopProcedure().setActionName(p2.getClass().getName() + (isRpcResponse ? ":Response" : ""));
				txn.setUserState(session);
				txn.runWhileCommit(() -> arg.setProtocolData(Binary.Empty)); // 这个字段不再需要读了,避免ProviderUserSession引用太久,置空
			} else // 应用框架不支持事务或者协议配置了"不需要事务”
				arg.setProtocolData(Binary.Empty); // 这个字段不再需要读了,避免ProviderUserSession引用太久,置空
			var p3 = p2;
			var r = Task.call(() -> {
				if (isRpcResponse)
					return processRpcResponse(outRpcContext, p3);
				// protocol or rpc request
				var handler = factoryHandle.Handle;
				return handler != null ? handler.handleProtocol(p3) : Procedure.NotImplement;
			}, p3, (p4, code) -> {
				p4.setResultCode(code);
				session.sendResponse(p4);
			});
			if (PerfCounter.ENABLE_PERF) {
				PerfCounter.instance.addRecvInfo(typeId, factoryHandle.Class,
						Protocol.HEADER_SIZE + psize, System.nanoTime() - timeBegin);
			}
			return r;
		} catch (Exception ex) {
			var desc = "ProcessDispatch(" + (p2 != null ? p2.getClass().getName() : typeId) + ") exception:";
			logger.error(desc, ex);
			sendKick(sender, linkSid, BKick.ErrorProtocolException, desc + ' ' + ex);
			return Procedure.Success;
		} finally {
			localDispatch.remove();
		}
	}

	private long processRpcResponse(OutObject<Rpc<?, ?>> outRpcContext, Protocol<?> p3) throws Exception {
		var res = (Rpc<?, ?>)p3;
		// 获取context并保存下来，redo的时候继续使用。
		if (outRpcContext.value == null) {
			outRpcContext.value = providerApp.providerService.removeRpcContext(res.getSessionId());
			// 再次检查，因为context可能丢失
			if (outRpcContext.value == null) {
				logger.warn("rpc response: lost context, maybe timeout. {}", p3);
				return Procedure.Unknown;
			}
		}

		return res.setupRpcResponseContext(outRpcContext.value).setFutureResultOrCallHandle();
	}

	@Override
	protected long ProcessAnnounceLinkInfo(AnnounceLinkInfo protocol) {
		// var linkSession = (ProviderService.LinkSession)protocol.getSender().getUserState();
		return Procedure.Success;
	}
}
