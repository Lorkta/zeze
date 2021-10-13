﻿using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Compare : Visitor
	{
		public static void Make(BeanKey bean, System.IO.StreamWriter sw, String prefix)
		{
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public int compareTo(" + bean.Name + " _o1_) {");
            sw.WriteLine(prefix + "    if (_o1_ == this) return 0;");
            sw.WriteLine(prefix + "    if (_o1_ is " + bean.Name + ") {");
            sw.WriteLine(prefix + "        var _o_ = (" + bean.Name + ")_o1_;");
            sw.WriteLine(prefix + "        int _c_" + (bean.Variables.Count > 0 ? ";" : " = 0;"));
            foreach (Variable var in bean.Variables)
			{
                Compare e = new Compare(var, "_o_");
				var.VariableType.Accept(e);
				sw.WriteLine(prefix + "        _c_ = " + e.text + ";");
                sw.WriteLine(prefix + "        if (0 != _c_) return _c_;");
			}
			sw.WriteLine(prefix + "        return _c_;");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    throw new RuntimeException(\"CompareTo: another object is not " + bean.FullName + "\");");
            sw.WriteLine(prefix + "}");
			sw.WriteLine("");
		}

        private Variable variable;
        private String another;
        private String text;
        
        public Compare(Variable var, string another)
        {
            this.variable = var;
            this.another = another;
        }

        public void Visit(Bean type)
        {
            text = variable.NamePrivate + ".compareTo(" + another + "." + variable.NamePrivate + ")";
        }

        public void Visit(BeanKey type)
        {
            text = variable.NamePrivate + ".compareTo(" + another + "." + variable.NamePrivate + ")";
        }

        public void Visit(TypeByte type)
        {
            text = $"Byte.compareTo({variable.NamePrivate}, {another}.{variable.NamePrivate})";
        }

        public void Visit(TypeDouble type)
        {
            text = $"Double.compareTo({variable.NamePrivate}, {another}.{variable.NamePrivate})";
        }

        public void Visit(TypeInt type)
        {
            text = $"Integer.compareTo({variable.NamePrivate}, {another}.{variable.NamePrivate})";
        }

        public void Visit(TypeLong type)
        {
            text = $"Long.compareTo({variable.NamePrivate}, {another}.{variable.NamePrivate})";
        }

        public void Visit(TypeBool type)
        {
            text = $"Boolean.compareTo({variable.NamePrivate}, {another}.{variable.NamePrivate})";
        }

        public void Visit(TypeBinary type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeString type)
        {
            text = variable.NamePrivate + ".compareTo(" + another + "." + variable.NamePrivate + ")";
        }

        public void Visit(TypeList type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeSet type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeMap type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeFloat type)
        {
            text = $"Float.compareTo({variable.NamePrivate}, {another}.{variable.NamePrivate})";
        }

        public void Visit(TypeShort type)
        {
            text = $"Short.compareTo({variable.NamePrivate}, {another}.{variable.NamePrivate})";
        }

        public void Visit(TypeDynamic type)
        {
            throw new NotImplementedException();
        }
    }
}
