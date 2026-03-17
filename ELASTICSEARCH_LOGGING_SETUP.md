# Elasticsearch + Grafana 日志查看配置

## 📋 方案说明

使用 Elasticsearch 存储日志，在 Grafana 中查看。

### 架构

```
Spring Boot 应用
    ↓ Logback Elasticsearch Appender
Elasticsearch (存储日志)
    ↓ Grafana 查询
Grafana Dashboard (可视化)
```

---

## 🚀 步骤 1: 添加 Elasticsearch Logback Appender

### 1.1 添加 Maven 依赖

编辑 `gateway-admin/pom.xml` 和 `gateway-core/pom.xml`，添加：

```xml
<!-- Elasticsearch Logback Appender -->
<dependency>
    <groupId>com.internetitem</groupId>
    <artifactId>logback-elasticsearch-appender</artifactId>
    <version>1.6</version>
</dependency>
```

### 1.2 更新 Logback 配置

**gateway-admin/src/main/resources/logback-spring.xml**

在现有配置中添加 Elasticsearch Appender：

```xml
<!-- Elasticsearch Appender -->
<appender name="ELASTIC" class="com.internetitem.logback.elasticsearch.ElasticsearchAppender">
    <url>http://localhost:9200/_bulk</url>
    <index>gateway-admin-logs-%date{yyyy-MM-dd}</index>
    <type>_doc</type>
    <loggerName>es-logger</loggerName>
    <errorLoggerName>es-error-logger</errorLoggerName>
    <connectTimeout>30000</connectTimeout>
    <errorsToStderr>false</errorsToStderr>
    <includeCallerData>false</includeCallerData>
    <logsToStderr>false</logsToStderr>
    <maxQueueSize>104857600</maxQueueSize>
    <maxRetries>3</maxRetries>
    <readTimeout>30000</readTimeout>
    <sleepTime>250</sleepTime>
    <rawJsonMessage>false</rawJsonMessage>
    <includeMdc>true</includeMdc>
    <maxMessageSize>100</maxMessageSize>
    
    <property>
        <name>host</name>
        <value>${HOSTNAME}</value>
        <allowEmpty>false</allowEmpty>
    </property>
    <property>
        <name>application</name>
        <value>gateway-admin</value>
    </property>
    <property>
        <name>severity</name>
        <value>%level</value>
    </property>
    <property>
        <name>thread</name>
        <value>%thread</value>
    </property>
    <property>
        <name>logger</name>
        <value>%logger</value>
    </property>
    <property>
        <name>traceId</name>
        <value>%X{traceId}</value>
    </property>
</appender>

<!-- 在 root logger 中添加 ELASTIC appender -->
<root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
    <appender-ref ref="ERROR_FILE"/>
    <appender-ref ref="ELASTIC"/>  <!-- 添加这行 -->
</root>
```

**gateway-core/src/main/resources/logback-spring.xml**

同样添加，但 index 和 application 改为：

```xml
<index>gateway-core-logs-%date{yyyy-MM-dd}</index>
<property>
    <name>application</name>
    <value>gateway-core</value>
</property>
```

---

## 🔧 步骤 2: 配置 Elasticsearch

### 2.1 确认 Elasticsearch 运行

```bash
# 检查 ES 是否运行
curl http://localhost:9200

# 应该返回 ES 版本信息
```

### 2.2 创建索引模板（可选）

```bash
# 创建日志索引模板
curl -X PUT "http://localhost:9200/_index_template/gateway-logs" -H 'Content-Type: application/json' -d'
{
  "index_patterns": ["gateway-*-logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0,
      "index.lifecycle.name": "logs-policy",
      "index.lifecycle.rollover_alias": "gateway-logs"
    },
    "mappings": {
      "properties": {
        "@timestamp": { "type": "date" },
        "message": { "type": "text" },
        "level": { "type": "keyword" },
        "logger": { "type": "keyword" },
        "thread": { "type": "keyword" },
        "application": { "type": "keyword" },
        "host": { "type": "keyword" },
        "traceId": { "type": "keyword" }
      }
    }
  }
}
'
```

---

## 📊 步骤 3: 在 Grafana 中配置 Elasticsearch 数据源

### 3.1 添加数据源

1. 打开 Grafana (http://localhost:3000)
2. 登录
3. 点击左侧菜单 "Configuration" → "Data Sources"
4. 点击 "Add data source"
5. 选择 "Elasticsearch"

### 3.2 配置 Elasticsearch 数据源

- **Name**: Elasticsearch Logs
- **URL**: `http://localhost:9200`
- **Index name**: `gateway-*-logs-*`
- **Time field name**: `@timestamp`
- **Version**: 选择你的 ES 版本（7.x 或 8.x）
- **Min time interval**: `10s`

点击 "Save & Test"，应该显示 "Data source is working"。

---

## 🔍 步骤 4: 在 Grafana 中查看日志

### 使用 Explore

1. 点击左侧菜单 "Explore"
2. 在顶部选择数据源：`Elasticsearch Logs`
3. 输入查询

**查询示例：**

```
# 查看所有日志
application:*

# 查看 Gateway Admin 日志
application:gateway-admin

# 查看 Gateway Core 日志
application:gateway-core

# 查看错误日志
level:ERROR

# 查看特定 trace ID
traceId:abc123

# 组合查询
application:gateway-core AND level:ERROR

# 查看包含特定关键词的日志
message:*DeepSeek*
```

### 创建 Dashboard

1. 点击左侧菜单 "Dashboards" → "New" → "New Dashboard"
2. 点击 "Add visualization"
3. 选择数据源：`Elasticsearch Logs`
4. 配置查询和可视化

---

## 🔄 步骤 5: 重新编译和启动

### 5.1 重新编译项目

```bash
cd D:\wk\AIGATEWAY
mvn clean install -DskipTests
```

### 5.2 启动应用

```bash
# Gateway Admin
cd gateway-admin
java -jar target/gateway-admin-1.0.0.jar

# Gateway Core
cd gateway-core
java -jar target/gateway-core-1.0.0.jar
```

### 5.3 验证日志写入

```bash
# 查看 ES 中的索引
curl http://localhost:9200/_cat/indices?v

# 应该看到类似：
# gateway-admin-logs-2026-03-16
# gateway-core-logs-2026-03-16

# 查询日志数量
curl http://localhost:9200/gateway-*-logs-*/_count

# 查看最新的几条日志
curl http://localhost:9200/gateway-*-logs-*/_search?size=5&sort=@timestamp:desc
```

---

## 🎨 Dashboard 示例

### Panel 1: 实时日志流

- **类型**: Logs
- **Query**: `application:*`
- **Sort**: `@timestamp desc`

### Panel 2: 错误日志统计

- **类型**: Time series
- **Query**: `level:ERROR`
- **Metric**: Count

### Panel 3: 日志级别分布

- **类型**: Pie chart
- **Query**: `application:*`
- **Group by**: `level.keyword`

### Panel 4: 各应用日志量

- **类型**: Bar chart
- **Query**: `application:*`
- **Group by**: `application.keyword`

---

## 🐛 故障排查

### 问题 1: 应用无法连接 Elasticsearch

**检查：**
```bash
# 确认 ES 正在运行
curl http://localhost:9200

# 查看应用日志中的错误
```

**解决：**
- 确保 Elasticsearch 正在运行
- 检查 logback-spring.xml 中的 URL 是否正确
- 检查防火墙设置

### 问题 2: Grafana 看不到日志

**检查：**
```bash
# 确认索引已创建
curl http://localhost:9200/_cat/indices?v | findstr gateway

# 确认有数据
curl http://localhost:9200/gateway-*-logs-*/_count
```

**解决：**
- 确认应用正在运行并产生日志
- 检查 Grafana 数据源配置
- 检查索引名称模式是否匹配

### 问题 3: 日志没有写入 ES

**检查：**
- 查看应用启动日志，搜索 "elasticsearch" 或 "error"
- 确认 Maven 依赖已添加
- 确认项目已重新编译

**解决：**
```bash
# 重新编译
mvn clean install -DskipTests

# 重启应用
```

---

## 📚 Elasticsearch 查询语法

### 基础查询

```
# 精确匹配
field:value

# 模糊匹配
field:*value*

# 范围查询
field:[min TO max]

# 布尔查询
field1:value1 AND field2:value2
field1:value1 OR field2:value2
NOT field:value

# 存在性查询
_exists_:field
```

### 常用查询示例

```
# 查看最近 5 分钟的错误
level:ERROR AND @timestamp:[now-5m TO now]

# 查看特定 logger 的日志
logger:com.aigateway.core.service.ChatService

# 查看包含异常的日志
message:*Exception*

# 查看特定线程的日志
thread:http-nio-9080-exec-1
```

---

## ✅ 验证清单

- [ ] Elasticsearch 正在运行 (http://localhost:9200)
- [ ] Maven 依赖已添加 (logback-elasticsearch-appender)
- [ ] logback-spring.xml 已更新
- [ ] 项目已重新编译
- [ ] 应用已重启
- [ ] ES 中可以看到索引 (gateway-*-logs-*)
- [ ] Grafana 中已添加 Elasticsearch 数据源
- [ ] 可以在 Grafana Explore 中查询到日志

---

## 🎉 完成！

现在你可以在 Grafana 中查看存储在 Elasticsearch 中的日志了！

**优势：**
- ✅ 使用现有的 Elasticsearch
- ✅ 强大的全文搜索能力
- ✅ 灵活的查询语法
- ✅ 与 Grafana 完美集成
- ✅ 支持大规模日志存储
