# Ŀ��

	0) �����ܼ���ʵ��ҵ���߼�ʱ��Ҫ�ļ����ֶΡ�����Ӧ�ÿ����Ѷȡ����ͳɱ���
	   ��ʵ�ִ�����ҵ���߼������ܽ��������ܶ���������
	1) �������ϸ񱣻����ݲ��𻵡�
	2) ����������⣬���Ժ����׹�����ǧ����̨������Ⱥ�ķ���
	3) ����߿��������⣬�ﵽ7x24Сʱ����Ϲ�����
	4) ��ֱ�ӵı�̽ӿڣ�ֱ�����������ԡ�

# ��Ҫ����

	0) ����
	   �ṩһ�������ϵ����ݿ������
	   �������ʧ�ܣ���ѷ��������޸ģ������ݻָ����޸�ǰ��״̬��
	   a) �쳣��ȫ
	      ����������κεط��������׳��쳣����ô��û������֧�ֵĻ�����
	      д����ȷ�Ĵ����Ƿǳ�����ġ������ڵ����Զ������ǻ��׳��쳣��
	   b) ������ģ�黯
	      ��ģ�����ǰ�������޸����ݣ����ú����߼����������ϣ����Իع���
	      �����װ�������ģ��ӿڣ�������ʵ�ִ��뾡���ӽ�ҵ��������

	1) ���ڻ���ͬ���ķֲ�ʽ����
	   ����ͬ���� CPU �� MESI������һ����Э�飩���ơ�
	   ��������ɵײ�ʵ�֣�Ӧ�ó������ò���Ҫ�����κνӿڡ�
	   ���Ǽ�Ⱥ���ݵĻ�������̵�ʱ�򣬾͸��Լ���ռ����һ����
	   �Ժ���������ʵ�����ɴﵽ����Ŀ�ġ�

	2) �������
	   ��ķǳ��򵥣�������װ�󣬶�һ���߼�������ȫ��������ϸ�ڡ�
	   a) Protocol
	   b) Rpc

	3) ���ݿ��װ
	   �Զ��־û���������ݿ⡣��Ӧ����˵�����Ե������ݿⲻ���ڡ�
	   ֧�ֵ����ݿ⣨�Ժ������Ҫ��ӣ������ף���
	   a) SQLServer
	   b) Mysql
	   c) Tikv

	4) Raft
	   ����֧�ָ߿ɿ��ԡ�

#### ��װ�̳�

	0) Zeze ��һ����⣬���еĺ��Ĺ��ܶ������
	   a) ��������������ֱ�Ӱ�Zeze\Zeze.csproj�ӵ��Լ���sln�֡�
	      ֱ������Դ����Ŀ��
	   b) �ͻ��ˣ�unity��������ֱ�ӿ���Zeze�µ�Դ�������Ŀ��
	      Ҳ�����Լ�������Ӧƽ̨�İ汾�������� unity plugins �¡�

	1) Gen
	   ��һ������̨���򡣱�����Ժ��������ɴ��롣

	2) GlobalCacheManager
	   ��һ������̨���򡣵����Application�������ݿ�ʱ������������ͬ����

	3) ServiceManager
	   �������ע��Ͷ��ġ�
	   �÷�������Ⱥ�������ס�

#### ʹ��˵��

	����Ҫ��׽�쳣��
	�������Ҫ������󣬲�׽�������Ժ��ٴ��׳���

	0) �������ݽṹ(Bean)
	   ��xml�ж��壬ʹ��Gen���ɴ��롣
	   a) ֧�ֻ������ͺ�������
	      ���ļ�'zeze\UnitTest\solution.xml' ���� '<bean name="Value">'��
	      ��������Ӱ��������еĻ������ͺͼ�������ʵ����
	   b) ֧�����ݽṹ�����
	      ������ʱ��ɾ������������Ҫ���ݿ�ת��������
	   c) Bean.dynamic
	      ����ʵ�ֶ�̬������ݱ��档

	1) ����Э��(protocol or rpc)
	   ���ļ�'zeze\UnitTest\solution.xml' ���� 'protocol' or 'rpc'��

	2) �������ݿ���(table)
	   ���ļ�'zeze\UnitTest\solution.xml' ���� 'table'��

	3) ����ģ��(module)
	   ģ������������Bean,Protocol,Rpc,Table�ġ�
	   ���ļ�'zeze\UnitTest\solution.xml' ���� 'module'��

	4) ������Ŀ(project)
	   һ����Ŀ��Ӧһ�����̡������ﶨ��service��
	   ��������ĿĿǰ��֧��c#��
	   �ͻ�����ĿĿǰ֧��c#��c++��
	   ֧�ֵĽű�ts��lua��
	   ���ļ�'zeze\UnitTest\solution.xml' ���� 'project'��

	5) ���ݱ������
	   ����֧�����ݱ�����ģ�ChangeListener����
	   �����޸����ݵĲ����ж��ٸ��������ݷ������ʱ���ص�Listener��
	   ��ô�����ж��ٸ��ط�����ʽ���޸ĸ����ݣ�ֻ��Ҫһ���ط��������ݱ����
	   һ�����ڿͻ�������ͬ����
	   ��ע�⡿��������ͬ������������ҵ���߼�ʵ����ʹ��������ԡ�

	6) ֧�ֺ�˶����ݿ�
	   �������������ݿ����ܲ��㡣
	   ����Zeze��������Դ��Ҫ���Ա���cache�����У�һ�㲻�ᷢ��������ݿ����ܲ��㡣

	7) ��������֧�ּ���ѹ����
	   ʹ�� Diffie�CHellman key exchange �����ܳס�

	8) ���ã�zeze.xml��
	   �ο���Game\zeze.xml; UnitTest\zeze.xml;Game2\zeze.xml
	   һ����˵����ʼ��Ҫ�ṩһ�����ݿ����ã�������������Ĭ�ϵġ�
	   ���ṩ���õĻ������ݿ����ڴ�ġ�

	9) ʲôʱ�򴴽��洢���̣�Zeze.NewProcedure��
	   ���ڿ��Ĭ��Ϊÿ��Э�鴴���洢���̣�һ����˵������Ҫ�Լ�������
	   �������Ҫ���񲿷�ʧ�ܵ�ʱ�򲻻ع����������Ǿ���ҪǶ������
	   ��ʱ��Ҫ�����Լ��Ĵ洢���̲��ж�ִ�н����
	   int nestProcedureResult = Zeze.NewProcedure(myaction, "myactionname").Call();
	   // check nestProcedureResult

	10) Bean More
	    ��������ݶ���������԰���Bean�����������������ֿ��԰���Bean��
	   ��reference��
	    ���е� bean ���ò������ظ����������л���TODO Gen��ʱ���⻷����
	   ��null��
	    ���е� bean ���ò���Ϊ null��ʹ�õ�ʱ����Ҫ�жϣ����Լ򻯴��롣
	   ��Assign & Copy��
	    ��ֵ�Ϳ�����
	   ��Managed��
	    Bean������Table���߱�����һ���Ѿ�Managed״̬��Bean��������֮ǰ�Ƿ�Managed״̬��
	    ��ʱ�޸�Bean���ᱻ��¼��־��Managed״̬һ�����ã��Ͳ���ָ�����ʹ���Table��
	    ����������ɾ�����������Table����������ɾ����Ҫ�ٴμ����ȥ����ҪCopyһ�Ρ�
	    Managed״ֻ̬�ܱ�����һ�Σ��ο������reference˵���������������ظ��Ķ���
	    ʹ�� Bean.Copy ��������һ�ݡ�
	   ��binary��
	    Zeze.Net.Binary�������Ժ����޸ģ�ֻ�������滻��
	   ��dynamic��
	    Bean�ı��������Ƕ�̬�ģ����������汣�治ͬ��bean��
	    see Game\solutions.xml��Game.Bag.BItem�Ķ��塣
	    dynamic ������֧�ֵ�Bean����ʱ������ָ������Bean�ķ�Χ��Ψһ��TypeId��
	    ���Id�ᱻ�־û������û��ָ��TypeId����Ĭ��ʹ�� Bean.TypeId��
	    ��Bean.TypeId��
	     Bean.TypeId Ĭ��ʹ��Zeze.Transaction.Bean.Hash64(Bean.FullName)���ɡ�
	     ��ͻʱ�������ֶ�ָ��һ����
	     Bean��FullName�����仯����������ݾɵ�����ʱ����Ҫ�ֶ���TypeId���óɾɵ�hashֵ��

	11) Protocol.id Rpc.id
	   Ĭ��ʹ��Zeze.Transaction.Bean.Hash16(Protocol.FullName)���ɡ�
	   ��ͻʱ����Ҫ�ֶ�ָ��һ����


	12) Session & UserState
	    AsyncSocket.UserState
	    ����������
	    Protocol.UserState
	    ��ĳ�������յ�������Э��������ģ�Ĭ�ϴ����������ĸ������á�
	    Procedure.UserState
	    ��ΪЭ�鴦�����洢����ִ������ʱ��Ĭ�ϴ�Э�������ĸ������á�

	    see Game\game.sln, Game2\game2.sln

	13) ��־��ͳ��
	    ��¼�˼������еĴ�����־��
	    ͳ���˼������и��ֿ��ܵ������������ͨ������ȫ�رգ���
	    Zeze ��¼��־��ʱ���� UserState.ToString Ҳ��¼��ȥ��
	    Ӧ�ÿ������Լ���UserState����ʵ��������Ӹ�����������Ϣ��
	    ����, Login.Session.SetLastError("detail");
	    ����д��ʱ��ֻ��Ҫ���ش��󣬲���ÿ���ط��Լ���¼��־��

	14) Э��洢���̴���������ֵ�滮����
	    0  Success
	    <0 Used By Zeze 
	    >0 User Defined. �Զ��������ʱ�������� (Module.Id << 16) | CodeInModule��
	    ע��Э��洢���̷���ֵʹ��ͬһ������ռ䡣

	����Ҫ��׽�쳣��
	�������Ҫ������󣬲�׽�������Ժ��ٴ��׳���

	15) Zeze.Net.Service
	    ����������¼���ͨ�������ص���
	    ͨ�����ز�override��Ҫ�ķ����������⴦��
	    ���̡߳�
	    �����¼�ֱ����io-thread�лص�����Ҫ�ڻص���ִ�п��������Ĳ�����
	    �����Ҫ�봴���µ�Task��

	    ���ӣ�
	    Zeze.Services.GlobalCacheManger,
	    Zeze.Services.ServiceManager,
	    Game2\linkd\gnet\LinkdService,Game2\linkd\gnet\ProviderService
	    Game2\server\Game\Server,

	16) Zeze.Util.Task
	    ����������ṩִ�в���¼��־��ͳ�ƵĹ��ܡ�
	    �����Ҫ�����Լ���Task������ʹ�á�

#### ����˵��

	����Ҫ��׽�쳣��
	�������Ҫ������󣬲�׽�������Ժ��ٴ��׳���

	0) AutoKey��������key����֧�� long ���͡�
	   ��Ϸ������Ҫ�������ֳɲ�ͬ�ķ�������Ȼ������Ҫ�����������Ժ�ķ������ϲ���
	   ����Ա���keyû��һ���滮���ϲ���ʱ��ͺܸ��ӡ�
	   �ṩһ��������key��һ��ʼ�ͶԹ滮��Χ�ڵķ���������Ψһ��key���ϲ����Ͳ����ͻ��
	   ���òο���UnitTest\zeze.xml
	   ���� AutoKeyLocalId="0" ���ط�������Id�����з�������Ψһ���ù���Ҳ�����ٴ�ʹ�á�
	   ���� AutoKeyLocalStep="4096" ������keyÿ�����Ӳ�����Ҳ�ǿ��Դ����ķ��������������
	   �滮����������������������key�ͻ��ṩ����Ψһ��id��
	   ��AutoKeyLocalId��
	   �����û���ͬ���Ժ��������Ҳ��Ϊÿ������ʵ����ΨһId��

	1) �����ݿ�֧��
	   �ṩ��� DatabaseConf ���á�������ݿ���Ҫ�ò�ͬ Name ���֡�
	   Ȼ���� TableConf ��ʹ������ DatabaseName �ѱ����䵽ĳ�����ݿ��С�
	   ���òο���UnitTest\zeze.xml

	2) ���ϵ����ݿ���װ������
	   ��ʹ��ĳЩǶ��ʽ���ݿ⣨����bdb��ʱ�����ĳ�����ݿ��ļ��ܴ󣬵��ǻ�Ծ���ݿ����ֲ��࣬
	   ÿ�α������ȽϷ�ʱ�����Կ��ǰѱ���Ƶ��µ����ݿ⣬Ȼ��ϵͳ���¿����Ҳ�����¼ʱ��
	   �Զ����Ͽ���װ�����ݡ��������Ͽ���ֻ���ģ�����ÿ�α��ݡ�
	   TableConf ��ʹ������ DatabaseOldName ָ���ϵ����ݿ⣬������ DatabaseOldMode ��Ϊ 1��
	   ����Ҫʱ��Zeze �ͻ��Զ����Ͽ���װ�ؼ�¼��
	   ���òο���UnitTest\zeze.xml

	3) ��� Zeze.Application ֮�������
	   һ����˵�������������һ�� Zeze.Application �����ݿ���
	   �����Ҫ�ڶ�� Zeze.Application ֮��֧������Ӧ��ֱ�ӷ��ʲ�ͬ App.Module 
	   ����ı�񼴿��������֧�֡��������������ύ(Checkpoint)Ĭ������һ�� Zeze.Application
	   ��ִ�еģ�Ϊ���������ύҲԭ�ӻ�����Ҫ��App.Startǰ����ͳһCheckpoint��
	   ���ô������ӣ�

	   Zeze.Checkpoint checkpoint = new Zeze.Checkpoint();
	   // �Ѷ��App�����ݿ���뵽Checkpoint�С�
	   checkpoint.Add(demo1.App.Zeze.Databases.Values);
	   checkpoint.Add(demo2.App.Zeze.Databases.Values);
	   // ����App��Checkpoint��
	   demo1.App.Zeze.Checkpoint = checkpoint;
	   demo2.App.Zeze.Checkpoint = checkpoint;
	   // ����App������������ǰ���á�
	   demo1.App.Start();
	   demo2.App.Start();

	4) ����ͬ��
	   ��� Zeze.Application ʵ������ͬһ��������ݿ�
	   һ���ģʽ�Ǻ�����ݿ����һ�� Zeze.Application ���ʡ�
	   �����Ҫ���ʵ������ͬһ�����ݿ⣬��Ҫ��������ͬ�����ܡ�
	   1) ���� GlobalCacheManager
	   2) ���� zeze.xml �����ԣ�GlobalCacheManagerHostNameOrAddress="127.0.0.1" GlobalCacheManagerPort="5555"
	      ���òο���UnitTest\zeze.xml
	   *) ע�⣬��֧�ֶ��ʹ��ͬһ�� GlobalCacheManager ͬ����Cache�� Zeze.Application ֮�������
	      �μ�����ĵ�3�㡣��Ϊ Cache ͬ����Ҫͬ����¼�ĳ���״̬�������ʱ Application ʹ��ͬһ�� Checkpoint��
	      ��¼ͬ������Ҫ�ȴ��Լ�����������
	   *) �����߼���������GlobalCacheManager֮������ӷǳ���Ҫ����������Ӧ��������һ���ɿ��������У�
	      һ����˵����������һ�������С�
   
	5) �ͻ���ʹ��Unity(csharp)+TypeScript
	   a) �� zeze/Zeze �����������Ŀ��ֱ�ӿ������������Ҫ�Լ����뷢�������ơ�
	   b) �� zeze/TypeScript/ts/ �µ� zeze.ts ��������� typescript Դ��Ŀ¼��
	      ���� npm install https://github.com/inexorabletash/text-encoding.git
	   c) �� zeze/Zeze/Services/ToTypeScriptService.cs �ļ��� #if USE_PUERTS ���ڵĴ��뿽�������c#Դ��Ŀ¼�µ�
	      ToTypeScriptService.cs �ļ��С���Ȼ�����������һ���ļ�����
	      �� typeof(ToTypeScriptService) �ӵ� puerts �� Bindings �б��С�
	      Ȼ��ʹ�� puerts �� unity ����˵����ɴ��롣
	   d) ���� solutions.xml ʱ��ts�ͻ���Ҫ�����Э��� handle ����Ϊ clientscript.
	      ʹ�� gen ����Э��Ϳ�ܴ��롣
	   e) ���ӿ��Կ��� https://gitee.com/e2wugui/zeze-unity.git
	      ��֪����ô�������������ڲ��������ǰ�encoding.js encoding-indexes.js ������output�¡�
	      ���� encoding.js ����Ϊ text-encoding.js��

	6) �ͻ���ʹ��Unreal(cxx)+TypeScript
	   a) ��zeze\cxx�µ����д��뿽�������Դ��Ŀ¼���Ҽӵ���Ŀ�С�����Lua��صļ����ļ���
	   b) �� zeze/TypeScript/ts/ �µ� zeze.ts ��������� typescript Դ��Ŀ¼��
	      ���� npm install https://github.com/inexorabletash/text-encoding.git
	   c) ��װpuerts����������ue.d.ts��
	   d) ���� solutions.xml ʱ��ts�ͻ���Ҫ�����Э��� handle ����Ϊ clientscript.
	      ʹ�� gen ����Э��Ϳ�ܴ��롣
	   e) zeze\cxx\ToTypeScriptService.h ����ĺ� ZEZEUNREAL_API �ĳ������Ŀ�ĺ����֡�
	   f) ���� https://gitee.com/e2wugui/ZezeUnreal.git
	      ��֪����ô��������(text-encoding)������unreal�и�puerts�ã����Կ��ǰ�encoding.js encoding-indexes.js
	      ������Content\JavaScript\���棬���� encoding.js ����Ϊ text-encoding.js��

	7) �ͻ���ʹ��Unity(csharp)+lua
	   a) ��Ҫѡ�����Lua-Bind����⣬ʵ��һ��ILuaʵ�֣��ο� Zeze.Service.ToLuaService.cs����
	   b) ���� solutions.xml ʱ���ͻ���Ҫ�����Э��� handle ����Ϊ clientscript.
	   c) ʹ�����ӣ�zeze\UnitTestClient\Program.cs��

	8) �ͻ���ʹ��Unreal(cxx)+lua
	   a) ����lualib, ��Ҫ����includepath
	   b) ֱ�Ӱ�cxx�µ����д���ӵ���Ŀ�С�����ToTypeScript��صġ�
	   c) ���� solutions.xml ʱ���ͻ���Ҫ�����Э��� handle ����Ϊ clientscript.
	   d) ʹ�����ӣ�zeze\UnitTestClientCxx\main.cpp

	9) ����ͬ�� More
	   ��Zeze ���������Ի���������ʡ���
	   �����û���ͬ���Ժ�������ڲ�ͬ����ʵ��֮�䲻ͣ�������ݣ����ܻ�����������½���
	   ��ʱ����Ҫ��һ���Ĺ滮��Game2 �ṩ�� ModuleRedirect �� Transmit ֧��������������ʡ�
	   ��ϸ���Ķ�������ĵ����ߴ��롣
	   a) Game2\linkd.provider.txt
	   b) Game2\README.md 
	   c) Game2\server\Game\Login\Onlines.cs

	����Ҫ��׽�쳣��
	�������Ҫ������󣬲�׽�������Ժ��ٴ��׳���

#### ����Ļ���

	0. ���ǲ���������������Ҳ����Ҫ�����⡣

	1. ������Ļ��ֹ���Ӧ�ø������������������Ƿ����һ�������С�

	2. һ��������Event��ģʽ����ʱҪע��Event��ִ���Ƿ���Ƕ���ڴ�����������ִ�С�
	   �����ǳ���������������ģ��μ���һ������Ӧ������Event�ɷ��ŵ�����������С�
	   �����������ɷ�ѡ��ȫ���ɷ�һ���������ÿһ���ɷ�һ������Ҳ�����ɷ���������ִ�С�
	   ���ӣ�ChangeListener��ʹ�á�Zeze.Util.EventDispatcher

#### ��ʷ

	д����һ��ʼ���ҾͶԼ��״̬���޸����ݸе��������ر��ǳ����ӷ�ģ���Ժ�
	��ʱ������е�״̬������޸����ݣ�����Ҫÿ��ģ��״̬��������ȡ������ǰһ��
	�жϡ�����һֱϣ�����и����񻷾���������״̬����ȷʱ���ع����е��޸ģ�������
	�ָ�����ʼ��ʱ��2007���ʱ�򣬿�ʼ����Ϸ������javaд�˸�xdb���ڳ�����֧������
	����汾����Ҫ��������ʱ�����ϼ��������ڷ������ݵ�˳����߼���أ����п���������
	��ʱ�Ľ��������ʹ��java��������⣬���������ʹ�����������߳���Ա��һ������
	��ʼ��ʱ���������Ҫ���ʵ����ݵ�������ǰ�����ǿ����������ϡ�
	�����ͳ�Ϊxdb�������⣬Ҳ��xdb������õĵط���
	2013���ʱ�򣬵�ʱ��ͬ�� pirunxi ������ֹ��������е������޸��Ƚ��ڱ������пɼ���
	ִ�������Ժ󣨴�ʱ����֪�����е����ݣ��Ϳ�������������Ͳ��������ˣ���
	�������ж�����״̬������ͻ�Ļ�������ɹ�����ͻ�Ļ��������е���������
	�������������������⣬ϵͳ�����Դ��������
	��2014-2017��䣬pirunxi ʵ���˺ö�������ֹ����İ汾��
	�Ҵ����2015�꿪ʼ�������ۡ�
	���꣨2020���¹������ڼ䣬���ź��Ӳ�����ߣ�������û�¡�
	��һ�ξ����� pirunxi ���°汾�������
	Ȼ������û�£���д�� Zeze ����汾��
	�����ҵĵ�һ�� c# ����
	���統ʱxdb���ҵĵ�һ��java����

#### ��ϵ

QQȺ��118321800
