package Zege;

import org.w3c.dom.Element;

public class ZegeConfig implements Zeze.Config.ICustomize {
	public int AboutHasRead = 3;
	public int AboutLast = 20;
	public int MessageLimit = 20;
	public int GroupChatLimit = 1000;
	public int DepartmentChildrenLimit = 300;
	public int GroupInviteLimit = 10;
	public int BelongDepartmentLimit = 100;

	public int FriendCountPerNode = 10; // TODO 现在为了测试多节点，设置的比较小。以后记得改大，比如200。
	public int NotifyCountPerNode = 100;

	@Override
	public String getName() {
		return "zege";
	}

	@Override
	public void parse(Element self) {
		String attr;

		attr = self.getAttribute("AboutHasRead");
		if (!attr.isEmpty())
			AboutHasRead = Integer.parseInt(attr);

		attr = self.getAttribute("AboutLast");
		if (!attr.isEmpty())
			AboutLast = Integer.parseInt(attr);

		attr = self.getAttribute("MessageLimit");
		if (!attr.isEmpty())
			MessageLimit = Integer.parseInt(attr);

		attr = self.getAttribute("GroupChatLimit");
		if (!attr.isEmpty())
			GroupChatLimit = Integer.parseInt(attr);

		attr = self.getAttribute("DepartmentChildrenLimit");
		if (!attr.isEmpty())
			DepartmentChildrenLimit = Integer.parseInt(attr);

		attr = self.getAttribute("GroupInviteLimit");
		if (!attr.isEmpty())
			GroupInviteLimit = Integer.parseInt(attr);

		attr = self.getAttribute("BelongDepartmentLimit");
		if (!attr.isEmpty())
			BelongDepartmentLimit = Integer.parseInt(attr);

		attr = self.getAttribute("FriendCountPerNode");
		if (!attr.isEmpty())
			FriendCountPerNode = Integer.parseInt(attr);

		attr = self.getAttribute("NotifyCountPerNode");
		if (!attr.isEmpty())
			NotifyCountPerNode = Integer.parseInt(attr);
	}
}
