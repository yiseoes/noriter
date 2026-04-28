@echo off
chcp 65001 > nul
echo [놀이터] 서버 종료 중...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    echo PID %%a 종료 중...
    taskkill /F /PID %%a
)
echo [놀이터] 서버 종료 완료
