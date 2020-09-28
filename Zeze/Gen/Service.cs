﻿using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen
{
    public class Service // Zeze.Net.Service
    {
        public Project Project { get; private set; }
        public string Name { get; private set; }
        public string Handle { get; private set; }
        public int HandleFlags { get; }
        public string Base { get; private set; }

        private XmlElement self;

        public string FullName => Project.Solution.Path(".", Name);

        // setup when compile
        public List<Module> Modules { get; private set; }

        private HashSet<Protocol> AllProtocols;
        public HashSet<Protocol> GetAllProtocols()
        {
            if (AllProtocols != null)
                return AllProtocols;
            AllProtocols = new HashSet<Protocol>();
            foreach (Module module in Modules)
            {
                module.Depends(AllProtocols);
            }
            return AllProtocols;
        }

        //public HashSet<Module> AllModules { get; private set; } = new HashSet<Module>();

        public Service(Project project, XmlElement self)
        {
            this.self = self;
            Project = project;
            Name = self.GetAttribute("name").Trim();
            Handle = self.GetAttribute("handle");
            HandleFlags = Program.ToHandleFlags(Handle);
            if (HandleFlags == 0)
                throw new Exception("handle miss. " + Name + " in project " + project.Name);
            Base = self.GetAttribute("base");

            Program.AddNamedObject(FullName, this);

            if (project.Services.ContainsKey(Name))
                throw new Exception("duplicate service " + Name + " in project " + project.Name);
            project.Services.Add(Name, this);

            /*
            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                switch (e.Name)
                {
                    case "module":
                        // ref 对象在编译的时候查找和设置。将保存在 Modules 中。
                        break;
                }
            }
            */
        }

        public void Compile()
        {
            ICollection<string> refs = Program.Refs(self, "module");
            List<string> refFulNames = Program.ToFullNameIfNot(Project.Solution.Name, refs);
            for (int i = 0; i < refFulNames.Count; ++i)
                refFulNames[i] = refFulNames[i] + ".Module";
            Modules = Program.CompileModuleRef(refFulNames);
        }

        public void SetModuleReference()
        {
            foreach (Module m in Modules)
            {
                m.SetReferenceService(this);
            }
        }
    }
}
