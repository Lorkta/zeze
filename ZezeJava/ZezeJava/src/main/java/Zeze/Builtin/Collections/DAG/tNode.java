// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// Table: 有向图节点表
@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tNode extends TableX<Zeze.Builtin.Collections.DAG.BDAGNodeKey, Zeze.Builtin.Collections.DAG.BDAGNode>
        implements TableReadOnly<Zeze.Builtin.Collections.DAG.BDAGNodeKey, Zeze.Builtin.Collections.DAG.BDAGNode, Zeze.Builtin.Collections.DAG.BDAGNodeReadOnly> {
    public tNode() {
        super("Zeze_Builtin_Collections_DAG_tNode");
    }

    @Override
    public int getId() {
        return -1059152625;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_Value = 1;

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGNodeKey decodeKey(ByteBuffer _os_) {
        Zeze.Builtin.Collections.DAG.BDAGNodeKey _v_ = new Zeze.Builtin.Collections.DAG.BDAGNodeKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Collections.DAG.BDAGNodeKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGNode newValue() {
        return new Zeze.Builtin.Collections.DAG.BDAGNode();
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGNodeReadOnly getReadOnly(Zeze.Builtin.Collections.DAG.BDAGNodeKey key) {
        return get(key);
    }
}