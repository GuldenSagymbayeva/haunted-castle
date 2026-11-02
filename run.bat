@echo off
rem Build and start the game. Extra arguments go to the game, e.g. run.bat --seed 42
cd /d "%~dp0"
if exist out rmdir /s /q out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
set BUILD=%errorlevel%
del sources.txt
if not %BUILD%==0 exit /b %BUILD%
java -cp out Main %*
