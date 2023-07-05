@echo off
setlocal
pushd %~dp0

set classes=../ZezeJava/build/classes/java/main;../ZezeJava/build/resources/main

start "ServiceManagerServer" java -Dlogname=ServiceManagerServer -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.ServiceManagerServer
start "GlobalCacheManagerServer" java -Dlogname=GlobalCacheManagerServer -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.GlobalCacheManagerServer
