# Grafana 看不到 Elasticsearch 日志 - 排查指南

## 问题：ES 有日志，但 Grafana 看不到

---

## 🔍 排查步骤

### 步骤 1: 确认 ES 中有日志

```bash
# 查看所有索引
curl http://localhost:9200/_cat/indices?v

# 查看日志索引
curl http://localhost:9200/_cat/indices?v | findstr log

# 查看日志数量
curl http://localhost:9200/你的索引名/_count

# 查看最新的日志
curl http://localhost:9200/你的索引名/_search?size=1&sort=@timestamp:desc
```

**记下你的索引名称**，例如：
- `gateway-admin-logs-2026-03-16`
- `logstash-2026.03.16`
- `logs-2026-03-16`

---

### 步骤 2: 检查 Grafana 数据源配置

#### 2.1 检查连接

1. 打开 Grafana: http://localhost:3000
2. 进入 Configuration → Data Sources
3. 找到你的 Elasticsearch 数据源
4. 点击进入编辑

#### 2.2 关键配置项

**必须正确配置：**

1. **URL**: `http://localhost:9200`
   - 如果 ES 在其他地址，改为实际地址

2. **Index name**: 
   - 使用通配符匹配你的索引
   - 例如：`gateway-*-logs-*`
   - 或者：`logstash-*`
   - 或者：`logs-*`
   
3. **Time field name**: 
   - 通常是：`@timestamp`
   - 或者：`timestamp`
   - 必须与 ES 中的时间字段名称一致

4. **Version**: 
   - 选择你的 ES 版本（7.x 或 8.x）

5. **Min time interval**: 
   - 设置为：`10s` 或 `1m`

#### 2.3 测试连接

点击 "Save & Test"，应该显示：
- ✅ "Data source is working"
- ✅ "Index OK. Time field name OK."

如果显示错误，记下错误信息。

---

### 步骤 3: 在 Explore 中正确查询

#### 3.1 选择正确的数据源

1. 点击 Explore
2. 在顶部下拉框选择你的 Elasticsearch 数据源

#### 3.2 选择正确的查询模式

Grafana 有两种查询模式：

**模式 1: Logs（推荐）**
- 在查询编辑器上方，点击 "Logs" 标签
- 这会显示日志列表

**模式 2: Metrics**
- 显示图表，不适合查看日志内容

#### 3.3 输入查询

在查询框中输入：

```
# 查询所有日志（最简单）
*

# 或者按字段查询
application:gateway-admin

# 或者
level:INFO
```

#### 3.4 调整时间范围

在右上角的时间选择器中：
- 选择 "Last 1 hour" 或 "Last 6 hours"
- 或者选择 "Last 24 hours"

确保时间范围包含你的日志时间！

---

### 步骤 4: 常见问题和解决方案

#### 问题 1: "No data"

**原因：**
- 时间范围不对
- 索引名称不匹配
- 时间字段名称不对

**解决：**
```bash
# 1. 确认索引中的时间字段名称
curl http://localhost:9200/你的索引名/_mapping

# 查找时间字段，通常是 @timestamp 或 timestamp

# 2. 确认日志的时间范围
curl http://localhost:9200/你的索引名/_search?size=1&sort=@timestamp:desc

# 查看返回的 @timestamp 值，确保在 Grafana 选择的时间范围内
```

#### 问题 2: "Index not found"

**原因：**
- Index name 配置错误
- 索引名称不匹配

**解决：**
1. 查看 ES 中的实际索引名称
2. 在 Grafana 数据源配置中使用正确的通配符

**示例：**
- ES 索引：`gateway-admin-logs-2026-03-16`
- Grafana 配置：`gateway-*-logs-*`

或者：
- ES 索引：`logstash-2026.03.16`
- Grafana 配置：`logstash-*`

#### 问题 3: "Time field not found"

**原因：**
- Time field name 配置错误

**解决：**
```bash
# 查看索引的 mapping
curl http://localhost:9200/你的索引名/_mapping

# 找到 type: "date" 的字段
# 通常是 @timestamp 或 timestamp
```

在 Grafana 数据源配置中，将 "Time field name" 改为实际的字段名。

#### 问题 4: 看到数据但是是空的

**原因：**
- 日志字段映射不对
- 需要配置显示的字段

**解决：**
在 Explore 的 Logs 视图中：
1. 点击右侧的 "Settings" 图标
2. 在 "Displayed fields" 中选择要显示的字段：
   - `message`
   - `level`
   - `logger`
   - `application`

---

## 🎯 快速诊断脚本

创建一个诊断脚本来检查所有配置：

```bash
@echo off
echo ========================================
echo Elasticsearch + Grafana Diagnostic
echo ========================================
echo.

echo [1] Checking Elasticsearch...
curl -s http://localhost:9200 >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Elasticsearch not running
) else (
    echo [OK] Elasticsearch is running
)

echo.
echo [2] Listing indices...
curl -s http://localhost:9200/_cat/indices?v

echo.
echo [3] Checking log count...
curl -s http://localhost:9200/*log*/_count

echo.
echo [4] Sample log entry...
curl -s http://localhost:9200/*log*/_search?size=1

echo.
echo [5] Checking Grafana...
curl -s http://localhost:3000 >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Grafana not running
) else (
    echo [OK] Grafana is running
)

echo.
pause
```

---

## 📝 正确的配置示例

### Grafana 数据源配置

```
Name: Elasticsearch Logs
Type: Elasticsearch

HTTP:
  URL: http://localhost:9200
  Access: Server (default)

Elasticsearch details:
  Index name: gateway-*-logs-*
  Pattern: Daily
  Time field name: @timestamp
  Version: 8.x (或你的版本)
  Max concurrent Shard Requests: 5
  Min time interval: 10s
```

### Explore 查询示例

```
# 在 Logs 模式下
Query: *
Time range: Last 1 hour

# 或者更具体的查询
Query: application:gateway-admin AND level:INFO
```

---

## ✅ 检查清单

- [ ] ES 正在运行 (http://localhost:9200)
- [ ] ES 中有日志索引（用 curl 确认）
- [ ] 记下了实际的索引名称
- [ ] 记下了时间字段名称（@timestamp 或 timestamp）
- [ ] Grafana 正在运行 (http://localhost:3000)
- [ ] Grafana 中已添加 ES 数据源
- [ ] 数据源配置中的 Index name 匹配实际索引
- [ ] 数据源配置中的 Time field name 正确
- [ ] 数据源测试通过（Save & Test 成功）
- [ ] 在 Explore 中选择了正确的数据源
- [ ] 在 Explore 中选择了 "Logs" 模式
- [ ] 时间范围包含日志的时间

---

## 🆘 如果还是不行

请提供以下信息：

1. **ES 索引名称**
   ```bash
   curl http://localhost:9200/_cat/indices?v | findstr log
   ```

2. **索引的 mapping**
   ```bash
   curl http://localhost:9200/你的索引名/_mapping
   ```

3. **一条示例日志**
   ```bash
   curl http://localhost:9200/你的索引名/_search?size=1
   ```

4. **Grafana 数据源配置截图**
   - Index name
   - Time field name
   - Version

5. **Grafana Explore 的查询和错误信息**

---

## 💡 最可能的原因

根据经验，90% 的情况是以下三个问题之一：

1. **Index name 不匹配** - 检查通配符是否正确
2. **Time field name 错误** - 确认是 @timestamp 还是 timestamp
3. **时间范围不对** - 扩大时间范围到 Last 24 hours

先检查这三个！
