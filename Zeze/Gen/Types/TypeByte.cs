using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen.Types
{
	public class TypeByte : Type
	{
		public override Type Compile(ModuleSpace space, string key, string value, Variable var)
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

		public override string Name => "byte";

		public override bool IsNeedNegativeCheck => false;

		internal TypeByte(SortedDictionary<string, Type> types)
		{
			types.Add(Name, this);
		}

        public override bool IsImmutable => true;
	}
}
