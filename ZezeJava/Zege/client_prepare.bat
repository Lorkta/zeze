@echo off
setlocal
pushd %~dp0

echo ===================================
echo       ������Ҫ�����������!
echo ===================================

cd ../test
call build.bat

cd /d %~dp0
dir

mkdir lib
xcopy /Y ..\ZezeJava\lib lib

pause
