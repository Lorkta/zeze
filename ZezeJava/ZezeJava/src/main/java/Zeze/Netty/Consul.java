package Zeze.Netty;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.Helper;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;

/**
 * 向 Consul 注册 HttpServer 服务，便于第三方（比如Ngnix)发现。
 * todo 为了支持更多注册和管理，可能需要抽象一个接口。先不写。
 */
public class Consul {
	// 这个path对所有的httpserver都一样。
	private static final String PassiveKeepAlivePath = "/Zeze_Netty_Consul_PassiveKeepAlivePath";
	private final ConsulClient client;
	private final ConcurrentHashMap<HttpServer, String> services = new ConcurrentHashMap<>();

	public Consul(String consulHost) {
		client = new ConsulClient(consulHost);
	}

	public void register(String serviceName, HttpServer httpServer) throws Exception {
		httpServer.getChannelFuture().sync();
		var addr = httpServer.getLocalAddress();
		assert addr != null;
		var host = addr.getAddress().isAnyLocalAddress()
				? Helper.selectOneIpAddress(false)
				: addr.getAddress().getHostAddress();
		var port = addr.getPort();
		var serviceId = host + ":" + port;
		if (null != services.putIfAbsent(httpServer, serviceId))
			throw new IllegalStateException("duplicate register " + serviceId);

		httpServer.addHandler(PassiveKeepAlivePath, 1024, null, null, Consul::passiveKeepAlive);

		var newService = new NewService();
		newService.setAddress(host);
		newService.setPort(port);
		newService.setId(serviceId);
		newService.setName(serviceName);
		// todo ... 里面还有很多参数。

		var checker = new NewService.Check();
		checker.setHttp("http://" + serviceId + PassiveKeepAlivePath);
		newService.setCheck(checker);
		/* checker 网上的配置，需要都设置？
			{
			"check": {
			"id": "api",
			"name": "HTTP API on port 5000",
			"http": "https://localhost:5000/health",
			"tls_skip_verify": false,
			"method": "POST",
			"header": {"x-foo":["bar", "baz"]},
			"interval": "10s",
			"timeout": "1s"
			}
		 */
		client.agentServiceRegister(newService); // response value is void.
	}

	public void stop() {
		for (var e : services.entrySet()) {
			var httpServer = e.getKey();
			var serviceId = e.getValue();
			client.agentServiceDeregister(serviceId);
			httpServer.removeHandler(PassiveKeepAlivePath);
		}
		services.clear();
	}

	private static void passiveKeepAlive(@NotNull HttpExchange x) throws Exception {
		x.sendPlainText(HttpResponseStatus.OK, ""); // todo consul被动保活结果有没有什么规定
	}
}
