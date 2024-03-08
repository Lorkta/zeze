@echo off
setlocal
pushd %~dp0

rd /s /q server\hot 2>nul
mkdir server\hot\distributes

rem ����cd������Ŀ¼ִ�У�����Ŀ¼��������
cd server

set classes=../../ZezeJava/build/classes/java/main;build/classes/java/main

set JAVA_HOME=C:/Users/guoqing.ma/.jdks/openjdk-21.0.2/
@rem %JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

echo ���...
java -cp %classes%;../../ZezeJavaTest/lib/* Zeze.Hot.Distribute -privateBean -app Game.App -workingDir hot -classes  build/classes/java/main -providerModuleBinds ../provider.module.binds.xml -config server.xml
echo OK

cd ..

pause
