
#include "ToLua.h"
#include "Net.h"
#include "Protocol.h"

namespace Zeze
{
	namespace Net
	{
		int ToLua::ZezeSendProtocol(lua_State* luaState)
        {
            LuaHelper lua(luaState);
            Service* service = lua.ToObject<Service*>(-3);
            long long sessionId = lua.ToInteger(-2);
            std::shared_ptr<Socket> socket = service->GetSocket();
            if (NULL == socket.get())
                return 0;
            service->ToLua.SendProtocol(socket.get());
            return 0;
        }

        int ToLua::ZezeUpdate(lua_State* luaState)
        {
            LuaHelper lua(luaState);
            Service* service = lua.ToObject<Service*>(-1);
            service->Helper.Update(service, service->ToLua);
            return 0;
        }

        void ToLua::SendProtocol(Socket* socket)
        {
            if (false == Lua.IsTable(-1))
                throw std::exception("SendProtocol param is not a table.");

            Lua.GetField(-1, "TypeId");
            int typeId = (int)Lua.ToInteger(-1);
            Lua.Pop(1);
            Lua.GetField(-1, "ResultCode");
            int resultCode = (int)Lua.ToInteger(-1);
            Lua.Pop(1);

            ProtocolMetasMap::iterator pit = ProtocolMetas.find(typeId);
            if (pit == ProtocolMetas.end())
                throw std::exception("protocol not found in meta for typeid=" + typeId);
            long long argumentBeanTypeId = pit->second;

            // see Protocol.Encode
            Zeze::Serialize::ByteBuffer bb(1024);
            bb.WriteInt4(typeId);
            int outstate;
            bb.BeginWriteWithSize4(outstate);
            bb.WriteInt(resultCode);
            Lua.GetField(-1, "Argument");
            EncodeBean(bb, argumentBeanTypeId);
            Lua.Pop(1);
            bb.EndWriteWithSize4(outstate);
            socket->Send(bb.Bytes, bb.ReadIndex, bb.Size());
        }

        void Helper::Update(Service* service, ToLua& toLua)
        {
            ToLuaHandshakeDoneMap handshakeTmp;
            ToLuaBufferMap inputTmp;
            {
                std::lock_guard<std::mutex> lock(mutex);
                handshakeTmp.swap(ToLuaHandshakeDone);
                inputTmp.swap(ToLuaBuffer);
            }

            for (auto& e : handshakeTmp)
            {
                toLua.CallHandshakeDone(e.second, e.first);
            }

            for (auto& e : inputTmp)
            {
                std::shared_ptr<Socket> sender = service->GetSocket(e.first);
                if (NULL == sender.get())
                    continue;
                Zeze::Serialize::ByteBuffer bb((char *)e.second.data(), 0, e.second.size());
                Protocol::DecodeProtocol(service, sender, bb, &toLua);
                e.second.erase(0, bb.ReadIndex);
            }

            {
                std::lock_guard<std::mutex> lock(mutex);
                for (auto & e : inputTmp)
                {
                    if (e.second.empty())
                        continue; // ����ȫ��������ɡ�

                    ToLuaBufferMap::iterator bit = ToLuaBuffer.find(e.first);
                    if (bit != ToLuaBuffer.end())
                    {
                        // ����������������ݵ������ӵ���ǰʣ�����ݺ��棬Ȼ�󸲸ǵ�buffer��
                        e.second.append(bit->second);
                        ToLuaBuffer[e.first] = e.second;
                    }
                    else
                    {
                        // û�������ݵ�������ʣ�࣬�ӻ�ȥ����һ��update�ٴ���
                        ToLuaBuffer[e.first] = e.second;
                    }
                }
            }
        }

        void ToLua::RegisterGlobalAndCallback(Service * service)
        {
            if (Lua.DoString("local Zeze = require 'Zeze'\nreturn Zeze"))
                throw std::exception("load Zeze.lua faild");
            if (false == Lua.IsTable(-1))
                throw std::exception("Zeze.lua not return a table");

            Lua.PushString(std::string("Service") + service->Name());
            Lua.PushObject(service);
            Lua.SetTable(-3);
            Lua.PushString("CurrentService");
            Lua.PushObject(service);
            Lua.SetTable(-3); // �����ڶ��serviceʱ�����ﱣ�����һ����

            // ��һ��������Service��ȫ�ֱ�����������һ��ע���ȥ�ġ�
            // void ZezeUpdate(ZezeService##Name)
            Lua.Register("ZezeUpdate", ZezeUpdate);
            // �� Protocol �� lua ���ɴ�����ã����� sesionId ��ȫ�ֱ��� ZezeCurrentSessionId �ж�ȡ��
            // ���ڿͻ��ˣ����� HandshakeDone �Ժ󱣴� sessionId �� ZezeCurrentSessionId �У��Ժ����������á�
            // ���ڷ����������� HandshakeDone �Ժ󱣴� sessionId �Լ��Ľṹ�У�����ǰ��Ҫ�ѵ�ǰ�������õ� ZezeCurrentSessionId �С� 
            // void ZezeSendProtocol(ZezeService##Name, sessionId, protocol)
            Lua.Register("ZezeSendProtocol", ZezeSendProtocol);
        }
    }
}