@echo off
echo ========================================
echo Loki Installation Check
echo ========================================
echo.

echo Checking D:\soft\loki directory...
echo.

cd /d D:\soft\loki 2>nul
if errorlevel 1 (
    echo [ERROR] Directory D:\soft\loki not found
    echo Please create it first: mkdir D:\soft\loki
    pause
    exit /b 1
)

echo Files in D:\soft\loki:
echo.
dir /b

echo.
echo ========================================
echo.

if exist loki.exe (
    echo [OK] loki.exe found - Ready to start!
    echo.
    echo Run: loki.exe -config.file=loki-local-config.yaml
) else (
    echo [MISSING] loki.exe NOT found
    echo.
    echo Download from: https://github.com/grafana/loki/releases
    echo File: loki-windows-amd64.exe.zip
    echo Rename to: loki.exe
)

echo.

if exist loki-local-config.yaml (
    echo [OK] Config file found
) else (
    echo [MISSING] Config file not found
    echo Creating loki-local-config.yaml...
    echo Will be created when you run the setup
)

echo.
pause
