@echo off
setlocal

REM Root directory (where run.bat lives)
set ROOT=%~dp0

REM Go to compiled output directory
cd /d "%ROOT%out"

java ^
-Djava.library.path="%ROOT%lib\natives" ^
-cp ".;%ROOT%lib\*" ^
Adventurous

pause
