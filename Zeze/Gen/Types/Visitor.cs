namespace Zeze.Gen.Types
{
    public interface Visitor
    {
        public void Visit(TypeBool type);    // ���л�����: 0
        public void Visit(TypeByte type);    // ���л�����: 0
        public void Visit(TypeShort type);   // ���л�����: 0
        public void Visit(TypeInt type);     // ���л�����: 0
        public void Visit(TypeLong type);    // ���л�����: 0
        public void Visit(TypeFloat type);   // ���л�����: 1
        public void Visit(TypeDouble type);  // ���л�����: 2
        public void Visit(TypeBinary type);  // ���л�����: 3
        public void Visit(TypeString type);  // ���л�����: 3
        public void Visit(TypeList type);    // ���л�����: 4
        public void Visit(TypeSet type);     // ���л�����: 4
        public void Visit(TypeMap type);     // ���л�����: 5
        public void Visit(Bean type);        // ���л�����: 6
        public void Visit(BeanKey type);     // ���л�����: 6
        public void Visit(TypeDynamic type); // ���л�����: 7
    }
}
