# Loki Windows 本地安装指南（无需 Docker）

## 📋 前提条件

你已经有：
- ✅ Prometheus（本地安装）
- ✅ Grafana（本地安装）

只需要安装：
- ⏳ Loki（日志存储）

**不需要 Promtail！** 我们使用 Logback 直接推送日志到 Loki。

---

## 🚀 步骤 1: 下载 Loki

### 下载地址

访问 Loki 的 GitHub Releases 页面：
https://github.com/grafana/loki/releases

找到最新版本，下载 Windows 版本：
- 文件名类似：`loki-windows-amd64.exe.zip`

### 解压和重命名

```bash
# 1. 解压下载的 zip 文件
# 2. 将 loki-windows-amd64.exe 重命名为 loki.exe
# 3. 放到一个固定目录，例如：
D:\soft\loki\loki.exe
```

---

## 🔧 步骤 2: 配置 Loki

在 Loki 目录下创建配置文件 `loki-local-config.yaml`：

**路径**: `D:\soft\loki\loki-local-config.yaml`

```yaml
auth_enabled: false

server:
  http_listen_port: 3100
  grpc_listen_port: 9096

common:
  path_prefix: D:/soft/loki/data
  storage:
    filesystem:
      chunks_directory: D:/soft/loki/data/chunks
      rules_directory: D:/soft/loki/data/rules
  replication_factor: 1
  ring:
    instance_addr: 127.0.0.1
    kvstore:
      store: inmemory

schema_config:
  configs:
    - from: 2020-10-24
      store: boltdb-shipper
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 24h

limits_config:
  retention_period: 720h  # 30 天
  ingestion_rate_mb: 10
  ingestion_burst_size_mb: 20
```

**注意**: 将路径 `D:/soft/loki/data` 改为你实际的路径。

---

## 🎯 步骤 3: 启动 Loki

### 方式 1: 命令行启动

```bash
cd D:\soft\loki
loki.exe -config.file=loki-local-config.yaml
```

### 方式 2: 创建启动脚本

创建 `start-loki.bat`:

```batch
@echo off
echo Starting Loki...
cd /d D:\soft\loki
loki.exe -config.file=loki-local-config.yaml
pause
```

双击运行即可。

### 验证 Loki 启动

打开浏览器访问：
```
http://localhost:3100/ready
```

应该返回：`ready`

---

## 📝 步骤 4: 配置 Spring Boot 推送日志到 Loki

### 4.1 添加 Maven 依赖

编辑 `gateway-admin/pom.xml` 和 `gateway-core/pom.xml`，添加：

```xml
<!-- Loki Logback Appender -->
<dependency>
    <groupId>com.github.loki4j</groupId>
    <artifactId>loki-logback-appender</artifactId>
    <version>1.4.2</version>
</dependency>
```

### 4.2 更新 Logback 配置

我已经为你创建了配置文件，只需要添加 Loki appender。

**gateway-admin/src/main/resources/logback-spring.xml** 添加：

```xml
<!-- Loki Appender -->
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http>
        <url>http://localhost:3100/loki/api/v1/push</url>
    </http>
    <format>
        <label>
            <pattern>app=gateway-admin,host=${HOSTNAME},level=%level</pattern>
        </label>
        <message>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n</pattern>
        </message>
    </format>
</appender>

<!-- 在 root logger 中添加 LOKI appender -->
<root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
    <appender-ref ref="ERROR_FILE"/>
    <appender-ref ref="LOKI"/>  <!-- 添加这行 -->
</root>
```

**gateway-core/src/main/resources/logback-spring.xml** 同样添加（app 改为 gateway-core）。

---

## 🎨 步骤 5: 在 Grafana 中配置 Loki 数据源

### 5.1 添加数据源

1. 打开你本地的 Grafana（通常是 http://localhost:3000）
2. 登录
3. 点击左侧菜单 "Configuration" → "Data Sources"
4. 点击 "Add data source"
5. 选择 "Loki"

### 5.2 配置 Loki 数据源

- **Name**: Loki
- **URL**: `http://localhost:3100`
- **Access**: Server (default)

点击 "Save & Test"，应该显示 "Data source is working"。

---

## 📊 步骤 6: 在 Grafana 中查看日志

### 使用 Explore

1. 点击左侧菜单 "Explore"
2. 在顶部选择数据源：`Loki`
3. 输入查询：

```logql
# 查看所有 Gateway Admin 日志
{app="gateway-admin"}

# 查看所有 Gateway Core 日志
{app="gateway-core"}

# 查看错误日志
{app=~"gateway-.*"} |= "ERROR"

# 查看特定 trace ID
{app=~"gateway-.*"} |= "traceId=abc123"
```

---

## 🔄 完整启动流程

### 1. 启动 Loki

```bash
cd D:\soft\loki
loki.exe -config.file=loki-local-config.yaml
```

### 2. 启动 Prometheus（你已经有了）

```bash
# 你的 Prometheus 启动命令
prometheus.exe --config.file=prometheus.yml
```

### 3. 启动 Grafana（你已经有了）

```bash
# 你的 Grafana 启动命令
# 或者如果是 Windows 服务，确保服务正在运行
```

### 4. 启动 Spring Boot 应用

```bash
# Gateway Admin
cd D:\wk\AIGATEWAY\gateway-admin
java -jar target/gateway-admin-1.0.0.jar

# Gateway Core
cd D:\wk\AIGATEWAY\gateway-core
java -jar target/gateway-core-1.0.0.jar
```

### 5. 访问 Grafana 查看日志

打开浏览器：http://localhost:3000

---

## 🎯 架构图

```
Spring Boot 应用
    ↓ Loki Logback Appender (HTTP)
Loki (localhost:3100)
    ↓ Grafana 查询
Grafana (localhost:3000)
```

**优点：**
- ✅ 不需要 Docker
- ✅ 不需要 Promtail
- ✅ 直接从应用推送日志
- ✅ 实时日志查看
- ✅ 与现有 Prometheus + Grafana 集成

---

## 🐛 故障排查

### 问题 1: Loki 启动失败

**检查：**
```bash
# 查看错误信息
loki.exe -config.file=loki-local-config.yaml
```

**常见原因：**
- 端口 3100 被占用
- 配置文件路径错误
- 数据目录没有写权限

**解决：**
```bash
# 检查端口占用
netstat -ano | findstr :3100

# 创建数据目录
mkdir D:\soft\loki\data
mkdir D:\soft\loki\data\chunks
mkdir D:\soft\loki\data\rules
```

### 问题 2: Spring Boot 无法连接 Loki

**检查：**
1. Loki 是否正在运行
2. 访问 http://localhost:3100/ready
3. 查看应用日志是否有连接错误

**解决：**
- 确保 Loki 正在运行
- 检查 logback-spring.xml 中的 URL 是否正确
- 检查防火墙设置

### 问题 3: Grafana 看不到日志

**检查：**
1. Loki 数据源是否配置正确
2. 应用是否正在运行并产生日志
3. 使用正确的 LogQL 查询

**解决：**
```logql
# 先查询所有日志
{}

# 然后按 app 过滤
{app="gateway-admin"}
```

---

## 📚 下载链接

### Loki 下载

- GitHub Releases: https://github.com/grafana/loki/releases
- 选择最新版本的 `loki-windows-amd64.exe.zip`

### 如果你还没有 Grafana

- Grafana 下载: https://grafana.com/grafana/download?platform=windows
- 选择 Windows Installer

---

## ✅ 验证清单

- [ ] Loki 已下载并解压
- [ ] Loki 配置文件已创建
- [ ] Loki 可以成功启动
- [ ] 可以访问 http://localhost:3100/ready
- [ ] Maven 依赖已添加（loki-logback-appender）
- [ ] logback-spring.xml 已更新
- [ ] 项目已重新编译
- [ ] Spring Boot 应用已启动
- [ ] Grafana 中已添加 Loki 数据源
- [ ] 可以在 Grafana Explore 中查询到日志

---

## 🎉 完成！

现在你可以在 Grafana 中实时查看 AI Gateway 的日志了，无需 Docker！

**优势：**
- 轻量级，只需要一个 Loki 可执行文件
- 与现有 Prometheus + Grafana 完美集成
- 实时日志推送，无需文件采集
- 支持强大的 LogQL 查询语言
