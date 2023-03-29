// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// Table: 有向图边表
@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tEdge extends TableX<Zeze.Builtin.Collections.DAG.BDAGEdgeKey, Zeze.Builtin.Collections.DAG.BDAGEdge>
        implements TableReadOnly<Zeze.Builtin.Collections.DAG.BDAGEdgeKey, Zeze.Builtin.Collections.DAG.BDAGEdge, Zeze.Builtin.Collections.DAG.BDAGEdgeReadOnly> {
    public tEdge() {
        super("Zeze_Builtin_Collections_DAG_tEdge");
    }

    @Override
    public boolean isRelationalMapping() {
        return false;
    }

    @Override
    public int getId() {
        return 1544681320;
    }

    public static final int VAR_From = 1;
    public static final int VAR_To = 2;

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGEdgeKey decodeKey(ByteBuffer _os_) {
        Zeze.Builtin.Collections.DAG.BDAGEdgeKey _v_ = new Zeze.Builtin.Collections.DAG.BDAGEdgeKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Collections.DAG.BDAGEdgeKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGEdgeKey decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        var parents = new java.util.ArrayList<String>();
        Zeze.Builtin.Collections.DAG.BDAGEdgeKey _v_ = new Zeze.Builtin.Collections.DAG.BDAGEdgeKey();
        parents.add("__key");
        _v_.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Zeze.Builtin.Collections.DAG.BDAGEdgeKey _v_) {
        var parents = new java.util.ArrayList<String>();
        parents.add("__key");
        _v_.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGEdge newValue() {
        return new Zeze.Builtin.Collections.DAG.BDAGEdge();
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGEdgeReadOnly getReadOnly(Zeze.Builtin.Collections.DAG.BDAGEdgeKey key) {
        return get(key);
    }
}
