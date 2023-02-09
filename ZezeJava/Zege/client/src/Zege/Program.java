package Zege;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import Zege.Friend.*;
import Zege.Message.*;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Cert;
import Zeze.Util.Counters;

public class Program {
	public static final Counters counters = new Counters();

	public static final Program Instance = new Program();
	private Benchmark benchmark;

	private static String getComputerName() throws UnknownHostException {
		InetAddress addr;
		addr = InetAddress.getLocalHost();
		return addr.getHostName();
	}

	public synchronized static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-perf":
				Counters.enable = true;
				break;
			}
		}

		Instance.run(args);
	}

	private final ArrayList<Window> Windows = new ArrayList<>();
	private MainWindow Main;
	public String Self;

	public void run(String[] args) throws Exception {
		Zeze.Collections.LinkedMap.beanFactory.register(BFriend.class);
		Zeze.Collections.LinkedMap.beanFactory.register(BGroupMember.class);
		Zeze.Collections.LinkedMap.beanFactory.register(BDepartmentMember.class);

		var app = App.Instance;
		var linkIp = "127.0.0.1";
		var linkPort = 5100;
		var links = new ArrayList<String[]>();
		var clientId = "PC";
		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-ip":
				linkIp = args[++i];
				break;
			case "-port":
				linkPort = Integer.parseInt(args[++i]);
				break;
			case "-app":
				clientId = args[++i];
				break;
			default:
				links.add(args[i].split(":"));
				break;
			}
		}
		var account = getComputerName().toLowerCase(Locale.ROOT);
		if (links.isEmpty()) {
			app.Start(linkIp, linkPort);
		} else {
			var address = links.get(Zeze.Util.Random.getInstance().nextInt(links.size()));
			app.Start(address[0], Integer.parseInt(address[1]));
		}
		if (Counters.enable)
			counters.start();
		try {
			app.Connector.WaitReady();
			app.Zege_Linkd.create(account);
			app.Zege_Linkd.waitAuthed();
			app.Zeze_Builtin_Online.login(clientId).await();
			Main = new MainWindow();
			Windows.add(Main);
			Self = account;
			refresh();
			var console = new BufferedReader(new InputStreamReader(System.in));
			String line;
			do {
				line = console.readLine();
				try {
					if (!Windows.get(Windows.size() - 1).process(line))
						System.out.println("Unknown Command!");
				} catch (Exception e) {
					e.printStackTrace();
				}
			} while (!line.equals("exit"));
		} finally {
			if (null != benchmark) {
				benchmark.stopAndJoin();
				benchmark = null;
			}
			app.Stop();
		}
	}

	public class Window {
		public String Name;

		public static long tryParseLong(String value) {
			try {
				return Long.parseLong(value);
			} catch (Exception ex) {
				return -1L;
			}
		}
		public void back() throws Exception {
			Windows.remove(Windows.size() - 1);
			Program.this.refresh();
		}

		public boolean process(String line) throws Exception {
			if (line.isEmpty()) {
				Program.this.refresh();
				return true;
			}

			var cmd = line.split(" ");
			switch (cmd[0])
			{
			case "b": case "back":
				if (cmd.length > 1) {
					var find = Windows.indexOf(cmd[1]);
					if (find >= 0) {
						for (int i = find + 1; i < Windows.size(); ++i)
							Windows.remove(i);
						Program.this.refresh();
						return true;
					}
				} else if (Windows.size() > 1) {
					back();
					return true;
				}
				break;
			case "benchmark":
				if (benchmark == null) {
					benchmark = new Benchmark();
					benchmark.start();
				}
				return true;
			case "benchmarkstop":
				if (null != benchmark) {
					benchmark.stopAndJoin();
					benchmark = null;
				}
				return true;
			case "af":
				App.Instance.Zege_Friend.addFriend(cmd[1]).await();
				Program.this.refresh();
				return true;
			case "exit":
				return true; // 外面主循环会退出，这里认为已经处理即可。
			}
			return false;
		}

		public void refresh() throws Exception {
		}

		public boolean processNotifyMessage(BMessage notify) throws Exception {
			return false;
		}
	}

	public void OnMessage(NotifyMessage r) throws Exception {
		if (r.Argument.getGroup().isEmpty())
			Program.counters.increment("RecvFriendMessage");
		else
			Program.counters.increment("RecvGroupMessage:" + r.Argument.getGroup() + "#" + r.Argument.getDepartmentId());

		// benchmark 消息，不显示。
		if (r.Argument.getSecureMessage().size() > 0) {
			var current = Windows.get(Windows.size() - 1);
			if (current.processNotifyMessage(r.Argument))
				return;
			Main.processNotifyMessage(r.Argument);
		}
	}

	public class DepartmentWindow extends Window {
		public final String Group;
		public final String DepartmentName;
		public final long DepartmentId;
		public BDepartmentNode Department;
		public long MemberNodeId;
		public BGetDepartmentMemberNode MemberNode;

		public DepartmentWindow(String group, long id, String name) {
			this.Group = group;
			this.DepartmentId = id;
			this.DepartmentName = name;
			Name = name + "(" + DepartmentId + ")";
		}

		@Override
		public boolean process(String line) throws Exception {
			// 聊天消息先处理基本命令。
			if (super.process(line))
				return true;

			var cmd = line.split(" ");
			if (cmd.length == 0)
				return true;

			switch (cmd[0]) {
			case "add":
				App.Instance.Zege_Friend.addDepartmentMember(Group, DepartmentId, cmd[1]);
				break;
			case "create":
				var newId = App.Instance.Zege_Friend.createDepartment(Group, DepartmentId, cmd[1]);
				addWindow(new DepartmentWindow(Group, newId.getDepartmentId(), cmd[1]));
				Program.this.refresh();
				break;
			case "delete":
				App.Instance.Zege_Friend.deleteDepartment(Group, DepartmentId);
				back();
				break;
			case "move":
				App.Instance.Zege_Friend.moveDepartment(Group, DepartmentId, Long.parseLong(cmd[1]));
				System.out.println("Move Success. Window Not Change Because This Is A Simple Program.");
				break;
			case "open":
				var id = tryParseLong(cmd[1]);
				if (id > 0) {
					addWindow(new DepartmentWindow(Group, id, findChildName(id)));
					return true;
				}
				var child = findChildId(cmd[1]);
				if (null != child) {
					addWindow(new DepartmentWindow(Group, child, cmd[1]));
					return true;
				}
				for (var m : MemberNode.getNode().getValues()) {
					if (m.getId().equals(cmd[1])) {
						addWindow(new ChatWindow(m.getId()));
						return true;
					}
				}
				System.out.println("open '" + cmd[1] + "' not found");
				return true;
			}
			if (!line.isEmpty()) // super处理了空行，这里本来不需要了，多判断一下吧。
				App.Instance.Zege_Message.send(Group, line, DepartmentId).await();
			return true;
		}

		public String findChildName(long id) {
			for (var child : Department.getChilds()) {
				if (child.getValue() == id)
					return child.getKey();
			}
			throw new RuntimeException("child not found with id=" + id);
		}

		public Long findChildId(String name) {
			return Department.getChilds().get(name);
		}

		@Override
		public boolean processNotifyMessage(BMessage notify) {
			if (notify.getGroup().equals(Group) && notify.getDepartmentId() == DepartmentId) {
				if (notify.getSecureKeyIndex() == -1) {
					System.out.println(notify.getFrom() + ": This Is A Encrypted Message.");
				} else {
					var bb = ByteBuffer.Wrap(notify.getSecureMessage());
					var bMsg = new BTextMessage();
					bMsg.decode(bb);
					System.out.println(notify.getFrom() + ": " + bMsg.getMessage());
				}
				return true;
			}
			return false;
		}

		@Override
		public void refresh() {
			Department = App.Instance.Zege_Friend.getDepartmentNode(Group, DepartmentId);
			for (var child : Department.getChilds()) {
				System.out.println("[" + child.getKey() + "(" + child.getValue() + ")]");
			}
			MemberNode = App.Instance.Zege_Friend.getDepartmentMemberNode(Group, DepartmentId, MemberNodeId);
			for (var member : MemberNode.getNode().getValues()) {
				System.out.println(member.getId());
			}

			var list = Main.ReceivedMessages.remove(new MessageTarget(Group, DepartmentId));
			if (null != list) {
				for (var notify : list)
					processNotifyMessage(notify);
			}
		}
	}

	public class GroupWindow extends Window {
		public final String Group;
		public BGroup Root;
		public long MemberNodeId;
		public BGetGroupMemberNode MemberNode;

		public GroupWindow(String group) {
			this.Group = group;
			Name = Group;
		}

		@Override
		public boolean process(String line) throws Exception {
			// 聊天消息先处理基本命令。
			if (super.process(line))
				return true;

			var cmd = line.split(" ");
			if (cmd.length == 0)
				return true;

			switch (cmd[0]) {
			case "add":
				App.Instance.Zege_Friend.addDepartmentMember(Group, 0, cmd[1]);
				break;
			case "create":
				var newId = App.Instance.Zege_Friend.createDepartment(Group, 0, cmd[1]);
				addWindow(new DepartmentWindow(Group, newId.getDepartmentId(), cmd[1]));
				Program.this.refresh();
				break;
			case "open":
				var id = tryParseLong(cmd[1]);
				if (id > 0) {
					addWindow(new DepartmentWindow(Group, id, findChildName(id)));
					return true;
				}

				var child = findChildId(cmd[1]);
				if (null != child) {
					addWindow(new DepartmentWindow(Group, child, cmd[1]));
					return true;
				}

				for (var m : MemberNode.getNode().getValues()) {
					if (m.getId().equals(cmd[1])) {
						addWindow(new ChatWindow(m.getId()));
						return true;
					}
				}
				System.out.println("open '" + cmd[1] + "' not found");
				return true;
			}
			if (!line.isEmpty()) // super处理了空行，这里本来不需要了，多判断一下吧。
				App.Instance.Zege_Message.send(Group, line, 0).await();
			return true;
		}

		public String findChildName(long id) {
			for (var child : Root.getChilds()) {
				if (child.getValue() == id)
					return child.getKey();
			}
			throw new RuntimeException("child not found with id=" + id);
		}

		public Long findChildId(String name) {
			return Root.getChilds().get(name);
		}

		@Override
		public boolean processNotifyMessage(BMessage notify) {
			if (notify.getGroup().equals(Group) && notify.getDepartmentId() == 0) {
				if (notify.getSecureKeyIndex() == -1) {
					System.out.println(notify.getFrom() + ": This Is A Encrypted Message.");
				} else {
					var bb = ByteBuffer.Wrap(notify.getSecureMessage());
					var bMsg = new BTextMessage();
					bMsg.decode(bb);
					System.out.println(bMsg.getMessage());
				}
				return true;
			}
			return false;
		}

		@Override
		public void refresh() {
			Root = App.Instance.Zege_Friend.getGroupRoot(Group);
			for (var child : Root.getChilds()) {
				System.out.println("[" + child.getKey() + "(" + child.getValue() + ")]");
			}
			MemberNode = App.Instance.Zege_Friend.getGroupMemberNode(Group, MemberNodeId);
			for (var member : MemberNode.getNode().getValues()) {
				System.out.println(member.getId());
			}

			var list = Main.ReceivedMessages.remove(new MessageTarget(Group, 0));
			if (null != list) {
				for (var notify : list)
					processNotifyMessage(notify);
			}
		}
	}

	public class ChatWindow extends Window {
		public final String Target;
		public ChatWindow(String target) {
			this.Target = target;
			Name = "chat:" + target;
		}

		@Override
		public boolean process(String line) throws Exception {
			// 聊天消息先处理基本命令。
			if (super.process(line))
				return true;

			if (!line.isEmpty()) // super处理了空行，这里本来不需要了，多判断一下吧。
				App.Instance.Zege_Message.send(Target, line, 0).await();
			return true;
		}

		@Override
		public boolean processNotifyMessage(BMessage notify) throws Exception {
			if (notify.getGroup().isEmpty() && notify.getFrom().equals(Target)) {
				if (notify.getSecureKeyIndex() < 0) {
					var bb = ByteBuffer.Wrap(notify.getSecureMessage());
					var bMsg = new BTextMessage();
					bMsg.decode(bb);
					System.out.println(bMsg.getMessage());
				} else {
					var file = Instance.Self + ".pkcs12";
					var passwd = "123";
					var keyStore = Cert.loadKeyStore(new FileInputStream(file), passwd);
					var privateKey = Cert.getPrivateKey(keyStore, passwd, Instance.Self + "." + notify.getSecureKeyIndex());
					var origin = Cert.decryptRsa(privateKey,
							notify.getSecureMessage().bytesUnsafe(),
							notify.getSecureMessage().getOffset(),
							notify.getSecureMessage().size());
					var bb = ByteBuffer.Wrap(origin);
					var bMsg = new BTextMessage();
					bMsg.decode(bb);
					System.out.println(bMsg.getMessage());
				}
				return true;
			}
			return false;
		}

		@Override
		public void refresh() throws Exception {
			var list = Main.ReceivedMessages.remove(new MessageTarget(Target, 0));
			if (null != list) {
				for (var notify : list)
					processNotifyMessage(notify);
			}
		}
	}

	public static class MessageTarget {
		public final String Name;
		public final long   Id;

		@Override
		public int hashCode() {
			return Name.hashCode() ^ Long.hashCode(Id);
		}

		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (o instanceof MessageTarget) {
				var other = (MessageTarget)o;
				return Name.equals(other.Name) && Id == other.Id;
			}
			return false;
		}

		public MessageTarget(String name, long id) {
			Name = name;
			Id = id;
		}
	}

	public class MainWindow extends Window {
		public MainWindow() {
			Name = "main";
		}

		public long NodeId;
		public BGetFriendNode Node;

		public BFriend find(String value) {
			for (var friend : Node.getNode().getValues()) {
				if (friend.getId().equals(value))
					return (BFriend)friend.getValue().getBean();
			}
			return null;
		}

		@Override
		public boolean process(String line) throws Exception {
			var cmd = line.split(" ");
			switch (cmd[0]) {
			case "open":
				if (cmd.length > 1) {
					var target = cmd[1];
					if (null != find(target)) {
						addWindow(target.endsWith("@group") ? new GroupWindow(target) : new ChatWindow(target));
						return true;
					}
				}
				break;

			default:
				return super.process(line);
			}
			return false;
		}

		@Override
		public void refresh() {
			Node = App.Instance.Zege_Friend.getFriendNode(NodeId);
			for (var friend : Node.getNode().getValues()) {
				System.out.print(friend.getId());
				// 子部门的消息不统计。
				var list = ReceivedMessages.get(new MessageTarget(friend.getId(), 0));
				System.out.println(null == list ? "" : "(" + list.size() + ")");
			}
		}

		private final HashMap<MessageTarget, ArrayList<BMessage>> ReceivedMessages = new HashMap<>();

		@Override
		public boolean processNotifyMessage(BMessage notify) {
			var target = notify.getGroup().isEmpty() ? notify.getFrom() : notify.getGroup();
			var list = ReceivedMessages.computeIfAbsent(
					new MessageTarget(target, notify.getDepartmentId()),
					k -> new ArrayList<>());
			list.add(notify);
			return true;
		}
	}

	private void addWindow(Window layer) throws Exception {
		Windows.add(layer);
		refresh();
	}

	private void refresh() throws Exception {
		System.out.print("You Are '" + Self + "' Window=");
		for (var layer : Windows) {
			System.out.print("/");
			System.out.print(layer.Name);
		}
		System.out.println();
		Windows.get(Windows.size() - 1).refresh();
		System.out.print(">");
	}
}
