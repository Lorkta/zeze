package Zeze.Util;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;

public class CommandConsoleService extends Service {
	private CommandConsole cc;

	public void setCommandConsole(CommandConsole cc) {
		this.cc = cc;
	}

	public CommandConsoleService(String name, Config config) {
		super(name, config);
	}

	@Override
	public void OnSocketAccept(AsyncSocket so) throws Exception {
		if (null != cc)
			so.setUserState(CommandConsole.dup(cc));
		super.OnSocketAccept(so);
	}

	@Override
	public boolean OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input) {
		var cc = (CommandConsole)so.getUserState();
		if (null != cc)
			cc.input(so, input.Bytes, input.ReadIndex, input.size());

		input.ReadIndex = input.WriteIndex; // all processed
		return true;
	}
}
