# 本地监控（Prometheus + Grafana）

前提：你已经能访问下面两个指标端点：

- `gateway-core`：`http://localhost:9080/actuator/prometheus`
- `gateway-admin`：`http://localhost:9082/actuator/prometheus`

## 1) 安装（本机，不用 Docker）

### Prometheus
- 下载 Windows 版压缩包并解压（目录里需要有 `prometheus.exe`）

### Grafana
- 建议下载 Windows 的 ZIP 版并解压（目录里需要有 `bin/grafana-server.exe`）

## 2) 一键启动

在项目根目录执行（PowerShell）：

```powershell
.\monitoring\start-monitor.ps1
```

如果你的安装目录不在默认候选路径里，设置环境变量后再执行：

```powershell
$env:PROMETHEUS_HOME="D:\soft\prometheus"
$env:GRAFANA_HOME="D:\soft\grafana"
.\monitoring\start-monitor.ps1
```

启动后访问：

- Prometheus：`http://localhost:9090/targets`
- Grafana：`http://localhost:3000`（默认 `admin/admin`）

## 3) Grafana 连接 Prometheus

Grafana → Connections → Data sources → Add data source → Prometheus

- URL：`http://localhost:9090`

## 4) 快速验证（Grafana → Explore）

```promql
up
```

正常会看到：
- `gateway-core = 1`
- `gateway-admin = 1`

