﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Tikv
{
    public struct GoString : IDisposable
    {
        public IntPtr Str { get; set; }
        public long Len { get; set; }

        public GoString(string str)
        {
            var utf8 = Encoding.UTF8.GetBytes(str);
            Len = utf8.Length;
            Str = Marshal.AllocHGlobal(utf8.Length);
            Marshal.Copy(utf8, 0, Str, utf8.Length);
        }

        public void Dispose()
        {
            Marshal.FreeHGlobal(Str);
        }
    }

    public struct GoSlice : IDisposable
    {
        public IntPtr Data { get; set; }
        public long Len { get; set; }
        public long Cap { get; set; }

        public GoSlice(byte [] bytes, int offset, int size)
        {
            Len = size;
            Cap = size;
            Data = Marshal.AllocHGlobal(size);
            Marshal.Copy(bytes, offset, Data, size);
        }

        public GoSlice(int allcateOnly)
        {
            Len = allcateOnly; // 如果是0，传入go时是空的。本来还以为cap此时能被用上。
            Cap = allcateOnly;
            Data = Marshal.AllocHGlobal(allcateOnly);
        }

        public void Dispose()
        {
            Marshal.FreeHGlobal(Data);
        }
    }

    public abstract class Tikv
    {
        public static readonly Tikv Driver = Create();

        private static Tikv Create()
        {
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux))
                return new TikvLinux();
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
                return new TikvWindows();
            /*
            if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX))
                return TikvLinux();
            */
            throw new Exception("unknown platform.");
        }

        public abstract long NewClient(string pdAddrs);
        public abstract void CloseClient(long clientId);
        public abstract long Begin(long clientId);
        public abstract void Commit(long txnId);
        public abstract void Rollback(long txnId);
        public abstract void Put(long txnId, Serialize.ByteBuffer key, Serialize.ByteBuffer value);
        public abstract Serialize.ByteBuffer Get(long txnId, Serialize.ByteBuffer key);
        public abstract void Delete(long txnId, Serialize.ByteBuffer key);
        public abstract long Scan(long txnId, Serialize.ByteBuffer keyprefix, Func<byte[], byte[], bool> callback);

        protected string GetErrorString(long rc, GoSlice outerr)
        {
            if (rc >= 0)
                return string.Empty;
            int len = (int)-rc;
            return Marshal.PtrToStringUTF8(outerr.Data, len);
        }
    }

    // 不同平台复制代码，比引入接口少调用一次函数。
    public sealed class TikvWindows : Tikv
    {
        [DllImport("tikv.dll")]
        private static extern long NewClient(GoString pdAddrs, GoSlice outerr);
        [DllImport("tikv.dll")]
        private static extern long CloseClient(long clientId, GoSlice outerr);
        [DllImport("tikv.dll")]
        private static extern long Begin(long clientId, GoSlice outerr);
        [DllImport("tikv.dll")]
        private static extern long Commit(long txnId, GoSlice outerr);
        [DllImport("tikv.dll")]
        private static extern long Rollback(long txnId, GoSlice outerr);
        [DllImport("tikv.dll")]
        private static extern long Put(long txnId, GoSlice key, GoSlice value, GoSlice outerr);
        [DllImport("tikv.dll")]
        private static extern long Get(long txnId, GoSlice key, GoSlice outvalue, GoSlice outerr);
        [DllImport("tikv.dll")]
        private static extern long Delete(long txnId, GoSlice key, GoSlice outerr);

        [DllImport("tikv.dll")]
        private static extern long Scan(long txnId, GoSlice keyprefix, Walker walker, GoSlice outerr);
        public delegate int Walker(IntPtr key, int keylen, IntPtr value, int valuelen);

        public override long Scan(long txnId, Serialize.ByteBuffer keyprefix, Func<byte[], byte[], bool> callback)
        {
            using var _keyprefix = new GoSlice(keyprefix.Bytes, keyprefix.ReadIndex, keyprefix.Size);
            using var error = new GoSlice(1024);
            long rc = Scan(txnId, _keyprefix,
                new Walker(
                    (key, keylen, value, valuelen) =>
                    {
                        var _key = new byte[keylen];
                        var _value = new byte[valuelen];
                        Marshal.Copy(key, _key, 0, _key.Length);
                        Marshal.Copy(value, _value, 0, _value.Length);
                        return callback(_key, _value) ? 0 : -1;
                    }),
                error);

            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));

            return rc;
        }

        public override long NewClient(string pdAddrs)
        {
            using var _pdAddrs = new GoString(pdAddrs);
            using var error = new GoSlice(1024);
            long rc = NewClient(_pdAddrs, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
            return rc;
        }

        public override void CloseClient(long clientId)
        {
            using var error = new GoSlice(1024);
            long rc = CloseClient(clientId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }

        public override long Begin(long clientId)
        {
            using var error = new GoSlice(1024);
            long rc = Begin(clientId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
            return rc;
        }

        public override void Commit(long txnId)
        {
            using var error = new GoSlice(1024);
            long rc = Commit(txnId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }

        public override void Rollback(long txnId)
        {
            using var error = new GoSlice(1024);
            long rc = Rollback(txnId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }

        public override void Put(long txnId, Serialize.ByteBuffer key, Serialize.ByteBuffer value)
        {
            using var _key = new GoSlice(key.Bytes, key.ReadIndex, key.Size);
            using var _value = new GoSlice(value.Bytes, value.ReadIndex, value.Size);
            using var error = new GoSlice(1024);
            long rc = Put(txnId, _key, _value, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }

        public override Serialize.ByteBuffer Get(long txnId, Serialize.ByteBuffer key)
        {
            int outValueBufferLen = 64 * 1024;
            while (true)
            {
                using var _key = new GoSlice(key.Bytes, key.ReadIndex, key.Size);
                using var error = new GoSlice(1024);
                using var outvalue = new GoSlice(outValueBufferLen);
                long rc = Get(txnId, _key, outvalue, error);
                if (rc < 0)
                {
                    var str = GetErrorString(rc, error);
                    if (str.Equals("key not exist")) // 这是tikv clieng.go 返回的错误。
                        return null;
                    if (str.Equals("ZezeSpecialError: value is nil."))
                        return null;
                    var strBufferNotEnough = "ZezeSpecialError: outvalue buffer not enough. BufferNeed=";
                    if (str.StartsWith(strBufferNotEnough))
                    {
                        outValueBufferLen = int.Parse(str.Substring(strBufferNotEnough.Length));
                        continue;
                    }
                    throw new Exception(str);
                }
                byte[] rcvalue = new byte[rc];
                Marshal.Copy(outvalue.Data, rcvalue, 0, rcvalue.Length);
                return Serialize.ByteBuffer.Wrap(rcvalue);
            }
        }

        public override void Delete(long txnId, Serialize.ByteBuffer key)
        {
            using var _key = new GoSlice(key.Bytes, key.ReadIndex, key.Size);
            using var error = new GoSlice(1024);
            long rc = Delete(txnId, _key, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }
    }

    public sealed class TikvLinux : Tikv
    {
        [DllImport("tikv.so")]
        private static extern long NewClient(GoString pdAddrs, GoSlice outerr);
        [DllImport("tikv.so")]
        private static extern long CloseClient(long clientId, GoSlice outerr);
        [DllImport("tikv.so")]
        private static extern long Begin(long clientId, GoSlice outerr);
        [DllImport("tikv.so")]
        private static extern long Commit(long txnId, GoSlice outerr);
        [DllImport("tikv.so")]
        private static extern long Rollback(long txnId, GoSlice outerr);
        [DllImport("tikv.so")]
        private static extern long Put(long txnId, GoSlice key, GoSlice value, GoSlice outerr);
        [DllImport("tikv.so")]
        private static extern long Get(long txnId, GoSlice key, GoSlice outvalue, GoSlice outerr);
        [DllImport("tikv.so")]
        private static extern long Delete(long txnId, GoSlice key, GoSlice outerr);
        [DllImport("tikv.so")]
        private static extern long Scan(long txnId, GoSlice keyprefix, Walker walker, GoSlice outerr);
        public delegate int Walker(IntPtr key, int keylen, IntPtr value, int valuelen);

        public override long Scan(long txnId, Serialize.ByteBuffer keyprefix, Func<byte[], byte[], bool> callback)
        {
            using var _keyprefix = new GoSlice(keyprefix.Bytes, keyprefix.ReadIndex, keyprefix.Size);
            using var error = new GoSlice(1024);
            long rc = Scan(txnId, _keyprefix,
                new Walker(
                    (key, keylen, value, valuelen) =>
                    {
                        var _key = new byte[keylen];
                        var _value = new byte[valuelen];
                        Marshal.Copy(key, _key, 0, _key.Length);
                        Marshal.Copy(value, _value, 0, _value.Length);
                        return callback(_key, _value) ? 0 : -1;
                    }),
                error);

            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));

            return rc;
        }

        public override long NewClient(string pdAddrs)
        {
            using var _pdAddrs = new GoString(pdAddrs);
            using var error = new GoSlice(1024);
            long rc = NewClient(_pdAddrs, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
            return rc;
        }

        public override void CloseClient(long clientId)
        {
            using var error = new GoSlice(1024);
            long rc = CloseClient(clientId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }

        public override long Begin(long clientId)
        {
            using var error = new GoSlice(1024);
            long rc = Begin(clientId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
            return rc;
        }

        public override void Commit(long txnId)
        {
            using var error = new GoSlice(1024);
            long rc = Commit(txnId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }

        public override void Rollback(long txnId)
        {
            using var error = new GoSlice(1024);
            long rc = Rollback(txnId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }

        public override void Put(long txnId, Serialize.ByteBuffer key, Serialize.ByteBuffer value)
        {
            using var _key = new GoSlice(key.Bytes, key.ReadIndex, key.Size);
            using var _value = new GoSlice(value.Bytes, value.ReadIndex, value.Size);
            using var error = new GoSlice(1024);
            long rc = Put(txnId, _key, _value, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }

        public override Serialize.ByteBuffer Get(long txnId, Serialize.ByteBuffer key)
        {
            int outValueBufferLen = 64 * 1024;
            while (true)
            {
                using var _key = new GoSlice(key.Bytes, key.ReadIndex, key.Size);
                using var error = new GoSlice(1024);
                using var outvalue = new GoSlice(outValueBufferLen);
                long rc = Get(txnId, _key, outvalue, error);
                if (rc < 0)
                {
                    var str = GetErrorString(rc, error);
                    if (str.Equals("key not exist")) // 这是tikv clieng.go 返回的错误。
                        return null;
                    if (str.Equals("ZezeSpecialError: value is nil."))
                        return null;
                    var strBufferNotEnough = "ZezeSpecialError: outvalue buffer not enough. BufferNeed=";
                    if (str.StartsWith(strBufferNotEnough))
                    {
                        outValueBufferLen = int.Parse(str.Substring(strBufferNotEnough.Length));
                        continue;
                    }
                    throw new Exception(str);
                }
                byte[] rcvalue = new byte[rc];
                Marshal.Copy(outvalue.Data, rcvalue, 0, rcvalue.Length);
                return Serialize.ByteBuffer.Wrap(rcvalue);
            }
        }

        public override void Delete(long txnId, Serialize.ByteBuffer key)
        {
            using var _key = new GoSlice(key.Bytes, key.ReadIndex, key.Size);
            using var error = new GoSlice(1024);
            long rc = Delete(txnId, _key, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }
    }
}
