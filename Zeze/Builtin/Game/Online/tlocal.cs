// auto-generated
using Zeze.Serialize;
using Zeze.Transaction;
using System.Threading.Tasks;

// ReSharper disable JoinDeclarationAndInitializer RedundantNameQualifier
namespace Zeze.Builtin.Game.Online
{
    public sealed class tlocal : Table<long, Zeze.Builtin.Game.Online.BLocal>, TableReadOnly<long, Zeze.Builtin.Game.Online.BLocal, Zeze.Builtin.Game.Online.BLocalReadOnly>
    {
        public tlocal() : base("Zeze_Builtin_Game_Online_tlocal")
        {
        }

        public override int Id => -1657900798;
        public override bool IsMemory => true;
        public override bool IsAutoKey => false;

        public const int VAR_LoginVersion = 1;
        public const int VAR_Datas = 2;

        public override long DecodeKey(ByteBuffer _os_)
        {
            long _v_;
            _v_ = _os_.ReadLong();
            return _v_;
        }

        public override ByteBuffer EncodeKey(long _v_)
        {
            ByteBuffer _os_ = ByteBuffer.Allocate();
            _os_.WriteLong(_v_);
            return _os_;
        }

        async Task<Zeze.Builtin.Game.Online.BLocalReadOnly> TableReadOnly<long, Zeze.Builtin.Game.Online.BLocal, Zeze.Builtin.Game.Online.BLocalReadOnly>.GetAsync(long key)
        {
            return await GetAsync(key);
        }
    }
}
