
@echo off
setlocal
pushd %~dp0

cd ..
call gradlew.bat build copyJar
cd ZezexJava

call distribute.bat
copy server\hot\modules\Game.Equip.jar server\hotrun\distributes\
copy server\hot\interfaces\Game.Equip.interface.jar server\hotrun\distributes\
copy server\hot\__hot_schemas__Game.jar server\hotrun\distributes\

echo "" > hotrun\distributes\ready

