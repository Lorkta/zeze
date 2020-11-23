


declare module 'csharp' {
    // ��������Ķ��嵽 puerts �� d.ts ���棬�����󶨵� c#(cxx?) �Ĵ���
    namespace Zeze {
        type CallbackOnSocketHandshakeDone = (sessionId: number) => void;
        type CallbackOnSocketClose = (sessionId: number) => void;
        type CallbackOnSocketProcessInputBuffer = (sessionId: number, buffer: ArrayBuffer, offset: number, len: number) => void;

        class ToTypeScriptService extends System.Object {
            public constructor(name: string, cb1: CallbackOnSocketHandshakeDone, cb2: CallbackOnSocketClose, cb3: CallbackOnSocketProcessInputBuffer);
            public Connect(hostNameOrAddress: string, port: number, autoReconnect: boolean): void;
            public Send(sessionId: number, buffer: ArrayBuffer, offset: number, len: number): void;
            public Close(sessionId: number): void
            // �������¼�֪ͨ����ǰts�С����֧�������߳���ts������Ϣ���ٸĳ�ͨ�淽ʽ��
            public TickUpdate(): void;
        }
    }
}