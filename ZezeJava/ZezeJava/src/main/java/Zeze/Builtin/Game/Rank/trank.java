// auto-generated @formatter:off
package Zeze.Builtin.Game.Rank;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class trank extends Zeze.Transaction.TableX<Zeze.Builtin.Game.Rank.BConcurrentKey, Zeze.Builtin.Game.Rank.BRankList> {
    public trank() {
        super("Zeze_Builtin_Game_Rank_trank");
    }

    @Override
    public int getId() {
        return -2043108039;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
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
    public Zeze.Builtin.Game.Rank.BRankList newValue() {
        return new Zeze.Builtin.Game.Rank.BRankList();
    }

    public Zeze.Builtin.Game.Rank.BRankListReadOnly getReadOnly(Zeze.Builtin.Game.Rank.BConcurrentKey k) {
        return get(k);
    }
}
