@echo off
echo ========================================
echo Loki Installation Check
echo ========================================
echo.

set LOKI_DIR=D:\soft\loki

echo [1] Checking Loki directory...
if exist "%LOKI_DIR%" (
    echo [OK] Directory exists: %LOKI_DIR%
) else (
    echo [ERROR] Directory not found: %LOKI_DIR%
    echo Please create the directory first
    pause
    exit /b 1
)

echo.
echo [2] Checking files in directory...
dir "%LOKI_DIR%"

echo.
echo [3] Checking for required files...

if exist "%LOKI_DIR%\loki.exe" (
    echo [OK] loki.exe found
) else (
    echo [MISSING] loki.exe NOT found
    echo.
    echo You need to download loki-windows-amd64.exe
    echo From: https://github.com/grafana/loki/releases
    echo And rename it to: loki.exe
)

if exist "%LOKI_DIR%\logcli-windows-amd64.exe" (
    echo [INFO] logcli-windows-amd64.exe found (this is the CLI tool, not the server)
)

if exist "%LOKI_DIR%\loki-local-config.yaml" (
    echo [OK] loki-local-config.yaml found
) else (
    echo [MISSING] loki-local-config.yaml NOT found
    echo Creating default configuration...
    (
        echo auth_enabled: false
        echo.
        echo server:
        echo   http_listen_port: 3100
        echo   grpc_listen_port: 9096
        echo.
        echo common:
        echo   path_prefix: %LOKI_DIR%\data
        echo   storage:
        echo     filesystem:
        echo       chunks_directory: %LOKI_DIR%\data\chunks
        echo       rules_directory: %LOKI_DIR%\data\rules
        echo   replication_factor: 1
        echo   ring:
        echo     instance_addr: 127.0.0.1
        echo     kvstore:
        echo       store: inmemory
        echo.
        echo schema_config:
        echo   configs:
        echo     - from: 2020-10-24
        echo       store: boltdb-shipper
        echo       object_store: filesystem
        echo       schema: v11
        echo       index:
        echo         prefix: index_
        echo         period: 24h
    ) > "%LOKI_DIR%\loki-local-config.yaml"
    echo [CREATED] loki-local-config.yaml
)

echo.
echo [4] Checking data directory...
if exist "%LOKI_DIR%\data" (
    echo [OK] Data directory exists
) else (
    echo [INFO] Creating data directory...
    mkdir "%LOKI_DIR%\data"
    mkdir "%LOKI_DIR%\data\chunks"
    mkdir "%LOKI_DIR%\data\rules"
    echo [CREATED] Data directories
)

echo.
echo ========================================
echo Summary
echo ========================================
echo.
echo Loki Directory: %LOKI_DIR%
echo.

if exist "%LOKI_DIR%\loki.exe" (
    echo Status: Ready to start
    echo.
    echo Next steps:
    echo 1. Run: start-loki.bat
    echo 2. Verify: http://localhost:3100/ready
) else (
    echo Status: NOT READY
    echo.
    echo You need to:
    echo 1. Download loki-windows-amd64.exe from:
    echo    https://github.com/grafana/loki/releases/latest
    echo 2. Rename it to: loki.exe
    echo 3. Place it in: %LOKI_DIR%
    echo 4. Run this check script again
)

echo.
pause
