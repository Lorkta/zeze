// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

public interface BTaskConditionReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTaskCondition copy();

    public long getTaskConditionId();
    public Zeze.Transaction.DynamicBeanReadOnly getTaskConditionCustomDataReadOnly();

}
