
start ..\GlobalCacheManager\bin\Debug\net8.0\GlobalCacheManager.exe
start ..\ServiceManager\bin\Debug\net8.0\ServiceManager.exe

copy /y linkd\linkd.xml .\linkd\bin\Debug\net8.0\
copy /y server\zeze.xml .\server\bin\Debug\net8.0\serverd.xml

start .\linkd\bin\Debug\net8.0\linkd.exe
start .\server\bin\Debug\net8.0\server.exe -AutoKeyLocalId 0

pause

goto end
@rem �����������֮���ٲ��Ը���gsʵ����ʹ����ͬ�������ļ�������AutoKeyLocalId�������ҪΨһ��ͨ���������á�
.\server\bin\Debug\net8.0\server.exe -AutoKeyLocalId 1
:end
