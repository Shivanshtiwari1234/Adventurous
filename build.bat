@echo off
setlocal

set ROOT=%~dp0

javac ^
-cp "%ROOT%lib\*" ^
-d "%ROOT%out" ^
"%ROOT%src\*.java"

if errorlevel 1 (
    echo BUILD FAILED
    pause
    exit /b
)

echo BUILD SUCCESS
pause
