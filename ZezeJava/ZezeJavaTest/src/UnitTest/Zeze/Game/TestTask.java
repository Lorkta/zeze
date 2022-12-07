package UnitTest.Zeze.Game;

import java.util.ArrayList;
import ClientGame.Login.BRole;
import ClientGame.Login.CreateRole;
import ClientGame.Login.GetRoleList;
import Zeze.Builtin.Game.Online.Login;
import Zeze.Builtin.Game.Online.Logout;
import Zeze.Builtin.Game.Online.ReLogin;
import Zeze.Builtin.Game.TaskBase.BCollectCoinEvent;
import Zeze.Builtin.Game.TaskBase.BTConditionNPCTalk;
import Zeze.Builtin.Game.TaskBase.BTConditionNPCTalkEvent;
import Zeze.Builtin.Game.TaskBase.TriggerTaskEvent;
import Zeze.Game.Task.ConditionNPCTalk;
import Zeze.Game.Task.ConditionNamedCount;
import Zeze.Game.Task.NPCTask;
import Zeze.Game.TaskBase;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action0;
import Zeze.Util.Action1;
import Zezex.Linkd.Auth;
import junit.framework.TestCase;
import org.junit.Assert;

public class TestTask extends TestCase {
	final ArrayList<ClientGame.App> clients = new ArrayList<>();
	final ArrayList<Zezex.App> links = new ArrayList<>();
	final ArrayList<Game.App> servers = new ArrayList<>();
	final static int ClientCount = 1;
	final static int LinkCount = 1;
	final static int ServerCount = 1;
	final static int RoleCount = 1;

	private void start() throws Throwable {
		for (int i = 0; i < ClientCount; ++i) {
			var client = new ClientGame.App();
			clients.add(client);
		}
		for (int i = 0; i < LinkCount; ++i)
			links.add(new Zezex.App());
		for (int i = 0; i < ServerCount; ++i)
			servers.add(new Game.App());

		for (int i = 0; i < LinkCount; ++i)
			links.get(i).Start(10000 + i, 15000 + i);
		for (int i = 0; i < ServerCount; ++i)
			servers.get(i).Start(i, 20000 + i);
		Thread.sleep(2000); // wait server ready
		for (int i = 0; i < ClientCount; ++i) {
			var link = links.get(i % LinkCount); // 按顺序选择link
			var ipport = link.LinkdService.getOnePassiveAddress();
			clients.get(i).Start(ipport.getKey(), ipport.getValue());
			// wait client connected
			clients.get(i).Connector.WaitReady();
		}
	}

	private void stop() throws Throwable {
		for (var client : clients)
			client.Stop();
		for (var server : servers)
			server.Stop();
		for (var link : links)
			link.Stop();
	}

	// ======================================== 测试用例1：NameCount的一个任务实例 - 吃金币 ========================================
	public void test1() throws Throwable {
		Zeze.Util.Task.tryInitThreadPool(null, null, null);

		try {
			start();

			System.out.println("=============== 在Client0注册Role0 ===============");
			var client0 = clients.get(0);
			auth(client0, "account0");
			var role = getRole(client0);
			var roleId = null != role ? role.getId() : createRole(client0, "role0");
			login(client0, roleId);

			var server0 = servers.get(0);

			Assert.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {

				var module = server0.taskModule;
				// ==================== 创建一个任务 ====================
				NPCTask.NPCTaskOpt taskOpt = new NPCTask.NPCTaskOpt();
				taskOpt.id = 1;
				taskOpt.name = "吃金币";
				taskOpt.description = "";
				taskOpt.SubmitNpcId = 1001;
				taskOpt.ReceiveNpcId = 1002;
				var task1 = module.newNPCTask(taskOpt);
				// ==================== 设置任务的各个Phase ====================
				/*
				 * ==>==>==>==>==>==>==>==>
				 * 		   Phase2
				 *		 /		 \
				 * Phase1		  Phase4
				 *		 \		 /
				 *		  Phase3
				 * ==>==>==>==>==>==>==>==>
				 */
				TaskPhase.TaskPhaseOpt phaseOpt1 = new TaskPhase.TaskPhaseOpt();
				phaseOpt1.id = 1;
				phaseOpt1.name = "阶段一";
				phaseOpt1.description = "";
				phaseOpt1.afterPhaseIds.add(2L);
				phaseOpt1.afterPhaseIds.add(3L);
				phaseOpt1.commitType = TaskPhase.CommitAuto;
				TaskPhase.TaskPhaseOpt phaseOpt2 = new TaskPhase.TaskPhaseOpt();
				phaseOpt2.id = 2;
				phaseOpt2.name = "阶段二";
				phaseOpt2.description = "";
				phaseOpt2.afterPhaseIds.add(4L);
				phaseOpt2.commitType = TaskPhase.CommitAuto;
				TaskPhase.TaskPhaseOpt phaseOpt3 = new TaskPhase.TaskPhaseOpt();
				phaseOpt3.id = 3;
				phaseOpt3.name = "阶段三";
				phaseOpt3.description = "";
				phaseOpt3.afterPhaseIds.add(4L);
				phaseOpt3.commitType = TaskPhase.CommitAuto;
				TaskPhase.TaskPhaseOpt phaseOpt4 = new TaskPhase.TaskPhaseOpt();
				phaseOpt4.id = 4;
				phaseOpt4.name = "阶段四";
				phaseOpt4.description = "";
				phaseOpt4.afterPhaseIds.clear();
				phaseOpt4.commitType = TaskPhase.CommitNPCTalk;
				phaseOpt4.commitNPCId = 1002;
				var phase1 = task1.addPhase(phaseOpt1);
				var phase2 = task1.addPhase(phaseOpt2);
				var phase3 = task1.addPhase(phaseOpt3);
				var phase4 = task1.addPhase(phaseOpt4);
				// ==================== 设置任务Phase的各个条件 ====================
				ConditionNPCTalk dialog1 = phase1.addCondition(new ConditionNPCTalk(phase1));
				dialog1.setOnComplete(new Action1<TaskConditionBase<BTConditionNPCTalk, BTConditionNPCTalkEvent>>() {
					@Override
					public void run(TaskConditionBase<BTConditionNPCTalk, BTConditionNPCTalkEvent> condition) throws Throwable {
						var phase = condition.getPhase();
						var extendedBean = condition.getExtendedBean();
						var dialogSelected = extendedBean.getDialogSelected();
						if (dialogSelected.get(1) == 1) // 如果在第一个对话中选了1选项，则影响任务路线，推进到第2个Phase
							phase.setNextPhaseId(2L);
						else if (dialogSelected.get(1) == 2) // 如果在第一个对话中选了2选项，则影响任务路线，推进到第3个Phase
							phase.setNextPhaseId(3L);
					}
				});
//				ConditionNamedCount goldCondition10 = new ConditionNamedCount("收集金币", 0, 10);
//				ConditionNamedCount goldCondition20 = new ConditionNamedCount("收集金币", 0, 20);
//				ConditionNamedCount goldCondition30 = new ConditionNamedCount("收集金币", 0, 30);
//				ConditionNamedCount goldCondition40 = new ConditionNamedCount("收集金币", 0, 40);
//				phase1.addCondition(goldCondition10);
//				phase2.addCondition(goldCondition20);
//				phase3.addCondition(goldCondition30);
//				phase4.addCondition(goldCondition40);
//				task1.linkPhase(phase1, phase2);
//				task1.linkPhase(phase2, phase3);
//				task1.linkPhase(phase2, phase4);
//				task1.linkPhase(phase3, phase4);
//				task1.setupTask();
				// 测试一：金币收集任务（ConditionNamedCount）
				collectCoin(client0, roleId, task1, 9); // 已经收集9个金币，任务Phase未完成
				collectCoin(client0, roleId, task1, 11); // 已经收集11个金币，任务Phase完成，推动任务前进
				collectCoin(client0, roleId, task1, 21); // 已经收集21个金币，任务Phase完成，推动任务前进
				collectCoin(client0, roleId, task1, 31); // 已经收集31个金币，任务Phase完成，任务全部完成

				return Procedure.Success;
			}, "testTask01 - GetGold").call());
			Thread.sleep(2000);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			stop();
		}
	}

	// 一个简单的任务，用于测试。
	private static void collectCoin(ClientGame.App app, long roleId, TaskBase<?> task, long count) {
		TriggerTaskEvent taskEvent = new TriggerTaskEvent();
		taskEvent.Argument.setRoleId(roleId);
		var bean = new BCollectCoinEvent("收集金币", count);
		taskEvent.Argument.getExtendedData().setBean(bean);
		taskEvent.SendForWait(app.ClientService.GetSocket()).await();
		Assert.assertEquals(0, taskEvent.getResultCode());
	}

	// ======================================== 测试用例1：对话任务的一个任务实例 - NPC对话 ========================================

	// 全局角色登录状态函数

	private static void relogin(ClientGame.App app, long roleId) {
		var relogin = new ReLogin();
		relogin.Argument.setRoleId(roleId);
		relogin.SendForWait(app.ClientService.GetSocket()).await();
		Assert.assertEquals(0, relogin.getResultCode());
	}

	private static void logout(ClientGame.App app, long roleIdForLogOnly) {
		var logout = new Logout();
		logout.SendForWait(app.ClientService.GetSocket()).await();
		Assert.assertEquals(0, logout.getResultCode());
	}

	private static void login(ClientGame.App app, long roleId) {
		var login = new Login();
		login.Argument.setRoleId(roleId);
		login.SendForWait(app.ClientService.GetSocket()).await();
		Assert.assertEquals(0, login.getResultCode());
	}

	private static void auth(ClientGame.App app, String account) {
		var auth = new Auth();
		auth.Argument.setAccount(account);
		auth.SendForWait(app.ClientService.GetSocket()).await();
		Assert.assertEquals(0, auth.getResultCode());
	}

	private static long createRole(ClientGame.App app, String role) {
		var createRole = new CreateRole();
		createRole.Argument.setName(role);
		createRole.SendForWait(app.ClientService.GetSocket()).await();
		Assert.assertEquals(0, createRole.getResultCode());
		return createRole.Result.getId();
	}

	private static BRole getRole(ClientGame.App app) {
		var get = new GetRoleList();
		get.SendForWait(app.ClientService.GetSocket()).await();
		Assert.assertEquals(0, get.getResultCode());
		if (get.Result.getRoleList().isEmpty())
			return null;
		return get.Result.getRoleList().get(0);
	}

}
