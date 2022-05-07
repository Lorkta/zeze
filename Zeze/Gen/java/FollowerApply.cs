﻿using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Gen.java
{
    public class FollowerApply : Types.Visitor
    {
        readonly StreamWriter sw;
        readonly Types.Variable var;
        readonly string prefix;

        public static void Make(Types.Bean bean, StreamWriter sw, string prefix)
        {
            foreach (var v in bean.Variables)
            {
                if (!v.Transient && v.VariableType is Types.BeanKey)
                {
                    sw.WriteLine(prefix + "@SuppressWarnings(\"unchecked\")");
                    break;
                }
            }
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void FollowerApply(Zeze.Transaction.Log log) {");
            sw.WriteLine(prefix + "    var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();");
            sw.WriteLine(prefix + "    if (vars == null)");
            sw.WriteLine(prefix + "        return;");
            sw.WriteLine(prefix + "    for (var it = vars.iterator(); it.moveToNext(); ) {");
            sw.WriteLine(prefix + "        var vlog = it.value();");
            sw.WriteLine(prefix + "        switch (vlog.getVariableId()) {");
            foreach (var v in bean.Variables)
            {
                if (v.Transient)
                    continue;
                v.VariableType.Accept(new FollowerApply(v, sw, prefix + "        "));
            }
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(Types.TypeBool type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).Value; break;");
        }

        public void Visit(Types.TypeByte type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).Value; break;");
        }

        public void Visit(Types.TypeShort type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).Value; break;");
        }

        public void Visit(Types.TypeInt type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).Value; break;");
        }

        public void Visit(Types.TypeLong type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).Value; break;");
        }

        public void Visit(Types.TypeFloat type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).Value; break;");
        }

        public void Visit(Types.TypeDouble type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).Value; break;");
        }

        public void Visit(Types.TypeBinary type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).Value; break;");
        }

        public void Visit(Types.TypeString type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).Value; break;");
        }

        public void Visit(Types.TypeList type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.FollowerApply(vlog); break;");
        }

        public void Visit(Types.TypeSet type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.FollowerApply(vlog); break;");
        }

        public void Visit(Types.TypeMap type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.FollowerApply(vlog); break;");
        }

        public void Visit(Types.Bean type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.FollowerApply(vlog); break;");
        }

        public void Visit(Types.BeanKey type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).Value; break;");
        }

        public void Visit(Types.TypeDynamic type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate}.FollowerApply(vlog); break;");
        }

        public void Visit(Types.TypeQuaternion type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).Value; break;");
        }

        public void Visit(Types.TypeVector2 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).Value; break;");
        }

        public void Visit(Types.TypeVector2Int type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).Value; break;");
        }

        public void Visit(Types.TypeVector3 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).Value; break;");
        }

        public void Visit(Types.TypeVector3Int type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).Value; break;");
        }

        public void Visit(Types.TypeVector4 type)
        {
            sw.WriteLine(prefix + $"    case {var.Id}: {var.NamePrivate} = (({Property.GetLogName(type)})vlog).Value; break;");
        }

        public FollowerApply(Types.Variable var, StreamWriter sw, string prefix)
        {
            this.var = var;
            this.sw = sw;
            this.prefix = prefix;
        }
    }
}
