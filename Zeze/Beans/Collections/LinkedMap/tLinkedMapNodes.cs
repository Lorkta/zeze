// auto-generated
using Zeze.Serialize;

namespace Zeze.Beans.Collections.LinkedMap
{
    public sealed class tLinkedMapNodes : Zeze.Transaction.Table<Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeKey, Zeze.Beans.Collections.LinkedMap.BLinkedMapNode>
    {
        public tLinkedMapNodes() : base("Zeze_Beans_Collections_LinkedMap_tLinkedMapNodes")
        {
        }

        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_All = 0;
        public const int VAR_PrevNodeId = 1;
        public const int VAR_NextNodeId = 2;
        public const int VAR_Values = 3;

        public override Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeKey DecodeKey(ByteBuffer _os_)
        {
            Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeKey _v_ = new Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeKey();
            _v_.Decode(_os_);
            return _v_;
        }

        public override ByteBuffer EncodeKey(Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeKey _v_)
        {
            ByteBuffer _os_ = ByteBuffer.Allocate();
            _v_.Encode(_os_);
            return _os_;
        }

        public override Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId)
        {
            return variableId switch
            {
                0 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                1 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                2 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                3 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                _ => null,
            };
        }
    }
}
