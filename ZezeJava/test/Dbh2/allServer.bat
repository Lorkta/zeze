@echo off
setlocal
pushd %~dp0

set classes=.;../../ZezeJava/build/classes/java/main;../../ZezeJava/lib/*

start "Master"   java -Dlogname=master   -DZeze.Dbh2.Master.stat=10 -cp %classes% Zeze.Dbh2.Master.Main zeze.xml
ping -n 3 127.1 > nul
start "Manager0" java -Dlogname=manager0 -DZeze.Dbh2.Server.stat=10 -cp %classes% Zeze.Dbh2.Dbh2Manager manager0 zeze.xml
start "Manager1" java -Dlogname=manager1 -DZeze.Dbh2.Server.stat=10 -cp %classes% Zeze.Dbh2.Dbh2Manager manager1 zeze.xml
start "Manager2" java -Dlogname=manager2 -DZeze.Dbh2.Server.stat=10 -cp %classes% Zeze.Dbh2.Dbh2Manager manager2 zeze.xml
