using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen.Types
{
	public class TypeInt : Type
	{

		public override Type Compile(ModuleSpace space, string key, string value, object param)
		{
			if (key != null && key.Length > 0)
				throw new Exception(Name + " type does not need a key. " + key);

			if (value != null && value.Length > 0)
				throw new Exception(Name + " type does not need a value. " + value);

			return this;
		}

		public override void Accept(Visitor visitor)
		{
			visitor.Visit(this);
		}

		public override void Depends(HashSet<Type> includes)
		{
			includes.Add(this);
		}


		public override string Name => "int";
        public override bool IsImmutable => true;
		public override bool IsNeedNegativeCheck => true;

		internal TypeInt(SortedDictionary<string, Type> types)
		{
			types.Add(Name, this);
		}
	}
}
