// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BVersionReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BVersion copy();

    public long getLoginVersion();
    public Zeze.Transaction.Collections.PSet1ReadOnly<String> getReliableNotifyMarkReadOnly();
    public long getReliableNotifyIndex();
    public long getReliableNotifyConfirmIndex();
    public int getServerId();
}