Module ���� Table Ĭ��˽�У�����ͨ�����ӷ������ز���¶��ȥ
Module ʹ�� partical �����ɲ��ַŵ�gen��
Config ������
ByteBuffer.SkipUnknownField �Ƶ������ط�
XXX ͳһ bean �� xbean ����Ļ�����ô�������ɣ��� table ���õ� bean �Զ�����һ�ݵ�ĳ���ر��Ŀ¼
ȥ�� xbean.Const xbean.Data
ȥ�� select����������ֱ���� bean duplicate ֧��
managed
���ݱ��

<application name="demo��>
	<bean name="b1">
		<var id="1" default="1" name="s" type="int"/>
	</bean>

	<module name="m1">
		<bean name="Value">
			<var id="1" default="1" name="s" type="int"/>
		</bean>
		<cbean name="Key">
			<var id="1" default="1" name="s" type="short"/>
		</cbean>

		<protocol name="p1" id="1"/>
		<table name="table1" key="Key" value="Value"/>
	</module>
</Application>