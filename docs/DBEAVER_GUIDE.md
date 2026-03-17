# DBeaver 数据库管理指南

使用 DBeaver 管理 AI Gateway 项目的 MySQL 数据库。

## 安装 DBeaver

### 下载

访问官网下载：https://dbeaver.io/download/

- Windows: 下载 `.exe` 安装包
- macOS: 下载 `.dmg` 文件
- Linux: 下载 `.deb` 或 `.rpm` 包

### 安装

双击安装包，按照提示完成安装。

## 连接 MySQL 数据库

### 1. 创建新连接

1. 打开 DBeaver
2. 点击左上角"新建连接"图标（闪电图标）
3. 或者菜单栏：`Database` → `New Database Connection`
4. 或者快捷键：`Ctrl+N` (Windows/Linux) / `Cmd+N` (macOS)

### 2. 选择数据库类型

1. 在弹出的窗口中选择 `MySQL`
2. 点击"下一步"

### 3. 配置连接信息

填写以下信息：

```
Server Host: localhost
Port: 3306
Database: (留空，稍后创建)
Username: root
Password: 你的MySQL密码
```

**重要提示：**
- 如果是首次连接，DBeaver 会提示下载 MySQL 驱动，点击"下载"即可
- 勾选"保存密码"可以避免每次都输入密码

### 4. 测试连接

1. 点击"测试连接"按钮
2. 如果显示"Connected"，说明连接成功
3. 如果失败，检查：
   - MySQL 服务是否启动
   - 用户名密码是否正确
   - 端口 3306 是否被占用

### 5. 完成连接

点击"完成"按钮，连接会出现在左侧数据库导航器中。

## 创建数据库

### 方式一：使用 SQL 编辑器（推荐）

1. 在左侧数据库列表中，右键点击你的连接（例如 `localhost`）
2. 选择 `SQL Editor` → `New SQL Script`
3. 在编辑器中输入：

```sql
CREATE DATABASE ai_gateway 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

4. 点击工具栏的"执行"按钮（▶️ 图标）
5. 或者按快捷键：`Ctrl+Enter` (Windows/Linux) / `Cmd+Enter` (macOS)
6. 看到"Query executed successfully"表示成功

### 方式二：使用图形界面

1. 右键点击连接名称
2. 选择 `Create` → `Database`
3. 填写：
   - Database name: `ai_gateway`
   - Charset: `utf8mb4`
   - Collation: `utf8mb4_unicode_ci`
4. 点击"OK"

### 验证

1. 在左侧数据库列表中，右键点击连接
2. 选择"刷新"（或按 `F5`）
3. 展开连接，应该看到 `ai_gateway` 数据库

## 导入表结构

### 1. 打开 SQL 脚本

1. 在左侧展开连接，找到 `ai_gateway` 数据库
2. 右键点击 `ai_gateway`
3. 选择 `SQL Editor` → `Open SQL Script`
4. 浏览到项目目录，选择 `docs/database/schema.sql`
5. 点击"打开"

### 2. 执行脚本

1. 确认当前数据库是 `ai_gateway`（在编辑器顶部可以看到）
2. 点击"执行 SQL 脚本"按钮（▶️ 图标）
3. 或者按 `Ctrl+Enter`
4. 等待执行完成，应该看到成功消息

### 3. 验证表结构

1. 在左侧展开 `ai_gateway` → `Tables`
2. 应该看到 3 张表：
   - `api_key` - API Key 管理表
   - `model` - 模型配置表
   - `key_application` - API Key 申请表

## 查看和编辑数据

### 查看表数据

1. 在左侧表列表中，右键点击表名（例如 `model`）
2. 选择 `View Data` 或双击表名
3. 数据会在右侧显示

### 查看表结构

1. 右键点击表名
2. 选择 `View Table` → `Properties`
3. 可以看到列定义、索引、外键等信息

### 编辑数据

1. 在数据查看界面，双击单元格即可编辑
2. 编辑完成后，点击工具栏的"保存"按钮
3. 或者按 `Ctrl+S`

### 添加新数据

1. 在数据查看界面，点击工具栏的"添加行"按钮（+ 图标）
2. 填写数据
3. 点击"保存"

### 删除数据

1. 选中要删除的行
2. 点击工具栏的"删除行"按钮（- 图标）
3. 点击"保存"确认删除

## 执行 SQL 查询

### 1. 打开 SQL 编辑器

- 右键点击数据库 → `SQL Editor` → `New SQL Script`
- 或者快捷键：`Ctrl+]` (Windows/Linux) / `Cmd+]` (macOS)

### 2. 编写 SQL

```sql
-- 查询所有模型
SELECT * FROM model;

-- 查询启用的模型
SELECT * FROM model WHERE status = 1;

-- 查询某个用户的 API Keys
SELECT * FROM api_key WHERE user_id = 1;

-- 统计每个提供商的模型数量
SELECT provider, COUNT(*) as count 
FROM model 
GROUP BY provider;
```

### 3. 执行查询

- 执行全部：点击"执行 SQL 脚本"按钮
- 执行选中：选中 SQL 语句，按 `Ctrl+Enter`
- 执行当前语句：光标放在语句上，按 `Ctrl+Enter`

### 4. 查看结果

- 结果会显示在编辑器下方的"结果"标签页
- 可以导出结果为 CSV、JSON 等格式

## 常用操作

### 刷新数据库

- 右键点击连接或数据库 → `Refresh` (或按 `F5`)

### 导出数据

1. 右键点击表 → `Export Data`
2. 选择导出格式（CSV, JSON, SQL, Excel 等）
3. 选择保存位置
4. 点击"开始"

### 导入数据

1. 右键点击表 → `Import Data`
2. 选择数据文件
3. 配置导入选项
4. 点击"开始"

### 生成 SQL

1. 右键点击表 → `Generate SQL`
2. 选择 SQL 类型：
   - `SELECT` - 生成查询语句
   - `INSERT` - 生成插入语句
   - `UPDATE` - 生成更新语句
   - `DELETE` - 生成删除语句
   - `DDL` - 生成建表语句

### 查看 ER 图

1. 右键点击数据库 → `View Diagram`
2. 可以看到表之间的关系图
3. 可以拖拽调整布局

## 实用技巧

### 1. 快捷键

- `Ctrl+Enter` - 执行 SQL
- `Ctrl+Space` - 自动补全
- `Ctrl+/` - 注释/取消注释
- `Ctrl+F` - 查找
- `Ctrl+H` - 替换
- `F5` - 刷新

### 2. SQL 格式化

1. 在 SQL 编辑器中，右键点击
2. 选择 `Format` → `Format SQL`
3. 或者按 `Ctrl+Shift+F`

### 3. 保存常用查询

1. 编写 SQL 查询
2. 点击工具栏的"保存"按钮
3. 给查询命名
4. 下次可以在左侧"脚本"中找到

### 4. 多标签页

- 可以同时打开多个 SQL 编辑器和数据查看器
- 使用 `Ctrl+Tab` 切换标签页

### 5. 深色主题

1. 菜单栏：`Window` → `Preferences`
2. 选择 `User Interface` → `Appearance`
3. 选择 `Dark` 主题
4. 点击"Apply and Close"

## 常见问题

### 1. 连接失败

**错误：** `Communications link failure`

**解决方法：**
- 确认 MySQL 服务已启动
- 检查端口 3306 是否正确
- 检查防火墙设置
- 尝试使用 `127.0.0.1` 代替 `localhost`

### 2. 驱动下载失败

**解决方法：**
1. 手动下载 MySQL Connector/J：https://dev.mysql.com/downloads/connector/j/
2. 在 DBeaver 中：`Database` → `Driver Manager`
3. 找到 MySQL 驱动，点击"编辑"
4. 在"Libraries"标签页，点击"添加文件"
5. 选择下载的 jar 文件

### 3. 中文乱码

**解决方法：**
1. 确保数据库字符集是 `utf8mb4`
2. 在连接配置中，添加参数：
   - 点击"编辑连接"
   - 切换到"Driver properties"标签
   - 添加：`characterEncoding=utf8`

### 4. 权限不足

**错误：** `Access denied for user`

**解决方法：**
```sql
-- 授予权限
GRANT ALL PRIVILEGES ON ai_gateway.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

## 项目特定操作

### 查看预置模型

```sql
SELECT 
    model_name as '模型名称',
    provider as '提供商',
    input_price as '输入价格',
    output_price as '输出价格',
    status as '状态'
FROM model
ORDER BY provider, model_name;
```

### 查看所有 API Keys

```sql
SELECT 
    key_name as 'Key名称',
    key_value as 'Key值',
    status as '状态',
    used_quota as '已用配额',
    total_quota as '总配额',
    create_time as '创建时间'
FROM api_key
WHERE deleted = 0
ORDER BY create_time DESC;
```

### 查看待审批的申请

```sql
SELECT 
    key_name as 'Key名称',
    user_id as '用户ID',
    reason as '申请理由',
    approval_status as '审批状态',
    create_time as '申请时间'
FROM key_application
WHERE approval_status = 0
ORDER BY create_time DESC;
```

## 更多资源

- DBeaver 官方文档：https://dbeaver.io/docs/
- DBeaver GitHub：https://github.com/dbeaver/dbeaver
- MySQL 文档：https://dev.mysql.com/doc/

---

使用 DBeaver 可以让数据库管理变得更加直观和高效！
