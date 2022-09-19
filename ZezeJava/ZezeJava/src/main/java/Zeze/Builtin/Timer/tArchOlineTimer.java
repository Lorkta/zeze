// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tArchOlineTimer extends Zeze.Transaction.TableX<Long, Zeze.Builtin.Timer.BArchOnlineTimer> {
    public tArchOlineTimer() {
        super("Zeze_Builtin_Timer_tArchOlineTimer");
    }

    @Override
    public int getId() {
        return 1665324784;
    }

    @Override
    public boolean isMemory() {
        return true;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_Account = 1;
    public static final int VAR_ClientId = 2;
    public static final int VAR_TimerObj = 3;
    public static final int VAR_LoginVersion = 4;
    public static final int VAR_NamedName = 5;

    @Override
    public Long decodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteLong(_v_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Timer.BArchOnlineTimer newValue() {
        return new Zeze.Builtin.Timer.BArchOnlineTimer();
    }
}