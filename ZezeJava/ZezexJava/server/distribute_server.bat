
@echo off
setlocal
pushd %~dp0

set classes=../../ZezeJava/build/classes/java/main
set hotrun=hotrun/*;hotrun/modules/*;hotrun/interfaces/*

echo "�������������������������c#.Gen��ѯ��������Bean�Ľṹ"

set JAVA_HOME=C:/Users/guoqing.ma/.jdks/openjdk-21.0.2/
@rem %JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

"%JAVA_EXE%" -cp %classes%;%hotrun%;../../ZezeJavaTest/lib/jgrapht-core-1.5.2.jar;../../ZezeJava/lib/* Zeze.Hot.DistributeServer -solution Game

