﻿using System;
using System.Collections.Generic;
using System.Text;
using System.Transactions;
using Zeze.Transaction;

namespace Zeze
{
    public class Zeze
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public Dictionary<string, Transaction.Database> Databases { get; private set; } = new Dictionary<string, Transaction.Database>();
        public Config Config { get; set; }
        public bool IsStart { get; private set; }
        internal TableSys TableSys { get; private set; }

        public void AddTable(string dbName, Transaction.Table table)
        {
            if (Databases.TryGetValue(dbName, out var db))
            {
                db.AddTable(table);
                return;
            }
            throw new Exception($"database not found dbName={dbName}");
        }

        public void Start()
        {
            lock (this)
            {
                if (IsStart)
                    return;

                IsStart = true;

                if (null == Config)
                    Config = Config.Load();

                switch (Config.DatabaseType)
                {
                    case Config.DbType.Memory:
                        Databases = new DatabaseMemory();
                        break;
                    case Config.DbType.MySql:
                        Databases = new DatabaseMySql(Config.DatabaseUrl);
                        break;
                    case Config.DbType.SqlServer:
                        Databases = new DatabaseSqlServer(Config.DatabaseUrl);
                        break;
                    default:
                        throw new Exception("unknown database type.");
                }
                // 由于 AutoKey，TableSys需要先打开。
                TableSys = new TableSys();
                storages.Add(TableSys.Open(this, Databases));
                foreach (Transaction.Table table in tables.Values)
                {
                    Transaction.Storage storage = table.Open(this, Databases);
                    if (null != storage)
                        storages.Add(storage);
                }
                AddTable(TableSys);
                if (Config.CheckpointPeriod > 0)
                {
                    Util.Scheduler.Instance.Schedule(Checkpoint, Config.CheckpointPeriod, Config.CheckpointPeriod);
                }
            }
        }

        public void Stop()
        {
            logger.Fatal("final checkpoint start.");
            Checkpoint();
            logger.Fatal("final checkpoint end.");

            lock (this)
            {
                if (false == IsStart)
                    return;

                IsStart = false;
                foreach (Transaction.Table table in tables.Values)
                {
                    table.Close();
                }
                tables.Clear();
                storages.Clear();
                Databases.Close();
                TableSys = null;
                Databases = null;
            }
        }

        public void Checkpoint()
        {
            lock (this)
            {
                if (false == IsStart)
                    return;

                // try Encode. 可以多趟。
                for (int i = 1; i <= 1; ++i)
                {
                    int countEncodeN = 0;
                    foreach (Transaction.Storage storage in storages)
                    {
                        countEncodeN += storage.EncodeN();
                    }
                    logger.Info("Checkpoint EncodeN {0}/{1}", i, countEncodeN);
                }
                // snapshot
                {
                    int countEncode0 = 0;
                    int countSnapshot = 0;
                    Transaction.Transaction.FlushReadWriteLock.EnterWriteLock();
                    try
                    {
                        foreach (Transaction.Storage storage in storages)
                        {
                            countEncode0 += storage.Encode0();
                        }
                        foreach (Transaction.Storage storage in storages)
                        {
                            countSnapshot += storage.Snapshot();
                        }
                    }
                    finally
                    {
                        Transaction.Transaction.FlushReadWriteLock.ExitWriteLock();
                    }

                    logger.Info("Checkpoint Encode0 And Snapshot countEncode0={0} countSnapshot={1}", countEncode0, countSnapshot);
                }

                // flush checkpoint
                Databases.Checkpoint(() =>
                {
                    int countFlush = 0;
                    foreach (Transaction.Storage storage in storages)
                    {
                        countFlush += storage.Flush();
                    }
                    logger.Info("Checkpoint Flush count={0}", countFlush);
                }
                );

                // cleanup
                foreach (Transaction.Storage storage in storages)
                {
                    storage.Cleanup();
                }
            }
        }

        public Zeze()
        {
            var domain = AppDomain.CurrentDomain;
            domain.UnhandledException += UnhandledExceptionEventHandler;
            domain.ProcessExit += ProcessExit;
            // domain.DomainUnload += DomainUnload;
        }

        private void ProcessExit(object sender, EventArgs e)
        {
            Stop();
        }

        private void UnhandledExceptionEventHandler(object sender, UnhandledExceptionEventArgs args)
        {
            Exception e = (Exception)args.ExceptionObject;
            logger.Error(e, "UnhandledExceptionEventArgs");
        }
    }
}
