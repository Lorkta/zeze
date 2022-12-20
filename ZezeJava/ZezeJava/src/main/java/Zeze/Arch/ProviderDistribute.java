package Zeze.Arch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.ConsistentHash;
import Zeze.Util.OutLong;
import Zeze.Util.Random;

/**
 * Provider负载分发算法。
 * Linkd,Provider都需要使用。这里原则上必须是抽象的。
 */
public class ProviderDistribute {
	public final Application zeze;
	public final LoadConfig loadConfig;
	private final Service providerService;
	private final AtomicInteger feedFullOneByOneIndex = new AtomicInteger();
	private final ConcurrentHashMap<String, ConsistentHash<BServiceInfo>> consistentHashes = new ConcurrentHashMap<>();

	public ProviderDistribute(Application zeze, LoadConfig loadConfig, Service providerService) {
		this.zeze = zeze;
		this.loadConfig = loadConfig;
		this.providerService = providerService;
	}

	public void addServer(Agent.SubscribeState state, BServiceInfo s) {
		consistentHashes.computeIfAbsent(s.getServiceName(), key -> new ConsistentHash<>())
				.add(s.getServiceIdentity(), s);
	}

	public void removeServer(Agent.SubscribeState state, BServiceInfo s) {
		var consistentHash = consistentHashes.get(s.getServiceName());
		if (consistentHash != null)
			consistentHash.remove(s);
	}

	public void applyServers(Agent.SubscribeState ass) {
		var consistentHash = consistentHashes.computeIfAbsent(ass.getServiceName(), key -> new ConsistentHash<>());
		var nodeSet = consistentHash.getNodes();
		var nodes = nodeSet.toArray(new BServiceInfo[nodeSet.size()]);
		var current = new HashSet<BServiceInfo>();
		for (var node : ass.getServiceInfos().getServiceInfoListSortedByIdentity()) {
			consistentHash.add(node.getServiceIdentity(), node);
			current.add(node);
		}
		for (var node : nodes) {
			if (!current.contains(node))
				consistentHash.remove(node);
		}
	}

	public static String makeServiceName(String serviceNamePrefix, int moduleId) {
		return serviceNamePrefix + moduleId;
	}

	public ConsistentHash<BServiceInfo> getConsistentHash(String name) {
		return consistentHashes.get(name);
	}

	// ChoiceDataIndex 用于RedirectAll或者那些已知数据分块索引的地方。
	public BServiceInfo choiceDataIndex(Agent.SubscribeState providers, ConsistentHash<BServiceInfo> consistentHash,
											   int dataIndex, int dataConcurrentLevel) {
		if (consistentHash == null)
			return null;
//		if (consistentHash.getNodes().size() > dataConcurrentLevel)
//			throw new IllegalStateException("ChoiceDataIndex: too many servers: "
//			+ consistentHash.getNodes().size() + " > " + dataConcurrentLevel);
		var serviceInfo = consistentHash.get(ByteBuffer.calc_hashnr(dataIndex));
		if (null != serviceInfo) {
			var providerModuleState = (ProviderModuleState)providers.localStates.get(serviceInfo.getServiceIdentity());
			if (providerModuleState == null)
				return null;
			if (providerModuleState.sessionId == 0)
				return serviceInfo; // loop back 本机，不做过载保护。
			var providerSocket = providerService.GetSocket(providerModuleState.sessionId);
			if (providerSocket == null)
				return null;
			var ps = (ProviderSession)providerSocket.getUserState();
			if (ps.load.getOverload() == BLoad.eOverload)
				return null;
		}
		return serviceInfo;
	}

	public BServiceInfo choiceHash(Agent.SubscribeState providers, int hash, int dataConcurrentLevel) {
		var serviceName = providers.getServiceName();
		var consistentHash = consistentHashes.get(serviceName);
		if (consistentHash == null)
			throw new IllegalStateException("ChoiceHash: not found ConsistentHash for serviceName=" + serviceName);
		if (dataConcurrentLevel <= 1)
			return consistentHash.get(hash);

		return choiceDataIndex(providers, consistentHash, (int)((hash & 0xffff_ffffL) % dataConcurrentLevel), dataConcurrentLevel);
	}

	public BServiceInfo choiceHash(Agent.SubscribeState providers, int hash) {
		return choiceHash(providers, hash, 1);
	}

	public boolean choiceHash(Agent.SubscribeState providers, int hash, OutLong provider) {
		provider.value = 0L;
		var serviceInfo = choiceHash(providers, hash);
		if (serviceInfo == null)
			return false;

		var providerModuleState = (ProviderModuleState)providers.localStates.get(serviceInfo.getServiceIdentity());
		if (providerModuleState == null)
			return false;

		provider.value = providerModuleState.sessionId;
		return true;
	}

	public boolean choiceLoad(Agent.SubscribeState providers, OutLong provider) {
		provider.value = 0L;

		var list = providers.getServiceInfos().getServiceInfoListSortedByIdentity();
		var frees = new ArrayList<ProviderSession>(list.size());
		var all = new ArrayList<ProviderSession>(list.size());
		int TotalWeight = 0;

		// 新的provider在后面，从后面开始搜索。后面的可能是新的provider。
		for (int i = list.size() - 1; i >= 0; --i) {
			var serviceInfo = list.get(i);
			var providerModuleState = (ProviderModuleState)providers.localStates.get(serviceInfo.getServiceIdentity());
			if (providerModuleState == null) {
				continue;
			}
			// Object tempVar2 = App.ProviderService.GetSocket(providerModuleState.getSessionId()).getUserState();
			var ps = (ProviderSession)providerService.GetSocket(providerModuleState.sessionId).getUserState();
			if (ps == null)
				continue; // 这里发现关闭的服务，仅仅忽略.

			if (ps.load.getOverload() == BLoad.eOverload)
				continue; // 忽略过载的服务器

			all.add(ps);

			if (ps.load.getOnlineNew() > loadConfig.getMaxOnlineNew())
				continue;

			int weight = ps.load.getProposeMaxOnline() - ps.load.getOnline();
			if (weight <= 0)
				continue;

			frees.add(ps);
			TotalWeight += weight;
		}
		if (TotalWeight > 0) {
			int randWeight = Random.getInstance().nextInt(TotalWeight);
			for (var ps : frees) {
				int weight = ps.load.getProposeMaxOnline() - ps.load.getOnline();
				if (randWeight < weight) {
					provider.value = ps.getSessionId();
					return true;
				}
				randWeight -= weight;
			}
		}
		// 选择失败，一般是都满载了，随机选择一个。
		if (!all.isEmpty()) {
			provider.value = all.get(Random.getInstance().nextInt(all.size())).getSessionId();
			return true;
		}
		// no providers
		return false;
	}

	// 查找时增加索引，和喂饱时增加索引，需要原子化。提高并发以后慢慢想，这里应该足够快了。
	public synchronized boolean choiceFeedFullOneByOne(Agent.SubscribeState providers, OutLong provider) {
		provider.value = 0L;

		var list = providers.getServiceInfos().getServiceInfoListSortedByIdentity();
		// 最多遍历一次。循环里面 continue 时，需要递增索引。
		for (int i = 0; i < list.size(); ++i, feedFullOneByOneIndex.incrementAndGet()) {
			var index = Integer.remainderUnsigned(feedFullOneByOneIndex.get(), list.size()); // current
			var serviceInfo = list.get(index);
			var providerModuleState = (ProviderModuleState)providers.localStates.get(serviceInfo.getServiceIdentity());
			if (providerModuleState == null)
				continue;
			var providerSocket = providerService.GetSocket(providerModuleState.sessionId);
			if (providerSocket == null)
				continue;
			var ps = (ProviderSession)providerSocket.getUserState();

			// 这里发现关闭的服务，仅仅忽略.
			if (ps == null)
				continue;

			if (ps.load.getOverload() == BLoad.eOverload)
				continue; // 忽略过载服务器。

			// 这个和一个一个喂饱冲突，但是一下子给一个服务分配太多用户，可能超载。如果不想让这个生效，把MaxOnlineNew设置的很大。
			if (ps.load.getOnlineNew() > loadConfig.getMaxOnlineNew())
				continue;

			provider.value = ps.getSessionId();
			if (ps.load.getOnline() >= ps.load.getProposeMaxOnline())
				feedFullOneByOneIndex.incrementAndGet(); // 已经喂饱了一个，下一个。
			return true;
		}
		return false;
	}

	public boolean choiceProviderByServerId(String serviceNamePrefix, int moduleId, int serverId, OutLong provider) {
		var serviceName = makeServiceName(serviceNamePrefix, moduleId);
		var providers = zeze.getServiceManager().getSubscribeStates().get(serviceName);
		if (providers != null) {
			var si = providers.getServiceInfos().findServiceInfoByServerId(serverId);
			if (si != null) {
				provider.value = ((ProviderModuleState)providers.localStates.get(si.getServiceIdentity())).sessionId;
				return true;
			}
		}
		provider.value = 0L;
		return false;
	}
}
