
start ..\GlobalCacheManager\bin\Debug\net5.0\GlobalCacheManager.exe
start ..\ServiceManager\bin\Debug\net5.0\ServiceManager.exe

copy /y zeze.xml .\linkd\bin\Debug\net5.0\
copy /y zeze.xml .\server\bin\Debug\net5.0\

start .\linkd\bin\Debug\net5.0\linkd.exe
start .\server\bin\Debug\net5.0\server.exe -AutoKeyLocalId 0

pause

goto end
@rem �����������֮���ٲ��Ը���gsʵ����ʹ����ͬ�������ļ�������AutoKeyLocalId�������ҪΨһ��ͨ���������á�
.\server\bin\Debug\net5.0\server.exe -AutoKeyLocalId 1
:end
