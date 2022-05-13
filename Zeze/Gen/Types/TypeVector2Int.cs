using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen.Types
{
	public class TypeVector2Int : Type
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

		internal TypeVector2Int(SortedDictionary<string, Type> types)
		{
			types.Add(Name, this);
		}

		public override string Name => "vector2int";
		public override bool IsNeedNegativeCheck => false;

		public override bool IsImmutable => true;
	}
}
