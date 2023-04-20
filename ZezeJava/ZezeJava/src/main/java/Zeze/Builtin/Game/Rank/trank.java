// auto-generated @formatter:off
package Zeze.Builtin.Game.Rank;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class trank extends TableX<Zeze.Builtin.Game.Rank.BConcurrentKey, Zeze.Builtin.Game.Rank.BRankList>
        implements TableReadOnly<Zeze.Builtin.Game.Rank.BConcurrentKey, Zeze.Builtin.Game.Rank.BRankList, Zeze.Builtin.Game.Rank.BRankListReadOnly> {
    public trank() {
        super("Zeze_Builtin_Game_Rank_trank");
    }

    public trank(String suffix) {
        super("Zeze_Builtin_Game_Rank_trank" + suffix);
    }

    public String getOriginName() {
        return "Zeze_Builtin_Game_Rank_trank";
    }

    @Override
    public int getId() {
        return -2043108039;
    }

    public static final int VAR_RankList = 1;

    @Override
    public Zeze.Builtin.Game.Rank.BConcurrentKey decodeKey(ByteBuffer _os_) {
        Zeze.Builtin.Game.Rank.BConcurrentKey _v_ = new Zeze.Builtin.Game.Rank.BConcurrentKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Game.Rank.BConcurrentKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Game.Rank.BConcurrentKey decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        var parents = new java.util.ArrayList<String>();
        Zeze.Builtin.Game.Rank.BConcurrentKey _v_ = new Zeze.Builtin.Game.Rank.BConcurrentKey();
        parents.add("__key");
        _v_.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Zeze.Builtin.Game.Rank.BConcurrentKey _v_) {
        var parents = new java.util.ArrayList<String>();
        parents.add("__key");
        _v_.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }

    @Override
    public Zeze.Builtin.Game.Rank.BRankList newValue() {
        return new Zeze.Builtin.Game.Rank.BRankList();
    }

    @Override
    public Zeze.Builtin.Game.Rank.BRankListReadOnly getReadOnly(Zeze.Builtin.Game.Rank.BConcurrentKey key) {
        return get(key);
    }
}
