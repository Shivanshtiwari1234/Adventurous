@echo off
setlocal

set ROOT=%~dp0

cd out
java ^
 -Djava.library.path="%ROOT%lib\natives" ^
 -cp ".;%ROOT%lib\*" ^
 Adventurous
