#include "LuaHelper.h"
#include <iostream>
#include "Net.h"
#include "demo/App.h"

int main(int argc, char* argv[])
{
	argc;
	argv;

	printf("hello 1\n");

	Zeze::Net::Startup();
	lua_State * L = luaL_newstate();
	luaL_openlibs(L);
	try
	{
		Zeze::LuaHelper lua(L);
		if (lua.DoString("package.path = package.path .. ';./LuaSrc/?.lua;./LuaGen/?.lua'"))
			throw std::exception("package.path");
		demo::App::Instance().Client.InitializeLua(L);
		demo::App::Instance().Client.SetAutoConnect(true);
		//demo::App::Instance().Client.Connect("::1", 9999); // 改到在 main.lua 中连接。
		if (lua.DoString("require 'main'"))
		{
			std::cout << "run main error: " << lua.ToString(-1) << std::endl;
		}
	}
	catch (std::exception & ex)
	{
		std::cout << "main " << ex.what() << std::endl;
	}
	lua_close(L);
	Zeze::Net::Cleanup();
	return 0;
}
