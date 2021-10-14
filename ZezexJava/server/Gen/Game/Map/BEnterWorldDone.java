// auto-generated
package Game.Map;

import Zeze.Serialize.*;

public final class BEnterWorldDone extends Zeze.Transaction.Bean implements BEnterWorldDoneReadOnly {
    private int _MapInstanceId;

    public int getMapInstanceId(){
        if (false == this.isManaged())
            return _MapInstanceId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _MapInstanceId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__MapInstanceId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _MapInstanceId;
    }

    public void setMapInstanceId(int value){
        if (false == this.isManaged()) {
            _MapInstanceId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__MapInstanceId(this, value));
    }


    public BEnterWorldDone() {
         this(0);
    }

    public BEnterWorldDone(int _varId_) {
        super(_varId_);
    }

    public void Assign(BEnterWorldDone other) {
        setMapInstanceId(other.getMapInstanceId());
    }

    public BEnterWorldDone CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BEnterWorldDone Copy() {
        var copy = new BEnterWorldDone();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BEnterWorldDone a, BEnterWorldDone b) {
        BEnterWorldDone save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = 3901261098602822919L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__MapInstanceId extends Zeze.Transaction.Log1<BEnterWorldDone, Integer> {
        public Log__MapInstanceId(BEnterWorldDone self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._MapInstanceId = this.getValue(); }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(" ".repeat(level * 4)).append("Game.Map.BEnterWorldDone: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("MapInstanceId").append("=").append(getMapInstanceId()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(1); // Variables.Count
        _os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getMapInstanceId());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT): 
                    setMapInstanceId(_os_.ReadInt());
                    break;
                default:
                    ByteBuffer.SkipUnknownField(_tagid_, _os_);
                    break;
            }
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    public boolean NegativeCheck() {
        if (getMapInstanceId() < 0) return true;
        return false;
    }

}
