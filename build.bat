@echo off
setlocal

set ROOT=%~dp0

if not exist "%ROOT%out" mkdir "%ROOT%out"

javac ^
-cp "%ROOT%lib\*" ^
-d "%ROOT%out" ^
"%ROOT%src\Adventurous.java"

if errorlevel 1 (
    echo.
    echo BUILD FAILED
    pause
    exit /b 1
)

echo.
echo BUILD SUCCESS
