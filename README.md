# zeze

	zeze ��һ��֧�������Ӧ�ÿ�ܡ�
	ΪʲôҪ����
	����������κεط��������׳��쳣����ô��û������֧�ֵĻ�����д����ȷ�Ĵ����Ƿǳ�����ġ�
	��������֧�֣��쳣�ᵼ������ʧ�ܣ������������ݡ�

	��Ӧ�õ����ݷ�����������ִ�С�
	����Ҳ�ṩ��һ���򵥵������ܡ�
	��Ҫ����
	1) ֧�������������ʧ�ܣ���ѷ��������޸ģ������ݻָ����޸�ǰ��״̬��
	2) ֧���Զ��־û���������ݿ⣬Ŀǰ֧��SQLServer��Mysql��
	3) ֧�����ݽṹ(Bean)�������ɾ������������Ҫת���ɿ⡣
	4) ֧��dynamic����������Ϊ�������ָ���ĵ�Bean���ͣ�����������װʵ�����ݣ�bean������̬�̳����ӳ�䡣
	5) ֧�ֺ�˶����ݿ⣬�������������ݿ����ܲ��㡣����Zeze��������Դ��Ҫ���Ա���cache�����У�һ�㲻�ᷢ��������ݿ����ܲ��㡣
	6) ֧�ֶ���߼�����������һ��������ݿ⣨�Զ�cacheͬ��������������ʵ��ȫ��ͬ�����߼����������ܲ��㣬ֱ������Ϳ����ˡ�
	7) ��������֧�ּ���ѹ����ʹ�� Diffie�CHellman key exchange �����ܳס�
	8) �ͻ��������Ŀǰ֧��c++��cs��֧��Э�����ɵ�lua����lua��ʵ���߼���
	9) ����֧�����ݱ�����ģ�ChangeListener���Ϳͻ�������ͬ��������map��set���͵ı��������Եõ����������еĸı䣬
	   ��������Ҫ�Ѹı�ͬ�����ͻ��ˡ�����Ҫ����ʹ�������ַ�ʽͬ�����ݣ���ô�����ж��ٸ��ط�����ʽ���޸ĸ����ݣ�ֻ��Ҫһ���ط�ͬ�����ݡ�

#### ��װ�̳�

	Zeze ��һ����⣬���еĺ��Ĺ��ܶ������
		1 ��������������ֱ�Ӱ�Zeze\Zeze.csproj�ӵ��Լ���sln�֡�ֱ������Դ����Ŀ��
		2 �ͻ��ˣ�unity��������ֱ�ӿ���Zeze�µ�Դ�������Ŀ��Ҳ�����Լ�������Ӧƽ̨�İ汾�������� unity plugins �¡�
	Gen ��һ������̨���򡣱�����Ժ��������ɴ��롣
	GlobalCacheManager ��һ������̨���򡣵����Application�������ݿ�ʱ����������Cacheͬ�����μ�����"����ģʽ"�ĵ�4�㡣

#### ʹ��˵��

	. �����Լ��������������ݣ�������������(bean)��Э��(protocol)�����ݿ���(table)�ȡ�
	  �ο���Game\solution.xml; UnitTest\solution.xml

	. ʹ�� Gen.exe ���ɴ��롣

	. �����ɵ�Module���У�ʵ��Ӧ��Э�飬�������ݿ������߼�������

	. ���ã�zeze.xml��
	  �ο���Game\zeze.xml; UnitTest\zeze.xml
	  һ����˵����ʼ��Ҫ�ṩһ�����ݿ����ã�������������Ĭ�ϵġ�
	  ���ṩ���õĻ������ݿ����ڴ�ġ�

	. ʲôʱ�򴴽��洢���̣�Zeze.NewProcedure��
	  ���ڿ��Ĭ��Ϊÿ��Э�鴴���洢���̣�һ����˵������Ҫ�Լ�������
	  �������Ҫ���񲿷�ʧ�ܵ�ʱ�򲻻ع����������Ǿ���ҪǶ�����񣬴�ʱ��Ҫ�����Լ��Ĵ洢���̲��ж�ִ�н����
	  int nestProcedureResult = Zeze.NewProcedure(myaction, "myactionname").Call();
	  // check nestProcedureResult

	. Bean����������ݶ���������԰���Bean�����������������ֿ��԰���Bean��
	  reference�����е� bean ���ò������ظ����������л���TODO Gen��ʱ���⻷����
	  null�����е� bean ���ò���Ϊ null��ʹ�õ�ʱ����Ҫ�жϣ����Լ򻯴��롣
	  Assign��Bean �а�����Bean������������û�� setter�������Ҫ��������ֵ��ʹ�� Bean.Assign ������
	  Managed��Bean������Table���߱�����һ���Ѿ�Managed״̬��Bean��������֮ǰ�Ƿ�Managed״̬��
	    ��ʱ�޸�Bean���ᱻ��¼��־��Managed״̬һ�����ã��Ͳ���ָ�����ʹ���Table�л���������ɾ������
	    �����Table����������ɾ����Ҫ�ٴμ����ȥ����ҪCopyһ�Ρ�
	    Managed״ֻ̬�ܱ�����һ�Σ��ο������reference˵���������������ظ��Ķ���ʹ�� Bean.Copy ��������һ�ݡ�
	  binary��������͵��ڲ�ʵ����byte[]������ֱ����������û�������޸ı���������Ŀǰ����binary����ֱ�ӱ�����������
	    ֻ�ܶ�����Bean�У������ṩ��������Ժͷ������з��ʡ�
	  dynamic: Bean�ı��������Ƕ�̬�ģ����������汣�治ͬ��bean������������Կɶ�����ר�ŵ�д(setter)����������ΪvariablenameSet��
	    see Game\solutions.xml��Game.Bag.BItem�Ķ��塣

	.Bean.TypeId
	  Ĭ��ʹ��Zeze.Transaction.Bean.Hash64(Bean.FullName)���ɡ�
	  ��ͻʱ����Ҫָ��һ�������߸ı�Bean.FullName������������ɵ�����ʱ�����óɾɵ�hashֵ��

	.Protocol.id Rpc.id
	  Ĭ��ʹ��Zeze.Transaction.Bean.Hash16(Protocol.FullName)���ɡ�
	  ��ͻʱ����Ҫָ��һ����

	XXX Do Not Catch Exception
	  ԭ���ϲ�Ҫ��׽�쳣�������ʵ����Ҫ����׽�����Ժ����ٴ��׳���

	. UserState
	  AsyncSocket.UserState ����������
	  Protocol.UserState ��ĳ�������յ�������Э��������ģ�Ĭ�ϴ����������ĸ������á�
	  Transaction.RootProcedure.UserState ��ΪЭ�鴦�����洢����ִ������ʱ��Ĭ�ϴ�Э�������ĸ������á�
	  ���������Ӧ�ô�Ӧ�����忼����ʹ�ã�һ����˵���ڷ����������������û��ĵ�¼�Ự��Login.Session����
	  see Game\game.sln, Game2\game2.sln

	. ������־
	  ��Э�飨���ߴ洢���̣�ִ���׳��쳣���߷���ֵ����Procedure.Successʱ��Zeze���¼������־����Ϊÿһ�ַ���ֵͳ�ơ�
	  Zeze ��¼��־��ʱ���� UserState.ToString Ҳ��¼��ȥ��Ӧ�ÿ������Լ���UserState����ʵ��������Ӹ�����������Ϣ��
	  ����, Login.Session.SetLastError("detail");
	  ����д��ʱ��ֻ��Ҫ���ش��󣬲���ÿ���ط��Լ���¼��־��

	. Э��洢���̴���������ֵ�滮����
	  0  Success
	  <0 Used By Zeze 
	  >0 User Defined. �Զ��������ʱ�������� (Module.Id << 16) | CodeInModule��
	  ע��Э��洢���̷���ֵʹ��ͬһ������ռ䡣

#### ����ģʽ

	0. AutoKey��������key����֧�� long ���͡�
	   ��Ϸ������Ҫ�������ֳɲ�ͬ�ķ�������Ȼ������Ҫ�����������Ժ�ķ������ϲ�������Ա���keyû��һ���滮���ϲ���ʱ��ͺܸ��ӡ�
	   �ṩһ��������key��һ��ʼ�ͶԹ滮��Χ�ڵķ���������Ψһ��key���ϲ����Ͳ����ͻ��
	   ���òο���UnitTest\zeze.xml
	   ���� AutoKeyLocalId="0" ���ط�������Id�����з�������Ψһ���ù���Ҳ�����ٴ�ʹ�á�
	   ���� AutoKeyLocalStep="4096" ������keyÿ�����Ӳ�����Ҳ�ǿ��Դ����ķ��������������
	   �滮����������������������key�ͻ��ṩ�������е�idΨһ��

	1. �����ݿ�֧��
	   �ṩ��� DatabaseConf ���á�������ݿ���Ҫ�ò�ͬ Name ���֡�
	   Ȼ���� TableConf ��ʹ������ DatabaseName �ѱ����䵽ĳ�����ݿ��С�
	   ���òο���UnitTest\zeze.xml

	2. ���ϵ����ݿ���װ������
	   ��ʹ��ĳЩǶ��ʽ���ݿ⣨����bdb��ʱ�����ĳ�����ݿ��ļ��ܴ󣬵��ǻ�Ծ���ݿ����ֲ��࣬ÿ�α������ȽϷ�ʱ��
	   ���Կ��ǰѱ���Ƶ��µ����ݿ⣬Ȼ��ϵͳ���¿����Ҳ�����¼ʱ���Զ����Ͽ���װ�����ݡ�
	   �������Ͽ���ֻ���ģ�����ÿ�α��ݡ�
	   TableConf ��ʹ������ DatabaseOldName ָ���ϵ����ݿ⣬������ DatabaseOldMode ��Ϊ 1������Ҫʱ��Zeze �ͻ��Զ����Ͽ���װ�ؼ�¼��
	   ���òο���UnitTest\zeze.xml

	3. ��� Zeze.Application ֮�������
	   һ����˵�������������һ�� Zeze.Application �����ݿ���
	   �����Ҫ�ڶ�� Zeze.Application ֮��֧������Ӧ��ֱ�ӷ��ʲ�ͬ App.Module ����ı�񼴿��������֧�֡�
	   �������������ύ(Checkpoint)Ĭ������һ�� Zeze.Application ��ִ�еģ�Ϊ���������ύҲԭ�ӻ�����Ҫ��App.Startǰ����ͳһCheckpoint��
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

	4. Cacheͬ������� Zeze.Application ʵ������ͬһ��������ݿ�
	   һ���ģʽ�Ǻ�����ݿ����һ�� Zeze.Application ���ʡ������Ҫ���App����һ�����ݿ⣬��Ҫ����Cacheͬ�����ܡ�
	   1) ���� GlobalCacheManager
	   2) ���� zeze.xml �����ԣ�GlobalCacheManagerHostNameOrAddress="127.0.0.1" GlobalCacheManagerPort="5555"
	      ���òο���UnitTest\zeze.xml
	   *) ע�⣬��֧�ֶ��ʹ��ͬһ�� GlobalCacheManager ͬ����Cache�� Zeze.Application ֮������񡣲μ�����ĵ�3�㡣
	      ��Ϊ Cache ͬ����Ҫͬ����¼�ĳ���״̬�������ʱ Application ʹ��ͬһ�� Checkpoint����¼ͬ������Ҫ�ȴ��Լ�����������
	   *) �����߼���������GlobalCacheManager֮������ӷǳ���Ҫ����������Ӧ��������һ���ɿ��������У�һ����˵����������һ�������С�
   
	5. �ͻ���ʹ��Unity(csharp)+TypeScript
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
	      ��֪����ô�������������ڲ��������ǰ�encoding.js encoding-indexes.js ������output�¡����� encoding.js ����Ϊ text-encoding.js��

	6. �ͻ���ʹ��Unreal(cxx)+TypeScript
	   a) ��zeze\cxx�µ����д��뿽�������Դ��Ŀ¼���Ҽӵ���Ŀ�С�����Lua��صļ����ļ���
	   b) �� zeze/TypeScript/ts/ �µ� zeze.ts ��������� typescript Դ��Ŀ¼��
	      ���� npm install https://github.com/inexorabletash/text-encoding.git
	   c) ��װpuerts����������ue.d.ts��
	   d) ���� solutions.xml ʱ��ts�ͻ���Ҫ�����Э��� handle ����Ϊ clientscript.
	      ʹ�� gen ����Э��Ϳ�ܴ��롣
	   e) zeze\cxx\ToTypeScriptService.h ����ĺ� ZEZEUNREAL_API �ĳ������Ŀ�ĺ����֡�
	   f) ���� https://gitee.com/e2wugui/ZezeUnreal.git
	      ��֪����ô��������(text-encoding)������unreal�и�puerts�ã����Կ��ǰ�encoding.js encoding-indexes.js������Content\JavaScript\���棬
	      ���� encoding.js ����Ϊ text-encoding.js��

	7. �ͻ���ʹ��Unity(csharp)+lua
	   a) ��Ҫѡ�����Lua-Bind����⣬ʵ��һ��ILuaʵ�֣��ο� Zeze.Service.ToLuaService.cs����
	   b) ���� solutions.xml ʱ���ͻ���Ҫ�����Э��� handle ����Ϊ clientscript.
	   c) ʹ�����ӣ�zeze\UnitTestClient\Program.cs��

	8. �ͻ���ʹ��Unreal(cxx)+lua
	   a) ����lualib, ��Ҫ����includepath
	   b) ֱ�Ӱ�cxx�µ����д���ӵ���Ŀ�С�����ToTypeScript��صġ�
	   c) ���� solutions.xml ʱ���ͻ���Ҫ�����Э��� handle ����Ϊ clientscript.
	   d) ʹ�����ӣ�zeze\UnitTestClientCxx\main.cpp

	9. ChangeListener �Ϳɿ�����ͬ��
	   ����ͬ����ʹ��������ܿ���ֻ��Ҫ����һ�Σ��Ժ��κ��޸Ķ���õ�֪ͨ��
	   ���⣺
	     GetData �� ChangeNotify ֮���ԭ���ԡ�
	   ���������
	     �������ȱ�����ȷ�ϡ�
	     �� Online.Data ��������һ�� Queue��MarkNameSet��
	     GetData ͬʱ���� MarkNameSet.Add(ListenerName)
	     OnChange: Queue.Add(Notify), if (Online) Send(Notify)
	     Confirm: �ƽ�ConfirmCount��
	     ReLogin: ͬ�� Queue��ͬʱ�����ƽ�ConfirmCount����
	     �����������Ҳ˳�����˶������������⡣����ͬ��Queue��

	*. �����ο�
	   "Game/��Ϸʹ�÷�������.txt"

#### ����Ļ���

	0. ���ǲ���������������Ҳ����Ҫ�����⡣

	1. ������Ļ��ֹ���Ӧ�ø������������������Ƿ����һ�������С�

	2. һ��������Event��ģʽ����ʱҪע��Event��ִ���Ƿ���Ƕ���ڴ�����������ִ�С�
	   �����ǳ���������������ģ��μ���һ������Ӧ������Event�ɷ��ŵ�����������С�
	   �����������ɷ�ѡ��ȫ���ɷ�һ���������ÿһ���ɷ�һ������Ҳ�����ɷ���������ִ�С�
	   ���ӣ�ChangeListener��ʹ�á�

#### ���е����ݿ����񴫵ݵ�����

	��һ�������дӱ��еõ����������ô�����һ����������𲻿�Ԥ֪�����⡣
	��ǰxdb���˼�Ᵽ��������Zeze��û��������ơ�
	����Ҫ��������ʱ������ʹ���������ַ�ʽ��
	1��ʹ�� Bean.Copy
	2������ table.key���ȱ�Ҫ�Ĵ�ֵ���������������²��

#### ��ʷ

	д����һ��ʼ���ҾͶԼ��״̬���޸����ݸе��������ر��ǳ����ӷ�ģ���Ժ󣬴�ʱ������е�״̬������޸����ݣ�����Ҫÿ��ģ��״̬��������ȡ������ǰһ���жϡ�
	����һֱϣ�����и����񻷾���������״̬����ȷʱ���ع����е��޸ģ������ݻָ�����ʼ��ʱ��
	2007���ʱ�򣬿�ʼ����Ϸ������javaд�˸�xdb���ڳ�����֧����������汾����Ҫ��������ʱ�����ϼ��������ڷ������ݵ�˳����߼���أ����п���������
	��ʱ�Ľ��������ʹ��java��������⣬���������ʹ�����������߳���Ա��һ������ʼ��ʱ���������Ҫ���ʵ����ݵ�������ǰ�����ǿ����������ϡ�
	�����ͳ�Ϊxdb�������⣬Ҳ��xdb������õĵط���
	2013���ʱ�򣬵�ʱ��ͬ�� pirunxi ������ֹ��������е������޸��Ƚ��ڱ������пɼ���ִ�������Ժ󣨴�ʱ����֪�����е����ݣ��Ϳ�������������Ͳ��������ˣ���
	�������ж�����״̬������ͻ�Ļ�������ɹ�����ͻ�Ļ��������е����������������������������⣬ϵͳ�����Դ��������
	��2014-2017��䣬pirunxi ʵ���˺ö�������ֹ����İ汾���Ҵ����2015�꿪ʼ�������ۡ�
	���꣨2020���¹������ڼ䣬���ź��Ӳ�����ߣ�������û�¡���һ�ξ����� pirunxi ���°汾�������
	Ȼ������û�£���д�� Zeze ����汾�������ҵĵ�һ�� c# �������統ʱxdb���ҵĵ�һ��java����

#### ��ϵ

QQȺ��118321800
