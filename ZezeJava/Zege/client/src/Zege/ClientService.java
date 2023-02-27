package Zege;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;

public class ClientService extends ClientServiceBase {
    public ClientService(Zeze.Application zeze) {
        super(zeze);
    }
    // 重载需要的方法。
    @Override
    public <P extends Protocol<?>> void DispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
                                                            ProtocolFactoryHandle<?> factoryHandle) throws Exception {
        Task.runRpcResponse(() -> responseHandle.handle(rpc), rpc, factoryHandle.Mode);
    }

    @Override
    public final <P extends Protocol<?>> void dispatchProtocol2(Object key, P p, ProtocolFactoryHandle<P> factoryHandle) {
        getZeze().getTaskOneByOneByKey().Execute(key,
                () -> Task.call(() -> factoryHandle.Handle.handle(p), p, Protocol::trySendResultCode), factoryHandle.Mode);
    }

    @Override
    public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) {
        var p = decodeProtocol(typeId, bb, factoryHandle, so);
        Task.run(() -> p.handle(this, factoryHandle), p, null, null, factoryHandle.Mode);
    }

    @Override
    public void OnSocketClose(AsyncSocket so, Throwable e) throws Exception {
        super.OnSocketClose(so, e);
        if (null != e)
            e.printStackTrace();
        System.out.println("OnSocketClose");
    }
}
