﻿using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using Zeze.Transaction;

namespace Zeze.Gen
{
    public class Protocol
    {
        public ModuleSpace Space { get; private set; }
        public string Name { get; private set; }

        public string ShortNameIf(ModuleSpace holder)
        {
            return holder == Space ? Name : FullName;
        }

        public int Id { get; private set; }
        public long TypeId => (long)Space.Id << 32 | (Id & 0xffff_ffff);
        public string Argument { get; private set; }
        public string Handle { get; private set; }
        public int HandleFlags { get; }
        public TransactionLevel TransactionLevel { get; } = TransactionLevel.Serializable;
        public List<Types.Enum> Enums { get; private set; } = new List<Types.Enum>();
        public string FullName => Space.Path(".", Name);
        public string Comment { get; private set; }
        public bool UseData { get; private set; } = false;
 
        // setup in compile
        public Types.Type ArgumentType { get; private set; }

        public Protocol(ModuleSpace space, XmlElement self)
        {
            Space = space;
            Name = self.GetAttribute("name").Trim();
            space.Add(this);

            string attr = self.GetAttribute("id");
            Id = attr.Length > 0 ? int.Parse(attr) : Util.FixedHash.Hash32(FullName);
            space.ProtocolIdRanges.CheckAdd(Id);

            Argument = self.GetAttribute("argument");
            Handle = self.GetAttribute("handle");
            HandleFlags = Program.ToHandleFlags(Handle);

            var tlevel = self.GetAttribute("NoProcedure"); // 兼容旧的配置
            if (tlevel.Length > 0)
            {
                if ("true".Equals(tlevel))
                    TransactionLevel = TransactionLevel.None;
                else if ("false".Equals(tlevel))
                    TransactionLevel = TransactionLevel.Serializable;
                else
                    TransactionLevel = (TransactionLevel)Enum.Parse(typeof(TransactionLevel), tlevel);
            }
            else
            {
                tlevel = self.GetAttribute("TransactionLevel");
                if (tlevel.Length > 0)
                    TransactionLevel = (TransactionLevel)Enum.Parse(typeof(TransactionLevel), tlevel);
                else if (false == string.IsNullOrEmpty(space.DefaultTransactionLevel))
                    TransactionLevel = (TransactionLevel)Enum.Parse(typeof(TransactionLevel), space.DefaultTransactionLevel);
            }

            attr = self.GetAttribute("UseData");
            if (false == string.IsNullOrEmpty(attr))
                UseData = bool.Parse(attr);

            Comment = Types.Bean.GetComment(self);

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;

                string nodename = e.Name;
                switch (e.Name)
                {
                    case "enum":
                        Add(new Types.Enum(e));
                        break;
                    default:
                        throw new Exception("node=" + nodename);
                }
            }
        }

        public void Add(Types.Enum e)
        {
            Enums.Add(e); // check duplicate
        }

        public virtual void Compile()
        {
            ArgumentType = Argument.Length > 0 ? Types.Type.Compile(Space, Argument) : null;
        }

        public virtual void Depends(HashSet<Types.Type> depends)
        {
            ArgumentType?.Depends(depends);
        }
    }
}
