<!--
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

-->
ConcurrentDictionary

Assign
Copy

Bean3
{
	int i1;
	public void Assign(Bean3 other)
	{
		i1 = other.i1;
	}
	public Bean3 Copy()
	{
		var copy = new Bean3();
		copy.Assign(this);
		return copy;
	}
}

Bean2
{
	Bean3 b3;
	List<Bean3> lb3;
	
	public void Assign(Bean2 other)
	{
		b3.Assign(other.b3);
		lb3.Clear();
		foreach (var e in other.lb3)
			lb3.Add(e.Copy());
	}

	public Bean2 Copy()
	{
		var copy = new Bean2();
		copy.Assign(this);
		return copy;
	}
}

Bean1
{
	int V1;
	Map<int, int> V2;
	List<Bean2> V3;
	Bean2 V4;

        public void Assign(Bean1 other)
        {
            this.V1 = other.V1;
            this.V2.Clear();
            foreach (var e in other.V2)
                this.V2.Add(e.Key, e.Value);
            foeach (var e in other.V3
	        this.V3.Add(e.Copy());
            V4.Assign(other.V4);
        }

        public Bean1 Copy()
        {
            var copy = new Bean1();
            copy.Assign(this);
            return copy;
        }
}
