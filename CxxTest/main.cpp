
#include "Gen/demo/Bean1.hpp"
#include "Gen/demo/Module1/BValue.hpp"
#include "zeze/cxx/Net.h"
#include "demo/TestClient.h"
#include <cmath>

void TestSocket();
void TestEncode();
void TestProtocol();
void TestFuture();

int main(char* args[])
{
	TestFuture();
	int mills = 200;
	std::cout << std::ceil(mills / 1000.0) << std::endl;
	TestEncode();
	TestSocket();
	TestProtocol();
}

#include "Gen/demo/Module1/Protocol3.hpp"
#include "Gen/demo/Module1/Rpc1.hpp"
#include "Gen/demo/Module1/Rpc2.hpp"

class ProtocolClient : public Zeze::Net::Service
{
public:
	ProtocolClient()
	{
		AddProtocolFactory(demo::Module1::Protocol3::TypeId_, ProtocolFactoryHandle(
			[]()
			{
				return new demo::Module1::Protocol3();
			},
			[](Zeze::Net::Protocol* p)
			{
				std::cout << "ProcessProtocol3" << std::endl;
				return 0;
			}
			));

		AddProtocolFactory(demo::Module1::Rpc1::TypeId_, ProtocolFactoryHandle(
			[]()
			{
				return new demo::Module1::Rpc1();
			},
			[](Zeze::Net::Protocol* p)
			{
				auto r = (demo::Module1::Rpc1*)p;
				std::cout << "ProcessRpc1 Never!" << std::endl;
				r->SendResult();
				return 0;
			}
			));

		AddProtocolFactory(demo::Module1::Rpc2::TypeId_, ProtocolFactoryHandle(
			[]()
			{
				return new demo::Module1::Rpc2();
			},
			[](Zeze::Net::Protocol* p)
			{
				auto r = (demo::Module1::Rpc2*)p;
				std::cout << "ProcessRpc2" << std::endl;
				r->SendResult();
				return 0;
			}
			));
	}

	virtual void OnHandshakeDone(const std::shared_ptr<Zeze::Net::Socket>& sender) override
	{
		demo::Module1::Protocol3 p;
		p.Send(GetSocket().get());
		std::auto_ptr<demo::Module1::Rpc1> rpc1Async(new demo::Module1::Rpc1());
		if (rpc1Async->Send(GetSocket().get(), [](Zeze::Net::Protocol*p)
			{
				std::cout << "Rpc1 Async Response." << std::endl;
				return 0;
			}))
			rpc1Async.release();

		std::thread([this]()
			{
				demo::Module1::Rpc1 rpc1;
				rpc1.SendForWait(GetSocket().get())->Wait();
				std::cout << "Send Rpc1 Done." << std::endl;
			}).detach();
	}
};

#include "zeze/cxx/TaskCompletionSource.h"

void TestFuture()
{
	std::shared_ptr<Zeze::TaskCompletionSource<int>> future(new Zeze::TaskCompletionSource<int>());
	std::thread([future]
		{
			std::this_thread::sleep_for(std::chrono::milliseconds(2000));
			future->SetResult(1);
		}).detach();

	future->Wait();
	std::cout << "TaskCompletionSource Done -> " << future->Get() << std::endl;
}

void TestProtocol()
{
	Zeze::Net::Startup();
	ProtocolClient client;
	client.Connect("127.0.0.1", 7777);
	Sleep(2000);
	Zeze::Net::Cleanup();
}

class Client : public Zeze::Net::Service
{
public:
	void OnSocketProcessInputBuffer(const std::shared_ptr<Zeze::Net::Socket>& sender, Zeze::ByteBuffer& input) override
	{
		std::cout << std::string((char*)input.Bytes, input.ReadIndex, input.Size()) << std::endl;
		input.ReadIndex = input.WriteIndex;
	}

	void OnSocketConnected(const std::shared_ptr<Zeze::Net::Socket>& sender)
	{
		std::string req("HEAD / HTTP/1.0\r\n\r\n");
		sender->Send(req.data(), (int)req.size());
	}
};

void TestSocket()
{
	Zeze::Net::Startup();
	Client client;
	client.Connect("www.163.com", 80);
	Sleep(2000);
	Zeze::Net::Cleanup();
}

void TestEncode()
{
	Zeze::ByteBuffer bb(16);
	demo::Module1::BValue bValue;
	bValue.Int1 = 1;
	bValue.Long2 = 2;
	bValue.String3 = "3";
	bValue.Bool4 = true;
	bValue.Short5 = 5;
	bValue.Float6 = 6;
	bValue.Double7 = 7;
	bValue.Bytes8 = "8";
	bValue.List9.push_back(demo::Bean1());
	bValue.Set10.insert(10);
	bValue.Map11[11] = demo::Module2::BValue();
	bValue.Bean12.Int1 = 12;
	bValue.Byte13 = 13;
	bValue.Dynamic14.SetBean(new demo::Bean1());
	bValue.Dynamic14.SetBean(new demo::Module1::BSimple()); // set again
	bValue.Map15[15] = 15;
	demo::Module1::Key key(16);
	bValue.Map16[key] = demo::Module1::BSimple();
	bValue.Vector2.x = 17;
	bValue.Vector2Int.x = 18;
	bValue.Vector3.x = 19;
	bValue.Vector4.x = 20;
	bValue.Quaternion.x = 21;
	Zeze::Vector2Int v2i(22, 22);
	bValue.MapVector2Int[v2i] = v2i;
	bValue.ListVector2Int.push_back(v2i);
	bValue.Map25[key] = demo::Module1::BSimple();
	bValue.Map26[key] = demo::Module1::BValue::constructDynamicBean_Map26();
	bValue.Map26[key].SetBean(new demo::Module1::BSimple());
	bValue.Dynamic27.SetBean(new demo::Module1::BSimple());
	bValue.Key28.S = 28;
	bValue.Key28.Assign(demo::Module1::Key(28));
	bValue.Array29.push_back(29);
	bValue.LongList.push_back(30);
	bValue.Encode(bb);

	Zeze::ByteBuffer bb2(bb.Bytes, 0, bb.WriteIndex);
	demo::Module1::BValue bValueDecoded;
	bValueDecoded.Decode(bb2);
	bValue.Assign(bValueDecoded);
}
