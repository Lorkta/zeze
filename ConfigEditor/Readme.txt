
���ñ༭��

����
	��̬��ɾ�У�
	�Զ�����ʶ��(Gen)��
	֧������(list)��
	�Զ���ɣ�
	���ݼ�ʱ��֤(ֻ������߱����������������)��Properties: unique;id;server;client;url;dns;file;...

˵��
	ÿһ�б�ʶ�𿴳�һ��Bean��ÿһ�п���һ��������
	������ ','	��Bean�����������ܱ༭�����˫������ѡ�лس�ʱ�����µ��С�
	������ '[...'	������(List)�Ŀ�ʼ�����ܱ༭��
	������ ']...'	������(List)�Ľ��������ܱ༭�����˫������ѡ�лس�ʱ����list�е�bean��
	                  ע��������Item���û����д���ݲ��ᱻ���档

TODO
	CellToopTipTextNeeded 
1private void dgv_PropDemo_CellToolTipTextNeeded(object sender, DataGridViewCellToolTipTextNeededEventArgs e)
2{
3     //����ڵ����еĵ�Ԫ������ʱ��ʾ��ʾ��Ϣ
4     if (e.ColumnIndex == 2)
5     {
6        e.ToolTipText = "��:" + e.ColumnIndex.ToString() + ",��:" + e.RowIndex.ToString();
7     }
8 }

	define �༭��
	����ʶ���Gen��
	�Զ���ɺ�enumʶ��
	1 id��һ�������Զ����������ߴ���һ�е�id������һ��δ�õġ�
	2 ��ͨ����Ĭ�����ʹ�õ�n��ֵ���������������в�����ƥ��ġ�
	SaveAs

����
	1 Browse Dialog ��ʼ��ʾλ��ƫ�룬���ܸ�windows�Ŵ��йأ���������Ҳ����ԡ�
	2 Grid.Column.Width �����ڶ����Bean.Var�У����Bean���ദ���û��߶��ʵ��������List�У���
	  ��ô��Щ�й���һ������. ��Ȼ�༭��ʱ�򣬿��԰�ͬһ��Bean.Var���е����ɲ�ͬ��Width��
	  ��ʱBean.Var�б������һ�ε���Column.Widthʱ��ֵ��
	3 ��Ҫ�ڱ༭�����ⲿֱ�Ӹ�����
	  �ļ���������·��������ô����������ã�ɨ�����е������ļ���
	4 VarComment ��֧�ֵ���ע�͡������ٿ��ǡ�
