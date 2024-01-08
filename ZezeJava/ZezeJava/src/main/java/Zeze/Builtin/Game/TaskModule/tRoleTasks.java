// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tRoleTasks extends TableX<Long, Zeze.Builtin.Game.TaskModule.BRoleTasks>
        implements TableReadOnly<Long, Zeze.Builtin.Game.TaskModule.BRoleTasks, Zeze.Builtin.Game.TaskModule.BRoleTasksReadOnly> {
    public tRoleTasks() {
        super(-1718821525, "Zeze_Builtin_Game_TaskModule_tRoleTasks");
    }

    public tRoleTasks(String suffix) {
        super(-1718821525, "Zeze_Builtin_Game_TaskModule_tRoleTasks", suffix);
    }

    public static final int VAR_Tasks = 1;

    @Override
    public Long decodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(_v_));
        _os_.WriteLong(_v_);
        return _os_;
    }

    @Override
    public Long decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        long _v_;
        _v_ = rs.getLong("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Long _v_) {
        st.appendLong("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Game.TaskModule.BRoleTasks newValue() {
        return new Zeze.Builtin.Game.TaskModule.BRoleTasks();
    }

    @Override
    public Zeze.Builtin.Game.TaskModule.BRoleTasksReadOnly getReadOnly(Long key) {
        return get(key);
    }
}