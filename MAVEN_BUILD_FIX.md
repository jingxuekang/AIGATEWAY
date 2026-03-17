# Maven 编译错误修复

## 错误信息
```
Could not resolve dependencies for project com.aigateway:gateway-admin:jar:1.0.0
dependency: com.aigateway:gateway-common:jar:1.0.0 (compile)
com.aigateway:gateway-common:jar:1.0.0 was not found
```

## 原因
你在 IDEA 中单独编译 `gateway-admin` 模块，但它依赖 `gateway-common` 模块，而 `gateway-common` 还没有被安装到本地 Maven 仓库。

## 解决方案

### 方法 1: 在项目根目录编译（推荐）

```bash
# 1. 打开命令行，切换到项目根目录
cd D:\wk\AIGATEWAY

# 2. 清理并安装所有模块
mvn clean install -DskipTests

# 这会按顺序编译：
# gateway-common → gateway-provider → gateway-core → gateway-admin
```

### 方法 2: 在 IDEA 中正确编译

#### 步骤 1: 使用根 pom.xml

1. 在 IDEA 右侧打开 "Maven" 面板
2. 找到最顶层的 "AIGATEWAY" 项目（不是 gateway-admin）
3. 展开 "Lifecycle"
4. 双击 "clean"
5. 双击 "install"

#### 步骤 2: 或者使用 IDEA 的 Run Configuration

1. 点击 IDEA 顶部的 "Run" → "Edit Configurations"
2. 点击左上角的 "+" → "Maven"
3. 配置如下：
   - Name: `Build All Modules`
   - Working directory: `D:\wk\AIGATEWAY`
   - Command line: `clean install -DskipTests`
4. 点击 "OK"
5. 点击运行按钮

### 方法 3: 清理 Maven 缓存

如果上面的方法还不行，可能是 Maven 缓存问题：

```bash
# 1. 删除本地缓存的 gateway 相关依赖
rmdir /s /q C:\Users\osjingxuekang\.m2\repository\com\aigateway

# 2. 重新编译
cd D:\wk\AIGATEWAY
mvn clean install -DskipTests
```

### 方法 4: 在 IDEA 中配置 Maven

确保 IDEA 使用正确的 Maven 配置：

1. `File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Maven`
2. 检查以下设置：
   - Maven home directory: `D:\soft\Idea\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3`
   - User settings file: 使用默认或指定你的 settings.xml
   - Local repository: `C:\Users\osjingxuekang\.m2\repository`
3. 点击 "Apply"

## 正确的编译顺序

这是一个多模块 Maven 项目，模块之间有依赖关系：

```
gateway-common (基础模块，被所有模块依赖)
    ↓
gateway-provider (依赖 common)
    ↓
gateway-core (依赖 common + provider)
    ↓
gateway-admin (依赖 common)
```

**必须从根目录编译**，Maven 会自动按照依赖顺序编译各个模块。

## 验证编译成功

编译成功后，应该看到：

```
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for AIGATEWAY 1.0.0:
[INFO] 
[INFO] AIGATEWAY .......................................... SUCCESS [  0.123 s]
[INFO] Gateway Common ..................................... SUCCESS [  2.456 s]
[INFO] Gateway Provider ................................... SUCCESS [  3.789 s]
[INFO] Gateway Core ....................................... SUCCESS [  4.567 s]
[INFO] Gateway Admin ...................................... SUCCESS [  3.234 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

并且在各个模块的 target 目录下会生成 jar 文件：
- `gateway-common/target/gateway-common-1.0.0.jar`
- `gateway-provider/target/gateway-provider-1.0.0.jar`
- `gateway-core/target/gateway-core-1.0.0.jar`
- `gateway-admin/target/gateway-admin-1.0.0.jar`

## 常见问题

### Q1: 为什么不能单独编译某个模块？

A: 可以，但前提是依赖的模块已经安装到本地仓库。第一次编译必须从根目录开始。

### Q2: 每次修改代码都要编译所有模块吗？

A: 不需要。第一次完整编译后，后续可以：
- 只修改某个模块，就只编译那个模块
- 但如果修改了 gateway-common，需要重新编译所有依赖它的模块

### Q3: IDEA 自动编译和 Maven 编译有什么区别？

A: 
- IDEA 自动编译：只编译 .class 文件，不生成 jar 包
- Maven 编译：生成 jar 包并安装到本地仓库

运行 Spring Boot 应用时，建议使用 Maven 编译生成的 jar 包。

## 快速命令参考

```bash
# 完整编译（包含测试）
mvn clean install

# 跳过测试编译（推荐，更快）
mvn clean install -DskipTests

# 只编译不安装
mvn clean compile

# 只打包不测试
mvn clean package -DskipTests

# 清理编译产物
mvn clean

# 查看依赖树
mvn dependency:tree

# 强制更新依赖
mvn clean install -U -DskipTests
```

## 下一步

编译成功后，按照以下顺序启动服务：

```bash
# 1. 启动 Gateway Admin
cd gateway-admin
java -jar target/gateway-admin-1.0.0.jar

# 2. 启动 Gateway Core（新开一个命令行窗口）
cd gateway-core
java -jar target/gateway-core-1.0.0.jar

# 3. 启动前端（新开一个命令行窗口）
cd frontend
npm run dev
```
