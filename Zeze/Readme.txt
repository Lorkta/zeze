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
DataInDB DataInTrans                                                                 ʵ�ʲ���
   null           null                 ������ɾ�������ݿ���û�У�  cache.remove      delete
  !null           null                 ������ɾ�������ݿ����У�    db.delete
   null           !null                ������put,���ݿ���û�У�    db.replace        replace
  !null           !null                ������put,���ݿ����У�      db.replace

���� cache��ֻҪ���� table �ļ�¼���� cache �в�����ʱ��������һ�� record��
����ɾ�������ڵļ�¼�������Ҫ��ĥһ�£�Ҳ����˵ɾ��Ҳ���� lockandchecknoconflit����

��д��
Record.Commit
