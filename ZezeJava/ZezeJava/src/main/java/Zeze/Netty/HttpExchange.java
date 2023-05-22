package Zeze.Netty;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Str;
import Zeze.Util.Task;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AsciiString;
import io.netty.util.AttributeMap;
import io.netty.util.ReferenceCountUtil;

public class HttpExchange {
	static final int CHECK_IDLE_INTERVAL = 5; // 检查连接状态间隔(秒)
	static final int IDLE_TIMEOUT = 60; // 主动断开的超时时间(秒)

	static final int CLOSE_FINISH = 0; // 正常结束HttpExchange,不关闭连接
	static final int CLOSE_ON_FLUSH = 1; // 结束HttpExchange,发送完时关闭连接
	static final int CLOSE_FORCE = 2; // 结束HttpExchange,不等发送完强制关闭连接
	static final int CLOSE_TIMEOUT = 3; // 同上,只是因idle超时而关闭
	static final int CLOSE_PASSIVE = 4; // 同上,只是因远程主动关闭而关闭

	private static final Pattern rangePattern = Pattern.compile("[ =\\-/]");
	private static final OpenOption[] emptyOpenOptions = new OpenOption[0];
	private static final VarHandle detachedHandle;

	static {
		try {
			detachedHandle = MethodHandles.lookup().findVarHandle(HttpExchange.class, "detached", int.class);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private final HttpServer server; // 所属的HttpServer对象,每个对象管理监听端口的所有连接
	private ChannelHandlerContext context; // netty的连接上下文,每个连接可能会依次绑定到多个HttpExchange对象
	private HttpRequest request; // 收到完整HTTP header部分会赋值
	private HttpHandler handler; // 收到完整HTTP header部分会查找对应handler并赋值
	private ByteBuf content = Unpooled.EMPTY_BUFFER; // 当前收集的HTTP body部分, 只用于非流模式
	private int outBufHash; // 用于判断输出buffer是否有变化
	private short idleTime; // 当前统计的idle时间(秒)
	private boolean willCloseConnection; // true表示close时会关闭连接
	private boolean inStreamMode; // 是否在流/WebSocket模式过程中
	private Object userState;
	private volatile @SuppressWarnings("unused") int detached; // 0:not detached; 1:detached; 2:detached and closed

	public HttpExchange(HttpServer server, ChannelHandlerContext context) {
		this.server = server;
		this.context = context;
	}

	public Object getUserState() {
		return userState;
	}

	public void setUserState(Object userState) {
		this.userState = userState;
	}

	public boolean isActive() {
		return request != null;
	}

	// 通常不需要获取context,只给特殊需要时使用netty内部的方法
	public ChannelHandlerContext context() {
		return context;
	}

	public Channel channel() {
		return context.channel();
	}

	public AttributeMap attributes() {
		return context.channel();
	}

	public HttpRequest request() {
		return request;
	}

	public ByteBuf content() {
		return content;
	}

	public static String urlDecode(String s) {
		for (int i = 0, n = s.length(); i < n; i++) {
			var c = s.charAt(i);
			if (c == '%' || c == '+')
				return URLDecoder.decode(s, StandardCharsets.UTF_8);
		}
		return s;
	}

	public String path() {
		var uri = request.uri();
		var i = uri.indexOf('?');
		return urlDecode(i >= 0 ? uri.substring(0, i) : uri);
	}

	public String query() {
		var uri = request.uri();
		var i = uri.indexOf('?');
		return i >= 0 ? uri.substring(i + 1) : null;
	}

	public static Map<String, String> parseQuery(String s) {
		if (s == null)
			return Map.of();
		var m = new LinkedHashMap<String, String>();
		for (int i = 0, b = 0, e = -1, n = s.length(); ; i++) {
			int c = i < n ? s.charAt(i) : '&';
			if (c == '&') {
				m.put(urlDecode(s.substring(b, e >= 0 ? e : i)), e >= 0 ? urlDecode(s.substring(e + 1, i)) : "");
				if (i >= n)
					return m;
				b = i + 1;
				e = -1;
			} else if (c == '=' && e < 0)
				e = i;
		}
	}

	public Map<String, String> queryMap() {
		return parseQuery(query());
	}

	public Map<String, String> contentQueryMap() {
		return parseQuery(content().toString(StandardCharsets.UTF_8));
	}

	public static String filePath(String path) {
		var i = path.lastIndexOf(':'); // 过滤掉盘符,避免访问非法路径
		if (i < 0)
			i = 0;
		for (int e = path.length(); i < e; i++) {
			var c = path.charAt(i);
			if (c != '.' && c != '/' && c != '\\') // 过滤掉前面的特殊符号,避免访问非法路径
				break;
		}
		return path.substring(i);
	}

	void channelRead(Object msg) throws Exception {
		idleTime = 0;

		if (msg instanceof HttpRequest) {
			request = ReferenceCountUtil.retain((HttpRequest)msg);
			var path = path();
			handler = server.handlers.get(path);
			if (handler == null) {
				sendFile(filePath(path));
				return;
			}
			if (handler.isWebSocketMode() && context.pipeline().get(WebSocketServerProtocolHandler.class) == null) {
				context.pipeline().addLast(new WebSocketServerProtocolHandler(WebSocketServerProtocolConfig.newBuilder()
						.websocketPath(path).decoderConfig(WebSocketDecoderConfig.newBuilder().withUTF8Validator(false)
								.build()).build()));
				inStreamMode = true;
				//noinspection ConstantConditions
				handler.WebSocketHandle.onOpen(this);
				context.fireChannelRead(msg);
				return;
			}
			if (handler.isStreamMode()) {
				fireBeginStream();
				inStreamMode = true;
			}
			if (!(msg instanceof HttpContent)) {
				if (HttpUtil.is100ContinueExpected(request)) {
					if (!handler.isStreamMode() && HttpUtil.getContentLength(request, 0) > handler.MaxContentLength) {
						closeConnectionOnFlush(context.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
								HttpResponseStatus.EXPECTATION_FAILED, Unpooled.EMPTY_BUFFER, false)));
						return;
					}
					context.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
							HttpResponseStatus.CONTINUE, Unpooled.EMPTY_BUFFER, false), context.voidPromise());
				}
				return;
			}
		} else if (msg instanceof WebSocketFrame) {
			fireWebSocket((WebSocketFrame)msg);
			return;
		} else if (!(msg instanceof HttpContent)) {
			Netty.logger.error("unknown message type = {} from {}",
					(msg != null ? msg.getClass() : null), context.channel().remoteAddress());
			closeConnectionNow();
			return;
		} else if (request == null || handler == null) // 缺失上文的msg,可能很罕见,忽略吧
			return;

		var c = (HttpContent)msg;
		var b = c.content();
		var n = b.readableBytes();
		if (n > 0) {
			if (handler.isStreamMode())
				fireStreamContentHandle(c);
			else {
				if (content.readableBytes() + n > handler.MaxContentLength) {
					Netty.logger.error("content size = {} + {} > {} from {}", content.readableBytes(), n,
							handler.MaxContentLength, context.channel().remoteAddress());
					closeConnectionNow();
					return;
				}
				b.retain();
				if (content == Unpooled.EMPTY_BUFFER)
					content = b;
				else if (content instanceof CompositeByteBuf)
					((CompositeByteBuf)content).addComponent(true, b);
				else
					content = context.alloc().compositeBuffer().addComponent(true, content).addComponent(true, b);
			}
		}
		if (c instanceof LastHttpContent) {
			inStreamMode = false;
			fireEndStreamHandle(); // 流模式和非流模式通用
		}
	}

	@SuppressWarnings("ConstantConditions")
	private void fireBeginStream() throws Exception {
		var r = parseRange(HttpHeaderNames.CONTENT_RANGE);
		if (server.zeze != null && handler.Level != TransactionLevel.None) {
			var p = server.zeze.newProcedure(() -> {
				handler.BeginStreamHandle.onBeginStream(this, r[0], r[1], r[2]);
				return Procedure.Success;
			}, "fireBeginStream");
			if (handler.Mode == DispatchMode.Direct)
				Task.call(p);
			else
				server.task11Executor.Execute(context.channel().id(), p, null, handler.Mode);
		} else if (handler.Mode == DispatchMode.Direct) {
			Task.call(() -> handler.BeginStreamHandle.onBeginStream(this, r[0], r[1], r[2]), "fireBeginStream");
		} else {
			server.task11Executor.Execute(context.channel().id(),
					() -> handler.BeginStreamHandle.onBeginStream(this, r[0], r[1], r[2]),
					"fireBeginStream", handler.Mode);
		}
	}

	private void fireStreamContentHandle(HttpContent c) throws Exception {
		var handle = handler.StreamContentHandle;
		if (handle == null)
			return;
		if (server.zeze != null && handler.Level != TransactionLevel.None) {
			var p = server.zeze.newProcedure(() -> {
				handle.onStreamContent(this, c);
				return Procedure.Success;
			}, "fireStreamContentHandle");
			if (handler.Mode == DispatchMode.Direct)
				Task.call(p);
			else {
				c.retain();
				server.task11Executor.Execute(context.channel().id(), () -> {
					try {
						return p.call();
					} finally {
						c.release();
					}
				}, p.getActionName(), handler.Mode);
			}
		} else if (handler.Mode == DispatchMode.Direct) {
			Task.call(() -> handle.onStreamContent(this, c), "fireStreamContentHandle");
		} else {
			c.retain();
			server.task11Executor.Execute(context.channel().id(), () -> {
				try {
					handle.onStreamContent(this, c);
				} finally {
					c.release();
				}
			}, "fireStreamContentHandle", handler.Mode);
		}
	}

	public HttpExchange detach() {
		detachedHandle.compareAndSet(this, 0, 1);
		return this;
	}

	private void invokeEndStream() throws Exception {
		try {
			var handle = handler.EndStreamHandle;
			if (handle != null)
				handle.onEndStream(this);
		} finally {
			if (detached == 0)
				close(null);
		}
	}

	@SuppressWarnings("ConstantConditions")
	private void fireEndStreamHandle() throws Exception {
		if (server.zeze != null && handler.Level != TransactionLevel.None) {
			var p = server.zeze.newProcedure(() -> {
				var handle = handler.EndStreamHandle;
				if (handle != null)
					handle.onEndStream(this);
				return Procedure.Success;
			}, "fireEndStreamHandle");
			if (handler.Mode == DispatchMode.Direct) {
				try {
					Task.call(p);
				} finally {
					if (detached == 0)
						close(null);
				}
			} else {
				server.task11Executor.Execute(context.channel().id(), () -> {
					try {
						return p.call();
					} finally {
						if (detached == 0)
							close(null);
					}
				}, p.getActionName(), handler.Mode);
			}
		} else if (handler.Mode == DispatchMode.Direct) {
			Task.call(this::invokeEndStream, "fireEndStreamHandle");
		} else {
			server.task11Executor.Execute(context.channel().id(), this::invokeEndStream,
					"fireEndStreamHandle", handler.Mode);
		}
	}

	private void fireWebSocket(WebSocketFrame frame) throws Exception {
		if (server.zeze != null && handler.Level != TransactionLevel.None) {
			frame.retain();
			var p = server.zeze.newProcedure(() -> {
				try {
					fireWebSocket0(frame);
				} finally {
					frame.release();
				}
				return Procedure.Success;
			}, "fireWebSocket");
			if (handler.Mode == DispatchMode.Direct) {
				try {
					Task.call(p);
				} finally {
					frame.release();
				}
			} else {
				server.task11Executor.Execute(context.channel().id(), () -> {
					try {
						return p.call();
					} finally {
						frame.release();
					}
				}, p.getActionName(), handler.Mode);
			}
		} else if (handler.Mode == DispatchMode.Direct) {
			Task.call(() -> fireWebSocket0(frame), "fireWebSocket");
		} else {
			frame.retain();
			server.task11Executor.Execute(context.channel().id(), () -> {
				try {
					fireWebSocket0(frame);
				} finally {
					frame.release();
				}
			}, "fireWebSocket", handler.Mode);
		}
	}

	@SuppressWarnings("ConstantConditions")
	private void fireWebSocket0(WebSocketFrame frame) throws Exception {
		if (frame instanceof BinaryWebSocketFrame)
			handler.WebSocketHandle.onBinary(this, frame.content());
		else if (frame instanceof TextWebSocketFrame)
			handler.WebSocketHandle.onText(this, ((TextWebSocketFrame)frame).text());
		else if (frame instanceof CloseWebSocketFrame) {
			inStreamMode = false;
			//noinspection PatternVariableCanBeUsed
			var closeFrame = (CloseWebSocketFrame)frame;
			handler.WebSocketHandle.onClose(this, closeFrame.statusCode(), closeFrame.reasonText());
			closeConnectionOnFlush(null);
		} else if (frame instanceof PingWebSocketFrame)
			handler.WebSocketHandle.onPing(this, frame.content());
		else if (frame instanceof PongWebSocketFrame)
			handler.WebSocketHandle.onPong(this, frame.content());
		else {
			Netty.logger.error("unknown websocket message type = {} from {}",
					frame.getClass(), context.channel().remoteAddress());
			closeConnectionNow();
		}
	}

	// 下载请求/上传回复: range: bytes=[from]-[to] 范围是[from,to)
	// 上传请求/下载回复: content-range: bytes from-to/size 范围是[from,to]
	// 参考: https://www.jianshu.com/p/acca9656e250
	// 返回: [from, to, size]
	private long[] parseRange(AsciiString headerName) {
		var r = new long[]{-1, -1, -1};
		var headers = request.headers();
		var range = headers.get(headerName);
		if (range != null) {
			var p = range.indexOf(',');
			if (p >= 0)
				range = range.substring(0, p); // 暂不支持",",只下载第1段
			var ss = rangePattern.split(range);
			var sn = ss.length;
			if (sn > 1) {
				r[0] = parse(ss[1]);
				if (sn > 2) {
					r[1] = parse(ss[2]);
					if (sn > 3)
						r[2] = parse(ss[3]);
				}
			}
		} else if (headerName == HttpHeaderNames.CONTENT_RANGE) { // 如果没找到content-range可能只用content-length大小上传
			var len = headers.get(HttpHeaderNames.CONTENT_LENGTH);
			if (len != null) {
				var n = Integer.parseInt(len);
				r[0] = 0;
				r[1] = n - 1;
				r[2] = n;
			}
		}
		return r;
	}

	private static long parse(String s) {
		if (s.isEmpty())
			return -1;
		try {
			return Long.parseLong(s);
		} catch (Exception ignored) {
			return -1;
		}
	}

	void checkTimeout() {
		// 这里为了减小开销, 前半段IDLE_TIMEOUT期间只判断读超时
		if ((idleTime += CHECK_IDLE_INTERVAL) < IDLE_TIMEOUT / 2)
			return;
		// 后半段IDLE_TIMEOUT期间每次再判断写buffer的状态是否有变化,有变化则重新idle计时
		var outBuf = context.channel().unsafe().outboundBuffer();
		if (outBuf != null) {
			int hash = System.identityHashCode(outBuf.current()) ^ Long.hashCode(outBuf.currentProgress());
			if (hash != outBufHash) {
				outBufHash = hash;
				idleTime = 0;
				return;
			}
		}
		// 到这里至少有IDLE_TIMEOUT的时间没有任何读写了,那就强制关闭吧
		if (idleTime >= IDLE_TIMEOUT)
			close(CLOSE_TIMEOUT, null);
	}

	void channelReadClosed() {
		willCloseConnection = true;
	}

	private void closeInEventLoop() {
		if (inStreamMode) {
			inStreamMode = false;
			try {
				if (handler.isWebSocketMode()) {
					//noinspection ConstantConditions
					handler.WebSocketHandle.onClose(this, WebSocketCloseStatus.ABNORMAL_CLOSURE.code(), "");
				} else
					fireEndStreamHandle();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		if (content != Unpooled.EMPTY_BUFFER) {
			content.release();
			content = Unpooled.EMPTY_BUFFER;
		}
		if (request != null) {
			ReferenceCountUtil.release(request);
			request = null;
		}
		if (willCloseConnection && context != null) {
			context.close();
			context = null;
		}
	}

	void close(int method, ChannelFuture cf) {
		if ((int)detachedHandle.getAndSet(this, 2) == 2)
			return;
		server.exchanges.remove(context.channel().id(), this); // 尝试删除,避免继续接收当前请求的消息
		if (method != CLOSE_FINISH) {
			willCloseConnection = true;
			Netty.logger.info("close({}): {}", method, context.channel().remoteAddress());
		}
		if (method <= CLOSE_ON_FLUSH) { // CLOSE_FINISH | CLOSE_ON_FLUSH
			if (cf == null)
				cf = context.writeAndFlush(Unpooled.EMPTY_BUFFER);
			cf.addListener(__ -> closeInEventLoop());
		} else {
			var eventLoop = context.channel().eventLoop();
			if (eventLoop.inEventLoop()) {
				context.flush();
				closeInEventLoop();
			} else {
				eventLoop.execute(() -> {
					context.flush();
					closeInEventLoop();
				});
			}
		}
	}

	// 正常结束HttpExchange,不关闭连接
	public void close(ChannelFuture future) {
		close(CLOSE_FINISH, future);
	}

	// 结束HttpExchange,发送完时关闭连接
	public void closeConnectionOnFlush(ChannelFuture future) {
		close(CLOSE_ON_FLUSH, future);
	}

	// 结束HttpExchange,不等发送完强制关闭连接
	public void closeConnectionNow() {
		close(CLOSE_FORCE, null);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// send response
	public ChannelFuture send(HttpResponseStatus status, String contentType, ByteBuf content) { // content所有权会被转移
		var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content, false);
		var headers = Netty.setDate(res.headers())
				.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
				.set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
		if (contentType != null)
			headers.set(HttpHeaderNames.CONTENT_TYPE, contentType);
		return context.writeAndFlush(res);
	}

	public ChannelFuture send(HttpResponseStatus status, String contentType, String content) {
		return send(status, contentType, content == null || content.isEmpty() ? Unpooled.EMPTY_BUFFER
				: Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8)));
	}

	public ChannelFuture sendPlainText(HttpResponseStatus status, String text) {
		return send(status, "text/plain; charset=utf-8", text);
	}

	public ChannelFuture sendHtml(HttpResponseStatus status, String html) {
		return send(status, "text/html; charset=utf-8", html);
	}

	public ChannelFuture sendJson(HttpResponseStatus status, String json) {
		return send(status, "application/json; charset=utf-8", json);
	}

	public ChannelFuture sendXml(HttpResponseStatus status, String xml) {
		return send(status, "text/xml; charset=utf-8", xml);
	}

	public void sendFile(String filePath) throws Exception {
		var file = new File(server.fileHome, filePath);
		if (!file.isFile() || file.isHidden()) {
			close(send404());
			return;
		}

		// 检查 if-modified-since
		var lastModified = file.lastModified() / 1000;
		var ifModifiedSince = request.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
		if (ifModifiedSince != null && !ifModifiedSince.isEmpty() && lastModified == Netty.parseDate(ifModifiedSince)) {
			var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED, // 文件未改变
					Unpooled.EMPTY_BUFFER, false);
			Netty.setDate(res.headers())
					.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
					.set(HttpHeaderNames.CONTENT_LENGTH, 0);
			close(context.writeAndFlush(res));
			return;
		}

		var fc = FileChannel.open(file.toPath(), emptyOpenOptions);
		var fsize = fc.size();
		var r = parseRange(HttpHeaderNames.RANGE);
		var from = Math.max(r[0], 0);
		var to = r[1];
		var contentLen = Math.max((to >= 0 ? to : fsize) - from, 0L);

		var res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, false);
		Netty.setDate(res.headers())
				.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
				.set(HttpHeaderNames.CONTENT_TYPE, Mimes.fromFileName(filePath))
				.set(HttpHeaderNames.CONTENT_LENGTH, contentLen)
				.set(HttpHeaderNames.CONTENT_RANGE, "bytes " + from + '-' + (from + contentLen - 1) + '/' + fsize)
				.set(HttpHeaderNames.EXPIRES, Netty.getDate(Netty.getLastDateSecond() + server.fileCacheSeconds))
				.set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + server.fileCacheSeconds)
				.set(HttpHeaderNames.LAST_MODIFIED, Netty.getDate(lastModified));
		context.write(res, context.voidPromise());

		if (contentLen > 0 && !HttpMethod.HEAD.equals(request.method())) // 发文件任务全部交给Netty，并且发送完毕时关闭。
			context.write(new DefaultFileRegion(fc, from, contentLen), context.voidPromise());
		close(context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(__ -> fc.close()));
	}

	public ChannelFuture send404() {
		return sendPlainText(HttpResponseStatus.NOT_FOUND, null);
	}

	public ChannelFuture send500(Throwable ex) {
		return sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, Str.stacktrace(ex));
	}

	public ChannelFuture send500(String text) {
		return sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, text);
	}

	public ChannelFuture sendWebSocket(WebSocketFrame frame) { // frame所有权会被转移
		return context.writeAndFlush(frame);
	}

	public ChannelFuture sendWebSocket(String text) {
		return context.writeAndFlush(new TextWebSocketFrame(text));
	}

	public ChannelFuture sendWebSocket(byte[] data) {
		return context.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data, 0, data.length)));
	}

	public ChannelFuture sendWebSocket(byte[] data, int offset, int count) {
		return context.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data, offset, count)));
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////
	// 流接口功能最大化，不做任何校验：状态校验，不正确的流起始Response（headers）等。
	public ChannelFuture beginStream(HttpResponseStatus status, HttpHeaders headers) {
		if (!headers.contains(HttpHeaderNames.CONTENT_LENGTH))
			headers.set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
		return context.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, status, headers));
	}

	// 发送后data内容在回调前不能修改
	public ChannelFuture sendStream(byte[] data) {
		return context.writeAndFlush(new DefaultHttpContent(Unpooled.wrappedBuffer(data, 0, data.length)));
	}

	// 发送后data内容在回调前不能修改
	public ChannelFuture sendStream(byte[] data, int offset, int count) {
		return context.writeAndFlush(new DefaultHttpContent(Unpooled.wrappedBuffer(data, offset, count)));
	}

	public void endStream() {
		close(context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT));
	}
}
