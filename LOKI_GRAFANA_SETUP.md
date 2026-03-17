# Loki + Grafana 日志查看完整指南

## 📋 概述

本指南帮助你配置 Loki + Promtail + Grafana 来查看 AI Gateway 的日志。

### 架构

```
Spring Boot 应用
    ↓ 写入日志文件
logs/gateway-admin.log
logs/gateway-core.log
    ↓ Promtail 采集
Loki (存储)
    ↓ Grafana 查询
Grafana Dashboard (可视化)
```

---

## 🚀 快速开始

### 步骤 1: 启动 Loki 和 Grafana

```bash
# 在项目根目录执行
docker-compose up -d loki promtail grafana
```

**验证服务启动：**
```bash
# 检查 Loki
curl http://localhost:3100/ready

# 检查 Grafana
curl http://localhost:3002
```

### 步骤 2: 创建日志目录

```bash
# 在项目根目录创建 logs 文件夹
mkdir logs
```

### 步骤 3: 启动 Spring Boot 应用

```bash
# 启动 Gateway Admin
cd gateway-admin
java -jar target/gateway-admin-1.0.0.jar

# 启动 Gateway Core（新窗口）
cd gateway-core
java -jar target/gateway-core-1.0.0.jar
```

应用会自动在 `logs/` 目录下创建日志文件：
- `logs/gateway-admin.log`
- `logs/gateway-admin-error.log`
- `logs/gateway-core.log`
- `logs/gateway-core-error.log`

### 步骤 4: 访问 Grafana

1. 打开浏览器访问：http://localhost:3002
2. 登录：
   - 用户名：`admin`
   - 密码：`admin123`

---

## 📊 在 Grafana 中查看日志

### 方法 1: 使用 Explore

1. 点击左侧菜单 "Explore" (指南针图标)
2. 在顶部选择数据源：`Loki`
3. 在查询框中输入 LogQL 查询

**常用查询示例：**

```logql
# 查看所有 AI Gateway 日志
{service="ai-gateway"}

# 查看 Gateway Admin 日志
{job="gateway-admin"}

# 查看 Gateway Core 日志
{job="gateway-core"}

# 查看错误日志
{service="ai-gateway"} |= "ERROR"

# 查看特定 trace ID 的日志
{service="ai-gateway"} |= "traceId=abc123"

# 查看包含特定关键词的日志
{job="gateway-core"} |= "DeepSeek"

# 查看最近 5 分钟的错误
{service="ai-gateway"} |= "ERROR" [5m]

# 按日志级别过滤
{service="ai-gateway"} | regexp "\\s(ERROR|WARN)\\s"
```

### 方法 2: 创建 Dashboard

1. 点击左侧菜单 "Dashboards" → "New" → "New Dashboard"
2. 点击 "Add visualization"
3. 选择数据源：`Loki`
4. 配置查询和可视化

---

## 🔧 配置说明

### 已创建的配置文件

1. **docker-compose.yml** - 添加了 Loki 和 Promtail 服务
2. **monitoring/loki/loki-config.yaml** - Loki 配置
3. **monitoring/promtail/promtail-config.yaml** - Promtail 日志采集配置
4. **monitoring/grafana/provisioning/datasources/loki.yaml** - Grafana 数据源配置
5. **gateway-admin/src/main/resources/logback-spring.xml** - Admin 日志配置
6. **gateway-core/src/main/resources/logback-spring.xml** - Core 日志配置

### 日志格式

应用日志格式：
```
2026-03-16 17:11:25 [main] [trace-id-123] INFO  com.aigateway.core.GatewayApplication - Starting application
```

字段说明：
- `2026-03-16 17:11:25` - 时间戳
- `[main]` - 线程名
- `[trace-id-123]` - 链路追踪 ID
- `INFO` - 日志级别
- `com.aigateway.core.GatewayApplication` - Logger 名称
- `Starting application` - 日志消息

### Promtail 标签

Promtail 会自动为日志添加以下标签：
- `job` - gateway-admin 或 gateway-core
- `service` - ai-gateway
- `component` - admin 或 core
- `level` - 日志级别（INFO, DEBUG, ERROR, WARN）
- `thread` - 线程名
- `trace_id` - 链路追踪 ID
- `logger` - Logger 类名

---

## 📝 LogQL 查询语言

### 基础语法

```logql
# 日志流选择器
{label="value"}

# 日志行过滤
{label="value"} |= "search text"    # 包含
{label="value"} != "search text"    # 不包含
{label="value"} |~ "regex"          # 正则匹配
{label="value"} !~ "regex"          # 正则不匹配

# 组合过滤
{job="gateway-core"} |= "ERROR" |= "DeepSeek"
```

### 高级查询

```logql
# 统计错误数量
sum(count_over_time({service="ai-gateway"} |= "ERROR" [5m]))

# 按组件统计日志量
sum by (component) (count_over_time({service="ai-gateway"} [1h]))

# 计算错误率
sum(rate({service="ai-gateway"} |= "ERROR" [5m])) 
/ 
sum(rate({service="ai-gateway"} [5m]))

# 提取 JSON 字段
{job="gateway-core"} | json | line_format "{{.message}}"

# 按日志级别分组
sum by (level) (count_over_time({service="ai-gateway"} [1h]))
```

---

## 🎨 创建实用的 Dashboard

### Panel 1: 实时日志流

- **类型**: Logs
- **查询**: `{service="ai-gateway"}`
- **刷新**: 5s

### Panel 2: 错误日志统计

- **类型**: Time series
- **查询**: `sum(count_over_time({service="ai-gateway"} |= "ERROR" [1m]))`
- **刷新**: 10s

### Panel 3: 日志级别分布

- **类型**: Pie chart
- **查询**: `sum by (level) (count_over_time({service="ai-gateway"} [5m]))`

### Panel 4: 各组件日志量

- **类型**: Bar chart
- **查询**: `sum by (component) (count_over_time({service="ai-gateway"} [1h]))`

---

## 🔍 常见使用场景

### 场景 1: 调试 API 调用

```logql
# 查看所有 DeepSeek API 调用
{job="gateway-core"} |= "DeepSeek"

# 查看特定请求的完整日志
{service="ai-gateway"} |= "trace-id-abc123"
```

### 场景 2: 监控错误

```logql
# 查看所有错误
{service="ai-gateway"} |= "ERROR"

# 查看特定异常
{service="ai-gateway"} |= "NullPointerException"

# 查看最近 1 小时的错误趋势
sum(count_over_time({service="ai-gateway"} |= "ERROR" [5m]))
```

### 场景 3: 性能分析

```logql
# 查看慢查询
{job="gateway-admin"} |= "slow query"

# 查看 API 响应时间
{job="gateway-core"} |= "latency_ms"
```

### 场景 4: 用户行为追踪

```logql
# 查看特定用户的操作
{service="ai-gateway"} |= "user_id=123"

# 查看登录日志
{job="gateway-admin"} |= "login"
```

---

## ⚙️ 高级配置

### 配置日志保留时间

编辑 `monitoring/loki/loki-config.yaml`:

```yaml
schema_config:
  configs:
    - from: 2020-10-24
      store: boltdb-shipper
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 24h

# 添加保留策略
limits_config:
  retention_period: 720h  # 30 天
```

### 配置日志采集路径

编辑 `monitoring/promtail/promtail-config.yaml`:

```yaml
scrape_configs:
  - job_name: my-custom-logs
    static_configs:
      - targets:
          - localhost
        labels:
          job: my-app
          __path__: /path/to/your/logs/*.log
```

### 配置告警

在 Grafana 中创建告警规则：

1. 进入 Dashboard
2. 编辑 Panel
3. 点击 "Alert" 标签
4. 配置告警条件

**示例告警：错误率过高**
```
Query: sum(rate({service="ai-gateway"} |= "ERROR" [5m]))
Condition: WHEN last() OF query(A) IS ABOVE 10
```

---

## 🐛 故障排查

### 问题 1: Grafana 看不到 Loki 数据源

**检查：**
```bash
# 检查 Loki 是否运行
docker ps | grep loki

# 检查 Loki 健康状态
curl http://localhost:3100/ready

# 查看 Grafana 日志
docker logs ai-gateway-grafana
```

**解决：**
1. 确保 Loki 容器正在运行
2. 在 Grafana 中手动添加数据源：
   - URL: `http://loki:3100`
   - Access: `Server (default)`

### 问题 2: Promtail 没有采集到日志

**检查：**
```bash
# 查看 Promtail 日志
docker logs ai-gateway-promtail

# 检查日志文件是否存在
ls -la logs/

# 检查 Promtail 配置
cat monitoring/promtail/promtail-config.yaml
```

**解决：**
1. 确保 `logs/` 目录存在且有日志文件
2. 检查 docker-compose.yml 中的 volume 映射
3. 重启 Promtail: `docker-compose restart promtail`

### 问题 3: 日志文件没有生成

**检查：**
```bash
# 检查应用是否运行
jps -l | grep aigateway

# 检查 logback 配置
cat gateway-admin/src/main/resources/logback-spring.xml
```

**解决：**
1. 确保应用正在运行
2. 检查 logback-spring.xml 配置是否正确
3. 手动创建 logs 目录: `mkdir logs`
4. 重启应用

### 问题 4: Grafana 端口冲突

**错误信息：**
```
Error: port 3002 is already in use
```

**解决：**
```bash
# 查找占用端口的进程
netstat -ano | findstr :3002

# 修改 docker-compose.yml 中的端口映射
ports:
  - "3003:3000"  # 改为其他端口
```

---

## 📚 参考资源

- [Loki 官方文档](https://grafana.com/docs/loki/latest/)
- [LogQL 查询语言](https://grafana.com/docs/loki/latest/logql/)
- [Promtail 配置](https://grafana.com/docs/loki/latest/clients/promtail/configuration/)
- [Grafana Dashboard](https://grafana.com/docs/grafana/latest/dashboards/)

---

## ✅ 验证清单

- [ ] Docker 已安装并运行
- [ ] Loki 容器正在运行 (`docker ps | grep loki`)
- [ ] Promtail 容器正在运行 (`docker ps | grep promtail`)
- [ ] Grafana 容器正在运行 (`docker ps | grep grafana`)
- [ ] 日志目录已创建 (`logs/`)
- [ ] Spring Boot 应用正在运行
- [ ] 日志文件已生成 (`ls logs/`)
- [ ] 可以访问 Grafana (http://localhost:3002)
- [ ] Grafana 中可以看到 Loki 数据源
- [ ] 可以在 Explore 中查询到日志

---

## 🎉 完成！

现在你可以在 Grafana 中实时查看和分析 AI Gateway 的日志了！

**下一步：**
1. 创建自定义 Dashboard
2. 配置告警规则
3. 集成 Prometheus 指标
4. 添加链路追踪（Tempo）
