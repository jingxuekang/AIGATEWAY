# AI Model Gateway — Q1 规划与标准文档

> 基于项目真实代码生成 | 2026-03-13

---

## 目录

1. [事项 1：现状梳理——模型调用现状](#事项-1现状梳理模型调用现状)
2. [事项 2：网关选型——统一入口方案](#事项-2网关选型统一入口方案)
3. [事项 3：Usage 日志字段规范 v1](#事项-3usage-日志字段规范-v1)
4. [事项 4：Enterprise Model Onboarding Standard v1](#事项-4enterprise-model-onboarding-standard-v1)
5. [关于"单独作业面"的决策分析](#关于单独作业面的决策分析)

---

## 事项 1：现状梳理——模型调用现状

### 1.1 当前调用方式（基于代码实现）

本项目已建立统一网关层，所有模型调用**不再直接打厂商 API**，而是统一经过 gateway-core（端口 9080）。

```
业务方应用
    │
    │  POST /v1/chat/completions
    │  Authorization: Bearer sk-{网关虚拟Key}
    │
    ▼
gateway-core（统一入口）
    │
    ├─ AuthInterceptor 验证虚拟 Key
    ├─ ChatService 按 model 名路由
    │
    ├─ VolcanoProvider  ──→  火山方舟 API（真实 Key 在网关配置）
    ├─ OpenAIProvider   ──→  OpenAI API（待配置真实 Key）
    └─ AnthropicProvider ──→  Anthropic API（待配置真实 Key）
```

**关键设计**：供应商真实 API Key 只存在于 gateway-core 的配置文件（`application-providers.yml`），业务方永远看不到，完全隔离。

### 1.2 模型供应商现状清单

| 供应商 | 模型名称（supports 规则） | 接入状态 | 调用协议 | Key 归属 | 流式支持 |
|--------|--------------------------|---------|---------|---------|----------|
| **火山方舟**（字节） | `doubao-*` / `ep-*` / 含 `volcano` | 已接入，真实调用 | OpenAI 兼容格式（`/api/v3/chat/completions`） | 网关统一持有（`provider.volcano.api-key`） | 已支持 SSE |
| **OpenAI** | `gpt-*` / `o1-*` / `o3-*` | 代码已写，配置 Key 后生效 | OpenAI 原生格式 | 网关统一持有（`provider.openai.api-key`） | 待验证 |
| **Anthropic** | `claude-*` | 代码已写，配置 Key 后生效 | Anthropic 原生格式 | 网关统一持有（`provider.anthropic.api-key`） | 待验证 |
| **其他模型** | 不在上述规则内 | 未接入，抛 BusinessException | — | — | — |

### 1.3 API Key 管理现状

| 维度 | 现状 |
|------|------|
| 供应商 Key | 统一存放在 gateway-core 配置文件，不暴露给业务方 |
| 业务方 Key | 格式 `sk-{uuid}`，存放在 MySQL `api_key` 表，由 Admin 管理 |
| Key 创建方式 | 管理员直接创建，或业务方提交申请经审批自动生成 |
| Key 权限控制 | `allowed_models` 字段，逗号分隔，NULL=允许所有模型 |
| Key 配额控制 | `total_quota` / `used_quota` 字段（token 维度），已设计，扣减逻辑待实现 |
| Key 状态管理 | status=1 启用，status=0 禁用，支持吊销 |

### 1.4 需要与婵娟确认的问题（待填充）

| 问题 | 当前状态 | 需确认 |
|------|---------|--------|
| 存量业务方是否已直连厂商 API | 未知 | 是否需要迁移，迁移时间表 |
| 火山方舟除 doubao 外是否还有其他 endpoint | 未知 | 补充 supports() 规则 |
| 是否有自研/私有化部署模型 | 未知 | 是否需要新增 Provider 适配 |
| 小模型（embedding/rerank 等）是否需要接入网关 | 待定 | 业务需求驱动 |

---

## 事项 2：网关选型——统一入口方案

### 2.1 候选方案对比

| 维度 | new-api | Kong / APISIX | **本项目自研网关**（现状） |
|------|---------|--------------|---------------------------|
| 多格式兼容 | 支持（OpenAI/Anthropic/Azure） | 需 Lua 插件，复杂 | **已支持**（Provider 适配器模式） |
| SSE 流式 | 支持 | 支持，但配置复杂 | **已支持**（WebFlux + Flux） |
| API Key 管理 | 内置，有 Web UI | 需插件 | **已实现**（MySQL + 审批流） |
| 多租户隔离 | 支持 | 支持 | **已实现**（tenant_id/app_id） |
| Usage 日志 | 内置，可导出 | 需插件 | **已实现**（usage_log 表） |
| 模型路由 | 支持 | 支持 | **已实现**（ChatService 路由） |
| 定制审批流 | 不支持 | 不支持 | **已实现**（KeyApplication 审批） |
| 定制计费逻辑 | 部分 | 不支持 | **可自研扩展** |
| 部署复杂度 | 低（Docker） | 中（Gateway + DB） | 低（Spring Boot JAR） |
| 学习成本 | 低 | 高 | 低（团队熟悉 Spring） |

### 2.2 选型结论

**结论：继续使用并完善本项目自研网关，不引入 new-api / Kong / APISIX。**

**理由：**

1. **核心功能已实现**：SSE 流式、API Key 管理、多租户、UsageLog、审批流，自研网关都已覆盖，引入第三方工具反而是重复建设。

2. **深度定制需求多**：企业内部的审批流程、模型订阅规则、费用分摊逻辑都有公司特定规则，第三方工具无法满足，最终仍需自研。

3. **技术栈统一**：Spring WebFlux + MyBatis-Plus + MySQL，团队已熟悉，无额外学习成本。

4. **new-api 的局限**：new-api 是面向 C 端多用户场景设计的，缺乏企业审批流、部门级配额管理等 B 端能力。

### 2.3 自研网关接入方案

```
新业务方接入流程：

① 业务方在平台订阅所需模型（model_subscription 表）
        ↓
② 业务方提交 API Key 申请（key_application 表，指定 allowedModels）
        ↓
③ 管理员审批（校验是否已订阅对应模型）
        ↓
④ 审批通过，系统自动生成 sk-{uuid} 格式 Key（api_key 表）
        ↓
⑤ 业务方使用 Key 调用：
   POST http://gateway:9080/v1/chat/completions
   Authorization: Bearer sk-xxxxxxxx
   {"model": "doubao-pro-32k", "messages": [...]}
        ↓
⑥ 网关验证 Key → 路由到 VolcanoProvider → 调用火山方舟 → 返回结果
⑦ 同时记录 usage_log（token/延迟/租户信息）
```

### 2.4 待补充的网关能力（P1）

| 能力 | 现状 | 补充方案 |
|------|------|----------|
| 配额实时扣减 | 字段已有，未实现扣减 | `ApiKeyService.incrementUsedQuota()` + 调用后异步更新 |
| 限流 | 未实现 | 可用 Bucket4j 在 AuthInterceptor 中加令牌桶限流 |
| 熔断/降级 | 未实现 | Resilience4j 包装 Provider 调用 |
| 多 Key 轮转 | 未实现 | 同一供应商配置多个 Key，Provider 层轮转选取 |

---

## 事项 3：Usage 日志字段规范 v1

### 3.1 字段规范

以下字段已在代码中实现（`UsageLogDTO` + `UsageLogRecord` + `usage_log` 表）：

| 字段名 | 类型 | 说明 | 来源 | 实现状态 |
|--------|------|------|------|----------|
| `timestamp` | DATETIME | 请求发起时间戳 | `LocalDateTime.now()` | 已实现 |
| `trace_id` | VARCHAR(64) | 全链路追踪 ID，跨服务唯一 | `TraceIdUtil.getTraceId()` | 已实现 |
| `request_id` | VARCHAR(128) | 单次请求 UUID | `UUID.randomUUID()` | 已实现 |
| `tenant_id` | VARCHAR(255) | 租户标识 | `ApiKeyContext.get().getTenantId()` | 已实现 |
| `app_id` | VARCHAR(255) | 应用标识 | `ApiKeyContext.get().getAppId()` | 已实现 |
| `user_id` | VARCHAR(255) | 用户标识 | `ApiKeyContext.get().getUserId()` | 已实现 |
| `model` | VARCHAR(255) | 调用的模型名称 | `ChatRequest.getModel()` | 已实现 |

| provider | VARCHAR(255) | 路由到的供应商（volcano/openai/anthropic） | ModelProvider.getProviderName() | 已实现 |
| prompt_tokens | INT | Input Token 数 | ChatResponse.Usage.promptTokens | 已实现 |
| completion_tokens | INT | Output Token 数 | ChatResponse.Usage.completionTokens | 已实现 |
| 	otal_tokens | INT | 总 Token 数 | ChatResponse.Usage.totalTokens | 已实现 |
| cache_creation_tokens | INT | 缓存写入 Token（Anthropic Prompt Caching） | ChatResponse.Usage.cacheCreationTokens | 字段已有 |
| cache_read_tokens | INT | 缓存命中 Token（Anthropic Prompt Caching） | ChatResponse.Usage.cacheReadTokens | 字段已有 |
| status | VARCHAR(50) | 调用状态：success / error | ChatService 异常捕获 | 已实现 |
| latency_ms | BIGINT | 端到端延迟（ms） | System.currentTimeMillis() - startTime | 已实现 |
| 	tft_ms | BIGINT | 首 Token 延迟（Time To First Token） | 待实现 | 字段已有，逻辑待补 |
| 	pot_ms | BIGINT | 每 Token 平均时间（Time Per Output Token） | 待实现 | 字段已有，逻辑待补 |
| error_code | VARCHAR(64) | 错误码 | BusinessException.getCode() | 已实现 |
| error_message | TEXT | 错误详情 | Exception.getMessage() | 已实现 |


### 3.2 关于 Prometheus + Grafana / OpenTelemetry 的分析

| 方案 | 适用场景 | 优点 | 缺点 | Q1 建议 |
|------|---------|------|------|---------|
| MySQL（现状） | 业务统计、审计、费用分摊 | 事务保证、SQL 灵活、与 Admin 集成 | 不擅长时序聚合 | 保留，已满足需求 |
| Prometheus + Grafana | 运维监控、实时指标看板 | 时序专业、告警完善 | 不存原始日志 | Q1 末接入，Spring Boot Actuator 成本低 |
| OpenTelemetry | 分布式追踪、全链路观测 | 标准化协议 | 改造成本较高 | Q2 规划，traceId 已预留 |
| Elasticsearch | 海量日志检索分析 | 聚合 Pipeline 强 | 运维成本高 | pom 已引入，按需启用 |

分层建议：

```
层次      技术               存储内容                用途
业务层    MySQL              usage_log 完整记录      按租户/Key/模型查询，费用分摊
监控层    Prometheus         QPS、延迟、错误率        运维看板，SLA 告警
追踪层    TraceId（现有）    跨服务 traceId          单次请求追踪
```

### 3.3 TTFT / TPOT 实现方案（P1）

在 VolcanoProvider.chatStream() 中补充首 Token 计时：

在流式方法中增加 long startTime 和 long[] ttft = {0}，
每收到第一个非 DONE 的 chunk 时记录 ttft[0] = now - startTime，
在 doOnComplete 中记录总时长，并可计算 TPOT = totalTime / completionTokens。

---

## 事项 4：Enterprise Model Onboarding Standard v1

### 4.1 概述

本规范定义新模型接入 AI Model Gateway 平台的标准流程、技术要求和验收标准。
适用范围：所有需要通过本平台统一调用的 AI 大语言模型。

### 4.2 接入方式（技术协议）

**协议 A：OpenAI 兼容格式（推荐）**

请求示例：
```json
{
  "model": "模型名称",
  "messages": [{"role": "user", "content": "你好"}],
  "stream": false
}
```

响应必须包含 usage 字段：
```json
{
  "choices": [{"message": {"role": "assistant", "content": "你好！"}, "finish_reason": "stop"}],
  "usage": {"prompt_tokens": 20, "completion_tokens": 10, "total_tokens": 30}
}
```

流式响应（stream: true）：
```
data: {"choices":[{"delta":{"content":"你好"},"index":0}]}
data: {"choices":[{"delta":{},"finish_reason":"stop","index":0}]}
data: [DONE]
```

**协议 B：自有格式 + 网关适配器**

在 gateway-provider 模块新建 XxxProvider implements ModelProvider，
加 @Component 注解，Spring 自动发现。
参考 VolcanoProvider 实现格式转换逻辑，配置 provider.xxx.api-key 即可。

### 4.3 API Key 申请与分发流程

```
步骤 1  业务方订阅模型  →  model_subscription 表写入 status=1
步骤 2  提交 Key 申请  →  key_application 表写入 approval_status=0
步骤 3  管理员审批      →  校验订阅，通过后自动生成 sk-{uuid}
步骤 4  业务方获取 Key  →  在 API Keys 页面查看
步骤 5  调用验证：
  POST http://gateway:9080/v1/chat/completions
  Authorization: Bearer sk-xxxxxxxx
  {"model":"doubao-pro-32k","messages":[{"role":"user","content":"test"}]}
```

### 4.4 必须满足的技术要求

| 要求 | 验收标准 |
|------|----------|
| 兼容 OpenAI 格式或提供适配器 | chat() 能返回标准 ChatResponse |
| 必须返回 token 用量 | usage_log 中 prompt_tokens 和 completion_tokens 非 null |
| 支持流式输出 SSE | chatStream() 返回 Flux，客户端实时收到 chunk |
| 支持 HTTPS | WebClient 配置 https:// |
| 响应时间 | P99 延迟小于 60 秒 |
| 错误处理 | 失败请求 usage_log.status=error，error_code 有值 |

### 4.5 测试验收清单

```
[ ] 1. Provider 代码实现并通过单元测试
[ ] 2. 配置 provider.xxx.api-key 和 provider.xxx.api-url
[ ] 3. model 表录入模型信息（modelName/provider/inputPrice/outputPrice/maxTokens）
[ ] 4. 连通性测试：发送 hello，HTTP 200，choices 非空
[ ] 5. Token 计量测试：usage.prompt_tokens 和 completion_tokens 有值
[ ] 6. 流式输出测试：收到多个 SSE chunk，最后一条为 [DONE]
[ ] 7. UsageLog 写入验证：usage_log 表有对应记录
[ ] 8. 错误处理测试：非法参数返回标准错误，status=error
[ ] 9. 管理员在平台发布模型（status=1）
[ ] 10. 通知业务方可订阅并申请 Key
```

---

## 关于"单独作业面"的决策分析

### 问题背景

是否需要单独一个作业面（独立系统/页面），还是在现有 AI Model Gateway 管理台上扩展？

### 现有项目结构

```
AIGATEWAY（现有项目）
├── gateway-core    Spring WebFlux，port 9080，对外统一调用入口
├── gateway-admin   Spring MVC + MyBatis-Plus + MySQL，port 9081，管理后端
└── frontend        React 18 + TypeScript + Ant Design 5，port 5173，管理台
```

现有管理台已具备：角色差异化菜单、模型管理、API Key 申请审批、模型订阅、调用日志查询。

### 三种方案对比

| 方案 | 说明 | 优点 | 缺点 | 推荐 |
|------|------|------|------|------|
| A：在现有管理台扩展 | gateway-admin + frontend 上新增页面和接口 | 技术栈统一，已有权限/日志/Key 体系复用 | 功能多时需做好模块划分 | 强烈推荐 |
| B：单独新建前后端 | 新建独立的模型接入管理系统 | 职责分离 | 重复建设权限/日志/用户体系 | 不推荐 |
| C：接入现有模型空间 | 在公司已有产品上修改 | 与现有产品融合 | 改造成本未知，技术栈可能不同 | 视情况而定 |

### 结论：推荐方案 A

在现有 AI Model Gateway 管理台上扩展，理由：

1. 技术栈已统一（Spring Boot + React + MySQL），无额外学习成本。
2. 核心能力已有（Key 审批、模型订阅、UsageLog、角色权限），继续扩展即可。
3. 避免重复建设，单独建系统需重新做用户/权限/日志体系，工作量 3 倍以上。
4. Onboarding Standard 的所有流程在现有管理台均有对应页面。

### 需要补充的功能（Q1）

| 功能 | 对应页面 | 后端接口 | 状态 |
|------|---------|---------|------|
| 模型发布（管理员） | Models 页面 | POST /api/admin/models | 已完成 |
| 模型订阅（业务方） | ModelSubscriptions 页面 | POST /api/admin/model-subscriptions/subscribe | 已完成 |
| Key 申请表单 | KeyApplication 页面 | POST /api/admin/key-applications | 已完成 |
| Key 审批列表（管理员） | KeyApplications 页面 | GET /api/admin/key-applications | 接真实 Service（P1）|
| 调用日志列表 | Logs 页面 | GET /api/admin/logs | 已完成 |
| 接口文档 | Docs 页面 | iframe 内嵌 Swagger UI | 已完成 |
| 登录认证 | 登录页 | POST /api/admin/auth/login（JWT） | 待实现（P1）|
