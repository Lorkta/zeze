package Zeze.Arch;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Beans.Provider.AnnounceProviderInfo;
import Zeze.Beans.Provider.Bind;
import Zeze.Beans.Provider.Subscribe;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Services.ServiceManager.ServiceInfo;
import Zeze.Services.ServiceManager.ServiceInfos;
import Zeze.Util.OutObject;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProviderService extends Zeze.Services.HandshakeClient {
	private static final Logger logger = LogManager.getLogger(ProviderService.class);

	public ProviderApp ProviderApp;
	private final ConcurrentHashMap<String, Connector> Links = new ConcurrentHashMap<>();
	private volatile Connector[] LinkConnectors = new Connector[0];
	private final AtomicInteger LinkRandomIndex = new AtomicInteger();

	// 用来同步等待Provider的静态绑定完成。
	public final TaskCompletionSource<Boolean> ProviderStaticBindCompleted = new TaskCompletionSource<>();
	public final TaskCompletionSource<Boolean> ProviderDynamicSubscribeCompleted = new TaskCompletionSource<>();

	public ProviderService(String name, Zeze.Application zeze) throws Throwable {
		super(name, zeze);
	}

	/**
	 * 不使用 RemoteEndPoint 是怕有些系统返回 ipv6 有些 ipv4，造成不一致。
	 * 这里要求 linkName 在所有 provider 中都一样。
	 * 使用 Connector 配置得到名字，只要保证配置一样。
	 */
	public String GetLinkName(AsyncSocket sender) {
		return sender.getConnector().getName();
	}

	public String GetLinkName(ServiceInfo serviceInfo) {
		return serviceInfo.getPassiveIp() + ":" + serviceInfo.getPassivePort();
	}

	@Override
	public void Start() throws Throwable {
		// copy Config.Connector to Links
		getConfig().ForEachConnector((c) -> getLinks().putIfAbsent(c.getName(), c));
		super.Start();
	}

	public void Apply(ServiceInfos serviceInfos) {
		HashSet<String> current = new HashSet<>();
		for (var link : serviceInfos.getServiceInfoListSortedByIdentity()) {
			var linkName = GetLinkName(link);
			var connector = getLinks().computeIfAbsent(linkName, (key) -> {
				var outC = new OutObject<Connector>();
				if (getConfig().TryGetOrAddConnector(link.getPassiveIp(), link.getPassivePort(), true, outC)) {
					try {
						outC.Value.Start();
					} catch (Throwable e) {
						logger.error("", e);
						return null;
					}
				}
				return outC.Value;
			});
			if (connector != null)
				current.add(connector.getName());
		}
		// 删除多余的连接器。
		for (var linkName : getLinks().keySet()) {
			if (current.contains(linkName)) {
				continue;
			}
			var removed = getLinks().remove(linkName);
			if (null != removed) {
				getConfig().RemoveConnector(removed);
				removed.Stop();
			}
		}
		LinkConnectors = Links.values().toArray(new Connector[Links.size()]);
	}

	public static class LinkSession {
		private final String Name;
		private final long SessionId;
		private int LinkId; // 在和linkd连接建立完成以后，由linkd发送通告协议时保存。
		private long ProviderSessionId;

		public LinkSession(String name, long sid) {
			Name = name;
			SessionId = sid;
		}

		public final String getName() {
			return Name;
		}

		public final long getSessionId() {
			return SessionId;
		}

		public final int getLinkId() {
			return LinkId;
		}

		private void setLinkId(int value) {
			LinkId = value;
		}

		public final long getProviderSessionId() {
			return ProviderSessionId;
		}

		private void setProviderSessionId(long value) {
			ProviderSessionId = value;
		}

		public final void Setup(int linkId, long providerSessionId) {
			setLinkId(linkId);
			setProviderSessionId(providerSessionId);
		}
	}

	public ConcurrentHashMap<String, Connector> getLinks() {
		return Links;
	}

	public AsyncSocket RandomLink() {
		var volatileTmp = LinkConnectors;
		if (volatileTmp.length == 0)
			return null;

		var index = LinkRandomIndex.getAndIncrement();
		var connector = volatileTmp[Integer.remainderUnsigned(index, volatileTmp.length)];
		// 如果只选择已经连上的Link，当所有的连接都没准备好时，仍然需要GetReadySocket，
		// 所以简单处理成总是等待连接完成。
		return connector.GetReadySocket();
	}

	@Override
	public void OnHandshakeDone(AsyncSocket sender) throws Throwable {
		super.OnHandshakeDone(sender);
		var linkName = GetLinkName(sender);
		sender.setUserState(new LinkSession(linkName, sender.getSessionId()));

		var announce = new AnnounceProviderInfo();
		announce.Argument.setServiceNamePrefix(ProviderApp.ServerServiceNamePrefix);
		announce.Argument.setServiceIndentity(String.valueOf(getZeze().getConfig().getServerId()));
		announce.Argument.setProviderDirectIp(ProviderApp.DirectIp);
		announce.Argument.setProviderDirectPort(ProviderApp.DirectPort);
		announce.Send(sender);

		// static binds
		var rpc = new Bind();
		ProviderApp.StaticBinds.foreach(rpc.Argument.getModules()::put);
		rpc.Send(sender, (protocol) -> {
			ProviderStaticBindCompleted.SetResult(true);
			return 0;
		});
		var sub = new Subscribe();
		ProviderApp.DynamicModules.foreach(sub.Argument.getModules()::put);
		sub.Send(sender, (protocol) -> {
			ProviderDynamicSubscribeCompleted.SetResult(true);
			return 0;
		});
	}

	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) throws Throwable {
		// 防止Client不进入加密，直接发送用户协议。
		if (!IsHandshakeProtocol(p.getTypeId())) {
			p.getSender().VerifySecurity();
		}
		super.DispatchProtocol(p, factoryHandle);
	}

	/*
	public void ReportLoad(int online, int proposeMaxOnline, int onlineNew) {
		var report = new ReportLoad();

		report.Argument.setOnline(online);
		report.Argument.setProposeMaxOnline(proposeMaxOnline);
		report.Argument.setOnlineNew(onlineNew);

		for (var link : getLinks().values()) {
			if (link.isHandshakeDone()) {
				link.getSocket().Send(report);
			}
		}
	}
	*/
}
