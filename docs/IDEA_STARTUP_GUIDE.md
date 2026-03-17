# IDEA 启动指南

完整的 IntelliJ IDEA 启动步骤。

## 第一步：导入项目

### 1. 打开 IDEA

- 如果是首次打开：选择 `Open`
- 如果已有项目打开：`File` → `Open`

### 2. 选择项目目录

- 浏览到 `D:\wk\AIGATEWAY`
- 选择根目录（包含 `pom.xml` 的目录）
- 点击 `OK`

### 3. 等待项目加载

IDEA 会自动：
- ✅ 识别 Maven 项目
- ✅ 下载依赖（可能需要几分钟）
- ✅ 索引代码

**提示：** 右下角会显示进度条，等待完成。

## 第二步：安装 Lombok 插件

### 1. 打开插件设置

- Windows/Linux: `File` → `Settings` → `Plugins`
- macOS: `IntelliJ IDEA` → `Preferences` → `Plugins`

### 2. 搜索并安装

1. 在搜索框输入 `Lombok`
2. 找到 `Lombok` 插件（作者：Michail Plushnikov）
3. 点击 `Install`
4. 安装完成后点击 `Restart IDE`

### 3. 启用注解处理

1. `File` → `Settings` → `Build, Execution, Deployment` → `Compiler` → `Annotation Processors`
2. 勾选 `Enable annotation processing`
3. 点击 `Apply` → `OK`

## 第三步：配置数据库

### 1. 启动 MySQL

确保 MySQL 服务正在运行。

### 2. 创建数据库

使用 DBeaver 或命令行：

```sql
CREATE DATABASE ai_gateway CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 导入表结构

**使用 DBeaver：**
1. 右键点击 `ai_gateway` 数据库
2. `SQL Editor` → `Open SQL Script`
3. 选择 `docs/database/schema.sql`
4. 点击执行

**使用命令行：**
```bash
mysql -u root -p ai_gateway < docs/database/schema.sql
```

### 4. 修改配置文件

编辑 `gateway-admin/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_gateway?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 你的MySQL密码  # 修改这里
```

## 第四步：启动后端服务

### 1. 启动管理服务（Admin）

#### 找到启动类

在项目结构中找到：
```
gateway-admin
  └── src
      └── main
          └── java
              └── com.aigateway.admin
                  └── AdminApplication.java
```

#### 运行

- **方式一：** 右键点击 `AdminApplication.java` → `Run 'AdminApplication'`
- **方式二：** 打开文件，点击左侧的绿色三角形 ▶️
- **方式三：** 使用快捷键 `Ctrl+Shift+F10` (Windows/Linux) 或 `Ctrl+Shift+R` (macOS)

#### 验证启动成功

在控制台看到：
```
Started AdminApplication in X.XXX seconds
```

访问：http://localhost:8081/doc.html

### 2. 启动网关服务（Gateway）

#### 找到启动类

```
gateway-core
  └── src
      └── main
          └── java
              └── com.aigateway.core
                  └── GatewayApplication.java
```

#### 运行

右键点击 `GatewayApplication.java` → `Run 'GatewayApplication'`

#### 验证启动成功

在控制台看到：
```
Started GatewayApplication in X.XXX seconds
```

访问：http://localhost:8080/doc.html

## 第五步：启动前端

### 1. 打开 IDEA 终端

点击 IDEA 底部的 `Terminal` 标签（或按 `Alt+F12`）

### 2. 进入前端目录

```bash
cd frontend
```

### 3. 安装依赖（首次运行）

```bash
npm install
```

等待安装完成（可能需要几分钟）。

### 4. 启动开发服务器

```bash
npm run dev
```

### 5. 验证启动成功

看到输出：
```
  VITE v5.0.8  ready in 500 ms

  ➜  Local:   http://localhost:3000/
```

访问：http://localhost:3000

## 常见问题

### 1. 编译错误：找不到符号

**问题：** Lombok 注解未生效

**解决：**
1. 确认已安装 Lombok 插件
2. 确认已启用注解处理
3. `File` → `Invalidate Caches` → `Invalidate and Restart`

### 2. 端口被占用

**错误信息：**
```
Port 8080 is already in use
```

**解决方法一：** 修改端口

编辑 `application.yml`：
```yaml
server:
  port: 8082  # 改成其他端口
```

**解决方法二：** 停止占用端口的进程

Windows:
```bash
netstat -ano | findstr :8080
taskkill /PID <进程ID> /F
```

### 3. 数据库连接失败

**错误信息：**
```
Communications link failure
```

**检查清单：**
- [ ] MySQL 服务是否启动？
- [ ] 数据库 `ai_gateway` 是否存在？
- [ ] 用户名密码是否正确？
- [ ] 端口 3306 是否正确？

### 4. Maven 依赖下载失败

**解决：**

1. 检查网络连接
2. 使用阿里云镜像（项目已配置）
3. 手动刷新：右键点击项目 → `Maven` → `Reload Project`

### 5. 前端启动失败

**错误：** `npm: command not found`

**解决：** 安装 Node.js
- 下载：https://nodejs.org/
- 安装后重启 IDEA

## 调试技巧

### 1. 查看日志

在 IDEA 底部的 `Run` 或 `Debug` 标签中查看完整日志。

### 2. 断点调试

1. 在代码行号左侧点击，设置断点（红点）
2. 使用 `Debug` 模式启动（绿色虫子图标 🐛）
3. 程序会在断点处暂停

### 3. 热部署

安装 Spring Boot DevTools 后，修改代码会自动重启。

### 4. 查看数据库

在 IDEA 中：
1. 右侧点击 `Database` 标签
2. 点击 `+` → `Data Source` → `MySQL`
3. 填写连接信息
4. 可以直接在 IDEA 中查询数据库

## 运行配置

### 保存运行配置

首次运行后，IDEA 会自动保存运行配置。下次可以：
- 点击右上角的运行配置下拉框
- 选择 `AdminApplication` 或 `GatewayApplication`
- 点击运行按钮 ▶️

### 同时运行多个服务

1. 运行第一个服务（Admin）
2. 不要停止，直接运行第二个服务（Gateway）
3. IDEA 会在不同的标签页显示两个服务的日志

### 配置环境变量

如果需要设置环境变量：
1. 点击右上角运行配置下拉框
2. 选择 `Edit Configurations...`
3. 在 `Environment variables` 中添加
4. 例如：`SPRING_PROFILES_ACTIVE=dev`

## 快捷键

### 常用快捷键

- `Ctrl+Shift+F10`: 运行当前文件
- `Shift+F10`: 运行上次运行的配置
- `Ctrl+F2`: 停止运行
- `Alt+F12`: 打开终端
- `Ctrl+F9`: 编译项目
- `Ctrl+Shift+F9`: 重新编译当前文件

### 调试快捷键

- `Shift+F9`: 调试运行
- `F8`: 单步跳过
- `F7`: 单步进入
- `Shift+F8`: 单步跳出
- `F9`: 继续运行

## 项目结构

```
ai-model-gateway/
├── gateway-common/      # 公共模块
├── gateway-core/        # 网关服务 (8080)
│   └── GatewayApplication.java  ← 启动这个
├── gateway-admin/       # 管理服务 (8081)
│   └── AdminApplication.java    ← 启动这个
├── gateway-provider/    # 模型适配器
├── frontend/           # 前端 (3000)
│   └── npm run dev     ← 在终端运行
└── docs/              # 文档
```

## 成功标志

### 后端启动成功

- ✅ 控制台显示 "Started Application"
- ✅ 没有红色错误信息
- ✅ 可以访问 Swagger 文档

### 前端启动成功

- ✅ 终端显示 "ready in XXX ms"
- ✅ 显示本地访问地址
- ✅ 浏览器可以打开页面

## 下一步

启动成功后，你可以：

1. 📖 查看 API 文档：http://localhost:8081/doc.html
2. 🎨 访问前端页面：http://localhost:3000
3. 🔑 创建 API Key
4. 🤖 配置模型
5. 💬 测试聊天功能

需要帮助？查看其他文档：
- [快速启动指南](QUICKSTART.md)
- [DBeaver 使用指南](DBEAVER_GUIDE.md)
- [部署文档](DEPLOYMENT.md)
