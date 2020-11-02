
#include "Protocol.h"

namespace Zeze
{
	namespace Net
	{
		void Protocol::DecodeProtocol(Service * service, const std::shared_ptr<Socket>& sender, Zeze::Serialize::ByteBuffer& bb, ToLua* toLua /*= NULL*/)
		{
			Zeze::Serialize::ByteBuffer os(bb.Bytes, bb.ReadIndex, bb.Size()); // ����һ���µ�ByteBuffer������ȷ���˲��޸�bb������
			while (os.Size() > 0)
			{
				// ���Զ�ȡЭ�����ͺʹ�С
				int type;
				int size;
				int readIndexSaved = os.ReadIndex;

				if (os.Size() >= 8) // protocl header size.
				{
					type = os.ReadInt4();
					size = os.ReadInt4();
				}
				else
				{
					// SKIP! ֻ��Э�鷢�ͱ��ֳɺ�С�İ���Э��ͷ��������ʱ��Żᷢ������쳣�����������ܷ�����
					bb.ReadIndex = readIndexSaved;
					return;
				}

				// ��ǰд����ʵ�������ݲ���֮ǰ�����type���size�Ƿ�̫��
				// ����ȥ��Э�������С��������.���ܵĲ��� SocketOptions.InputBufferMaxProtocolSize ���ơ�
				// �ο� AsyncSocket
				if (size > os.Size())
				{
					// not enough data. try next time.
					bb.ReadIndex = readIndexSaved;
					return;
				}

				Zeze::Serialize::ByteBuffer pbb(os.Bytes, os.ReadIndex, size);
				os.ReadIndex += size;
				Service::ProtocolFactoryHandle factoryHandle;
				if (service->FindProtocolFactoryHandle(type, factoryHandle))
				{
					std::auto_ptr<Protocol> p(factoryHandle.Factory());
					p->Decode(pbb);
					p->Sender = sender;
					p.release()->Dispatch(service, factoryHandle);
					continue;
				}
				// �����ɷ�c++ʵ�֣�Ȼ����luaʵ�֣����UnknownProtocol��
				if (NULL != toLua && toLua->DecodeAndDispatch(service, sender->SessionId, type, pbb))
					continue;
				service->DispatchUnknownProtocol(sender, type, pbb);
			}
			bb.ReadIndex = os.ReadIndex;
		}
	} // namespace Net
} // namespace Zeze