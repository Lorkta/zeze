@echo off
setlocal
pushd %~dp0

del raft.xml.log
start "raft.xml"   gen -c RaftTest -Config raft.xml

del raft3.xml.log
start "raft3.xml"  gen -c RaftTest -Config raft3.xml

del raft6.xml.log
start "raft6.xml"  gen -c RaftTest -Config raft6.xml
