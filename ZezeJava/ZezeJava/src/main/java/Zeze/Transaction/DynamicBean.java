package Zeze.Transaction;

import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.CollOne;
import Zeze.Transaction.Collections.Collection;
import Zeze.Transaction.Collections.LogBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DynamicBean extends Bean implements DynamicBeanReadOnly {
	@NotNull Bean bean;
	long typeId;
	private transient final @NotNull ToLongFunction<Bean> getBean;
	private transient final @NotNull LongFunction<Bean> createBean;

	public DynamicBean(int variableId, @NotNull ToLongFunction<Bean> get, @NotNull LongFunction<Bean> create) {
		super(variableId);
		bean = new EmptyBean();
		typeId = EmptyBean.TYPEID;
		getBean = get;
		createBean = create;
	}

	@Override
	public @NotNull Bean getBean() {
		if (!isManaged())
			return bean;
		var txn = Transaction.getCurrentVerifyRead(this);
		if (txn == null)
			return bean;
		//noinspection DataFlowIssue
		var log = (LogDynamic)txn.getLog(parent().objectId() + variableId());
		return log != null ? log.value : bean;
	}

	public void setBean(@Nullable Bean bean) {
		if (bean == null)
			bean = new EmptyBean();
		setBeanWithSpecialTypeId(getBean.applyAsLong(bean), bean);
	}

	@SuppressWarnings("deprecation")
	private void setBeanWithSpecialTypeId(long specialTypeId, @NotNull Bean bean) {
		if (bean instanceof DynamicBean) // 不允许嵌套放入DynamicBean,否则序列化会输出错误的数据流
			bean = ((DynamicBean)bean).getBean();
		if (bean instanceof Collection) {
			if (bean instanceof CollOne)
				bean = ((CollOne<?>)bean).getValue();
			else
				throw new IllegalStateException("can not set Collection Bean into DynamicBean");
		}
		if (!isManaged()) {
			typeId = specialTypeId;
			this.bean = bean;
			return;
		}
		//noinspection DataFlowIssue
		bean.initRootInfoWithRedo(rootInfo, this);
		bean.variableId(1); // 只有一个变量
		var txn = Transaction.getCurrentVerifyWrite(this);
		//noinspection DataFlowIssue
		var log = (LogDynamic)txn.logGetOrAdd(parent().objectId() + variableId(), this::createLogBean);
		log.setValue(specialTypeId, bean);
	}

	@Override
	public long getTypeId() {
		if (!isManaged())
			return typeId;
		var txn = Transaction.getCurrentVerifyRead(this);
		if (txn == null)
			return typeId;
		// 不能独立设置，总是设置Bean时一起Commit，所以这里访问Bean的Log。
		//noinspection DataFlowIssue
		var log = (LogDynamic)txn.getLog(parent().objectId() + variableId());
		return log != null ? log.specialTypeId : typeId;
	}

	@Override
	public long typeId() {
		return getTypeId();
	}

	public @NotNull ToLongFunction<Bean> getGetBean() {
		return getBean;
	}

	public @NotNull LongFunction<Bean> getCreateBean() {
		return createBean;
	}

	public @NotNull Bean newBean(long typeId) {
		var bean = createBean.apply(typeId);
		if (bean == null) {
			if (typeId == EmptyBean.TYPEID)
				bean = new EmptyBean();
			else
				throw new IllegalStateException("incompatible DynamicBean typeId=" + typeId);
		} else if (typeId == EmptyBean.TYPEID && !(bean instanceof EmptyBean))
			typeId = getBean.applyAsLong(bean); // 再确认一下真正的typeId
		setBeanWithSpecialTypeId(typeId, bean);
		return bean;
	}

	public void assign(@NotNull DynamicBean other) {
		setBean(other.getBean().copy());
	}

	public void assign(@NotNull DynamicData other) {
		setBean(other.getData().toBean());
	}

	@Override
	public void assign(@NotNull Data data) {
		assign((DynamicData)data);
	}

	public boolean isEmpty() {
		return getTypeId() == EmptyBean.TYPEID && getBean().getClass() == EmptyBean.class;
	}

	public void reset() {
		setBeanWithSpecialTypeId(EmptyBean.TYPEID, new EmptyBean());
	}

	@Override
	public boolean negativeCheck() {
		return getBean().negativeCheck();
	}

	@Override
	public @NotNull DynamicBean copy() {
		var copy = new DynamicBean(variableId(), getBean, createBean);
		copy.bean = getBean().copy();
		copy.typeId = getTypeId();
		return copy;
	}

	@Override
	public void decode(@NotNull ByteBuffer bb) {
		// 由于可能在事务中执行，这里仅修改Bean
		// TypeId 在 Bean 提交时才修改，但是要在事务中读到最新值，参见 TypeId 的 getter 实现。
		var typeId = bb.ReadLong();
		var real = createBean.apply(typeId);
		if (real == null) {
			if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD || typeId == EmptyBean.TYPEID) {
				typeId = EmptyBean.TYPEID;
				real = new EmptyBean();
			} else
				throw new IllegalStateException("incompatible DynamicBean typeId=" + typeId);
		} else if (typeId == EmptyBean.TYPEID && !(real instanceof EmptyBean))
			typeId = getBean.applyAsLong(real); // 再确认一下真正的typeId
		real.decode(bb);
		setBeanWithSpecialTypeId(typeId, real);
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteLong(getTypeId());
		getBean().encode(bb);
	}

	@Override
	public int preAllocSize() {
		return 9 + getBean().preAllocSize(); // [9]typeId
	}

	@Override
	public void preAllocSize(int size) {
		getBean().preAllocSize(size - 1); // [1]typeId
	}

	@Override
	protected void initChildrenRootInfo(@NotNull Record.RootInfo root) {
		bean.initRootInfo(root, this);
	}

	@Override
	protected void initChildrenRootInfoWithRedo(@NotNull Record.RootInfo root) {
		bean.initRootInfoWithRedo(root, this);
	}

	@Override
	public void mapKey(@NotNull Object mapKey) {
		getBean().mapKey(mapKey);
	}

	@Override
	public void followerApply(@NotNull Log log) {
		var dLog = (LogDynamic)log;
		if (null != dLog.value) {
			typeId = dLog.specialTypeId;
			bean = dLog.value;
		} else if (null != dLog.logBean) {
			bean.followerApply(dLog.logBean);
		}
	}

	@Override
	public @NotNull LogBean createLogBean() {
		var dLog = new LogDynamic();
		dLog.setBelong(parent());
		dLog.setThis(this);
		dLog.setVariableId(variableId());
		return dLog;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(getTypeId()) ^ getBean().hashCode();
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (o == this)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		var that = (DynamicBean)o;
		return getTypeId() == that.getTypeId() && getBean().equals(that.getBean());
	}
}
