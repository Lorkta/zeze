// auto-generated @formatter:off
package Zeze.Builtin.World;

public interface BObjectReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BObject copy();

    Zeze.Serialize.Vector3 getPosition();
    Zeze.Transaction.DynamicBeanReadOnly getDataReadOnly();
}
