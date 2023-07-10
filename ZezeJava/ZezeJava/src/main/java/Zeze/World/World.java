package Zeze.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import Zeze.AppBase;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderUserSession;
import Zeze.Arch.ProviderWithOnline;
import Zeze.Builtin.Provider.Send;
import Zeze.Builtin.World.BCommand;
import Zeze.Builtin.World.BObject;
import Zeze.Builtin.World.Command;
import Zeze.Builtin.World.BObjectId;
import Zeze.Builtin.World.Query;
import Zeze.Collections.BeanFactory;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;
import Zeze.World.Aoi.MapManager;
import Zeze.World.Mmo.MoveMmo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * 【要点】
 * 1. 【要求】World模块的所有客户端协议必须经过TaskOneByOneByKey排队，以Account为key。
 *    这点由Arch框架实现。定制Arch时，需要保留这个特性。
 * 2. 【要求】每个场景地图在这里使用 int mapId 描述。要求有一张以mapId为key的索引表。
 * 3. 【要求】每个场景地图需要提供的必要数据（todo)
 * 4. 地图运行实例管理器可自定义，默认有一个。
 * 5. 地图内动态实体可以任意定义，在这里用dynamic描述数据，用脚本实现操作。
 *    dynamic支持的bean类型可以有多个，也可以使用同一个bean，自己在bean内部再抽象。
 *    World在传输dynamic给客户端时，需要客户端知道bean的具体类型。
 *    传输使用binary描述系列化的数据。
 * 6. 状态同步之移动部分World内部实现。多版本，可自定义。
 * 7. 状态同步之战斗（技能）World提供框架。多版本，可自定义。
 * 8. 地图内动态实体共享模式：（todo）
 * a) public, 地图实例内玩家全部可见，访问同一个实例。比如某些挖矿模式，挖完一定数量就消失了一段时间再重生。
 * b) protect, ？？？
 * c) private, 只有一个玩家可见。比如无法共享的任务物品拾取，当拥有这个任务时创建实体，仅由这个玩家使用。
 *
 * 【功能分级】
 * 0级，Util类。
 *    目前包含Cube,CubeIndex,CubeMap,Graphics2D,Graphics3D,LockGuard,Action2dLong。
 *    这些类不需要World组件，如果恰好符合需求，可以用来搭建自己的地图模块。
 * 1级，World框架。
 *    包含了协议，地图实例管理，等等。里面的功能大都可自定义。
 *    a) MapManager 地图运行实例管理器。
 *    b) MoveMmo 移动同步之mmo的一个实现。
 *    c) FightMmo 战斗计算及同步之mmo的一个实现。
 *    d) Scene 多人
 */
public class World extends AbstractWorld {
	private static final Logger logger = LogManager.getLogger(World.class);

	private final static BeanFactory beanFactory = new BeanFactory();
	public static long getSpecialTypeIdFromBean(Bean bean) {
		return bean.typeId();
	}
	public static long getSpecialTypeIdFromBean(Data data) {
		return data.typeId();
	}
	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}
	public static Data createDataFromSpecialTypeId(long typeId) {
		return beanFactory.createDataFromSpecialTypeId(typeId);
	}

	// framework
	public final ProviderApp providerApp;
	private final ConcurrentHashMap<Integer, ICommand> commandHandlers = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, IQuery> queryHandlers = new ConcurrentHashMap<>();
	private final HashSet<IComponent> componentHashSet = new HashSet<>();

	// world core
	private IMapManager mapManager;
	private final Function<ProviderUserSession, String> getPlayerId;


	public IMapManager getMapManager() {
		return mapManager;
	}

	public void setMapManager(IMapManager mapManager) {
		this.mapManager = mapManager;
	}

	public void registerCommand(int commandId, ICommand handler) {
		if (commandId <= BCommand.eReserveCommandId)
			throw new RuntimeException("command id is reserved");

		internalRegisterCommand(commandId, handler);
	}

	// 内部使用，或者用来黑内部的command处理。
	public void internalRegisterCommand(int commandId, ICommand handler) {
		if (null != commandHandlers.putIfAbsent(commandId, handler))
			throw new RuntimeException("duplicate command id=" + commandId);
	}

	public void registerQuery(int commandId, IQuery handler) {
		if (commandId > BCommand.eReserveCommandId)
			throw new RuntimeException("command id is reserved");

		internalRegisterQuery(commandId, handler);
	}

	// 内部使用。
	void internalRegisterQuery(int commandId, IQuery handler) {
		if (null != queryHandlers.putIfAbsent(commandId, handler))
			throw new RuntimeException("duplicate command id=" + commandId);
	}

	public void installComponent(IComponent c) throws Exception {
		if (!componentHashSet.add(c))
			throw new RuntimeException("component has installed. " + c);
		c.install(this);
	}

	@Override
	protected long ProcessCommand(Command p) throws Exception {
		var session = ProviderUserSession.get(p);

		var command = commandHandlers.get(p.Argument.getCommandId());
		if (null == command)
			return errorCode(eCommandHandlerMissing);

		return command.handle(getPlayerId.apply(session), p);
	}

	@Override
	protected long ProcessQueryRequest(Query r) throws Exception {
		var session = ProviderUserSession.get(r);

		var query = queryHandlers.get(r.Argument.getCommandId());
		if (null == query)
			return errorCode(eCommandHandlerMissing);

		return query.handle(getPlayerId.apply(session), r);
	}

	public static @NotNull World create(@NotNull AppBase app) {
		return GenModule.createRedirectModule(World.class, app);
	}

	protected World(AppBase app) {
		providerApp = app.getZeze().redirect.providerApp;
		RegisterProtocols(providerApp.providerService);
		RegisterZezeTables(providerApp.zeze);

		if (providerApp.providerImplement instanceof ProviderWithOnline)
			getPlayerId = ProviderUserSession::getAccount;
		else if (providerApp.providerImplement instanceof Zeze.Game.ProviderWithOnline)
			getPlayerId = ProviderUserSession::getContext;
		else
			getPlayerId = null;
	}

	public void start() throws Exception {
		for (var c : componentHashSet)
			c.start(this);
	}

	public void stop() {
		// 不支持动态调整。
		// 只能整个程序退出。
		// 以后需要了再说。
	}

	public void initializeDefaultMmo() throws Exception {
		setMapManager(new MapManager(this));
		installComponent(new MoveMmo(this));
	}

	public static String format(BObjectId oid) {
		return "(" + oid.getType() + "," + oid.getConfigId() + "," + oid.getInstanceId() + ")";
	}

	public static ByteBuffer encodeSend(Collection<Long> linkSids, int commandId, Data data) {
		var cmd = new Command();
		cmd.Argument.setCommandId(commandId);
		var bb = ByteBuffer.Allocate();
		data.encode(bb);
		cmd.Argument.setParam(new Binary(bb));

		var send = new Send();
		send.Argument.getLinkSids().addAll(linkSids);
		send.Argument.setProtocolType(cmd.getTypeId());
		send.Argument.setProtocolWholeData(new Binary(cmd.encode()));

		return send.encode();
	}

	public boolean sendLink(String linkName, ByteBuffer fullEncodedProtocol) {
		var link = providerApp.providerService.getLinks().get(linkName);
		if (null == link) {
			logger.info("link not found: {}", linkName);
			return false;
		}
		var socket = link.TryGetReadySocket();
		if (null == socket) {
			logger.info("link socket not ready. {}", linkName);
			return false;
		}
		return socket.Send(fullEncodedProtocol);
	}

	public boolean sendCommand(String linkName, long linkSid, int commandId, Data data) {
		return sendLink(linkName, encodeSend(java.util.List.of(linkSid), commandId, data));
	}

	public boolean sendCommand(Collection<Entity> targets, int commandId, Data data) {
		var group = new HashMap<String, ArrayList<Long>>();
		for (var target : targets) {
			var link = group.computeIfAbsent(target.getBean().getLinkName(), (key) -> new ArrayList<>());
			link.add(target.getBean().getLinkSid());
		}
		var result = true;
		for (var e : group.entrySet()) {
			result &= sendLink(e.getKey(), encodeSend(e.getValue(), commandId, data));
		}
		return result;
	}

	// todo 抽象Skill时再来决定参数。
	public void compute(Entity self, ICompute c) throws IOException {
		try (var ignored = new LockGuard(c.selector().select(self))) {
			c.compute();
		}
	}
}
