﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Tikv
{
    public class Test
    {
        public static void RunWrap(string url)
        {
            Console.WriteLine("RunWrap");

            var tikvDb = new DatabaseTikv(url);
            var table = tikvDb.OpenTable("_testtable_");
            var key = Zeze.Serialize.ByteBuffer.Allocate(64);
            key.WriteString("key");
            var value = Zeze.Serialize.ByteBuffer.Allocate(64);
            //value.WriteString("value");

            var outvalue = table.Find(key);
            Console.WriteLine("Find1 " + outvalue);
            tikvDb.Flush(null, () =>
            {
                table.Replace(key, value);
            });
            outvalue = table.Find(key);
            Console.WriteLine("Find2 " + outvalue);
            tikvDb.Flush(null, () =>
            {
                table.Remove(key);
            });
            outvalue = table.Find(key);
            Console.WriteLine("Find3 " + outvalue);
        }

        public static void RunBasic(string url)
        {
            Console.WriteLine("RunBasic");

            var clientId = Tikv.Driver.NewClient(url);
            try
            {
                var txnId = Tikv.Driver.Begin(clientId);
                try
                {
                    var key = Zeze.Serialize.ByteBuffer.Allocate(64);
                    key.WriteString("key");
                    var outvalue = Tikv.Driver.Get(txnId, key);
                    Console.WriteLine("1 " + outvalue);
                    var value = Zeze.Serialize.ByteBuffer.Allocate(64);
                    value.WriteString("value");
                    Tikv.Driver.Put(txnId, key, value);
                    outvalue = Tikv.Driver.Get(txnId, key);
                    Console.WriteLine("2 " + outvalue);
                    Tikv.Driver.Delete(txnId, key);
                    outvalue = Tikv.Driver.Get(txnId, key);
                    Console.WriteLine("3 " + outvalue);
                    Tikv.Driver.Commit(txnId);
                }
                catch (Exception)
                {
                    Tikv.Driver.Rollback(txnId);
                }
            }
            finally
            {
                Tikv.Driver.CloseClient(clientId);
            }
        }

        public static void Run(string url)
        {
            var ptr = Marshal.AllocHGlobal(0);
            if (IntPtr.Zero == ptr)
                Console.WriteLine("++++++++++++");
            Marshal.FreeHGlobal(ptr);
            RunBasic(url);
            RunWrap(url);
        }
    }
}
