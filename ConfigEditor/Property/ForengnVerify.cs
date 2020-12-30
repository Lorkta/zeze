﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ConfigEditor.Property
{
    public class ForengnVerify : IProperty
    {
        public override string Name => "foreign";

        public override string Comment => "";

        private bool ExistData(VerifyParam param, VarDefine foreignVar)
        {
            foreach (var beanData in foreignVar.Parent.Document.Beans)
            {
                Bean.VarData varData = beanData.GetLocalVarData(foreignVar.Name);
                if (null != varData && param.NewValue.Equals(varData.Value))
                    return true;
            }
            return false;
        }

        public override void VerifyCell(VerifyParam param)
        {
            string result = param.ColumnTag.PathLast.Define.OpenForeign(out var foreignVar);
            if (null == result)
            {
                if (ExistData(param, foreignVar))
                    param.FormMain.FormError.RemoveError(param.Cell, this);
                else
                    param.FormMain.FormError.AddError(param.Cell, this, ErrorLevel.Warn, "value not exist in foreign.");
            }
            else
            {
                param.FormMain.FormError.AddError(param.Cell, this, ErrorLevel.Warn, result);
            }
        }
    }
}
