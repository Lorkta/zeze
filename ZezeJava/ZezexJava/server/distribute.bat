@echo off
setlocal
pushd %~dp0

rd /s /q hot 2>nul
mkdir hot\distributes

set classes=../../ZezeJava/build/classes/java/main;build/classes/java/main

echo "��� TODO Gen��Ҫ����һ�����������ɵ�App.java����)��ָ���ȸ�ģ�飬�������������Zeze.Hot.Distribute(��Ҫ�ع�)"

java -cp %classes%;../../ZezeJava/lib/* Zeze.Hot.Distribute -privateBean -app Game.App

pause
