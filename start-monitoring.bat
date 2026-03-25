@echo off
echo ============================================
echo   AI Gateway 监控栈启动脚本
echo   Loki + Elasticsearch + Prometheus + Grafana
echo ============================================
echo.

set PROMETHEUS_EXE=D:\soft\prometheus\prometheus-3.10.0.windows-amd64\prometheus-3.10.0.windows-amd64\prometheus.exe
set PROMETHEUS_CFG=d:\wk\AIGATEWAY\monitoring\prometheus.yml
set GRAFANA_EXE=D:\soft\grafana\grafana-enterprise_12.4.1_22846628243_windows_amd64\grafana-12.4.1\bin\grafana-server.exe
set GRAFANA_HOME=D:\soft\grafana\grafana-enterprise_12.4.1_22846628243_windows_amd64\grafana-12.4.1
set LOKI_EXE=D:\soft\loki\loki-windows-amd64.exe\loki-windows-amd64.exe
set LOKI_CFG=D:\soft\loki\loki-local-config.yaml
set ES_HOME=D:\soft\elasticsearch\elasticsearch-8.13.0-windows-x86_64\elasticsearch-8.13.0

echo [1/4] Starting Loki (port 3100)...
start "Loki" /MIN cmd /c ""%LOKI_EXE%" -config.file="%LOKI_CFG%" > d:\wk\AIGATEWAY\loki.log 2>&1"
echo       OK - http://localhost:3100
echo.

echo [2/4] Starting Elasticsearch (port 9200)...
start "Elasticsearch" /MIN cmd /c ""%ES_HOME%\bin\elasticsearch.bat" > d:\wk\AIGATEWAY\elasticsearch.log 2>&1"
echo       OK - http://localhost:9200
echo.

echo [3/4] Starting Prometheus (port 9090)...
start "Prometheus" /MIN cmd /c ""%PROMETHEUS_EXE%" --config.file="%PROMETHEUS_CFG%" --storage.tsdb.path="D:\soft\prometheus\data" > d:\wk\AIGATEWAY\prometheus.log 2>&1"
echo       OK - http://localhost:9090
echo.

echo [4/4] Starting Grafana (port 3002)...
start "Grafana" /MIN cmd /c ""%GRAFANA_EXE%" --homepath="%GRAFANA_HOME%" --config="%GRAFANA_HOME%\conf\defaults.ini" cfg:server.http_port=3002 > d:\wk\AIGATEWAY\grafana.log 2>&1"
echo       OK - http://localhost:3002
echo.

echo ============================================
echo   All services starting in background.
echo.
echo   Loki:          http://localhost:3100
echo   Elasticsearch: http://localhost:9200
echo   Prometheus:    http://localhost:9090/targets
echo   Grafana:       http://localhost:3002  (admin / admin)
echo.
echo   Logs:
echo     d:\wk\AIGATEWAY\loki.log
echo     d:\wk\AIGATEWAY\elasticsearch.log
echo     d:\wk\AIGATEWAY\prometheus.log
echo     d:\wk\AIGATEWAY\grafana.log
echo ============================================
pause
