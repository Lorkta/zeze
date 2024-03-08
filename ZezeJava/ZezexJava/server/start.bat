
@echo off
setlocal
pushd %~dp0

@rem if not exist hot (
@rem	cd ..
@rem	call distribute.bat
@rem	cd server
@rem )
@rem if not exist hot run (
@rem	move hot hotrun
@rem )

set JAVA_HOME=C:/Users/guoqing.ma/.jdks/openjdk-21.0.2/
@rem %JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

set zeze_classes=../../ZezeJava/build/classes/java/main;../../ZezeJava/build/resources/main

start "server" java -cp %zeze_classes%;./hotrun/server.jar;../../ZezeJavaTest/lib/* Program

@rem start "server"

set linkd_classes=%zeze_classes%;../linkd/build/classes/java/main
"%JAVA_EXE%" -cp %linkd_classes%;../../ZezeJavaTest/lib/jgrapht-core-1.5.2.jar;../../ZezeJava/lib/* Program

@rem timeout 10
@rem set client_classes=%zeze_classes%;../client/build/classes/java/main
@rem "%JAVA_EXE%" -cp %client_classes%;../../ZezeJavaTest/lib/jgrapht-core-1.5.2.jar;../../ZezeJava/lib/* Program
