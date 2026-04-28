@echo off
chcp 65001 > nul
echo [놀이터] 서버 시작 중...
cd /d C:\workspace_NORITER\backend
call mvnw.cmd compile && call mvnw.cmd spring-boot:run
