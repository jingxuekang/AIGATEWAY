# AI Gateway 完整问题排查和启动指南

> 更新时间：2026-03-16
> 适用于：Windows 系统 + IntelliJ IDEA

---

## 📋 当前问题清单

### ✅ 已解决的问题

1. ✅ **DeepSeek 集成** - 已成功集成并测试
2. ✅ **前端显示问题** - 修复了 chatRequest 拦截器数据提取问题
3. ✅ **端口冲突** - Gateway Admin 改为 9082 端口
4. ✅ **Lombok 问题** - 使用显式 getter/setter 替代 @Data

### ⏳ 待解决的问题

1. ⏳ **Azure OpenAI 集成** - Provider 已创建，需要编译和配置 API Key
2. ⏳ **Maven 编译错误** - 需要从根目录编译
3. ⏳ **write_docs.py 乱码** - IDEA 编码设置问题
4. ⏳ **Grafana 安装** - 权限错误（可选，不影响核心功能）

---

## 🚀 正确的启动流程

### 第一步：编译项目

```bash
# 打开命令行（CMD 或 PowerShell）
# 切换到项目根目录
cd D:\wk\AIGATEWAY

# 完整编译所有模块（包含新创建的 AzureOpenAIProvider）
mvn clean install -DskipTests
```

**预期输出：**
```
[INFO] Reactor Summary for AIGATEWAY 1.0.0:
[INFO] 
[INFO] AIGATEWAY .......................................... SUCCESS
[INFO] Gateway Common ..................................... SUCCESS
[INFO] Gateway Provider ................................... SUCCESS
[INFO] Gateway Core ....................................... SUCCESS
[INFO] Gateway Admin ...................................... SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

**如果编译失败：**
- 检查是否在项目根目录（D:\wk\AIGATEWAY）
- 检查 Java 版本：`java -version`（需要 Java 17 或更高）
- 清理 Maven 缓存：`rmdir /s /q C:\Users\osjingxuekang\.m2\repository\com\aigateway`

### 第二步：启动 MySQL

```bash
# 检查 MySQL 是否运行
# 方法 1: 使用服务管理
services.msc
# 找到 MySQL 服务，确保状态为"正在运行"

# 方法 2: 使用命令行
net start MySQL80

# 验证连接
mysql -u root -p
# 输入密码（空密码直接回车）
```

### 第三步：启动 Gateway Admin（端口 9082）

```bash
# 新开一个命令行窗口
cd D:\wk\AIGATEWAY\gateway-admin
java -jar target/gateway-admin-1.0.0.jar
```

**预期输出：**
```
Started AdminApplication in 5.818 seconds
Tomcat started on port(s): 9082 (http)
```

**验证：**
- 浏览器访问：http://localhost:9082/actuator/health
- 应该返回：`{"status":"UP"}`

### 第四步：启动 Gateway Core（端口 9080）

```bash
# 新开一个命令行窗口
cd D:\wk\AIGATEWAY\gateway-core
java -jar target/gateway-core-1.0.0.jar
```

**预期输出：**
```
=== Registered Model Providers ===
Provider: anthropic - com.aigateway.provider.adapter.AnthropicProvider
Provider: azure-openai - com.aigateway.provider.adapter.AzureOpenAIProvider  ← 应该看到这个
Provider: deepseek - com.aigateway.provider.adapter.DeepSeekProvider
Provider: openai - com.aigateway.provider.adapter.OpenAIProvider
Provider: volcano - com.aigateway.provider.adapter.VolcanoProvider
=== Total 5 providers ===

Started CoreApplication in 6.234 seconds
Tomcat started on port(s): 9080 (http)
```

**验证：**
- 浏览器访问：http://localhost:9080/actuator/health
- 应该返回：`{"status":"UP"}`

### 第五步：启动前端（端口 3001）

```bash
# 新开一个命令行窗口
cd D:\wk\AIGATEWAY\frontend

# 如果是第一次运行，先安装依赖
npm install

# 启动开发服务器
npm run dev
```

**预期输出：**
```
VITE v5.x.x  ready in xxx ms

➜  Local:   http://localhost:3001/
➜  Network: use --host to expose
```

**验证：**
- 浏览器访问：http://localhost:3001
- 应该看到登录页面

---

## 🧪 测试 Azure OpenAI

### 1. 配置 API Key（如果有的话）

编辑 `gateway-core/src/main/resources/application.yml`：

```yaml
azure:
  openai:
    api-key: 你的实际API密钥  # 替换这里
    endpoint: https://kmo-japan.openai.azure.com
    deployment: gpt-4o-mini
    api-version: 2025-01-01-preview
```

### 2. 重启 Gateway Core

```bash
# 在 Gateway Core 的命令行窗口按 Ctrl+C 停止
# 然后重新启动
cd D:\wk\AIGATEWAY\gateway-core
java -jar target/gateway-core-1.0.0.jar
```

### 3. 测试

1. 打开浏览器：http://localhost:3001
2. 登录（admin/admin123）
3. 进入 "AI 对话" 页面
4. 选择模型：`azure-gpt-4o-mini`
5. 发送消息测试

**如果没有 Azure API Key：**
- 系统会返回 mock 响应
- 不会报错，可以看到 "This is a mock response from Azure OpenAI..."

---

## 🔧 常见问题解决

### 问题 1: Maven 编译失败 - 找不到 gateway-common

**错误信息：**
```
Could not resolve dependencies for project com.aigateway:gateway-admin:jar:1.0.0
dependency: com.aigateway:gateway-common:jar:1.0.0 was not found
```

**解决方案：**
```bash
# 必须在项目根目录编译，不要在子模块目录
cd D:\wk\AIGATEWAY
mvn clean install -DskipTests
```

### 问题 2: 端口被占用

**错误信息：**
```
Port 9080 was already in use
```

**解决方案：**
```bash
# 查找占用端口的进程
netstat -ano | findstr :9080

# 结束进程（替换 PID 为实际进程 ID）
taskkill /F /PID <PID>

# 或者修改配置文件中的端口
```

### 问题 3: 前端无法连接后端

**症状：**
- 前端显示网络错误
- 浏览器控制台显示 CORS 错误或连接失败

**检查清单：**
1. ✅ Gateway Admin 是否在 9082 端口运行
2. ✅ Gateway Core 是否在 9080 端口运行
3. ✅ 前端 Vite 配置的代理端口是否正确

**验证后端：**
```bash
# 测试 Gateway Admin
curl http://localhost:9082/actuator/health

# 测试 Gateway Core
curl http://localhost:9080/actuator/health
```

### 问题 4: MySQL 连接失败

**错误信息：**
```
Communications link failure
```

**解决方案：**
```bash
# 1. 检查 MySQL 服务是否运行
services.msc

# 2. 检查配置文件中的密码
# gateway-admin/src/main/resources/application.yml
spring:
  datasource:
    password: ""  # 空密码

# 3. 测试连接
mysql -u root -p
```

### 问题 5: write_docs.py 在 IDEA 中乱码

**解决方案：**

1. 在 IDEA 中打开 `write_docs.py`
2. 查看右下角状态栏的编码
3. 点击编码名称 → 选择 `UTF-8` → 选择 `Reload`

或者：

1. `File` → `Settings` → `Editor` → `File Encodings`
2. 将所有编码设置为 `UTF-8`
3. 重新打开文件

### 问题 6: 前端显示 "不支持的模型: azure-gpt-4o-mini"

**原因：**
- AzureOpenAIProvider 没有被编译或加载

**解决方案：**
```bash
# 1. 确认文件存在
dir gateway-provider\src\main\java\com\aigateway\provider\adapter\AzureOpenAIProvider.java

# 2. 重新编译
cd D:\wk\AIGATEWAY
mvn clean install -DskipTests

# 3. 重启 Gateway Core
cd gateway-core
java -jar target/gateway-core-1.0.0.jar

# 4. 检查日志，应该看到 azure-openai provider 注册成功
```

---

## 📊 服务状态检查

### 快速检查所有服务

```bash
# MySQL
mysql -u root -p -e "SELECT 1"

# Gateway Admin
curl http://localhost:9082/actuator/health

# Gateway Core
curl http://localhost:9080/actuator/health

# Frontend
curl http://localhost:3001
```

### 查看日志

**Gateway Admin 日志：**
- 在启动 Gateway Admin 的命令行窗口查看
- 或查看日志文件（如果配置了）

**Gateway Core 日志：**
- 在启动 Gateway Core 的命令行窗口查看
- 关键信息：Provider 注册、API 调用、错误信息

**前端日志：**
- 浏览器按 F12 打开开发者工具
- 查看 Console 标签（JavaScript 错误）
- 查看 Network 标签（API 请求）

---

## 🎯 完整的服务启动脚本

创建一个批处理文件 `start-all.bat`：

```batch
@echo off
echo ========================================
echo Starting AI Gateway Services
echo ========================================

echo.
echo [1/4] Starting Gateway Admin (Port 9082)...
start "Gateway Admin" cmd /k "cd /d D:\wk\AIGATEWAY\gateway-admin && java -jar target/gateway-admin-1.0.0.jar"
timeout /t 10

echo.
echo [2/4] Starting Gateway Core (Port 9080)...
start "Gateway Core" cmd /k "cd /d D:\wk\AIGATEWAY\gateway-core && java -jar target/gateway-core-1.0.0.jar"
timeout /t 10

echo.
echo [3/4] Starting Frontend (Port 3001)...
start "Frontend" cmd /k "cd /d D:\wk\AIGATEWAY\frontend && npm run dev"
timeout /t 5

echo.
echo ========================================
echo All services started!
echo ========================================
echo.
echo Gateway Admin:  http://localhost:9082
echo Gateway Core:   http://localhost:9080
echo Frontend:       http://localhost:3001
echo.
echo Press any key to open browser...
pause
start http://localhost:3001
```

---

## 📝 配置检查清单

### 编译前检查

- [ ] Java 版本 17 或更高：`java -version`
- [ ] Maven 已安装：`mvn -version`
- [ ] 在项目根目录：`cd D:\wk\AIGATEWAY`
- [ ] AzureOpenAIProvider.java 文件存在

### 启动前检查

- [ ] MySQL 服务正在运行
- [ ] 数据库 `aigateway` 已创建
- [ ] 端口 9080, 9082, 3001 未被占用
- [ ] 项目已成功编译（jar 文件存在）

### 运行时检查

- [ ] Gateway Admin 启动成功（日志显示 "Started AdminApplication"）
- [ ] Gateway Core 启动成功（日志显示 5 个 providers）
- [ ] 前端启动成功（显示 Vite 启动信息）
- [ ] 可以访问 http://localhost:3001

### Azure OpenAI 检查（可选）

- [ ] AzureOpenAIProvider 在日志中注册成功
- [ ] application.yml 中配置了 endpoint
- [ ] 如果有 API Key，已配置到 application.yml
- [ ] 前端可以选择 azure-gpt-4o-mini 模型

---

## 🆘 获取帮助

如果遇到问题：

1. **查看日志** - 大部分问题都能从日志中找到原因
2. **检查端口** - 确保所需端口未被占用
3. **验证配置** - 检查 application.yml 配置是否正确
4. **重新编译** - 修改代码后必须重新编译
5. **重启服务** - 修改配置后必须重启服务

---

## ✅ 成功标志

当所有服务正常运行时，你应该能够：

1. ✅ 访问 http://localhost:3001 看到登录页面
2. ✅ 使用 admin/admin123 登录成功
3. ✅ 在 "AI 对话" 页面看到多个模型选项（包括 azure-gpt-4o-mini）
4. ✅ 选择 deepseek-chat 发送消息，收到 AI 回复
5. ✅ 在 "使用日志" 页面看到调用记录
6. ✅ 在 "密钥管理" 页面看到 API Keys

---

## 📚 相关文档

- `AZURE_FIX_INSTRUCTIONS.md` - Azure OpenAI 修复说明
- `FRONTEND_DISPLAY_FIX.md` - 前端显示问题修复
- `MAVEN_BUILD_FIX.md` - Maven 编译问题
- `GRAFANA_INSTALL_FIX.md` - Grafana 安装问题（可选）
- `IDEA_ENCODING_FIX.md` - IDEA 编码问题
- `COMPLETE_SETUP_GUIDE.md` - 完整设置指南
