@echo off
echo ========================================
echo AI Gateway - DeepSeek Integration
echo ========================================
echo.

REM 检查是否设置了 DeepSeek API Key
if "%DEEPSEEK_API_KEY%"=="" (
    echo [WARNING] DEEPSEEK_API_KEY environment variable is not set!
    echo.
    echo Please set your DeepSeek API Key:
    echo   set DEEPSEEK_API_KEY=sk-your-api-key-here
    echo.
    echo Or get your API Key from: https://platform.deepseek.com/api_keys
    echo.
    echo The service will start with MOCK responses.
    echo.
    pause
) else (
    echo [OK] DEEPSEEK_API_KEY is configured
    echo.
)

echo Starting services...
echo.

REM 启动 gateway-core
echo [1/2] Starting gateway-core on port 9080...
start "Gateway Core" cmd /k "mvn spring-boot:run -pl gateway-core"

REM 等待 5 秒
timeout /t 5 /nobreak >nul

REM 启动 gateway-admin
echo [2/2] Starting gateway-admin on port 9081...
start "Gateway Admin" cmd /k "mvn spring-boot:run -pl gateway-admin"

echo.
echo ========================================
echo Services are starting...
echo ========================================
echo.
echo Gateway Core:  http://localhost:9080
echo Gateway Admin: http://localhost:9081
echo.
echo Frontend will be available at: http://localhost:3001
echo (Start frontend separately: cd frontend && npm run dev)
echo.
echo API Documentation: http://localhost:9080/swagger-ui.html
echo.
echo ========================================
echo.
pause
