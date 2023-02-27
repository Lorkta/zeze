package Zeze.Transaction;

import Zeze.Application;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Services.GlobalCacheManager.ReLogin;
import Zeze.Net.Service;
import Zeze.Util.Reflect;
import Zeze.Util.Task;

public final class GlobalClient extends Service {
	public GlobalClient(GlobalAgent agent, Application zeze) {
		super(agent.getZeze().getProjectName() + ".GlobalClient", zeze);
	}

	@Override
	public void OnHandshakeDone(AsyncSocket so) {
		// HandshakeDone 在 login|reLogin 完成以后才设置。

		var agent = (GlobalAgent.Agent)so.getUserState();
		if (agent.getLoginTimes().get() > 0) {
			var reLogin = new ReLogin();
			reLogin.Argument.serverId = getZeze().getConfig().getServerId();
			reLogin.Argument.globalCacheManagerHashIndex = agent.getGlobalCacheManagerHashIndex();
			logger.debug("GlobalClient Send ReLogin: {}", reLogin.Argument);
			reLogin.Send(so, (ThisRpc) -> {
				logger.debug("GlobalClient Recv Login. isTimeout={}, resultCode={}",
						reLogin.isTimeout(), reLogin.getResultCode());
				if (reLogin.isTimeout()) {
					so.close();
				} else if (reLogin.getResultCode() != 0) {
					// 清理本地已经分配的记录锁。
					// 1. 关闭网络。下面两行有点重复，就这样了。
					so.close(new Exception("GlobalAgent.ReLogin Fail code=" + reLogin.getResultCode()));
					//noinspection DataFlowIssue
					so.getConnector().stop();
					// 2. 开始清理，由守护线程保护，必须成功。
					agent.startRelease(getZeze(), () -> {
						// 3. 重置登录次数，下一次连接成功，会发送Login。
						agent.getLoginTimes().getAndSet(0);
						// 4. 开始网络连接。
						so.getConnector().start();
					});
				} else {
					agent.getLoginTimes().getAndIncrement();
					agent.setActiveTime(System.currentTimeMillis());
					super.OnHandshakeDone(so);
				}
				return 0;
			});
		} else {
			var login = new Login();
			login.Argument.serverId = getZeze().getConfig().getServerId();
			login.Argument.globalCacheManagerHashIndex = agent.getGlobalCacheManagerHashIndex();
			login.Argument.debugMode = Reflect.inDebugMode;
			logger.debug("GlobalClient Send Login: {}", login.Argument);
			login.Send(so, (ThisRpc) -> {
				logger.debug("GlobalClient Recv Login. isTimeout={}, resultCode={}",
						login.isTimeout(), login.getResultCode());
				if (login.isTimeout() || login.getResultCode() != 0) {
					so.close();
				} else {
					agent.setActiveTime(System.currentTimeMillis());
					agent.getLoginTimes().getAndIncrement();
					agent.initialize(login.Result.maxNetPing, login.Result.serverProcessTime, login.Result.serverReleaseTimeout);
					super.OnHandshakeDone(so);
				}
				return 0;
			});
		}
	}

	@Override
	public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) {
		var p = decodeProtocol(typeId, bb, factoryHandle, so);
		// Reduce 很重要。必须得到执行，不能使用默认线程池(Task.Run),防止饥饿。
		Task.getCriticalThreadPool().execute(() -> Zeze.Util.Task.call(() -> p.handle(this, factoryHandle), p));
	}
}
