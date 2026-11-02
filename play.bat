@echo off
rem Double-click to play, or run from a terminal: play.bat --difficulty hard
setlocal
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
    echo Java 17 or newer ^(JDK^) is required. Download it from https://adoptium.net
    pause
    exit /b 1
)
echo Building the game...
if exist out rmdir /s /q out
javac -d out -sourcepath src\main\java src\main\java\hauntedcastle\Main.java
if errorlevel 1 (
    pause
    exit /b 1
)
xcopy /e /i /q src\main\resources out >nul
java -cp out hauntedcastle.Main %*
pause
