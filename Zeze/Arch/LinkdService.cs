﻿using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.LinkdBase;
using Zeze.Builtin.Provider;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Util;

namespace Zeze.Arch
{
    public class LinkdService : Zeze.Services.HandshakeServer
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public LinkdApp LinkdApp { get; set; }

        public LinkdService(string name, Application zeze)
            : base(name, zeze)
        {
            StableLinkSids = new ConcurrentLruLike<StableLinkSidKey, StableLinkSid>(1000000, TryLruRemove);
        }

        private void ReportError(Dispatch dispatch)
        {
            // 如果是 rpc.request 直接返回Procedure.Busy错误。
            // see Zeze.Net.Rpc.decode/encode
            var bb = ByteBuffer.Wrap(dispatch.Argument.ProtocolData);
            var compress = bb.ReadInt();
            var familyClass = compress & FamilyClass.FamilyClassMask;
            var isRequest = familyClass == FamilyClass.Request;
            if (isRequest)
            {
                if ((compress & FamilyClass.BitResultCode) != 0)
                    bb.ReadLong();
                var sessionId = bb.ReadLong();
                // argument 忽略，必须要解析出来，也不知道是什么。

                // 开始响应rpc.response.
                // 【注意】复用了上面的变量 bb，compress。
                compress = FamilyClass.Response;
                compress |= FamilyClass.BitResultCode;
                bb = ByteBuffer.Allocate();
                bb.WriteInt(compress);
                bb.WriteLong(ResultCode.Busy);
                bb.WriteLong(sessionId);
                EmptyBean.Instance.Encode(bb); // emptyBean对应任意bean的默认值状态。
                var so = GetSocket(dispatch.Argument.LinkSid);
                if (null != so)
                    so.Send(bb.Bytes, bb.ReadIndex, bb.Size);
            }
            // 报告服务器繁忙，但不关闭连接。
            ReportError(dispatch.Argument.LinkSid, BReportError.FromLink, BReportError.CodeProviderBusy, "provider is busy.", false);
        }

        public void ReportError(long linkSid, int from, int code, string desc)
        {
            ReportError(linkSid, from, code, desc, true);
        }

        public void ReportError(long linkSid, int from, int code, string desc, bool closeLink)
        {
            var link = this.GetSocket(linkSid);
            if (null != link)
            {
                var error = new ReportError();
                error.Argument.From = from;
                error.Argument.Code = code;
                error.Argument.Desc = desc;
                error.Send(link);

                switch (from)
                {
                    case BReportError.FromLink:
                        switch (code)
                        {
                            case BReportError.CodeNoProvider:
                                // 没有服务时，不断开连接，允许客户端重试。
                                return;
                        }
                        break;

                    case BReportError.FromProvider:
                        break;
                }
                // 延迟关闭。等待客户端收到错误以后主动关闭，或者超时。
                // 虽然使用了写完关闭(CloseGracefully)方法，但是等待一下，尽量让客户端主动关闭，有利于减少 TCP_TIME_WAIT?
                if (closeLink)
                    Scheduler.Schedule((ThisTask) => this.GetSocket(linkSid)?.CloseGracefully(), 2000);
            }
        }

        class StableLinkSidKey
        {
            // 同一个账号同一个ClientId只允许一个登录。
            // ClientId 可能的分配方式：每个手机Client分配一个，所有电脑Client分配一个。
            public string Account { get; }
            public string ClientId { get; }

            public StableLinkSidKey(string account, string clientId)
            {
                Account = account;
                ClientId = clientId;
            }

            public override int GetHashCode()
            {
                const int _prime_ = 31;
                int _h_ = 0;
                _h_ = _h_ * _prime_ + Account.GetHashCode();
                _h_ = _h_ * _prime_ + ClientId.GetHashCode();
                return _h_;
            }

            public override bool Equals(object obj)
            {
                if (obj == this)
                    return true;
                if (obj is StableLinkSidKey other)
                {
                    return Account.Equals(other.Account) && ClientId.Equals(other.ClientId);
                }
                return false;
            }
        }

        public class StableLinkSid
        {
            public bool Removed { get; set; } = false;
            public long LinkSid { get; set; }
            public AsyncSocket AuthedSocket { get; set; }
        }

        private ConcurrentLruLike<StableLinkSidKey, StableLinkSid> StableLinkSids { get; set; }

        private bool TryLruRemove(StableLinkSidKey key, StableLinkSid value)
        {
            if (StableLinkSids.TryRemove(key, out var exist))
            {
                exist.Removed = true;
            }
            return true;
        }

        private void SetStableLinkSid(string account, string clientId, AsyncSocket client)
        {
            var key = new StableLinkSidKey(account, clientId);
            while (true)
            {
                var stable = StableLinkSids.GetOrAdd(key, (_) => new StableLinkSid());
                lock (stable)
                {
                    if (stable.Removed)
                        continue;

                    if (stable.AuthedSocket == client) // same client
                        return;

                    // Must Close Before Reuse LinkSid
                    stable.AuthedSocket?.Close(null);
                    if (stable.LinkSid != 0)
                    {
                        // Reuse Old LinkSid
                        client.SetSessionId(stable.LinkSid);
                    }
                    else
                    {
                        // first client
                        stable.LinkSid = client.SessionId;
                    }
                    stable.AuthedSocket = client;
                    //(client.UserState as LinkSession).StableLinkSid = stable;
                }
            }
        }

        private bool TryReportError(LinkdUserSession linkSession, int moduleId, Dispatch dispatch)
        {
            var pms = LinkdApp.LinkdProvider.GetProviderModuleState(moduleId);
            if (null == pms)
                return false;
            if (pms.ConfigType == BModule.ConfigTypeDynamic)
            {
                ReportError(linkSession.SessionId, BReportError.FromLink, BReportError.CodeNoProvider,
                        "no provider: " + moduleId + ", " + dispatch.ProtocolId);
                // 此后断开连接，不再继续搜索，返回true
                return true;
            }
            return false;
        }

        public bool FindSend(LinkdUserSession linkSession, int moduleId, Dispatch dispatch)
        {
            if (linkSession.TryGetProvider(moduleId, out var provider))
            {
                var socket = LinkdApp.LinkdProviderService.GetSocket(provider);
                if (null == socket)
                    return TryReportError(linkSession, moduleId, dispatch);

                var ps = (LinkdProviderSession)socket.UserState;
                if (ps.Load.Overload == BLoad.eOverload)
                {
                    // 过载时会直接拒绝请求以及报告错误。
                    ReportError(dispatch);
                    // 但是不能继续派发了。所以这里返回true，表示处理完成。
                    return true;
                }

                if (socket.Send(dispatch))
                    return true;

                return TryReportError(linkSession, moduleId, dispatch);
            }
            return false;
        }

        public override void DispatchUnknownProtocol(
            Zeze.Net.AsyncSocket so,
            int moduleId,
            int protocolId,
            Zeze.Serialize.ByteBuffer data)
        {
            var linkSession = so.UserState as LinkdUserSession;
            if (null == linkSession || string.IsNullOrEmpty(linkSession.Account))
            {
                ReportError(so.SessionId, BReportError.FromLink, BReportError.CodeNotAuthed, "not authed.");
                return;
            }

            if (moduleId == global::Zeze.Game.Online.ModuleId && protocolId == global::Zeze.Builtin.Game.Online.Login.ProtocolId_)
            {
                var login = new global::Zeze.Builtin.Game.Online.Login();
                login.Decode(Serialize.ByteBuffer.Wrap(data));
                SetStableLinkSid(linkSession.Account, login.Argument.RoleId.ToString(), so);
            }
            else if (moduleId == global::Zeze.Arch.Online.ModuleId && protocolId == global::Zeze.Builtin.Online.Login.ProtocolId_)
            {
                var login = new global::Zeze.Builtin.Online.Login();
                login.Decode(Serialize.ByteBuffer.Wrap(data));
                SetStableLinkSid(linkSession.Account, login.Argument.ClientId, so);
            }

            var dispatch = new Dispatch();
            dispatch.Argument.LinkSid = so.SessionId;
            dispatch.Argument.Account = linkSession.Account;
            dispatch.Argument.ProtocolType = Protocol.MakeTypeId(moduleId, protocolId);
            dispatch.Argument.ProtocolData = new Zeze.Net.Binary(data);
            dispatch.Argument.Context = linkSession.Context;
            dispatch.Argument.Contextx = linkSession.Contextx;

            if (FindSend(linkSession, moduleId, dispatch))
                return;

            if (LinkdApp.LinkdProvider.ChoiceProviderAndBind(moduleId, so, out var provider))
            {
                var providerSocket = LinkdApp.LinkdProviderService.GetSocket(provider);
                if (null != providerSocket)
                {
                    // ChoiceProviderAndBind 内部已经处理了绑定。这里只需要发送。
                    providerSocket.Send(dispatch);
                    return;
                }
                // 找到provider但是发送之前连接关闭，当作没有找到处理。这个窗口很小，再次查找意义不大。
            }
            ReportError(so.SessionId, BReportError.FromLink, BReportError.CodeNoProvider, "no provider.");
        }

        public override void DispatchProtocol(Zeze.Net.Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (null != factoryHandle.Handle)
            {
                _ = Mission.CallAsync(factoryHandle.Handle, p);
            }
            else
            {
                logger.Log(SocketOptions.SocketLogLevel, "Protocol Handle Not Found. {0}", p);
                p.Sender.Close(null);
            }
        }

        public override void OnSocketAccept(AsyncSocket sender)
        {
            sender.UserState = new LinkdUserSession(sender.SessionId);
            base.OnSocketAccept(sender);
        }

        public override void OnSocketClose(Zeze.Net.AsyncSocket so, System.Exception e)
        {
            base.OnSocketClose(so, e);
            var linkSession = so.UserState as LinkdUserSession;
            linkSession?.OnClose(LinkdApp.LinkdProviderService);
        }
    }
}
