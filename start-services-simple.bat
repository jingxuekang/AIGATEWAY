@echo off
echo ========================================
echo AI Gateway - Starting All Services
echo ========================================
echo.

echo Step 1: Starting Gateway Admin on port 9082...
start "Gateway-Admin-9082" cmd /k "cd /d D:\wk\AIGATEWAY\gateway-admin && java -jar target/gateway-admin-1.0.0.jar"
timeout /t 15 /nobreak >nul

echo Step 2: Starting Gateway Core on port 9080...
start "Gateway-Core-9080" cmd /k "cd /d D:\wk\AIGATEWAY\gateway-core && java -jar target/gateway-core-1.0.0.jar"
timeout /t 15 /nobreak >nul

echo Step 3: Starting Frontend on port 3001...
start "Frontend-3001" cmd /k "cd /d D:\wk\AIGATEWAY\frontend && npm run dev"
timeout /t 5 /nobreak >nul

echo.
echo ========================================
echo All services started successfully!
echo ========================================
echo.
echo URLs:
echo   Admin:    http://localhost:9082
echo   Core:     http://localhost:9080
echo   Frontend: http://localhost:3001
echo.
echo Press any key to open browser...
pause >nul
start http://localhost:3001
