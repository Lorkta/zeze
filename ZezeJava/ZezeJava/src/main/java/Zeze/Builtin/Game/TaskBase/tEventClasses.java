// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// key is 1, only one record
@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tEventClasses extends TableX<Integer, Zeze.Builtin.Game.TaskBase.BEventClasses>
        implements TableReadOnly<Integer, Zeze.Builtin.Game.TaskBase.BEventClasses, Zeze.Builtin.Game.TaskBase.BEventClassesReadOnly> {
    public tEventClasses() {
        super("Zeze_Builtin_Game_TaskBase_tEventClasses");
    }

    @Override
    public int getId() {
        return -1631158308;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_EventClasses = 1;

    @Override
    public Integer decodeKey(ByteBuffer _os_) {
        int _v_;
        _v_ = _os_.ReadInt();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Integer _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteInt(_v_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BEventClasses newValue() {
        return new Zeze.Builtin.Game.TaskBase.BEventClasses();
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BEventClassesReadOnly getReadOnly(Integer key) {
        return get(key);
    }
}