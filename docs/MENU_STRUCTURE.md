# 菜单结构和功能说明

根据用户提供的菜单图片，完整的菜单结构如下：

## 已实现的菜单

| 菜单名称 | 路由 | 功能说明 | 状态 |
|---------|------|---------|------|
| 仪表盘 (Dashboard) | `/` | 统计数据展示 | ✅ 已实现 |
| 令牌管理 (ApiKeys) | `/keys` | API Key 管理 | ✅ 已实现 |
| 模型管理 (Models) | `/models` | 模型配置管理 | ✅ 已实现 |
| 日志 (Logs) | `/logs` | 调用日志查询 | ✅ 已实现 |
| 聊天 (Chat) | `/chat` | 在线测试对话 | ✅ 已实现 |

## 需要补充的菜单

| 菜单名称 | 路由 | 功能说明 | 优先级 |
|---------|------|---------|--------|
| 渠道管理 (Channel) | `/channel` | 管理 AI 模型提供商渠道 | 高 |
| 用户管理 (User) | `/user` | 管理系统用户 | 高 |
| 充值 (TopUp) | `/topup` | 用户充值配额 | 中 |
| 模型定价 (Pricing) | `/pricing` | 配置模型价格 | 中 |
| 兑换码 (Redemption) | `/redemption` | 兑换码管理 | 中 |
| 系统设置 (Setting) | `/setting` | 系统配置 | 中 |
| 模型部署 (ModelDeployment) | `/deployment` | 模型部署管理 | 低 |
| 任务日志 (Task) | `/task` | 异步任务日志 | 低 |
| Playground | `/playground` | 模型测试平台 | 低 |

## 字段映射关系

### Usage 日志字段（已完整实现）

| 字段 | 数据库字段 | 前端展示 |
|------|-----------|---------|
| 调用时间 | `timestamp` | ✅ |
| 租户/应用/用户 | `tenantId`, `appId`, `userId` | ✅ |
| 模型名称 | `model` | ✅ |
| input tokens | `promptTokens` | ✅ |
| output tokens | `completionTokens` | ✅ |
| 调用状态 | `status` | ✅ |
| 延迟 (ms) | `latencyMs` | ✅ |
| 请求 ID | `requestId` | ✅ |

所有必需字段已在 `UsageLog` 实体和前端 Logs 页面中实现。
