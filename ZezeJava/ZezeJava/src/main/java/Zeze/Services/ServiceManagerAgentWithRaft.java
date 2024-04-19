package Zeze.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import Zeze.Builtin.ServiceManagerWithRaft.AllocateId;
import Zeze.Builtin.ServiceManagerWithRaft.KeepAlive;
import Zeze.Builtin.ServiceManagerWithRaft.Login;
import Zeze.Builtin.ServiceManagerWithRaft.NormalClose;
import Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify;
import Zeze.Builtin.ServiceManagerWithRaft.OfflineRegister;
import Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad;
import Zeze.Builtin.ServiceManagerWithRaft.Subscribe;
import Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe;
import Zeze.Builtin.ServiceManagerWithRaft.Edit;
import Zeze.Component.Threading;
import Zeze.Config;
import Zeze.Raft.Agent;
import Zeze.Raft.RaftConfig;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Services.ServiceManager.BEdit;
import Zeze.Services.ServiceManager.BOfflineNotify;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeArgument;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Services.ServiceManager.BUnSubscribeArgument;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action1;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class ServiceManagerAgentWithRaft extends AbstractServiceManagerAgentWithRaft {
	static final Logger logger = LogManager.getLogger(ServiceManagerAgentWithRaft.class);
	private final Agent raftClient;

	@Override
	public Threading getThreading() {
		throw new UnsupportedOperationException();
	}

	public ServiceManagerAgentWithRaft(Config config) throws Exception {
		super.config = config;

		var raftConf = RaftConfig.load(config.getServiceManagerConf().getRaftXml());
		raftClient = new Agent("servicemanager.raft", raftConf, config);
		raftClient.setOnSetLeader(this::raftOnSetLeader);
		raftClient.dispatchProtocolToInternalThreadPool = true;
		RegisterProtocols(raftClient.getClient());
	}

	private void raftOnSetLeader(Agent agent) {
		var client = agent.getClient();
		if (client == null)
			return;
		var zeze = client.getZeze();
		if (zeze == null)
			return;
		var config = zeze.getConfig();

		var future = startNewLogin();
		var login = new Login();
		login.Argument.setSessionName(config.getServiceManagerConf().getSessionName());

		agent.send(login, p -> {
			var rpc = (Login)p;
			if (rpc.isTimeout())
				raftOnSetLeader(agent);
			else if (rpc.getResultCode() != 0) {
				logger.error("Login Timeout Or ResultCode != 0. Code={}", rpc.getResultCode());
			} else {
				future.setResult(true);
			}
			return 0;
		});
	}

	////////////////////////////////////////////////////////////////////////
	@Override
	protected long ProcessKeepAliveRequest(KeepAlive r) throws Exception {
		if (onKeepAlive != null)
			Task.getCriticalThreadPool().execute(onKeepAlive);
		r.SendResult();
		return Procedure.Success;
	}

	@Override
	protected long ProcessOfflineNotifyRequest(OfflineNotify r) throws Exception {
		try {
			if (triggerOfflineNotify(r.Argument)) {
				r.SendResult();
				return 0;
			}
			r.trySendResultCode(2);
		} catch (Throwable ignored) { // ignored
			r.trySendResultCode(3);
		}
		return 0;
	}

	@Override
	protected long ProcessEditRequest(Edit r) {

		for (var it = r.Argument.remove.iterator(); it.hasNext(); /**/) {
			var unReg = it.next();
			var state = subscribeStates.get(unReg.getServiceName());
			if (null == state || !state.onUnRegister(unReg))
				it.remove();
		}

		for (var reg : r.Argument.put) {
			var state = subscribeStates.get(reg.getServiceName());
			if (null == state)
				continue; // 忽略本地没有订阅的。最好加个日志。
			state.onRegister(reg);
		}

		for (var it = r.Argument.update.iterator(); it.hasNext(); /**/ ) {
			var upd = it.next();
			var state = subscribeStates.get(upd.getServiceName());
			if (null == state || !state.onUpdate(upd))
				it.remove();
		}

		r.SendResult();
		triggerOnChanged(r.Argument);

		return 0;
	}

	@Override
	protected long ProcessSetServerLoadRequest(SetServerLoad r) throws Exception {
		loads.put(r.Argument.getName(), r.Argument);
		if (onSetServerLoad != null) {
			Task.getCriticalThreadPool().execute(() -> {
				try {
					onSetServerLoad.run(r.Argument);
				} catch (Throwable e) { // logger.error
					logger.error("", e);
				}
			});
		}
		r.SendResult();
		return 0;
	}

	@Override
	protected void allocate(AutoKey autoKey, int pool) {
		if (pool < 1)
			throw new IllegalArgumentException();
		var r = new AllocateId();
		r.Argument.setName(autoKey.getName());
		r.Argument.setCount(pool);
		raftClient.sendForWait(r).await();
		if (r.getResultCode() == 0) // setCurrentAndCount is in super.
			setCurrentAndCount(autoKey, r.Result.getStartId(), r.Result.getCount());
	}

	private volatile TaskCompletionSource<Boolean> loginFuture = new TaskCompletionSource<>();

	private void waitLoginReady() {
		var volatileTmp = loginFuture;
		if (volatileTmp.isDone()) {
			try {
				if (volatileTmp.get())
					return;
			} catch (InterruptedException | ExecutionException e) {
				Task.forceThrow(e);
			}
			throw new IllegalStateException("login fail.");
		}
		if (!volatileTmp.await(super.config.getServiceManagerConf().getLoginTimeout()))
			throw new IllegalStateException("login timeout.");
		// 再次查看结果。
		try {
			if (volatileTmp.isDone() && volatileTmp.get())
				return;
		} catch (InterruptedException | ExecutionException e) {
			Task.forceThrow(e);
		}
		// 只等待一次，不成功则失败。
		throw new IllegalStateException("login timeout.");
	}

	private TaskCompletionSource<Boolean> startNewLogin() {
		lock();
		try {
			loginFuture.cancel(true); // 如果旧的Future上面有人在等，让他们失败。
			return loginFuture = new TaskCompletionSource<>();
		} finally {
			unlock();
		}
	}

	@Override
	public void editService(BEdit arg) {
		for (var info : arg.put)
			verify(info.getServiceIdentity());
		waitLoginReady();

		var edit = new Edit(arg);
		raftClient.sendForWait(edit).await();
		logger.debug("EditService {}", arg);
	}

	@Deprecated
	@Override
	public BServiceInfo registerService(BServiceInfo info) {
		var edit = new BEdit();
		edit.put.add(info);
		editService(edit);
		return info;
	}

	@Deprecated
	@Override
	public BServiceInfo updateService(BServiceInfo info) {
		var edit = new BEdit();
		edit.update.add(info);
		editService(edit);
		return info;
	}

	@Deprecated
	@Override
	public void unRegisterService(BServiceInfo info) {
		var edit = new BEdit();
		edit.remove.add(info);
		editService(edit);
	}

	@Override
	public SubscribeState subscribeService(BSubscribeInfo info) {
		waitLoginReady();

		var infos = new BSubscribeArgument();
		infos.subs.add(info);
		var states = subscribeServices(infos);
		logger.debug("SubscribeService {}", info);
		return states.get(0);
	}

	@Override
	public void subscribeServicesAsync(BSubscribeArgument arg, @Nullable Action1<List<SubscribeState>> action) {
		waitLoginReady();
		logger.debug("subscribeServicesAsync {}", arg);
		var r = new Subscribe(arg);
		raftClient.send(r, __ -> {
			var states = new ArrayList<SubscribeState>();
			for (var info : r.Argument.subs) {
				var state = subscribeStates.computeIfAbsent(info.getServiceName(), (key) -> new SubscribeState(info));
				states.add(state);
				var result = r.Result.map.get(info.getServiceName());
				if (null != result)
					state.onFirstCommit(result);
			}
			if (null != action) {
				try {
					action.run(states);
				} catch (Exception ex) {
					logger.warn("", ex);
				}
			}
			return 0;
		});
	}

	@Override
	public void unSubscribeService(BUnSubscribeArgument arg) {
		waitLoginReady();
		logger.debug("UnSubscribeService {}", arg);
		var r = new UnSubscribe(arg);
		raftClient.sendForWait(r).await();
		for (var serviceName : arg.serviceNames)
			subscribeStates.remove(serviceName);
	}

	@Override
	public boolean setServerLoad(BServerLoad load) {
		raftClient.send(new SetServerLoad(load), p -> 0);
		return true;
	}

	@Override
	public void offlineRegister(BOfflineNotify argument, Action1<BOfflineNotify> handle) {
		waitLoginReady();
		onOfflineNotifies.putIfAbsent(argument.notifyId, handle);
		raftClient.sendForWait(new OfflineRegister(argument)).await();
	}

	@Override
	public void close() {
		try {
			var tmp = loginFuture;
			if (null != tmp) {
				tmp.cancel(true);
			}
			raftClient.sendForWait(new NormalClose()).await();
			raftClient.stop();
		} catch (Throwable e) { // rethrow RuntimeException
			Task.forceThrow(e);
		}
	}

	@Override
	public void start() throws Exception {
		raftClient.getClient().start();
	}

	@Override
	public void waitReady() {
		waitLoginReady();
	}
}
