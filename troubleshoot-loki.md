# Loki 日志未显示问题排查

## 问题：Grafana 中看不到日志

### 排查步骤

#### 1. 检查 Loki 是否正在运行

```bash
# 访问 Loki 健康检查
curl http://localhost:3100/ready

# 或在浏览器打开
http://localhost:3100/ready

# 应该返回: ready
```

#### 2. 检查 Loki 是否接收到日志

```bash
# 查询 Loki 的所有标签
curl http://localhost:3100/loki/api/v1/labels

# 查询所有日志流
curl http://localhost:3100/loki/api/v1/query?query={job=~".+"}
```

#### 3. 检查 Spring Boot 应用是否配置了 Loki Appender

**需要确认：**
- Maven 依赖是否添加
- logback-spring.xml 是否配置
- 应用是否重新编译
- 应用是否重启

#### 4. 检查应用日志中是否有错误

查看应用启动日志，搜索：
- `loki`
- `appender`
- `connection refused`
- `error`

---

## 解决方案

### 方案 1: 确认完整配置（推荐）

我来帮你创建完整的配置文件和检查脚本。

### 方案 2: 使用 Promtail（备选）

如果 Logback Appender 有问题，可以使用 Promtail 采集日志文件。

---

## 需要你提供的信息

请告诉我：

1. **Loki 是否在运行？**
   - 访问 http://localhost:3100/ready 看到什么？

2. **Spring Boot 应用是否重新编译？**
   - 是否运行了 `mvn clean install`？

3. **应用启动日志中有没有错误？**
   - 复制应用启动时的日志

4. **是否添加了 Maven 依赖？**
   - gateway-admin/pom.xml 和 gateway-core/pom.xml 中是否有 loki-logback-appender？

5. **logback-spring.xml 是否配置了 Loki Appender？**
   - 文件是否存在？
   - 是否添加了 LOKI appender？

---

## 快速测试

让我创建一个测试脚本来验证整个链路。
