package Zeze.Collections;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Collections.DepartmentTree.*;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Util.OutLong;

public class DepartmentTree<
		TManager extends Bean,
		TMember extends Bean,
		TDepartmentMember extends Bean,
		TGroupData extends Bean,
		TDepartmentData extends Bean> {
	private static final BeanFactory beanFactory = new BeanFactory();

	public static long GetSpecialTypeIdFromBean(Bean bean) {
		return BeanFactory.GetSpecialTypeIdFromBean(bean);
	}

	public static Bean CreateBeanFromSpecialTypeId(long typeId) {
		return beanFactory.CreateBeanFromSpecialTypeId(typeId);
	}

	public static class Module extends AbstractDepartmentTree {
		private final ConcurrentHashMap<String, DepartmentTree<?, ?, ?, ?, ?>> Trees = new ConcurrentHashMap<>();
		public final Zeze.Application Zeze;
		public final LinkedMap.Module LinkedMaps;

		public Module(Zeze.Application zeze, LinkedMap.Module linkedMapModule) {
			Zeze = zeze;
			RegisterZezeTables(zeze);
			LinkedMaps = linkedMapModule;
		}

		@Override
		public void UnRegister() {
			UnRegisterZezeTables(Zeze);
		}

		@SuppressWarnings("unchecked")
		public <TManager extends Bean,
				TMember extends Bean,
				TDepartmentMember extends Bean,
				TGroupData extends Bean,
				TDepartmentData extends Bean>
				DepartmentTree<TManager, TMember, TDepartmentMember, TGroupData, TDepartmentData>
			open(String name,
				 Class<TManager> managerClass,
				 Class<TMember> memberClass,
				 Class<TDepartmentMember> departmentMemberClass,
				 Class<TGroupData> groupDataClass,
				 Class<TDepartmentData> departmentDataClass) {
			return (DepartmentTree<
					TManager,
					TMember,
					TDepartmentMember,
					TGroupData,
					TDepartmentData>)
					Trees.computeIfAbsent(name,
					k -> new DepartmentTree<>(this, k,
							managerClass,
							memberClass,
							departmentMemberClass,
							groupDataClass,
							departmentDataClass));
		}

	}

	private final Module module;
	private final String name;
	private final MethodHandle managerConstructor;
	private final MethodHandle groupDataConstructor;
	private final MethodHandle departmentDataClassConstructor;
	//private final Class<TManager> managerClass;
	private final Class<TMember> memberClass;
	private final Class<TDepartmentMember> departmentMemberClass;

	private DepartmentTree(Module module, String name,
						   Class<TManager> managerClass,
						   Class<TMember> memberClass,
						   Class<TDepartmentMember> departmentMemberClass,
						   Class<TGroupData> groupDataClass,
						   Class<TDepartmentData> departmentDataClass) {
		this.module = module;
		this.name = name;
		this.managerConstructor = beanFactory.register(managerClass);
		this.groupDataConstructor = beanFactory.register(groupDataClass);
		this.departmentDataClassConstructor = beanFactory.register(departmentDataClass);
		//this.managerClass = managerClass;
		this.memberClass = memberClass;
		this.departmentMemberClass = departmentMemberClass;
	}

	public String getName() {
		return name;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Api 设计原则：
	// 1. 可以直接访问原始Bean，不进行包装。
	// 2. 不考虑使用安全性。
	// 3. 提供必要的辅助函数完成一些操作。
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	public TManager getManagerData(BDepartmentRoot root, String account) {
		var dynamicManager = root.getManagers().get(account);
		if (null == dynamicManager)
			return null;
		return (TManager)dynamicManager.getBean();
	}

	@SuppressWarnings("unchecked")
	public TManager getManagerData(BDepartmentTreeNode department, String account) {
		var dynamicManager = department.getManagers().get(account);
		if (null == dynamicManager)
			return null;
		return (TManager)dynamicManager.getBean();
	}

	@SuppressWarnings("unchecked")
	public TGroupData getGroupData(BDepartmentRoot root) {
		return (TGroupData)root.getData().getBean();
	}

	@SuppressWarnings("unchecked")
	public TDepartmentData getDepartmentData(BDepartmentTreeNode department) {
		return (TDepartmentData)department.getData().getBean();
	}

	// see checkManagePermission
	public long checkParentManagePermission(String account, long departmentId) {
		if (departmentId == 0) {
			// root
			var root = getRoot();
			if (root.getRoot().equals(account))
				return 0; // parent, 对于根节点定义成root，grant
			return module.ErrorCode(Module.ErrorManagePermission);
		}
		var department = getDepartmentTreeNode(departmentId);
		if (department == null)
			return module.ErrorCode(Module.ErrorDepartmentNotExist);

		// 开始检查parent
		return checkManagePermission(account, department.getParentDepartment());
	}

	// 检查account是否拥有部门的管理权限。
	// 规则：
	// 1. 如果部门拥有管理员，仅判断account是否管理员。
	// 2. 如果部门没有管理员，则递归检查父部门的管理员设置。
	// 3. 递归时优先规则1，直到根为止。Root总是拥有权限。
	// 其他：
	// 当管理管理员设置时，这个方法允许本级部门管理添加新的管理员和删除管理员。
	// 对于管理员的修改是否限定只能由上级操作？
	// 对于这个限定，可以在调用 checkParentManagePermission
	public long checkManagePermission(String account, long departmentId) {
		if (departmentId == 0) {
			// root
			var root = getRoot();
			if (root.getManagers().containsKey(account) || root.getRoot().equals(account))
				return 0; // grant
			return module.ErrorCode(Module.ErrorManagePermission);
		}

		var department = getDepartmentTreeNode(departmentId);
		if (department == null)
			return module.ErrorCode(Module.ErrorDepartmentNotExist);

		if (department.getManagers().isEmpty()) // 当前部门没有管理员，使用父部门的设置(递归)。
			return checkManagePermission(account, department.getParentDepartment());

		if (department.getManagers().containsKey(account))
			return 0; // grant

		// 当设置了管理员，不再递归。遵守权限不越级规则。
		return module.ErrorCode(Module.ErrorManagePermission);
	}

	public BDepartmentRoot getRoot() {
		return module._tDepartment.get(name);
	}

	public BDepartmentTreeNode getDepartmentTreeNode(long departmentId) {
		if (departmentId == 0)
			throw new IllegalArgumentException("root can not access use this method.");
		return module._tDepartmentTree.get(new BDepartmentKey(name, departmentId));
	}

	public LinkedMap<TDepartmentMember> getDepartmentMembers(long departmentId) {
		if (departmentId == 0)
			throw new RuntimeException("root can not access use this method.");
		return module.LinkedMaps.open(departmentId + "@" + name, departmentMemberClass);
	}

	public LinkedMap<TMember> getGroupMembers() {
		return module.LinkedMaps.open("0@" + name, memberClass);
	}

	public BDepartmentRoot selectRoot() {
		return module._tDepartment.selectDirty(name);
	}

	public BDepartmentTreeNode selectDepartmentTreeNode(long departmentId) {
		return module._tDepartmentTree.selectDirty(new BDepartmentKey(name, departmentId));
	}

	public BDepartmentRoot create() {
		var root = module._tDepartment.getOrAdd(name);
		root.getData().setBean(BeanFactory.invoke(groupDataConstructor));
		return root;
	}

	public long changeRoot(String oldRoot, String newRoot) {
		var dRoot = module._tDepartment.getOrAdd(name);
		if (!dRoot.getRoot().equals(oldRoot))
			return module.ErrorCode(Module.ErrorChangeRootNotOwner);
		dRoot.setRoot(newRoot);
		return 0;
	}

	@SuppressWarnings ("unchecked")
	private TManager getOrAddRootManager() {
		var dRoot = module._tDepartment.getOrAdd(name);
		return (TManager)dRoot.getManagers().computeIfAbsent(name, key -> {
			var value = new DynamicBean(0, DepartmentTree::GetSpecialTypeIdFromBean, DepartmentTree::CreateBeanFromSpecialTypeId);
			value.setBean(BeanFactory.invoke(managerConstructor));
			return value;
		}).getBean();
	}

	@SuppressWarnings ("unchecked")
	public TManager getOrAddManager(long departmentId, String name) {
		if (departmentId == 0)
			return getOrAddRootManager();

		var d = getDepartmentTreeNode(departmentId);
		return (TManager)d.getManagers().computeIfAbsent(name, key -> {
			var value = new DynamicBean(0, DepartmentTree::GetSpecialTypeIdFromBean, DepartmentTree::CreateBeanFromSpecialTypeId);
			value.setBean(BeanFactory.invoke(managerConstructor));
			return value;
		}).getBean();
	}

	@SuppressWarnings ("unchecked")
	private TManager deleteRootManager(String name) {
		var dRoot = module._tDepartment.getOrAdd(name);
		var m = dRoot.getManagers().remove(name);
		if (null == m)
			return null;
		return (TManager)m.getBean();
	}

	@SuppressWarnings ("unchecked")
	public TManager deleteManager(long departmentId, String name) {
		if (departmentId == 0)
			return deleteRootManager(name);

		var d = getDepartmentTreeNode(departmentId);
		var m = d.getManagers().remove(name);
		return (TManager)m.getBean();
	}

	public long createDepartment(long departmentParent, String dName, int childrenLimit, OutLong outDepartmentId) {
		var dRoot = module._tDepartment.getOrAdd(name);
		var dId = dRoot.getNextDepartmentId() + 1;

		if (departmentParent == 0) {
			if (dRoot.getChilds().size() > childrenLimit)
				return module.ErrorCode(Module.ErrorTooManyChildren);
			if (null != dRoot.getChilds().putIfAbsent(dName, dId))
				return module.ErrorCode(Module.ErrorDepartmentDuplicate);
		} else {
			var parent = getDepartmentTreeNode(departmentParent);
			if (parent.getChilds().size() > childrenLimit)
				return module.ErrorCode(Module.ErrorTooManyChildren);
			if (null != parent.getChilds().putIfAbsent(dName, dId))
				return module.ErrorCode(Module.ErrorDepartmentDuplicate);
		}
		var child = new BDepartmentTreeNode();
		child.getData().setBean(BeanFactory.invoke(departmentDataClassConstructor));
		child.setName(dName);
		child.setParentDepartment(departmentParent);
		module._tDepartmentTree.insert(new BDepartmentKey(name, dId), child);
		dRoot.setNextDepartmentId(dId);
		if (null != outDepartmentId)
			outDepartmentId.Value = dId;
		return 0;
	}

	public long deleteDepartment(long departmentId, boolean recursive) {
		var department = module._tDepartmentTree.get(new BDepartmentKey(name, departmentId));
		if (null == department)
			return module.ErrorCode(Module.ErrorDepartmentNotExist);
		if (!recursive && department.getChilds().size() > 0)
			return module.ErrorCode(Module.ErrorDeleteDepartmentRemainChilds);
		for (var child : department.getChilds().values()) {
			deleteDepartment(child, true);
		}
		if (department.getParentDepartment() == 0) {
			var root = module._tDepartment.get(name);
			root.getChilds().remove(department.getName());
		} else {
			var parent = getDepartmentTreeNode(department.getParentDepartment());
			parent.getChilds().remove(department.getName());
		}
		getDepartmentMembers(departmentId).clear();
		module._tDepartmentTree.remove(new BDepartmentKey(name, departmentId));
		return 0;
	}

	public boolean isRecursiveChild(long departmentId, long child) {
		if (departmentId == child)
			return true;

		var department = module._tDepartmentTree.get(new BDepartmentKey(name, departmentId));
		if (null == department)
			return false;
		for (var c : department.getChilds().values()) {
			if (isRecursiveChild(c, child))
				return true;
		}
		return false;
	}

	public long moveDepartment(long departmentId) {
		var department = module._tDepartmentTree.get(new BDepartmentKey(name, departmentId));
		var newParent = module._tDepartment.get(name);
		if (null == department || null == newParent)
			return module.ErrorCode(Module.ErrorDepartmentNotExist);
		if (department.getParentDepartment() == 0)
			return module.ErrorCode(Module.ErrorDepartmentSameParent);
		var oldParent = getDepartmentTreeNode(department.getParentDepartment());
		oldParent.getChilds().remove(department.getName());
		if (null != newParent.getChilds().putIfAbsent(department.getName(), departmentId))
			return module.ErrorCode(Module.ErrorDepartmentDuplicate);
		department.setParentDepartment(0);
		return 0;
	}

	public long moveDepartment(long departmentId, long parent) {
		if (parent == 0) // to root
			return moveDepartment(departmentId);

		if (isRecursiveChild(departmentId, parent))
			return module.ErrorCode(Module.ErrorCanNotMoveToChilds);
		var department = module._tDepartmentTree.get(new BDepartmentKey(name, departmentId));
		if (null == department)
			return module.ErrorCode(Module.ErrorDepartmentNotExist);
		if (department.getParentDepartment() == parent)
			return module.ErrorCode(Module.ErrorDepartmentSameParent);
		var newParent = getDepartmentTreeNode(parent);
		if (null == newParent)
			return module.ErrorCode(Module.ErrorDepartmentParentNotExist);
		var oldParent = getDepartmentTreeNode(department.getParentDepartment());
		oldParent.getChilds().remove(department.getName());
		if (null != newParent.getChilds().putIfAbsent(department.getName(), departmentId))
			return module.ErrorCode(Module.ErrorDepartmentDuplicate);
		department.setParentDepartment(parent);
		return 0;
	}
}
