@echo off
setlocal
pushd %~dp0

set zeze_src_dir=..
set project_dir=.
set gen=..\Gen\bin\Debug\net6.0\Gen.exe

%gen% -c ExportConf -ZezeSrcDir %zeze_src_dir%

md %project_dir%\Zeze 2> nul

md %project_dir%\Zeze\Net 2> nul
xcopy /Y %zeze_src_dir%\Zeze\Net %project_dir%\Zeze\Net

xcopy /Y %zeze_src_dir%\Zeze\IModule.cs %project_dir%\Zeze\
xcopy /Y %zeze_src_dir%\Zeze\AppBase.cs %project_dir%\Zeze\
xcopy /Y %zeze_src_dir%\Zeze\Config.cs  %project_dir%\Zeze\
xcopy /Y %zeze_src_dir%\Zeze\Config.cs  %project_dir%\Zeze\

md %project_dir%\Zeze\Util
xcopy /Y %zeze_src_dir%\Zeze\Util\ResultCode.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\Mission.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\Scheduler.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\AtomicLong.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\FixedHash.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\Reflect.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\Time.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\Comparer.cs %project_dir%\Zeze\Util\

md %project_dir%\Zeze\Services
xcopy /Y %zeze_src_dir%\Zeze\Services\Handshake.cs %project_dir%\Zeze\Services\
xcopy /Y %zeze_src_dir%\Zeze\Services\ToLuaService.cs %project_dir%\Zeze\Services\

md %project_dir%\Zeze\Transaction
xcopy /Y %zeze_src_dir%\Zeze\Transaction\TransactionLevel.cs %project_dir%\Zeze\Transaction\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Log.cs %project_dir%\Zeze\Transaction\

md %project_dir%\Zeze\Transaction\Collections
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogBean.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogList.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogList1.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogList2.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogMap.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogMap1.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogMap2.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogSet.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogSet1.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\CollApply.cs %project_dir%\Zeze\Transaction\Collections\

pause
