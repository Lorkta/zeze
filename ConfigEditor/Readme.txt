
���ñ༭��

����
	��̬��ɾ��;
	֧������(�༭ʱֻ��list��������������Map����);
	���ݼ�ʱ��֤(ֻ��ʾ���������������)��
	�Զ���ɣ�

˵��
	ÿһ�б�ʶ�𿴳�һ��Bean��ÿһ�п���һ��������
	������ ','	��Bean�����������ܱ༭�����˫������ѡ�лس�ʱ�����µ��С�
	������ '[...'	������(List)�Ŀ�ʼ�����ܱ༭��
	������ ']...'	������(List)�Ľ��������ܱ༭�����˫������ѡ�лس�ʱ����list�е�Item��
	                  ע��������Item���û����д���ݲ��ᱻ���档

ע��
	ʹ��Դ������
		�༭����ʹ��xml���ı������ļ��������ݱ���׷�ٱ仯��
		Build������Release����Ҳ�����ύһ�ݵ�Դ�����⣻
	���˱༭
		ע���漰id,unique���У�����������������ͬ��ֵ���ᵼ����֤ʧ�ܣ�
	��Ҫ�ڱ༭�����ⲿֱ�Ӹ��ļ�������·������
	VarComment ��֧�ֵ���ע�͡������ٿ���;
	����(��)������Ҫ����ȫ�����ã�������Ż���ʹ��ʱע��;

Test
	* BUG ������FormError�Ĵ���λ�á�������ʱ���ɳ�ʼ����λ�á�

	2021/1/10
	* CHANGE �ع���DataGridView �ĳ� VirtualMode���Ķ��ϴ��п��ܵĻ����һع����һ�¡�
	* XXX ˫�������б�λ���ĵ��Ĺ�����ʱ�������ˡ�

	2021/1/11
	* NEW ����ʱ����Home������һ���ļ�����������ͬʱ���������༭��

	2021/1/12
	* CHANGE װ���ĵ������첽ģʽ�����ڴ��ļ�ʱ������ʱ����ͬ��װ�ء�
	* Build �ĳ� async��ʵ����ֻ��һ���߳���ִ�У�����Ϊ����ʾ���ȺͿ���ȡ����
	* Build �󣬹رյ�û�д�View����View������Document��
	* FormError ������ UI-thread ����ִ�У�ֻ�� AddError RemoveError ������Ҫʹ�� BeginInvoke. 
	* FormBuildProgress ��ʾ��ɫ��

	2021/1/13
	Document.IsChanged���������е�public Property������������IsChanged������һ�£�����Ժ󲻺�ά����

����
	* ��ǧ�п�������ô����

TODO
	�������� ����Ҫ���� var ���� BeanDefine �����֣��Լ�������á������ʵ�� Bean �����ˡ�
	����������BeanDefine.ref ��������¼�������ĳ� File.RelateName + VarName����ΪǶ��list�����ֱ��뻹ûȷ����
		��һ�� {File.RelateName}:VarName;
		file0.BeanLevel0
			list1: file0.BeanLevel0.BeanList1 -> file0.BeanLevel0:list1
		file0.BeanLevel0.BeanList1
			refby file0.BeanLevel0:list1;
			list2: file1.BeanList2 -> file0.BeanLevel0:BeanList1:list2
			list4: file1.BeanList2.BeanList3 -> file0.BeanLevel0:BeanList1:list4
		file1.BeanList2
			refby: file0.BeanLevel0:BeanList1:list2
			list3: file1.BeanList2.BeanList3 -> file1.BeanList2:list3
		file1.BeanList2.BeanList3
			refby: file1.BeanList2:list3
			refby: file0.BeanLevel0:BeanList1:list4
			var: int
	
	VerifyAll async������Ƚ��鷳���������ǣ���Ҫ Document ��������ʵ��ʹ�ã��Ժ���˵�ˡ�
	�Զ����: Foreign
	�����Զ���ɣ�
		��ͨ����Ĭ�����ʹ�õ�n��ֵ���������������в�����ƥ��ġ�
	id Load ��ʱ���¼ maxid���Ժ�༭AddRow��ʹ�����������
	Bean��������Ҫ�������á�
	SaveAs
	enum ���ڲ�֧�������������ط�����ģ�����Ҫ�������ӡ�
