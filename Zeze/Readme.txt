Module ���� Table Ĭ��˽�У�����ͨ�����ӷ������ز���¶��ȥ
Module ʹ�� partical �����ɲ��ַŵ�gen��
Module ���� IModule �� Module������IModule�ӿڷ���������ɾ��������������ȣ����������Ա���

Э���������ֵĬ�ϴ��ڵ���0��֧�ָ�����Ҫ��ȷ������
Config ����
ByteBuffer.SkipUnknownField �Ƶ������ط�
XXX ͳһ bean �� xbean ����Ļ�����ô�������ɣ��� table ���õ� bean �Զ�����һ�ݵ�ĳ���ر��Ŀ¼
ͳһ���ɹ��ߣ�Ӧ�ÿ�ܺ����ݴ洢��ܶ���ֿ������ߵ�����bean�Ķ���Ӧ���ǲ�һ���ģ����õĿ����Ժ�С��
	��Ҫ��� application ���ݿⶨ�� database. database.module ������ application �д��ڡ�

ȥ�� xbean.Const xbean.Data
ȥ�� select����������ֱ���� bean duplicate ֧��
managed
���ݱ��?
Net.Manager ��ô���¶��壿�������������

<application name="demo" ModuleIdAllowRange="1-3,5">
	
	<bean name="b1">
		<enum name="Enum" value="4"/>
		<var id="1" name="s" type="int" default="1"/>
		<var id="2" name="m" type="map" key="int" value="int"/>
	</bean>

	<module name="m1" id="1">
		<bean name="Value">
			<var id="1" default="1" name="s" type="int"/>
		</bean>
		<cbean name="Key">
			<var id="1" default="1" name="s" type="short"/>
		</cbean>

		<protocol name="p1" id="1" argument="Value" handle="server,client"/>
		<rpc name="r1" id="2" argument="Value" result="Value" handle="server"/>
		<table name="table1" key="Key" value="Value"/>
	</module>

	<project name="gsd" language="cs">
		<!--
		mudule ���Ա���� manager ���ã���ֻ������һ��.
		�����Ļ���Э�鴦�������Ҫ���ݵ�ǰmanager����ͬʵ�֡�
		���ɵ�ʱ�򾯸洦��
		-->
		<manager name="Server" handletype="server|client" class="gsd.Provider">
			<module ref="m1"/>
		</manager>
	</project>
</application>
