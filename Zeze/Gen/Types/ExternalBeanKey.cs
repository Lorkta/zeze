﻿using System;
using System.Collections.Generic;
using System.Xml;

namespace Zeze.Gen.Types
{
    public class ExternalBeanKey : BeanKey
    {
        public override string FullName => _name;

        public ExternalBeanKey(ModuleSpace sol, XmlElement self)
        {
            Space = sol;
            _name = self.GetAttribute("beankey");
            Kind = "beankey";
            Program.CheckReserveFullName(_name, sol.Path());

            if (Types.ContainsKey(_name))
                throw new Exception("duplicate type: " + _name);
            Types.Add(_name, this);
            //Console.WriteLine($"external {_name}");
        }

        public override void Depends(HashSet<Type> includes, string parent)
        {
        }
    }
}
