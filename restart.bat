@echo off
chcp 949 > nul

echo [놀이터] 기존 서버 종료 중...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    taskkill /F /PID %%a > nul 2>&1
)
timeout /t 2 > nul

echo [놀이터] 빌드 및 서버 시작 중...
cd /d C:\workspace_NORITER\backend
call mvnw.cmd compile && call mvnw.cmd spring-boot:run
