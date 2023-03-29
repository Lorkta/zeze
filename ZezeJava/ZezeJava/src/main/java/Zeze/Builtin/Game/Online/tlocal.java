// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tlocal extends TableX<Long, Zeze.Builtin.Game.Online.BLocal>
        implements TableReadOnly<Long, Zeze.Builtin.Game.Online.BLocal, Zeze.Builtin.Game.Online.BLocalReadOnly> {
    public tlocal() {
        super("Zeze_Builtin_Game_Online_tlocal");
    }

    @Override
    public boolean isRelationalMapping() {
        return false;
    }

    @Override
    public int getId() {
        return -1657900798;
    }

    @Override
    public boolean isMemory() {
        return true;
    }

    public static final int VAR_LoginVersion = 1;
    public static final int VAR_Datas = 2;

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
    public Zeze.Builtin.Game.Online.BLocal newValue() {
        return new Zeze.Builtin.Game.Online.BLocal();
    }

    @Override
    public Zeze.Builtin.Game.Online.BLocalReadOnly getReadOnly(Long key) {
        return get(key);
    }
}
