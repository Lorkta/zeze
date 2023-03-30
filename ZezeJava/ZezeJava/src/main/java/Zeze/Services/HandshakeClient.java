package Zeze.Services;

import Zeze.Application;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Util.OutObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HandshakeClient extends HandshakeBase {
	public HandshakeClient(@NotNull String name, @Nullable Config config) {
		super(name, config);
		addHandshakeClientFactoryHandle();
	}

	public HandshakeClient(@NotNull String name, @NotNull Application app) {
		super(name, app);
		addHandshakeClientFactoryHandle();
	}

	public final void connect(@NotNull String hostNameOrAddress, int port) {
		connect(hostNameOrAddress, port, true);
	}

	public final void connect(@NotNull String hostNameOrAddress, int port, boolean autoReconnect) {
		var c = new OutObject<Connector>();
		getConfig().tryGetOrAddConnector(hostNameOrAddress, port, autoReconnect, c);
		//noinspection DataFlowIssue
		c.value.start();
	}

	@Override
	public void OnSocketConnected(@NotNull AsyncSocket so) {
		// 重载这个方法，推迟OnHandshakeDone调用
		addSocket(so);
	}
}
