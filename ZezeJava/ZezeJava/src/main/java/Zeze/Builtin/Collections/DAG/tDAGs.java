// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// Key: 有向图自己的名字
@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tDAGs extends TableX<String, Zeze.Builtin.Collections.DAG.BDAG>
        implements TableReadOnly<String, Zeze.Builtin.Collections.DAG.BDAG, Zeze.Builtin.Collections.DAG.BDAGReadOnly> {
    public tDAGs() {
        super("Zeze_Builtin_Collections_DAG_tDAGs");
    }

    @Override
    public boolean isRelationalMapping() {
        return false;
    }

    @Override
    public int getId() {
        return 1716762493;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_NodeSum = 1;
    public static final int VAR_EdgeSum = 2;
    public static final int VAR_StartNode = 3;
    public static final int VAR_EndNode = 4;

    @Override
    public String decodeKey(ByteBuffer _os_) {
        String _v_;
        _v_ = _os_.ReadString();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(String _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteString(_v_);
        return _os_;
    }

    @Override
    public String decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        String _v_;
        _v_ = rs.getString("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, String _v_) {
        st.appendString("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAG newValue() {
        return new Zeze.Builtin.Collections.DAG.BDAG();
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGReadOnly getReadOnly(String key) {
        return get(key);
    }
}
