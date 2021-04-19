﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Zeze.Serialize;
using Zeze.Transaction;

namespace Zeze.Tikv
{
    public sealed class DatabaseTikv : Zeze.Transaction.Database
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        internal TikvConnection CheckpointTikvConnection { get; private set; }
        //internal TikvTransaction Transaction { get; private set; }

        public DatabaseTikv(string databaseUrl) : base(databaseUrl)
        {
            DirectOperates = new OperatesTikv(this);
        }

        public override void Flush(Checkpoint sync, Action flushAction)
        {
            try
            {
                for (int i = 0; i < 60; ++i)
                {
                    using TikvConnection connection = new TikvConnection(DatabaseUrl);
                    connection.Open();
                    CheckpointTikvConnection = connection;
                    var Transaction = connection.BeginTransaction();
                    try
                    {
                        flushAction();
                        if (null != sync) // null for test
                        {
                            CommitReady.Set();
                            sync.WaitAllReady();
                        }
                        Transaction.Commit();
                        return;
                    }
                    catch (Exception ex)
                    {
                        CommitReady.Reset();
                        Transaction.Rollback();
                        logger.Warn(ex, "Checkpoint error.");
                    }
                    Thread.Sleep(1000);
                }
                logger.Fatal("Checkpoint too many try.");
                Environment.Exit(54321);
            }
            finally
            {
                CheckpointTikvConnection = null;
            }
        }

        public override Table OpenTable(string name)
        {
            return new TableTikv(this, name);
        }

        public sealed class OperatesTikv : Zeze.Transaction.Database.Operates
        {
            public OperatesTikv(DatabaseTikv tikv)
            {

            }

            public int ClearInUse(int localId, string global)
            {
                throw new NotImplementedException();
            }

            public (ByteBuffer, long) GetDataWithVersion(ByteBuffer key)
            {
                throw new NotImplementedException();
            }

            public bool SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, ref long version)
            {
                throw new NotImplementedException();
            }

            public void SetInUse(int localId, string global)
            {
                throw new NotImplementedException();
            }
        }

        public sealed class TableTikv : Database.Table
        {
            public DatabaseTikv Database { get; }
            public string Name { get; }
            public ByteBuffer KeyPrefix { get; }

            public TableTikv(DatabaseTikv database, string name)
            {
                Database = database;
                Name = name;
                var nameutf8 = Encoding.UTF8.GetBytes(name);
                KeyPrefix = ByteBuffer.Allocate(nameutf8.Length + 1);
                KeyPrefix.Append(nameutf8);
                KeyPrefix.WriteByte(0);
            }

            public void Close()
            {
            }

            private ByteBuffer WithKeyspace(ByteBuffer key)
            {
                var tikvKey = ByteBuffer.Allocate(KeyPrefix.Size + key.Size);
                tikvKey.Append(KeyPrefix.Bytes, KeyPrefix.ReadIndex, KeyPrefix.Size);
                tikvKey.Append(key.Bytes, key.ReadIndex, key.Size);
                return tikvKey;
            }

            public ByteBuffer Find(ByteBuffer key)
            {
                using TikvConnection connection = new TikvConnection(Database.DatabaseUrl);
                connection.Open();
                TikvTransaction transaction = connection.BeginTransaction();
                return Tikv.Get(transaction.TransactionId, WithKeyspace(key));
            }

            public void Remove(ByteBuffer key)
            {
                Tikv.Delete(Database.CheckpointTikvConnection.Transaction.TransactionId, WithKeyspace(key));
            }

            public void Replace(ByteBuffer key, ByteBuffer value)
            {
                Tikv.Put(Database.CheckpointTikvConnection.Transaction.TransactionId, WithKeyspace(key), value);
            }

            public void Walk(Database.Table.IWalk iw)
            {
                // TODO scan tikv
            }
        }
    }
}

