# 本地快速启动指南

无需 Docker，直接在本地启动项目。

## 前置条件

确保已安装以下软件：

- ✅ JDK 17+ 
- ✅ Maven 3.6+
- ✅ MySQL 8.0+ (本地安装)
- ✅ Node.js 18+

**可选（如果不需要日志功能可以暂时不装）：**
- Redis
- Elasticsearch

## 第一步：准备数据库

### 1. 启动 MySQL

```bash
# Windows
net start mysql

# macOS
brew services start mysql

# Linux
sudo systemctl start mysql
```

### 2. 创建数据库并导入表结构

#### 方式一：使用 DBeaver（推荐，图形化界面）

**步骤 1：连接 MySQL**

1. 打开 DBeaver
2. 点击左上角"新建连接"图标（或 `Ctrl+N`）
3. 选择 MySQL
4. 填写连接信息：
   - Host: `localhost`
   - Port: `3306`
   - Database: 留空（稍后创建）
   - Username: `root`
   - Password: 你的 MySQL 密码
5. 点击"测试连接"，确保连接成功
6. 点击"完成"

**步骤 2：创建数据库**

1. 在左侧数据库列表中，右键点击连接名称
2. 选择 `SQL Editor` → `New SQL Script`
3. 输入以下 SQL：
   ```sql
   CREATE DATABASE ai_gateway CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
4. 点击执行按钮（或按 `Ctrl+Enter`）

**步骤 3：导入表结构**

1. 在左侧数据库列表中，展开连接，找到 `ai_gateway` 数据库
2. 右键点击 `ai_gateway` → `SQL Editor` → `Open SQL Script`
3. 选择项目中的 `docs/database/schema.sql` 文件
4. 点击执行按钮（或按 `Ctrl+Enter`）
5. 等待执行完成

**步骤 4：验证**

1. 在左侧展开 `ai_gateway` → `Tables`
2. 应该看到 3 张表：
   - `api_key`
   - `model`
   - `key_application`
3. 右键点击 `model` 表 → `View Data`
4. 应该看到 4 条预置的模型数据

#### 方式二：使用命令行

```bash
# 登录 MySQL
mysql -u root -p

# 执行以下 SQL
CREATE DATABASE ai_gateway CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
exit;

# 导入表结构
mysql -u root -p ai_gateway < docs/database/schema.sql
```

### 3. 验证数据库

**使用 DBeaver：**
- 展开 `ai_gateway` → `Tables`，应该看到 3 张表
- 查看 `model` 表数据，应该有 4 条记录

**使用命令行：**
```bash
mysql -u root -p ai_gateway -e "SHOW TABLES;"
```

应该看到 `api_key`, `model`, `key_application` 三张表。

## 第二步：配置后端

### 1. 修改数据库配置

编辑 `gateway-admin/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_gateway?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 你的MySQL密码  # 修改这里
```

### 2. 临时禁用 Redis 和 Elasticsearch（可选）

如果暂时不需要日志功能，可以注释掉相关配置：

编辑 `gateway-core/src/main/resources/application.yml`：

```yaml
# 注释掉这些配置
# spring:
#   redis:
#     host: localhost
#     port: 6379
#   
#   elasticsearch:
#     uris: http://localhost:9200
```

## 第三步：启动后端服务

### 方式一：使用 Maven 命令（推荐）

打开两个终端窗口：

**终端 1 - 启动管理服务：**
```bash
cd gateway-admin
mvn spring-boot:run
```

等待启动完成，看到 "Started AdminApplication" 表示成功。
访问: http://localhost:8081/doc.html 查看 API 文档

**终端 2 - 启动网关服务：**
```bash
cd gateway-core
mvn spring-boot:run
```

等待启动完成，看到 "Started GatewayApplication" 表示成功。
访问: http://localhost:8080/doc.html 查看 API 文档

### 方式二：使用 IDE 启动

#### IDEA 启动步骤：

1. 用 IDEA 打开项目根目录
2. 等待 Maven 依赖下载完成
3. 找到 `gateway-admin/src/main/java/com/aigateway/admin/AdminApplication.java`
4. 右键 → Run 'AdminApplication'
5. 找到 `gateway-core/src/main/java/com/aigateway/core/GatewayApplication.java`
6. 右键 → Run 'GatewayApplication'

## 第四步：启动前端

打开新的终端窗口：

```bash
cd frontend

# 安装依赖（首次运行）
npm install

# 启动开发服务器
npm run dev
```

看到以下输出表示成功：
```
  VITE v5.0.8  ready in 500 ms

  ➜  Local:   http://localhost:3000/
  ➜  Network: use --host to expose
```

访问: http://localhost:3000

## 验证启动成功

### 1. 检查后端服务

```bash
# 检查管理服务
curl http://localhost:8081/api/admin/models

# 检查网关服务（需要 API Key，暂时会返回 401）
curl http://localhost:8080/v1/health
```

### 2. 检查前端

浏览器访问 http://localhost:3000，应该看到：
- 左侧菜单：仪表盘、API Keys、模型管理、调用日志、聊天测试
- 页面正常显示

### 3. 测试功能

1. 点击"模型管理"，应该看到 4 个预置模型（gpt-4, gpt-3.5-turbo, claude-3-opus, claude-3-sonnet）
2. 点击"API Keys"，可以创建新的 API Key
3. 点击"聊天测试"，可以测试模型调用（需要先配置真实的模型 API）

## 常见问题

### 1. 端口被占用

**错误信息：** `Port 8080 is already in use`

**解决方法：**
```bash
# Windows - 查找占用端口的进程
netstat -ano | findstr :8080
taskkill /PID <进程ID> /F

# macOS/Linux
lsof -i :8080
kill -9 <PID>
```

或者修改配置文件中的端口号。

### 2. 数据库连接失败

**错误信息：** `Communications link failure`

**检查清单：**
- MySQL 服务是否启动？
- 用户名密码是否正确？
- 数据库 `ai_gateway` 是否存在？
- 防火墙是否阻止了 3306 端口？

### 3. Maven 依赖下载慢

**解决方法：** 配置国内镜像

编辑 `~/.m2/settings.xml`：

```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <name>Aliyun Maven</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

### 4. 前端启动失败

**错误信息：** `Cannot find module`

**解决方法：**
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
```

### 5. 编译错误

**错误信息：** `Source option 17 is no longer supported`

**解决方法：** 确保使用 JDK 17+
```bash
java -version  # 应该显示 17 或更高版本
```

## 简化启动（不需要日志功能）

如果只想快速体验核心功能，可以：

### 1. 只启动必需服务

- ✅ MySQL（必需）
- ✅ 后端服务（必需）
- ✅ 前端（必需）
- ❌ Redis（可选，用于缓存）
- ❌ Elasticsearch（可选，用于日志存储）

### 2. 修改配置

编辑 `gateway-core/pom.xml`，注释掉 Redis 和 ES 依赖：

```xml
<!-- 暂时注释掉
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<dependency>
    <groupId>co.elastic.clients</groupId>
    <artifactId>elasticsearch-java</artifactId>
</dependency>
-->
```

### 3. 禁用日志服务

编辑 `gateway-core/src/main/java/com/aigateway/core/service/ChatService.java`：

```java
// 注释掉日志记录
// usageLogService.logUsage(request, response, latency);
```

这样就可以只用 MySQL 启动项目了！

## 启动脚本

为了方便，可以创建启动脚本：

### Windows (start.bat)

```batch
@echo off
echo Starting AI Gateway...

start cmd /k "cd gateway-admin && mvn spring-boot:run"
timeout /t 10
start cmd /k "cd gateway-core && mvn spring-boot:run"
timeout /t 10
start cmd /k "cd frontend && npm run dev"

echo All services started!
```

### macOS/Linux (start.sh)

```bash
#!/bin/bash

echo "Starting AI Gateway..."

# 启动管理服务
cd gateway-admin
mvn spring-boot:run &
ADMIN_PID=$!

sleep 10

# 启动网关服务
cd ../gateway-core
mvn spring-boot:run &
CORE_PID=$!

sleep 10

# 启动前端
cd ../frontend
npm run dev &
FRONTEND_PID=$!

echo "All services started!"
echo "Admin PID: $ADMIN_PID"
echo "Core PID: $CORE_PID"
echo "Frontend PID: $FRONTEND_PID"
```

使用方法：
```bash
chmod +x start.sh
./start.sh
```

## 下一步

启动成功后，你可以：

1. 📖 查看 API 文档：http://localhost:8081/doc.html
2. 🔑 创建 API Key
3. 🤖 配置真实的模型 API（OpenAI、Claude 等）
4. 💬 测试聊天功能
5. 📊 查看调用日志

需要帮助？查看 [完整部署文档](DEPLOYMENT.md)
