@echo off
setlocal
pushd %~dp0

..\..\Gen\bin\Debug\net6.0\Gen.exe world.xml

pause
