﻿using System.Collections.Generic;
using System.IO;

namespace Zeze.Gen.cs
{
    public class ModuleFormatter
    {
        readonly Project project;
        internal readonly Module module;
        readonly string genDir;
        readonly string srcDir;

        public ModuleFormatter(Project project, Module module, string genDir, string srcDir)
        {
            this.project = project;
            this.module = module;
            this.genDir = genDir;
            this.srcDir = srcDir;
        }

        public void Make()
        {
            MakeInterface();
            MakePartialImplement();
            MakePartialImplementInGen();
        }

        private string GetCollectionLogTemplateName(Types.Type type)
        {
            if (type is Types.TypeList tlist)
            {
                string value = rrcs.TypeName.GetName(tlist.ValueType);
                return "Zeze.Raft.RocksRaft.LogList" + (tlist.ValueType.IsNormalBean ? "2<" : "1<") + value + ">";
            }
            else if (type is Types.TypeSet tset)
            {
                string value = rrcs.TypeName.GetName(tset.ValueType);
                return "Zeze.Raft.RocksRaft.LogSet1<" + value + ">";
            }
            else if (type is Types.TypeMap tmap)
            {
                string key = rrcs.TypeName.GetName(tmap.KeyType);
                string value = rrcs.TypeName.GetName(tmap.ValueType);
                var version = tmap.ValueType.IsNormalBean ? "2<" : "1<";
                return $"Zeze.Raft.RocksRaft.LogMap{version}{key}, {value}>";
            }
            throw new System.Exception();
        }

        public void RegisterRocksTables(StreamWriter sw)
        {
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen) && table.IsRocks)
                {
                    var key = TypeName.GetName(table.KeyType);
                    var value = TypeName.GetName(table.ValueType);
                    var depends = new HashSet<Types.Type>();
                    table.ValueType.Depends(depends);
                    var tlogs = new HashSet<string>();
                    foreach (var dep in depends)
                    {
                        if (!dep.IsCollection)
                            continue;

                        tlogs.Add(GetCollectionLogTemplateName(dep));
                    }
                    foreach (var tlog in tlogs)
                    { 
                        sw.WriteLine($"            rocks.RegisterLog<{tlog}>();");
                    }
                    sw.WriteLine($"            rocks.RegisterTableTemplate<{key}, {value}>(\"{table.Name}\");");
                }
            }
        }

        public void RegisterZezeTables(StreamWriter sw, string zeze = null)
        {
            var zezeVar = string.IsNullOrEmpty(zeze) ? "App.Zeze" : zeze;
            sw.WriteLine("            // register table");
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen) && table.IsRocks == false)
                    sw.WriteLine($"            {zezeVar}.AddTable({zezeVar}.Config.GetTableConf(_{table.Name}.Name).DatabaseName, _{table.Name});");
            }
        }

        public void UnRegisterZezeTables(StreamWriter sw, string zeze = null)
        {
            var zezeVar = string.IsNullOrEmpty(zeze) ? "App.Zeze" : zeze;
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen) && table.IsRocks == false)
                    sw.WriteLine($"            {zezeVar}.RemoveTable({zezeVar}.Config.GetTableConf(_{table.Name}.Name).DatabaseName, _{table.Name});");
            }
        }

        public void RegisterProtocols(StreamWriter sw, string serviceVarName = null)
        {
            sw.WriteLine("            // register protocol factory and handles");
            sw.WriteLine("            var _reflect = new Zeze.Util.Reflect(this.GetType());");
            Service serv = module.ReferenceService;
            if (serv != null)
            {
                var serviceVar = string.IsNullOrEmpty(serviceVarName) ? $"App.{serv.Name}" : serviceVarName;
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        // rpc 可能作为客户端发送也需要factory，所以总是注册factory。
                        sw.WriteLine($"            {serviceVar}.AddFactoryHandle({rpc.TypeId}, new Zeze.Net.Service.ProtocolFactoryHandle()");
                        sw.WriteLine("            {");
                        sw.WriteLine($"                Factory = () => new {rpc.Space.Path(".", rpc.Name)}(),");
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                            sw.WriteLine($"                Handle = Process{rpc.Name}Request,");
                        sw.WriteLine($"                TransactionLevel = _reflect.GetTransactionLevel(\"Process{rpc.Name}Request\", Zeze.Transaction.TransactionLevel.{p.TransactionLevel}),");
                        sw.WriteLine("            });");
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine($"            {serviceVar}.AddFactoryHandle({p.TypeId}, new Zeze.Net.Service.ProtocolFactoryHandle()");
                        sw.WriteLine("            {");
                        sw.WriteLine($"                Factory = () => new {p.Space.Path(".", p.Name)}(),");
                        sw.WriteLine($"                Handle = Process{p.Name},");
                        sw.WriteLine($"                TransactionLevel = _reflect.GetTransactionLevel(\"Process{p.Name}p\", Zeze.Transaction.TransactionLevel.{p.TransactionLevel}),");
                        sw.WriteLine("            });");
                    }
                }
            }
        }

        public void UnRegisterProtocols(StreamWriter sw, string serviceVarName = null)
        {
            Service serv = module.ReferenceService;
            if (serv != null)
            {
                var serviceVar = string.IsNullOrEmpty(serviceVarName) ? $"App.{serv.Name}" : serviceVarName;
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        // rpc 可能作为客户端发送也需要factory，所以总是注册factory。
                        sw.WriteLine($"            {serviceVar}.Factorys.TryRemove({rpc.TypeId}, out var _);");
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine($"            {serviceVar}.Factorys.TryRemove({p.TypeId}, out var _);");
                    }
                }
            }
        }

        public void DefineZezeTables(StreamWriter sw)
        {
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen) && table.IsRocks == false)
                {
                    sw.WriteLine($"        {table.Name} _{table.Name} = new {table.Name}();");
                }
            }
        }

        public void MakePartialImplementInGen()
        {
            using StreamWriter sw = module.OpenWriter(genDir, $"Module{module.Name}Gen.cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine();
            sw.WriteLine("namespace " + module.Path());
            sw.WriteLine("{");
            sw.WriteLine($"    public partial class Module{module.Name} : AbstractModule");
            sw.WriteLine("    {");
            sw.WriteLine($"        public const int ModuleId = {module.Id};");
            sw.WriteLine();
            DefineZezeTables(sw);
            sw.WriteLine();
            sw.WriteLine($"        public {project.Solution.Name}.App App {{ get; }}");
            sw.WriteLine();
            sw.WriteLine($"        public Module{module.Name}({project.Solution.Name}.App app)");
            sw.WriteLine("        {");
            sw.WriteLine("            App = app;");
            RegisterProtocols(sw);
            RegisterZezeTables(sw);
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public override void UnRegister()");
            sw.WriteLine("        {");
            UnRegisterProtocols(sw);
            UnRegisterZezeTables(sw);
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void GenEmptyProtocolHandles(StreamWriter sw, string namePrefix = "", bool shortIf = true)
        {
            if (module.ReferenceService != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                        {
                            sw.WriteLine($"        protected override long Process{namePrefix}" + rpc.Name + "Request(Zeze.Net.Protocol _p)");
                            sw.WriteLine("        {");
                            sw.WriteLine($"            var p = _p as {(shortIf ? rpc.ShortNameIf(module) : rpc.FullName)};");
                            sw.WriteLine("            return Zeze.Transaction.Procedure.NotImplement;");
                            sw.WriteLine("        }");
                            sw.WriteLine();
                        }
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine($"        protected override long Process{namePrefix}" + p.Name + "(Zeze.Net.Protocol _p)");
                        sw.WriteLine("        {");
                        sw.WriteLine($"            var p = _p as {(shortIf ? p.ShortNameIf(module) : p.FullName)};");
                        sw.WriteLine("            return Zeze.Transaction.Procedure.NotImplement;");
                        sw.WriteLine("        }");
                        sw.WriteLine();
                    }
                }
            }
        }

        public void MakePartialImplement()
        {
            using StreamWriter sw = module.OpenWriter(srcDir, $"Module{module.Name}.cs", false);
            if (sw == null)
                return;

            sw.WriteLine();
            sw.WriteLine("namespace " + module.Path());
            sw.WriteLine("{");
            sw.WriteLine($"    public partial class Module{module.Name} : AbstractModule");
            sw.WriteLine("    {");
            sw.WriteLine("        public void Start(" + project.Solution.Name + ".App app)");
            sw.WriteLine("        {");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public void Stop(" + project.Solution.Name + ".App app)");
            sw.WriteLine("        {");
            sw.WriteLine("        }");
            sw.WriteLine();

            GenEmptyProtocolHandles(sw);
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void GenEnums(StreamWriter sw, string namePrefix = "")
        {
            // declare enums
            if (module.Enums.Count > 0)
                sw.WriteLine();
            foreach (Types.Enum e in module.Enums)
                sw.WriteLine($"        public const int {namePrefix}{e.Name} = {e.Value};{e.Comment}");
        }

        public void GenAbstractProtocolHandles(StreamWriter sw, string namePrefix = "")
        {
            if (module.ReferenceService != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                        {
                            sw.WriteLine();
                            sw.WriteLine($"        protected abstract long Process{namePrefix}" + rpc.Name + "Request(Zeze.Net.Protocol p);");
                        }
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine();
                        sw.WriteLine($"        protected abstract long Process{namePrefix}" + p.Name + "(Zeze.Net.Protocol p);");
                    }
                }
            }
        }

        public void MakeInterface()
        {
            using StreamWriter sw = module.OpenWriter(genDir, "AbstractModule.cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine();
            sw.WriteLine("namespace " + module.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public abstract class AbstractModule : Zeze.IModule");
            sw.WriteLine("    {");
            sw.WriteLine($"        public override string FullName => \"{module.Path()}\";");
            sw.WriteLine($"        public override string Name => \"{module.Name}\";");
            sw.WriteLine($"        public override int Id => {module.Id};");
            GenEnums(sw);
            GenAbstractProtocolHandles(sw);
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}
