# Grafana 安装权限错误修复

## 错误信息
```
Error writing to file: D:\soft\grafana\wix3\mspatchc.dll
Verify that you have access to that directory.
```

## 原因
安装程序没有足够的权限写入目标目录。

## 解决方案

### 方法 1: 以管理员身份运行安装程序（推荐）

1. 找到 Grafana 安装程序（.exe 或 .msi 文件）
2. 右键点击安装程序
3. 选择 "以管理员身份运行"
4. 在 UAC 提示中点击 "是"
5. 重新安装

### 方法 2: 更改安装目录

如果方法 1 不行，尝试安装到其他目录：

1. 取消当前安装
2. 重新运行安装程序
3. 选择自定义安装路径，例如：
   - `C:\Grafana`
   - `D:\Grafana`（不要放在 soft 文件夹下）
4. 确保该目录没有被其他程序占用

### 方法 3: 检查目录权限

1. 打开文件资源管理器
2. 导航到 `D:\soft\grafana`
3. 右键点击文件夹 → 属性
4. 切换到 "安全" 选项卡
5. 点击 "编辑"
6. 选择你的用户账户
7. 勾选 "完全控制"
8. 点击 "应用" 和 "确定"
9. 重新运行安装程序

### 方法 4: 关闭杀毒软件

某些杀毒软件可能会阻止安装：

1. 临时关闭杀毒软件（Windows Defender 或其他）
2. 重新运行安装程序
3. 安装完成后重新启用杀毒软件

### 方法 5: 使用 Docker 安装（推荐用于开发）

如果安装程序一直有问题，可以使用 Docker：

```bash
# 拉取 Grafana 镜像
docker pull grafana/grafana:latest

# 运行 Grafana
docker run -d -p 3000:3000 --name=grafana grafana/grafana:latest

# 访问 Grafana
# 浏览器打开: http://localhost:3000
# 默认用户名/密码: admin/admin
```

## 关于 Grafana 版本

### Grafana Enterprise vs Grafana OSS

你下载的是 Grafana Enterprise（企业版），这个版本：
- ✅ 功能更多（但需要许可证）
- ✅ 有高级功能（报表、RBAC 等）
- ❌ 需要注册和激活

### 推荐下载 Grafana OSS（开源版）

对于开发和测试，建议使用开源版：

1. 访问官网下载页面：
   https://grafana.com/grafana/download

2. 选择 "OSS" 版本（不是 Enterprise）

3. 选择 Windows 安装包：
   - Standalone Windows Binaries (推荐)
   - Windows Installer (.msi)

4. 下载并安装

## 本项目是否需要 Grafana？

根据你的项目配置，Grafana 是可选的：

### 当前项目监控配置

```yaml
# gateway-core/src/main/resources/application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    prometheus:
      enabled: true
```

### 监控方案

1. **基础监控**（无需 Grafana）
   - 访问 http://localhost:9080/actuator/health
   - 访问 http://localhost:9080/actuator/metrics
   - 访问 http://localhost:9080/actuator/prometheus

2. **完整监控**（需要 Prometheus + Grafana）
   - Prometheus 收集指标
   - Grafana 可视化展示

### 如果不需要 Grafana

你可以暂时跳过 Grafana 安装，项目依然可以正常运行：

```bash
# 只需要启动这些服务
1. MySQL (端口 3306)
2. Gateway Admin (端口 9082)
3. Gateway Core (端口 9080)
4. Frontend (端口 3001)
```

### 如果需要 Grafana

建议使用 Docker 方式安装，更简单：

```bash
# docker-compose.yml 中添加
services:
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-data:/var/lib/grafana

volumes:
  grafana-data:
```

## 总结

1. **立即解决**：以管理员身份运行安装程序
2. **推荐方案**：下载 Grafana OSS 版本
3. **最简单**：使用 Docker 安装
4. **可选**：如果不需要可视化监控，可以暂时不安装 Grafana

## 下一步

安装完成后，可以配置 Grafana 连接到 Prometheus：
1. 访问 http://localhost:3000
2. 登录（admin/admin）
3. 添加数据源 → Prometheus
4. URL: http://localhost:9090
5. 导入 Spring Boot 监控面板
