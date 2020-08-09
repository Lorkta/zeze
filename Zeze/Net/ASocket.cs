﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Zeze.Net
{
    public class ASocket : IDisposable
    {
        public const int InitInputBufferCapacity = 1024; // 输入buffer初始化大小和自增长大小
        public const int ThresholdInputBufferCapacity = 16 * 1024; // 输入buffer的容量超过这个数值后，如果可能重置buffer。

        private System.Net.Sockets.Socket _socket;
        private Zeze.Serialize.ByteBuffer _inputBuffer;
        private List<System.ArraySegment<byte>> _outputBufferList = null;
        private List<System.ArraySegment<byte>> _outputBufferListSending = null; // 正在发送的 buffers.

        public Manager Manager { get; private set; }
        public Exception Exception { get; private set; }
        public long SerialNo { get; private set; }
        public System.Net.Sockets.Socket Socket {  get { return _socket; } }

        private static Zeze.Util.AtomicLong SerialNoGen = new Zeze.Util.AtomicLong();

        /// <summary>
        /// for server socket
        /// </summary>
        public ASocket(Manager manager, System.Net.EndPoint localEP, int backlog)
        {
            this.Manager = manager;

            _socket.Bind(localEP);
            _socket.Listen(backlog);
            _socket.BeginAccept(InitInputBufferCapacity, OnAsyncAccept, this);
            this.SerialNo = SerialNoGen.IncrementAndGet();
        }

        /// <summary>
        /// use inner. create when accept;
        /// </summary>
        /// <param name="accepted"></param>
        ASocket(Manager manager, System.Net.Sockets.Socket accepted, byte[] bytes, int bytesTransferred)
        {
            this.Manager = manager;
            _socket = accepted;
            _inputBuffer = Zeze.Serialize.ByteBuffer.Wrap(bytes, 0, bytesTransferred);
            // BeginReceive called in OnAsyncAccept
            this.SerialNo = SerialNoGen.IncrementAndGet();
        }

        /// <summary>
        /// for client socket. connect
        /// </summary>
        /// <param name="host"></param>
        /// <param name="port"></param>
        public ASocket(Manager manager, string host, int port)
        {
            this.Manager = manager;
            _socket = new System.Net.Sockets.Socket(System.Net.Sockets.SocketType.Stream, System.Net.Sockets.ProtocolType.Tcp);
            System.Net.Dns.BeginGetHostAddresses(host, OnAsyncGetHostAddresses, port);
            this.SerialNo = SerialNoGen.IncrementAndGet();
        }

        public void Send(Zeze.Serialize.ByteBuffer bb)
        {
            Send(bb.Bytes, bb.ReadIndex, bb.Size);
        }

        public void Send(byte[] bytes)
        {
            Send(bytes, 0, bytes.Length);
        }

        /// <summary>
        /// 加到发送缓冲区，直接引用bytes数组。不能再修改了。
        /// </summary>
        /// <param name="bytes"></param>
        /// <param name="offset"></param>
        /// <param name="length"></param>
        public void Send(byte[] bytes, int offset, int length)
        {
            lock (this)
            {
                if (null == _outputBufferList)
                    _outputBufferList = new List<ArraySegment<byte>>();
                _outputBufferList.Add(new ArraySegment<byte>(bytes, offset, length));
            }
            BeginSend(0); // must be zero.
        }

        public void Send(string str)
        {
            Send(Encoding.UTF8.GetBytes(str));
        }

        private void OnAsyncAccept(IAsyncResult ar)
        {
            try
            {
                byte[] buffer;
                int bytesTransferred;
                System.Net.Sockets.Socket so = _socket.EndAccept(out buffer, out bytesTransferred, ar);
                ASocket aso = new ASocket(this.Manager, so, buffer, bytesTransferred);
                try
                {
                    this.Manager.OnSocketAccept(aso);
                    this.Manager.OnSocketProcessInputBuffer(aso, aso._inputBuffer);
                    aso.BeginReceive();
                }
                catch (Exception ce)
                {
                    aso.Close(ce);
                }
            }
            catch (Exception e)
            {
                Close(e);
            }
        }

        private void OnAsyncGetHostAddresses(IAsyncResult ar)
        {
            try
            {
                int port = (System.Int32)ar.AsyncState;
                System.Net.IPAddress[] addrs = System.Net.Dns.EndGetHostAddresses(ar);
                _socket.BeginConnect(addrs, port, OnAsyncConnect, this);
            }
            catch (Exception e)
            {
                this.Manager.OnSocketConnectError(this, e);
                Close(e);
            }
        }

        private void OnAsyncConnect(IAsyncResult ar)
        {
            try
            {
                this._socket.EndConnect(ar);
                BeginReceive();
                this.Manager.OnSocketConnected(this);
            }
            catch (Exception e)
            {
                this.Manager.OnSocketConnectError(this, e);
                Close(e);
            }
        }

        private void BeginReceive()
        {
            if (null == _inputBuffer)
            {
                _inputBuffer = new Serialize.ByteBuffer(InitInputBufferCapacity);
            }
            else
            {
                if (_inputBuffer.Size == 0 && _inputBuffer.Capacity > ThresholdInputBufferCapacity)
                    _inputBuffer = new Serialize.ByteBuffer(InitInputBufferCapacity);
                _inputBuffer.Campact();
            }

            _inputBuffer.EnsureWrite(InitInputBufferCapacity);

            byte[] buffer = _inputBuffer.Bytes;
            int offset = _inputBuffer.WriteIndex;
            int size = _inputBuffer.Capacity - _inputBuffer.WriteIndex;
            this._socket.BeginReceive(buffer, offset, size, System.Net.Sockets.SocketFlags.None, OnAsyncBeginReceive, this);
        }

        private void OnAsyncBeginReceive(IAsyncResult ar)
        {
            try
            {
                int received = this._socket.EndReceive(ar);
                if (received > 0)
                {
                    _inputBuffer.WriteIndex += received;
                    if (_inputBuffer.WriteIndex > _inputBuffer.Capacity) // 这个应该不会发生。
                    {
                        Close(new Exception("input buffer overflow."));
                        return;
                    }
                    this.Manager.OnSocketProcessInputBuffer(this, _inputBuffer);
                    BeginReceive();
                }
                else
                {
                    Close(null); // 正常关闭，不设置异常
                }
            }
            catch (Exception e)
            {
                Close(e);
            }
        }

        private void BeginSend(int hasSend)
        {
            lock(this)
            {
                if (hasSend > 0)
                {
                    // 听说 BeginSend 成功回调的时候，所有数据都会被发送，这样的话就可以直接清除_outputBufferSending，而不用这么麻烦。
                    // MUST 下面的条件必须满足，不做判断。
                    // _outputBufferSending != null
                    // _outputBufferSending.Count > 0
                    // sum(_outputBufferSending[i].Count) <= hasSend
                    for (int i = 0; i < _outputBufferListSending.Count; ++i)
                    {
                        int bytesCount = _outputBufferListSending[i].Count;
                        if (hasSend >= bytesCount)
                        {
                            hasSend -= bytesCount;
                            if (hasSend > 0)
                                continue;

                            _outputBufferListSending.RemoveRange(0, i + 1);
                            break;
                        }
                        // 已经发送的数据比数组中的少。
                        ArraySegment<byte> segment = _outputBufferListSending[i];
                        _outputBufferListSending[i] = segment.Slice(hasSend, segment.Count - hasSend);
                        _outputBufferListSending.RemoveRange(0, i);
                        hasSend = 0;
                        break;
                    }
                    if (hasSend > 0)
                        throw new Exception("hasSend too big.");

                    if (_outputBufferListSending.Count == 0)
                    {
                        _outputBufferListSending = _outputBufferList;
                        _outputBufferList = null;
                    }
                    else if (null != _outputBufferList)
                    {
                        _outputBufferListSending.AddRange(_outputBufferList);
                        _outputBufferList = null;
                    }
                    if (null != _outputBufferListSending)
                        _socket.BeginSend(_outputBufferListSending, System.Net.Sockets.SocketFlags.None, OnAsyncSend, this);

                    return;
                }
                
                if (null == _outputBufferListSending && null != _outputBufferList)
                {
                    _outputBufferListSending = _outputBufferList;
                    _outputBufferList = null;
                    _socket.BeginSend(_outputBufferListSending, System.Net.Sockets.SocketFlags.None, OnAsyncSend, this);
                }
            }
        }

        private void OnAsyncSend(IAsyncResult ar)
        {
            try
            {
                int hasSend = _socket.EndSend(ar);
                BeginSend(hasSend);
            }
            catch (Exception e)
            {
                Close(e);
            }
        }

        private void Close(Exception e)
        {
            this.Exception = e;
            Dispose();
        }

        public void Dispose()
        {
            lock(this)
            {
                try
                {
                    _socket?.Dispose();
                    _socket = null;
                    Manager?.OnSocketClose(this, this.Exception);
                    Manager = null;
                }
                catch (Exception)
                {
                    // skip Dispose error
                }
            }
        }
    }
}
