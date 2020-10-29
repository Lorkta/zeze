
#include "common.h"
#include "Net.h"
#include "Protocol.h"
#include <iostream>
#include "ByteBuffer.h"
#include "security.h"
#include "rfc2118.h"
#include <unordered_set>

namespace Zeze
{
namespace Net
{
	class CHandshakeArgument : public Zeze::Serialize::Serializable
	{
	public:
		char dh_group = 0;
		std::string dh_data;

		void Decode(Zeze::Serialize::ByteBuffer& bb) override
		{
			dh_group = bb.ReadByte();
			dh_data = bb.ReadBytes();
		}

		void Encode(Zeze::Serialize::ByteBuffer& bb) override
		{
			bb.WriteByte(dh_group);
			bb.WriteBytes(dh_data);
		}
	};

	class SHandshakeArgument : public Zeze::Serialize::Serializable
	{
	public:
		std::string dh_data;
		bool s2cneedcompress = true;
		bool c2sneedcompress = true;

		void Decode(Zeze::Serialize::ByteBuffer& bb) override
		{
			dh_data = bb.ReadBytes();
			s2cneedcompress = bb.ReadBool();
			c2sneedcompress = bb.ReadBool();
		}

		void Encode(Zeze::Serialize::ByteBuffer& bb) override
		{
			bb.WriteBytes(dh_data);
			bb.WriteBool(s2cneedcompress);
			bb.WriteBool(c2sneedcompress);
		}
	};

	class CHandshake : public ProtocolWithArgument<CHandshakeArgument>
	{
	public:
		int ModuleId() override { return 0; }
		int ProtocolId() override { return 1; }

		CHandshake()
		{
		}

		CHandshake(char dh_group, const std::string& dh_data)
		{
			Argument.dh_group = dh_group;
			Argument.dh_data = dh_data;
		}
	};

	class SHandshake : public ProtocolWithArgument<SHandshakeArgument>
	{
	public:
		int ModuleId() override { return 0; }
		int ProtocolId() override { return 2; }

		SHandshake()
		{
		}

		SHandshake(const std::string& dh_data, bool s2cneedcompress, bool c2sneedcompress)
		{
			Argument.dh_data = dh_data;
			Argument.s2cneedcompress = s2cneedcompress;
			Argument.c2sneedcompress = c2sneedcompress;
		}
	};

	Service::Service(const std::string& _name)
		: name(_name), socket(NULL)
	{
		SHandshake forTypeId;
		AddProtocolFactory(forTypeId.TypeId(), Zeze::Net::Service::ProtocolFactoryHandle(
			[]() { return new SHandshake(); }, std::bind(&Service::ProcessSHandshake, this, std::placeholders::_1)));
	}

	int Service::ProcessSHandshake(Protocol* _p)
	{
		SHandshake* p = (SHandshake*)_p;

		const std::vector<unsigned char> material = dhContext->computeDHKey((unsigned char*)p->Argument.dh_data.data(), (int32_t)p->Argument.dh_data.size());
		socklen_t key_len = p->Sender->LastAddress.size();
		int8_t* key = (int8_t*)p->Sender->LastAddress.data();
		int32_t half = (int32_t)material.size() / 2;
		{
			limax::HmacMD5 hmac(key, 0, key_len);
			hmac.update((int8_t*)&material[0], 0, half);
			p->Sender->SetOutputSecurity(p->Argument.c2sneedcompress, hmac.digest(), 16);
		}
		{
			limax::HmacMD5 hmac(key, 0, key_len);
			hmac.update((int8_t*)&material[0], half, (int32_t)material.size() - half);
			p->Sender->SetInputSecurity(p->Argument.s2cneedcompress, hmac.digest(), 16);
		}
		dhContext.reset();
		OnHandshakeDone(p->Sender);
		return 0;
	}

	class BufferedCodec : public limax::Codec
	{
	public:
		std::string buffer;

		BufferedCodec() { }

		virtual void update(int8_t c) override
		{
			buffer.append(1, (char)c);
		}

		virtual void update(int8_t data[], int32_t off, int32_t len) override
		{
			buffer.append((const char*)(data + off), len);
		}

		virtual void flush() override
		{
		}
	};

	void Socket::SetOutputSecurity(bool c2sneedcompress, const int8_t* key, int keylen)
	{
		std::shared_ptr<limax::Codec> codec = OutputBuffer;
		if (keylen > 0)
		{
			codec = std::shared_ptr<limax::Codec>(new limax::Encrypt(codec, (int8_t*)key, (int32_t)keylen));
		}
		if (c2sneedcompress)
		{
			codec = std::shared_ptr<limax::Codec>(new limax::RFC2118Encode(codec));
		}
		std::lock_guard<std::mutex> scoped(mutex);
		OutputCodec = codec;
	}

	void Socket::SetInputSecurity(bool s2cneedcompress, const int8_t* key, int keylen)
	{
		std::shared_ptr<limax::Codec> codec(InputBuffer);
		if (s2cneedcompress)
		{
			codec = std::shared_ptr<limax::Codec>(new limax::RFC2118Decode(codec));
		}
		if (keylen > 0)
		{
			codec = std::shared_ptr<limax::Codec>(new limax::Decrypt(codec, (int8_t*)key, (int32_t)keylen));
		}
		std::lock_guard<std::mutex> scoped(mutex);
		InputCodec = codec;
	}

	Service::~Service()
	{
	}

	void Service::InitializeLua(lua_State* L)
	{
		ToLua.LoadMeta(L);
		ToLua.RegisterGlobalAndCallback(this);
	}

	void Service::OnSocketClose(const std::shared_ptr<Socket>& sender, const std::exception* e)
	{
		if (e)
			std::cout << e->what() << std::endl;

		if (sender.get() == socket.get())
		{
			socket.reset();
		}
	}

	void Service::OnHandshakeDone(const std::shared_ptr<Socket>& sender)
	{
		sender->IsHandshakeDone = true;
		Helper.SetHandshakeDone(sender->SessionId, this);
	}

	void Service::OnSocketConnectError(const std::shared_ptr<Socket>& sender, const std::exception* e)
	{
		if (e)
			std::cout << e->what() << std::endl;
	}

	void Service::OnSocketConnected(const std::shared_ptr<Socket>& sender)
	{
		if (socket.get())
		{
			socket->Close(NULL);
		}
		socket = sender;
		dhContext = limax::createDHContext(dhGroup);
		const std::vector<unsigned char> dhResponse = dhContext->generateDHResponse();
		CHandshake hand(dhGroup, std::string((const char *)&dhResponse[0], dhResponse.size()));
		hand.Send(sender.get());
	}

	Protocol* Service::CreateProtocol(int typeId, Zeze::Serialize::ByteBuffer& os)
	{
		ProtocolFactoryMap::iterator it = ProtocolFactory.find(typeId);
		if (it != ProtocolFactory.end())
		{
			std::auto_ptr<Protocol> p(it->second.Factory());
			p->Decode(os);
			return p.release();
		}
		return NULL;
	}

	void Service::DispatchProtocol(Protocol* p)
	{
		std::auto_ptr<Protocol> at(p);

		ProtocolFactoryMap::iterator it = ProtocolFactory.find(p->TypeId());
		if (it != ProtocolFactory.end())
		{
			it->second.Handle(p);
		}
	}

	void Service::OnSocketProcessInputBuffer(const std::shared_ptr<Socket>& sender, Zeze::Serialize::ByteBuffer& input)
	{
		if (sender->IsHandshakeDone)
		{
			Helper.AppendInputBuffer(sender->SessionId, input);
			input.ReadIndex = input.WriteIndex;
		}
		else
		{
			try
			{
				Protocol::DecodeProtocol(this, sender, input);
			}
			catch (std::exception& ex)
			{
				sender->Close(&ex);
			}
		}
	}

	void Service::DispatchUnknownProtocol(const std::shared_ptr<Socket>& sender, int typeId, Zeze::Serialize::ByteBuffer& data)
	{
	}

	void Service::Connect(const std::string& host, int port, int timeoutSecondsPerConnect)
	{
		std::thread([this, host, port, timeoutSecondsPerConnect]
			{
				Socket* sptr = new Socket(this);
				std::shared_ptr<Socket> at = sptr->This;
				try
				{
					if (at->Connect(host, port, lastSuccessAddress, timeoutSecondsPerConnect))
					{
						lastSuccessAddress = at->LastAddress;
						return;
					}
					// ����ʧ�ܣ��ڲ��Ѿ�����Close�ͷ�shared_ptr��
				}
				catch (...)
				{
					at->Close(NULL); // XXX �쳣��ʱ����Ҫ�ֶ��ͷ�Socket�ڲ���shared_ptr��
				}
			}).detach();
	}

	Socket::Socket(Service* svr)
		: service(svr), This(this)
	{
		IsHandshakeDone = false;
		SessionId = NextSessionId();

		OutputBuffer.reset(new BufferedCodec());
		InputBuffer.reset(new BufferedCodec());
	}

	/// <summary>
	/// ����ʹ��ϵͳsocket-apiʵ���������������������ʹ����ͨapiƽ̨��ء�
	/// </summary>

	class Selector
	{
	public:
		static Selector* Instance;

		static const int OpRead = 1;
		static const int OpWrite = 2;
		static const int OpClose = 4;

		void Select(const std::shared_ptr<Socket>& sock, int add, int remove)
		{
			{
				std::lock_guard<std::mutex> g(mutex);
				sockets.insert(sock);
			}
			{
				std::lock_guard<std::mutex> g(sock->mutex);
				int oldFlags = sock->selectorFlags;
				int newFlags = (oldFlags & ~remove) | add;
				if (oldFlags != newFlags)
				{
					sock->selectorFlags = newFlags;
					Wakeup();
				}
			}
		}

		int wakeupfds[2];
		bool loop = true;
		std::thread * worker;
		std::unordered_set<std::shared_ptr<Socket>> sockets;
		std::mutex mutex;

		Selector()
		{
			pipe(wakeupfds);
			worker = new std::thread(std::bind(&Selector::Loop, this));
		}

		~Selector()
		{
			loop = false;
			worker->join();
			delete worker;

			for (auto& socket : sockets)
				socket->Close(NULL);
			sockets.clear();
		}

		void Loop()
		{
			while (loop)
			{
				fd_set setwrite, setread;
				FD_ZERO(&setwrite);
				FD_ZERO(&setread);

				std::vector<std::shared_ptr<Socket>> closing;
				int maxfd = 0;
				for (auto& socket : sockets)
				{
					if (socket->selectorFlags & OpClose)
					{
						closing.push_back(socket);
						continue;
					}

					if (socket->socket > maxfd)
						maxfd = socket->socket;

					if (socket->selectorFlags & OpRead)
						FD_SET(socket->socket, &setread);
					if (socket->selectorFlags & OpWrite)
						FD_SET(socket->socket, &setwrite);
				}
				FD_SET(wakeupfds[0], &setread); // wakeup fd

				for (auto& close : closing)
					sockets.erase(close);
				closing.clear();

				struct timeval timeout = { 0 };
				timeout.tv_sec = 1;
				timeout.tv_usec = 0;

				if (::select(maxfd + 1, &setread, &setwrite, NULL, &timeout) > 0)
				{
					if (FD_ISSET(wakeupfds[0], &setread))
					{
						char buf[16];
						::recv(wakeupfds[0], buf, sizeof(buf), 0);
					}
					for (auto& socket : sockets)
					{
						if (FD_ISSET(socket->socket, &setread))
							socket->OnRecv();
						if (FD_ISSET(socket->socket, &setwrite))
							socket->OnSend();
					}
				}
			}
		}

		void Wakeup()
		{
			::send(wakeupfds[1], " ", 1, 0);
		}

		// �ͻ��˲���Ҫ�������ӣ���ʵ��һ������ʹ��select�İ汾��
		// ����Ҫ��ʵ�������汾��
#ifdef LIMAX_OS_WINDOWS
		int pipe(int fildes[2])
		{
			int tcp1, tcp2;
			sockaddr_in name;
			memset(&name, 0, sizeof(name));
			name.sin_family = AF_INET;
			name.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
			int namelen = sizeof(name);
			tcp1 = tcp2 = -1;
			int tcp = socket(AF_INET, SOCK_STREAM, 0);
			if (tcp == -1) {
				goto clean;
			}
			if (bind(tcp, (sockaddr*)&name, namelen) == -1) {
				goto clean;
			}
			if (listen(tcp, 5) == -1) {
				goto clean;
			}
			if (getsockname(tcp, (sockaddr*)&name, &namelen) == -1) {
				goto clean;
			}
			tcp1 = socket(AF_INET, SOCK_STREAM, 0);
			if (tcp1 == -1) {
				goto clean;
			}
			if (-1 == connect(tcp1, (sockaddr*)&name, namelen)) {
				goto clean;
			}
			tcp2 = accept(tcp, (sockaddr*)&name, &namelen);
			if (tcp2 == -1) {
				goto clean;
			}
			if (closesocket(tcp) == -1) {
				goto clean;
			}
			fildes[0] = tcp1;
			fildes[1] = tcp2;
			return 0;
		clean:
			if (tcp != -1) {
				closesocket(tcp);
			}
			if (tcp2 != -1) {
				closesocket(tcp2);
			}
			if (tcp1 != -1) {
				closesocket(tcp1);
			}
			return -1;
	}
#endif
	};

	Selector* Selector::Instance = NULL;

	bool Startup()
	{
		bool sysresult = false;
#ifdef LIMAX_OS_WINDOWS
		WSADATA wData;
		sysresult = (0 == ::WSAStartup(MAKEWORD(2, 2), &wData));
#endif
		if (sysresult)
		{
			Selector::Instance = new Selector();
		}
		return true;
	}

	void Cleanup()
	{
		delete Selector::Instance;
		Selector::Instance = NULL;

#ifdef LIMAX_OS_WINDOWS
		::WSACleanup();
#endif
	}

	void Socket::Close(std::exception* e)
	{
		service->OnSocketClose(This, e);
		Selector::Instance->Select(This, Selector::OpClose, 0);
		This.reset();
	}

	inline void platform_close_socket(int & so)
	{
#ifdef LIMAX_OS_WINDOWS
		::closesocket(so);
#else
		::close(so);
#endif
		so = 0;
	}

	inline bool platform_ignore_error_for_send()
	{
#ifdef LIMAX_OS_WINDOWS
		return ::WSAGetLastError() == WSAEWOULDBLOCK;
#else
		return errno = EWOULDBLOCK;
#endif
	}

	Socket::~Socket()
	{
		std::cout << "~Socket" << std::endl;
		platform_close_socket(socket);
	}

	class Buffer
	{
	public:
		char* data;
		int capacity;
		Buffer()
		{
			capacity = 16 * 1024;
			data = new char[capacity];
		}
		~Buffer()
		{
			delete[] data;
		}
	};

	void Socket::OnRecv()
	{
		Buffer recvbuf;
		int rc = ::recv(socket, recvbuf.data, recvbuf.capacity, 0);
		if (-1 == rc)
		{
			if (false == platform_ignore_error_for_send())
			{
				std::exception senderr("onsend error");
				this->Close(&senderr);
				return;
			}
			return;
		}
		if (InputCodec.get())
		{
			InputCodec->update((int8_t*)recvbuf.data, 0, rc);
			InputCodec->flush();
			Zeze::Serialize::ByteBuffer bb((char*)InputBuffer->buffer.data(), 0, InputBuffer->buffer.size());
			service->OnSocketProcessInputBuffer(This, bb);
			InputBuffer->buffer.erase(0, bb.ReadIndex);
		}
		else if (InputBuffer->buffer.size() > 0)
		{
			InputBuffer->buffer.append(recvbuf.data, rc);
			Zeze::Serialize::ByteBuffer bb((char*)InputBuffer->buffer.data(), 0, InputBuffer->buffer.size());
			service->OnSocketProcessInputBuffer(This, bb);
			InputBuffer->buffer.erase(0, bb.ReadIndex);
		}
		else
		{
			Zeze::Serialize::ByteBuffer bb(recvbuf.data, 0, rc);
			service->OnSocketProcessInputBuffer(This, bb);
			if (bb.Size() > 0)
				InputBuffer->buffer.append(bb.Bytes + bb.ReadIndex, bb.Size());
		}

		if (InputBuffer->buffer.empty())
			InputBuffer->buffer = std::string(); // XXX release memory if empty
	}

	void Socket::OnSend()
	{
		std::lock_guard<std::mutex> g(mutex);

		int rc = ::send(socket, OutputBuffer->buffer.data(), OutputBuffer->buffer.size(), 0);
		if (-1 == rc)
		{
			if (false == platform_ignore_error_for_send())
			{
				std::exception senderr("onsend error");
				this->Close(&senderr);
				return;
			}
			rc = 0;
		}
		OutputBuffer->buffer.erase(0, rc);
		if (OutputBuffer->buffer.empty())
			Selector::Instance->Select(This, 0, Selector::OpWrite);
	}

	void Socket::Send(const char* data, int offset, int length)
	{
		std::lock_guard<std::mutex> g(mutex);

		bool noPendingSend = OutputBuffer->buffer.empty();
		bool hasCodec = false;
		if (OutputCodec.get())
		{
			OutputCodec->update((int8_t*)data, offset, length);
			OutputCodec->flush();
			data = OutputBuffer->buffer.data();
			offset = 0;
			length = OutputBuffer->buffer.size();
			hasCodec = true;
		}

		if (noPendingSend)
		{
			// try send direct
			int rc = ::send(socket, data + offset, length, 0);
			if (rc == -1)
			{
				if (false == platform_ignore_error_for_send())
				{
					std::exception senderr("send error");
					this->Close(&senderr);
					return;
				}
				rc = 0;
			}

			if (hasCodec)
			{
				OutputBuffer->buffer.erase(0, rc);
				if (false == OutputBuffer->buffer.empty())
					Selector::Instance->Select(This, Selector::OpWrite, 0);
				return;
			}
			if (rc >= length)
			{
				return; // all send and hasn't Codec
			}
			// part send
			offset += rc;
			length -= rc;
			OutputBuffer->buffer.append(data + offset, length);
			Selector::Instance->Select(This, Selector::OpWrite, 0);
			return;
		}
		// in sending
		if (false == hasCodec) // �����Codec����ô��Ҫ���͵������Ѿ�������(update)��buffer�У�����Ҫ�ٴ���ӡ�
			OutputBuffer->buffer.append(data + offset, length);
	}

	bool Socket::Connect(const std::string& host, int _port, const std::string& lastSuccessAddress, int timeoutSecondsPerConnect)
	{
		struct addrinfo hints, * res;

		memset(&hints, 0, sizeof(hints));
		hints.ai_family = AF_UNSPEC;
		hints.ai_socktype = SOCK_STREAM;

		std::stringstream sport;
		sport << _port;
		std::string port(sport.str());

		if (0 != ::getaddrinfo(host.c_str(), port.c_str(), &hints, &res))
		{
			if (lastSuccessAddress.empty() || 0 != ::getaddrinfo(lastSuccessAddress.c_str(), port.c_str(), &hints, &res))
			{
				std::exception dnsfail("dns query fail");
				this->Close(&dnsfail);
				return false;
			}
		}

		int so = 0;
		for (struct addrinfo* ai = res; ai != NULL; ai = ai->ai_next)
		{
			so = ::socket(ai->ai_family, ai->ai_socktype, ai->ai_protocol);
			if (so == 0)
				continue;

			// �����첽ģʽ
#ifdef LIMAX_OS_WINDOWS
			unsigned long ul = 1;
			if (SOCKET_ERROR == ::ioctlsocket(so, FIONBIO, &ul))
#else
			if (-1 == fcntl(so, F_SETFL, fcntl(sock, F_GETFL) | O_NONBLOCK))
#endif
			{
				platform_close_socket(so);
				continue;
			}

			int ret = ::connect(so, ai->ai_addr, static_cast<int>(ai->ai_addrlen));
			if (ret != -1) // �������ϳɹ����첽socket��windowsӦ�ò����������سɹ�������д��
			{
				char addrName[256];
				if (::getnameinfo(ai->ai_addr, static_cast<socklen_t>(ai->ai_addrlen), addrName, sizeof(addrName), NULL, 0, NI_NUMERICHOST) == 0)
					LastAddress = addrName; // ���óɹ����ӵĵ�ַ
				break;
			}
#ifdef LIMAX_OS_WINDOWS
			if (::WSAGetLastError() == WSAEWOULDBLOCK) // ���Ӵ����С�����
#else
			if (errno == EINPROGRESS)
#endif
			{
				fd_set setw;
				FD_ZERO(&setw);
				FD_SET(so, &setw);
				struct timeval timeout = { 0 };
				timeout.tv_sec = timeoutSecondsPerConnect;
				timeout.tv_usec = 0;
				ret = ::select(so + 1, NULL, &setw, NULL, &timeout);
				if (ret <= 0)
				{
					// ������߳�ʱ
					platform_close_socket(so);
					continue;
				}
				int err = -1;
				socklen_t socklen = sizeof(err);
				if (0 == ::getsockopt(so, SOL_SOCKET, SO_ERROR, (char*)&err, &socklen))
				{
					if (err == 0)
					{
						char addrName[256];
						if (::getnameinfo(ai->ai_addr, static_cast<socklen_t>(ai->ai_addrlen), addrName, sizeof(addrName), NULL, 0, NI_NUMERICHOST) == 0)
							LastAddress = addrName; // ���óɹ����ӵĵ�ַ
						break;
					}
				}
			}
			platform_close_socket(so);
		}
		::freeaddrinfo(res);

		if (0 == so)
		{
			std::exception connfail("connect fail");
			this->Close(&connfail);
			return false;
		}
		this->socket = so;
		Selector::Instance->Select(This, Selector::OpRead, 0);
		service->OnSocketConnected(This);
		return true;
	}

} // namespace Net
} // namespace Zeze
