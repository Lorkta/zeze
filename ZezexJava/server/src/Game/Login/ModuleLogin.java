package Game.Login;

import Zeze.Net.Protocol;
import Zeze.Transaction.*;
import Game.*;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleLogin extends AbstractModule {
	public void Start(App app) {
		setOnlines(new Onlines(_tonline));
	}

	public void Stop(App app) {
	}

	private Onlines Onlines;
	public Onlines getOnlines() {
		return Onlines;
	}
	private void setOnlines(Onlines value) {
		Onlines = value;
	}

	@Override
	public int ProcessCreateRoleRequest(Protocol _rpc) {
		var rpc = (CreateRole)_rpc;
		var session = Session.Get(rpc);

		BRoleData tempVar = new BRoleData();
		tempVar.setName(rpc.Argument.getName());
		long roleid = _trole.Insert(tempVar);

		// duplicate name check
		BRoleId tempVar2 = new BRoleId();
		tempVar2.setId(roleid);
		if (false == _trolename.TryAdd(rpc.Argument.getName(), tempVar2)) {
			return ReturnCode(ResultCodeCreateRoleDuplicateRoleName);
		}

		var account = _taccount.GetOrAdd(session.getAccount());
		account.getRoles().add(roleid);

		// initialize role data
		App.Game_Bag.GetBag(roleid).SetCapacity(50);

		session.SendResponse(rpc);
		return Procedure.Success;
	}

	@Override
	public int ProcessGetRoleListRequest(Protocol _rpc) {
		var rpc = (GetRoleList)_rpc;
		var session = Session.Get(rpc);

		BAccount account = _taccount.Get(session.getAccount());
		if (null != account) {
			for (var roleId : account.getRoles()) {
				BRoleData roleData = _trole.Get(roleId);
				if (null != roleData) {
					BRole tempVar = new BRole();
					tempVar.setId(roleId);
					tempVar.setName(roleData.getName());
					rpc.Result.getRoleList().add(tempVar);
				}
			}
			rpc.Result.setLastLoginRoleId(account.getLastLoginRoleId());
		}

		session.SendResponse(rpc);
		return Procedure.Success;
	}

	@Override
	public int ProcessLoginRequest(Protocol _rpc) {
		var rpc = (Login)_rpc;
		var session = Session.Get(rpc);

		BAccount account = _taccount.Get(session.getAccount());
		if (null == account) {
			return ReturnCode(ResultCodeAccountNotExist);
		}

		account.setLastLoginRoleId(rpc.Argument.getRoleId());
		BRoleData role = _trole.Get(rpc.Argument.getRoleId());
		if (null == role) {
			return ReturnCode(ResultCodeRoleNotExist);
		}

		BOnline online = _tonline.GetOrAdd(rpc.Argument.getRoleId());
		online.setLinkName(session.getLinkName());
		online.setLinkSid(session.getSessionId());
		online.setState(BOnline.StateOnline);

		online.setReliableNotifyConfirmCount(0);
		online.setReliableNotifyTotalCount(0);
		online.getReliableNotifyMark().clear();
		online.getReliableNotifyQueue().clear();

		var linkSession = (Server.LinkSession)session.getLink().getUserState();
		online.setProviderId(App.Zeze.getConfig().getServerId());
		online.setProviderSessionId(linkSession.getProviderSessionId());

		// 先提交结果再设置状态。
		// see linkd::Zezex.Provider.ModuleProvider。ProcessBroadcast
		session.SendResponseWhileCommit(rpc);
		Transaction.getCurrent().RunWhileCommit(() -> {
				var setUserState = new Zezex.Provider.SetUserState();
				setUserState.Argument.setLinkSid(session.getSessionId());
				setUserState.Argument.getStates().add(rpc.Argument.getRoleId());
				rpc.getSender().Send(setUserState); // 直接使用link连接。
		});
		App.getLoad().getLoginCount().incrementAndGet();
		return Procedure.Success;
	}

	@Override
	public int ProcessReLoginRequest(Protocol _rpc) {
		var rpc = (ReLogin)_rpc;
		var session = Session.Get(rpc);

		BAccount account = _taccount.Get(session.getAccount());
		if (null == account) {
			return ReturnCode(ResultCodeAccountNotExist);
		}

		if (account.getLastLoginRoleId() != rpc.Argument.getRoleId()) {
			return ReturnCode(ResultCodeNotLastLoginRoleId);
		}

		BRoleData role = _trole.Get(rpc.Argument.getRoleId());
		if (null == role) {
			return ReturnCode(ResultCodeRoleNotExist);
		}

		BOnline online = _tonline.Get(rpc.Argument.getRoleId());
		if (null == online) {
			return ReturnCode(ResultCodeOnlineDataNotFound);
		}

		online.setLinkName(session.getLinkName());
		online.setLinkSid(session.getSessionId());
		online.setState(BOnline.StateOnline);

		// 先发结果，再发送同步数据（ReliableNotifySync）。
		// 都使用 WhileCommit，如果成功，按提交的顺序发送，失败全部不会发送。
		session.SendResponseWhileCommit(rpc);
		Transaction.getCurrent().RunWhileCommit(() -> {
				var setUserState = new Zezex.Provider.SetUserState();
				setUserState.Argument.setLinkSid(session.getSessionId());
				setUserState.Argument.getStates().add(rpc.Argument.getRoleId());
				rpc.getSender().Send(setUserState); // 直接使用link连接。
		});

		var syncResultCode = ReliableNotifySync(session, rpc.Argument.getReliableNotifyConfirmCount(), online);

		if (syncResultCode != ResultCodeSuccess) {
			return ReturnCode(syncResultCode);
		}

		App.getLoad().getLoginCount().incrementAndGet();
		return Procedure.Success;
	}


	private int ReliableNotifySync(Session session, long ReliableNotifyConfirmCount, BOnline online) {
		return ReliableNotifySync(session, ReliableNotifyConfirmCount, online, true);
	}

	private int ReliableNotifySync(Session session, long ReliableNotifyConfirmCount, BOnline online, boolean sync) {
		if (ReliableNotifyConfirmCount < online.getReliableNotifyConfirmCount()
				|| ReliableNotifyConfirmCount > online.getReliableNotifyTotalCount()
				|| ReliableNotifyConfirmCount - online.getReliableNotifyConfirmCount() > online.getReliableNotifyQueue().size()) {
			return ResultCodeReliableNotifyConfirmCountOutOfRange;
		}

		int confirmCount = (int)(ReliableNotifyConfirmCount - online.getReliableNotifyConfirmCount());

		if (sync) {
			var notify = new SReliableNotify();
			notify.Argument.setReliableNotifyTotalCountStart(ReliableNotifyConfirmCount);
			for (int i = confirmCount; i < online.getReliableNotifyQueue().size(); ++i) {
				notify.Argument.getNotifies().add(online.getReliableNotifyQueue().get(i));
			}
			session.SendResponseWhileCommit(notify);
		}
		for (int ir = 0; ir < confirmCount; ++ir)
			online.getReliableNotifyQueue().remove(0);
		//online.getReliableNotifyQueue().RemoveRange(0, confirmCount);
		online.setReliableNotifyConfirmCount(ReliableNotifyConfirmCount);
		return ResultCodeSuccess;
	}

	@Override
	public int ProcessReliableNotifyConfirmRequest(Protocol _rpc) {
		var rpc = (ReliableNotifyConfirm)_rpc;
		var session = Session.Get(rpc);

		BOnline online = _tonline.Get(session.getRoleId().longValue());
		if (null == online || online.getState() == BOnline.StateOffline) {
			return ReturnCode(ResultCodeOnlineDataNotFound);
		}

		session.SendResponseWhileCommit(rpc); // 同步前提交。
		var syncResultCode = ReliableNotifySync(session, rpc.Argument.getReliableNotifyConfirmCount(), online, false);

		if (ResultCodeSuccess != syncResultCode) {
			return ReturnCode(syncResultCode);
		}

		return Procedure.Success;
	}

	@Override
	public int ProcessLogoutRequest(Protocol _rpc) {
		var rpc = (Logout)_rpc;
		var session = Session.Get(rpc);

		if (session.getRoleId() == null) {
			return ReturnCode(ResultCodeNotLogin);
		}

		_tonline.Remove(session.getRoleId().longValue());

		// 先设置状态，再发送Logout结果。
		Transaction.getCurrent().RunWhileCommit(() -> {
				var setUserState = new Zezex.Provider.SetUserState();
				setUserState.Argument.setLinkSid(session.getSessionId());
				rpc.getSender().Send(setUserState); // 直接使用link连接。
		});
		session.SendResponseWhileCommit(rpc);
		// 在 OnLinkBroken 时处理。可以同时处理网络异常的情况。
		// App.Load.LogoutCount.IncrementAndGet();
		return Procedure.Success;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public static final int ModuleId = 1;

    private taccount _taccount = new taccount();
    private tonline _tonline = new tonline();
    private trole _trole = new trole();
    private trolename _trolename = new trolename();

    public Game.App App;

    public ModuleLogin(Game.App app) {
        App = app;
        // register protocol factory and handles
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Game.Login.CreateRole();
            factoryHandle.Handle = (_p) -> ProcessCreateRoleRequest(_p);
            App.Server.AddFactoryHandle(108094, factoryHandle);
         }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Game.Login.GetRoleList();
            factoryHandle.Handle = (_p) -> ProcessGetRoleListRequest(_p);
            App.Server.AddFactoryHandle(91931, factoryHandle);
         }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Game.Login.Login();
            factoryHandle.Handle = (_p) -> ProcessLoginRequest(_p);
            App.Server.AddFactoryHandle(83324, factoryHandle);
         }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Game.Login.Logout();
            factoryHandle.Handle = (_p) -> ProcessLogoutRequest(_p);
            App.Server.AddFactoryHandle(113969, factoryHandle);
         }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Game.Login.ReliableNotifyConfirm();
            factoryHandle.Handle = (_p) -> ProcessReliableNotifyConfirmRequest(_p);
            App.Server.AddFactoryHandle(80485, factoryHandle);
         }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Game.Login.ReLogin();
            factoryHandle.Handle = (_p) -> ProcessReLoginRequest(_p);
            App.Server.AddFactoryHandle(108643, factoryHandle);
         }
        // register table
        App.Zeze.AddTable(App.Zeze.getConfig().GetTableConf(_taccount.getName()).getDatabaseName(), _taccount);
        App.Zeze.AddTable(App.Zeze.getConfig().GetTableConf(_tonline.getName()).getDatabaseName(), _tonline);
        App.Zeze.AddTable(App.Zeze.getConfig().GetTableConf(_trole.getName()).getDatabaseName(), _trole);
        App.Zeze.AddTable(App.Zeze.getConfig().GetTableConf(_trolename.getName()).getDatabaseName(), _trolename);
    }

    public void UnRegister() {
        App.Server.getFactorys().remove(108094);
        App.Server.getFactorys().remove(91931);
        App.Server.getFactorys().remove(83324);
        App.Server.getFactorys().remove(113969);
        App.Server.getFactorys().remove(80485);
        App.Server.getFactorys().remove(108643);
        App.Zeze.RemoveTable(App.Zeze.getConfig().GetTableConf(_taccount.getName()).getDatabaseName(), _taccount);
        App.Zeze.RemoveTable(App.Zeze.getConfig().GetTableConf(_tonline.getName()).getDatabaseName(), _tonline);
        App.Zeze.RemoveTable(App.Zeze.getConfig().GetTableConf(_trole.getName()).getDatabaseName(), _trole);
        App.Zeze.RemoveTable(App.Zeze.getConfig().GetTableConf(_trolename.getName()).getDatabaseName(), _trolename);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
