@echo off
echo ========================================
echo AI Gateway Service Stop Script
echo ========================================
echo.

REM Check for running Java processes
tasklist | findstr /I "java.exe" >nul
if %errorlevel% == 1 (
    echo [Info] No Java processes found
    goto :check_node
)

echo [Detected] Found running Java processes:
echo.
tasklist | findstr /I "java.exe"
echo.

echo Stop all Java processes (Gateway Admin and Gateway Core)? (Y/N)
choice /C YN /N
if errorlevel 2 goto :check_node
if errorlevel 1 (
    echo [Action] Stopping Java processes...
    taskkill /F /IM java.exe >nul 2>&1
    if %errorlevel% == 0 (
        echo [Done] Java processes stopped
    ) else (
        echo [Error] Failed to stop Java processes, may need admin rights
    )
)

:check_node
echo.

REM Check for running Node processes
tasklist | findstr /I "node.exe" >nul
if %errorlevel% == 1 (
    echo [Info] No Node processes found
    goto :check_ports
)

echo [Detected] Found running Node processes:
echo.
tasklist | findstr /I "node.exe"
echo.

echo Stop all Node processes (Frontend)? (Y/N)
choice /C YN /N
if errorlevel 2 goto :check_ports
if errorlevel 1 (
    echo [Action] Stopping Node processes...
    taskkill /F /IM node.exe >nul 2>&1
    if %errorlevel% == 0 (
        echo [Done] Node processes stopped
    ) else (
        echo [Error] Failed to stop Node processes, may need admin rights
    )
)

:check_ports
echo.
echo ========================================
echo Checking Port Usage
echo ========================================
echo.

echo [Check] Port 9082 (Gateway Admin):
netstat -ano | findstr :9082
if %errorlevel% == 1 echo   Not in use

echo.
echo [Check] Port 9080 (Gateway Core):
netstat -ano | findstr :9080
if %errorlevel% == 1 echo   Not in use

echo.
echo [Check] Port 3001 (Frontend):
netstat -ano | findstr :3001
if %errorlevel% == 1 echo   Not in use

echo.
echo ========================================
echo Operation Complete
echo ========================================
echo.
pause
