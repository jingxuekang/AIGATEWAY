@echo off
echo ========================================
echo AI Gateway Service Startup
echo ========================================
echo.

REM Check for running Java processes
echo [Check] Looking for Java processes...
tasklist | findstr /I "java.exe" >nul
if %errorlevel% == 0 (
    echo [Warning] Found running Java processes
    echo.
    echo Stop all Java processes? (Y/N)
    choice /C YN /N
    if errorlevel 2 goto :skip_kill
    if errorlevel 1 (
        echo [Action] Stopping Java processes...
        taskkill /F /IM java.exe >nul 2>&1
        timeout /t 2 >nul
        echo [Done] Java processes stopped
    )
)
:skip_kill

echo.
echo ========================================
echo Starting Services
echo ========================================
echo.

REM Start Gateway Admin
echo [1/3] Starting Gateway Admin (Port 9082)...
start "Gateway Admin - Port 9082" cmd /k "cd /d D:\wk\AIGATEWAY\gateway-admin && echo [Gateway Admin] Starting... && java -jar target/gateway-admin-1.0.0.jar"
echo [Wait] Waiting for Gateway Admin to start...
timeout /t 15 >nul

REM Start Gateway Core
echo.
echo [2/3] Starting Gateway Core (Port 9080)...
start "Gateway Core - Port 9080" cmd /k "cd /d D:\wk\AIGATEWAY\gateway-core && echo [Gateway Core] Starting... && java -jar target/gateway-core-1.0.0.jar"
echo [Wait] Waiting for Gateway Core to start...
timeout /t 15 >nul

REM Start Frontend
echo.
echo [3/3] Starting Frontend (Port 3001)...
start "Frontend Dev Server - Port 3001" cmd /k "cd /d D:\wk\AIGATEWAY\frontend && echo [Frontend] Starting... && npm run dev"
echo [Wait] Waiting for Frontend to start...
timeout /t 10 >nul

echo.
echo ========================================
echo All Services Started!
echo ========================================
echo.
echo Service URLs:
echo   Gateway Admin:  http://localhost:9082
echo   Gateway Core:   http://localhost:9080
echo   Frontend:       http://localhost:3001
echo.
echo Health Checks:
echo   Admin Health:   http://localhost:9082/actuator/health
echo   Core Health:    http://localhost:9080/actuator/health
echo.
echo ========================================
echo.

REM Ask to open browser
echo Open browser? (Y/N)
choice /C YN /N
if errorlevel 2 goto :end
if errorlevel 1 (
    echo [Action] Opening browser...
    start http://localhost:3001
)

:end
echo.
echo [Info] Press any key to exit (services will continue running)
echo [Info] To stop services, close the command windows or run stop-all-services.bat
pause >nul
