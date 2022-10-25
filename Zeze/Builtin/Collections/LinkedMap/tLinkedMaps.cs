// auto-generated
using Zeze.Serialize;
using Zeze.Transaction;
using System.Threading.Tasks;

// key: LinkedMap的Name
namespace Zeze.Builtin.Collections.LinkedMap
{
    public sealed class tLinkedMaps : Table<string, Zeze.Builtin.Collections.LinkedMap.BLinkedMap>, TableReadOnly<string, Zeze.Builtin.Collections.LinkedMap.BLinkedMap, Zeze.Builtin.Collections.LinkedMap.BLinkedMapReadOnly>
    {
        public tLinkedMaps() : base("Zeze_Builtin_Collections_LinkedMap_tLinkedMaps")
        {
        }

        public override int Id => -72689413;
        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_HeadNodeId = 1;
        public const int VAR_TailNodeId = 2;
        public const int VAR_Count = 3;
        public const int VAR_LastNodeId = 4;

        public override string DecodeKey(ByteBuffer _os_)
        {
            string _v_;
            _v_ = _os_.ReadString();
            return _v_;
        }

        public override ByteBuffer EncodeKey(string _v_)
        {
            ByteBuffer _os_ = ByteBuffer.Allocate();
            _os_.WriteString(_v_);
            return _os_;
        }

        async Task<Zeze.Builtin.Collections.LinkedMap.BLinkedMapReadOnly> TableReadOnly<string, Zeze.Builtin.Collections.LinkedMap.BLinkedMap, Zeze.Builtin.Collections.LinkedMap.BLinkedMapReadOnly>.GetAsync(string key)
        {
            return await GetAsync(key);
        }
    }
}
