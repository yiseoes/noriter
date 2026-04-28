@echo off

echo [NoriTer] Killing port 8080...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    taskkill /F /PID %%a > nul 2>&1
)
timeout /t 2 > nul

echo [NoriTer] Starting server in new window...
start "NoriTer Server" cmd /k "cd /d C:\workspace_NORITER\backend && mvnw.cmd compile && mvnw.cmd spring-boot:run"

echo [NoriTer] Waiting 40 seconds for server startup...
timeout /t 40 > nul

echo [NoriTer] Resetting DB status...
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -proot noriter -e "UPDATE project SET status='COMPLETED', feedback_count=0 WHERE id='prj_8809c54a';" 2>nul

echo [NoriTer] Sending feedback...
curl -s -X POST "http://localhost:8080/api/projects/prj_8809c54a/feedback" -H "Content-Type: application/json" -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyIiwiZW1haWwiOiIxIiwiaWF0IjoxNzc3MjcxMTMzLCJleHAiOjE3NzczNTc1MzN9.SheS2_Q-xjy30kPCnKvDmO64_kpP7yYc-fQdke469z4" -d "{\"feedback\":\"fix handleInput to check isComposing flag\"}"

echo.
echo [NoriTer] Done! Check pipeline log in 5 min.
