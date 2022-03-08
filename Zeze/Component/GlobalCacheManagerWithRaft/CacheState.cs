// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Component.GlobalCacheManagerWithRaft
{
    public sealed class CacheState : Zeze.Raft.RocksRaft.Bean
    {
        int _AcquireStatePending;
        long _GlobalSerialId;
        int _Modify; // ServerId
        readonly Zeze.Raft.RocksRaft.CollSet1<int> _Share;

        public int AcquireStatePending { get { return _AcquireStatePending; } set { _AcquireStatePending = value; } }
        public long GlobalSerialId
        {
            get
            {
                if (!IsManaged)
                    return _GlobalSerialId;
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                if (txn == null) return _GlobalSerialId;
                var log = txn.GetLog(ObjectId + 2);
                return log != null ? ((Zeze.Raft.RocksRaft.Log<long>)log).Value : _GlobalSerialId;
            }
            set
            {
                if (!IsManaged)
                {
                    _GlobalSerialId = value;
                    return;
                }
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                txn.PutLog(new Zeze.Raft.RocksRaft.Log<long>() { Belong = this, VariableId = 2, Value = value, });
            }
        }

        public int Modify
        {
            get
            {
                if (!IsManaged)
                    return _Modify;
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                if (txn == null) return _Modify;
                var log = txn.GetLog(ObjectId + 3);
                return log != null ? ((Zeze.Raft.RocksRaft.Log<int>)log).Value : _Modify;
            }
            set
            {
                if (!IsManaged)
                {
                    _Modify = value;
                    return;
                }
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                txn.PutLog(new Zeze.Raft.RocksRaft.Log<int>() { Belong = this, VariableId = 3, Value = value, });
            }
        }

        public Zeze.Raft.RocksRaft.CollSet1<int> Share => _Share;

        public CacheState() : this(0)
        {
        }

        public CacheState(int _varId_) : base(_varId_)
        {
            _Share = new Zeze.Raft.RocksRaft.CollSet1<int>() { VariableId = 4 };
        }

        public void Assign(CacheState other)
        {
            AcquireStatePending = other.AcquireStatePending;
            GlobalSerialId = other.GlobalSerialId;
            Modify = other.Modify;
            Share.Clear();
            foreach (var e in other.Share)
                Share.Add(e);
        }

        public CacheState CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public CacheState Copy()
        {
            var copy = new CacheState();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(CacheState a, CacheState b)
        {
            CacheState save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Raft.RocksRaft.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = -5694711273313404316;
        public override long TypeId => TYPEID;

        public override string ToString()
        {
            System.Text.StringBuilder sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Component.GlobalCacheManagerWithRaft.CacheState: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("AcquireStatePending").Append('=').Append(AcquireStatePending).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("GlobalSerialId").Append('=').Append(GlobalSerialId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Modify").Append('=').Append(Modify).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Share").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in Share)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Item).Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = GlobalSerialId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                int _x_ = Modify;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                var _x_ = Share;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                    foreach (var _v_ in _x_)
                        _o_.WriteLong(_v_);
                }
            }
            _o_.WriteByte(0);
        }

        public override void Decode(ByteBuffer _o_)
        {
            int _t_ = _o_.ReadByte();
            int _i_ = _o_.ReadTagSize(_t_);
            while (_t_ != 0 && _i_ < 2)
            {
                _o_.SkipUnknownField(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                GlobalSerialId = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                Modify = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                var _x_ = Share;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                        _x_.Add(_o_.ReadInt(_t_));
                }
                else
                    _o_.SkipUnknownField(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }

        protected override void InitChildrenRootInfo(Zeze.Raft.RocksRaft.Record.RootInfo root)
        {
            _Share.InitRootInfo(root, this);
        }

        public override void LeaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog)
        {
            switch (vlog.VariableId)
            {
                case 2: _GlobalSerialId = ((Zeze.Raft.RocksRaft.Log<long>)vlog).Value; break;
                case 3: _Modify = ((Zeze.Raft.RocksRaft.Log<int>)vlog).Value; break;
                case 4: _Share.LeaderApplyNoRecursive(vlog); break;
            }
        }

        public override void FollowerApply(Zeze.Raft.RocksRaft.Log log)
        {
            var blog = (Zeze.Raft.RocksRaft.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 2: _GlobalSerialId = ((Zeze.Raft.RocksRaft.Log<long>)vlog).Value; break;
                    case 3: _Modify = ((Zeze.Raft.RocksRaft.Log<int>)vlog).Value; break;
                    case 4: _Share.FollowerApply(vlog); break;
                }
            }
        }

    }
}
