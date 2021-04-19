﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Tikv
{
    public class TikvConnection : IDisposable
    {
        public int ClientId { get; }
        public TikvTransaction Transaction { get; private set; }

        public TikvConnection(string databaseUrl)
        {
            // TODO Client Pool ?
            ClientId = Tikv.NewClient(databaseUrl);
        }

        public void Open()
        {
            // 不需要实现，和Sql,Mysql的一致，先保留。
        }

        public void Dispose()
        {
            // 没做事情或者事务成功时，保存到Pool中。其他情况都关闭连接。
            if (null == Transaction || Transaction.CommitDone)
            {
                // TODO pool
                Transaction = null;
            }
            Tikv.CloseClient(ClientId);
        }

        public TikvTransaction BeginTransaction()
        {
            if (null != Transaction)
                throw new Exception("Transaction Has Begin.");
            Transaction = new TikvTransaction(this);
            return Transaction;
        }
    }
}
