﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Gen.Types
{
	public class DynamicParams
	{
		public string Base { get; set; }
		public HashSet<string> Beans { get; } = new();
		public string GetSpecialTypeIdFromBean { get; set; }
		public string CreateBeanFromSpecialTypeId { get; set; }
		public string GetSpecialTypeIdFromBean_Csharp => Program.Upper1LastName(GetSpecialTypeIdFromBean.Replace("::", "."));
        public string CreateBeanFromSpecialTypeId_Csharp => Program.Upper1LastName(CreateBeanFromSpecialTypeId.Replace("::", "."));
    }
}
