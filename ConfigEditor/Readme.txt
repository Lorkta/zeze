
���ñ༭��

����
	��̬��ɾ�У�
	�Զ�����ʶ��(Gen)��
	֧������(�༭ʱֻ��list��������������ʱ������Map)��
	���ݼ�ʱ��֤(ֻ��ʾ���������������),
	�Զ���ɣ�

˵��
	ÿһ�б�ʶ�𿴳�һ��Bean��ÿһ�п���һ��������
	������ ','	��Bean�����������ܱ༭�����˫������ѡ�лس�ʱ�����µ��С�
	������ '[...'	������(List)�Ŀ�ʼ�����ܱ༭��
	������ ']...'	������(List)�Ľ��������ܱ༭�����˫������ѡ�лس�ʱ����list�е�Item��
	                  ע��������Item���û����д���ݲ��ᱻ���档

Test
	define �༭�����깤��
	Ƕ��list���⣺define��addʱ��������item�����ֹ�һ�Σ�����û�з��֣������ˣ�����ԣ���

TODO
                DataGridViewCellStyle cstyle = new DataGridViewCellStyle();
                cstyle.BackColor = Color.GreenYellow;

                for (int i = 0; i < Flag.Length; i++)
                {
                    if (Flag[i] == "1")
                    {

                        //dgr.DefaultCellStyle.ForeColor = Color.Blue;

                        dgr.Cells[0].Style = cstyle;
                    }	����˳���������֧����Define��ʱ��ı䡣Ȼ������װ�����д򿪵�grid���ֲ��޸�̫�鷳����
��ʽ3 ,����DataGridViewButtonCell��FlatStyle���ԣ�Popup����Flat.

DataGridViewRow row = new DataGridViewRow();
DataGridViewButtonCell dg_btn_cell = new DataGridViewButtonCell();
dg_btn_cell.Value = "Component" + i;
dg_btn_cell.FlatStyle = FlatStyle.Flat;//FlatStyle.Popup;
dg_btn_cell.Style.BackColor = Color.Red;
dg_btn_cell.Style.ForeColor = Color.Black;

	��Ϣ���ش����̡߳�FormMain.BeginInvoke();
	������������Ҫ����Foreign��
	Bean��������Ҫ������Ӵ���á����Ҹ����鷳��
	����ʶ���Gen��
	�Զ���ɺ�enumʶ��
	1 id��һ�������Զ����������ߴ���һ�е�id������һ��δ�õġ�
	2 ��ͨ����Ĭ�����ʹ�õ�n��ֵ���������������в�����ƥ��ġ�
	SaveAs

	Gen ��ת�����ݣ�������ʶ���ͳ�ƣ���Ȼ�����ɴ��롣
	?List.Count <= 1ʱ������һ��Beanʵ���������ܵݹ�(����null-able�Ļ����Ҳ������ϵݹ�����Ҳ����)?
	Bean1
	   List<Bean2> varlist2;
	   --> Bean2 varlist2
	Bean2
	   List<Bean2> varlist2; // ���ֵݹ�Ļ����Ͳ��ܼ򻯣���Ϊ�ڲ�Ƕ�׿��ܴ���1���ݹ��ж�Ƕ��Ҳ<=1������̫����Ҳ���ô���
	   List<Bean3> varlist3; // ���ֵݹ�Ļ���������ԣ���ȷ�ϣ���������������
	Bean3
	   List<Bean2> varlist3;

����
	1 Browse Dialog ��ʼ��ʾλ��ƫ�룬���ܸ�windows�Ŵ��йأ���������Ҳ����ԡ�
	2 Grid.Column.Width �����ڶ����Bean.Var�У����Bean���ദ���û��߶��ʵ��������List�У���
	  ��ô��Щ�й���һ������. ��Ȼ�༭��ʱ�򣬿��԰�ͬһ��Bean.Var���е����ɲ�ͬ��Width��
	  ��ʱBean.Var�б������һ�ε���Column.Widthʱ��ֵ��
	3 ��Ҫ�ڱ༭�����ⲿֱ�Ӹ�����
	  �ļ���������·��������ô����������ã�ɨ�����е������ļ���
	4 VarComment ��֧�ֵ���ע�͡������ٿ��ǡ�
