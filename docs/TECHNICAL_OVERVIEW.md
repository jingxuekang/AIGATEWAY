# AI Model Gateway — 技术全景文档

> 基于真实代码生成 | 2026-03-13

---

## 目录

1. [项目定位](#1-项目定位)
2. [架构总览](#2-架构总览)
3. [模块职责](#3-模块职责)
4. [为什么拆两个后端服务](#4-为什么拆两个后端服务)
5. [为什么不用 Spring AI](#5-为什么不用-spring-ai)
6. [数据模型设计](#6-数据模型设计)
7. [核心调用链路](#7-核心调用链路)
8. [核心功能原理](#8-核心功能原理)
9. [Q1 目标完成情况](#9-q1-目标完成情况)

---

## 1. 项目定位

AI Model Gateway 是企业级 AI 模型统一接入平台，核心价值：

| 痛点 | 没有网关 | 有了网关 |
|------|---------|----------|
| 多厂商 API 格式各异 | 各业务方重复对接 | 统一 OpenAI 兼容接口，一次接入 |
| 供应商真实 Key 泄露风险 | Key 散落在各业务代码 | Key 只在网关，业务方持有虚拟 Key |
| 无法统计各方消耗 | 费用无法分摊 | 每次调用自动记录 tenant/app/token/延迟 |
| 无法控制谁能用哪些模型 | 全开或全关 | Key 级别绑定允许模型列表 |
| 无业务方接入流程 | 直接给 Key | 申请→审批→自动生成 Key 的完整闭环 |

---

## 2. 架构总览

```
┌──────────────────────────────────────────────────────┐
│                    业务方应用                        │
│  POST /v1/chat/completions                           │
│  Authorization: Bearer sk-xxxxxxxx                   │
└───────────────────────┬──────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────┐
│          gateway-core  (port 9080)                   │
│  Spring WebFlux · 非阻塞 I/O · SSE 流式              │
│                                                      │
│  AuthInterceptor                                     │
│    ├─ 提取 API Key                                   │
│    ├─ HTTP → Admin 验证 Key 身份                     │
│    └─ ThreadLocal 写入 traceId / tenantId            │
│                                                      │
│  ChatService                                         │
│    ├─ 按 model 名路由到对应 Provider                 │
│    └─ 调用结束后推送 UsageLog → Admin                │
│                                                      │
│  VolcanoProvider / OpenAIProvider / AnthropicProvider│
│    └─ WebClient 真实调用上游 LLM，转换格式           │
└──────────┬──────────────────────┬────────────────────┘
           │ GET /keys/validate    │ POST /logs
           ▼                      ▼
┌──────────────────────────────────────────────────────┐
│          gateway-admin  (port 9081)                  │
│  Spring MVC · MyBatis-Plus · MySQL 事务              │
│                                                      │
│  ApiKey · Model · KeyApplication                     │
│  UsageLog · Channel · ModelSubscription              │
│  User · TopUp · Dashboard · Setting                  │
└─────────────────────────┬────────────────────────────┘
                          │ REST API
                          ▼
┌──────────────────────────────────────────────────────┐
│          frontend  (port 5173)                       │
│  React 18 · TypeScript · Vite · Ant Design 5         │
│                                                      │
│  admin 视图：渠道·用户·充值·系统·Key审批·任务日志   │
│  user  视图：Key申请·模型订阅·调用日志·Playground   │
└──────────────────────────────────────────────────────┘
```

---

## 3. 模块职责

### 3.1 gateway-common（公共库，无 HTTP 接口）

| 类 | 职责 |
|----|------|
| `Result<T>` | 统一响应体 `{code, message, data}`，所有接口共用 |
| `UsageLogDTO` | 网关→Admin 传输的日志对象，19 个字段 |
| `ApiKeyInfo` | Key 验证后的身份信息（userId/tenantId/appId/allowedModels） |
| `ApiKeyContext` | `ThreadLocal<ApiKeyInfo>`，请求生命周期内存储调用者身份 |
| `TraceIdUtil` | `ThreadLocal<String>`，全链路 traceId，请求结束后 clear |
| `CommonConstants` | `API_KEY_PREFIX = "sk-"`、Key 状态码等全局常量 |
| `BusinessException` | 带 HTTP code 的业务异常 |

### 3.2 gateway-provider（LLM 适配层）

统一接口 `ModelProvider`：

```java
public interface ModelProvider {
    String getProviderName();            // 供应商名称，用于日志
    boolean supports(String model);      // 判断是否能处理此模型
    ChatResponse chat(ChatRequest);      // 同步调用
    Flux<String> chatStream(ChatRequest);// 流式调用
}
```

| Provider | supports() 规则 | 实现状态 |
|----------|----------------|----------|
| `VolcanoProvider` | `doubao-*` / `ep-*` / 含 `volcano` | 真实 WebClient 调用，含完整格式转换 |
| `OpenAIProvider` | `gpt-*` / `o1-*` / `o3-*` | 配置 `provider.openai.api-key` 后生效 |
| `AnthropicProvider` | `claude-*` | 配置 `provider.anthropic.api-key` 后生效 |

扩展新厂商：新建类 `implements ModelProvider` + `@Component`，Spring 自动注入到 `List<ModelProvider>`，路由逻辑零改动。

### 3.3 gateway-core（统一网关，port 9080）

| 类 | 职责 |
|----|------|
| `AuthInterceptor` | 前置拦截：提取 Key → 调 Admin 验证 → 写 ThreadLocal |
| `ChatController` | 暴露 `/v1/chat/completions`（同步）和 `/stream`（SSE） |
| `ChatService` | Provider 路由 + 错误捕获 + 触发日志上报 |
| `UsageLogService` | 从 ThreadLocal 取身份，组装 DTO，推送 Admin |
| `AdminClient` | WebClient 封装：`validateApiKey()` + `sendUsageLog()` |

### 3.4 gateway-admin（管理控制台，port 9081）

| Controller | 路径前缀 | 核心能力 |
|-----------|----------|----------|
| `ApiKeyController` | `/api/admin/keys` | 创建/列表/吊销/验证 Key |
| `LogController` | `/api/admin/logs` | 接收网关日志 / 查询 / 统计 |
| `KeyApplicationController` | `/api/admin/key-applications` | 提交申请 / 审批通过 / 审批拒绝 |
| `ModelController` | `/api/admin/models` | 发布模型 / 更新 / 列表 |
| `ModelSubscriptionController` | `/api/admin/model-subscriptions` | 订阅 / 取消 / 查询 |
| `DashboardController` | `/api/admin/dashboard` | 汇总统计 |
| 其余 Controller | channels / users / topup / tasks / settings | CRUD |

### 3.5 frontend（管理台，port 5173）

从 `localStorage.getItem('role')` 读取角色，渲染不同菜单：

| 菜单项 | admin | user |
|--------|:-----:|:----:|
| 渠道管理 / 用户管理 / 充值 / 系统设置 / 任务日志 | ✅ | — |
| API Key 管理（全量） | ✅ | — |
| API Key 申请 & 我的申请 | ✅ | ✅ |
| 模型管理（发布/定价） | ✅ | ✅ 只读 |
| 模型订阅 | ✅ | ✅ |
| 调用日志（全量 vs 自己的） | ✅ | ✅ |
| 接口文档 / Playground | ✅ | ✅ |

---

## 4. 为什么拆两个后端服务

### 根本矛盾：两种 I/O 模型天然冲突

**gateway-core 必须用 WebFlux**

LLM 流式调用特征：单次请求保持连接 10～60 秒，企业场景数百并发连接同时存在。

```
用 Spring MVC（阻塞线程模型）：
  200 个并发请求 → 200 个线程全部阻塞 30 秒
  → 线程池耗尽 → 第 201 个请求 503
  → 200 × 1MB 线程栈 = 200MB 仅用于等待

用 Spring WebFlux（Netty 事件循环）：
  200 个并发请求 → 只需 32 个线程（16核×2）
  → 线程不阻塞，等待 I/O 期间继续处理其他事件
  → 支持数千并发，内存稳定
```

代码层证明（`ChatController.java`）：

```java
// Flux<String> + TEXT_EVENT_STREAM_VALUE 只能在 WebFlux 中运行
@PostMapping(value = "/chat/completions/stream",
             produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chatCompletionsStream(@RequestBody ChatRequest request) {
    return chatService.chatStream(request); // 不 block，直接返回响应式流
}
```

**gateway-admin 必须用 Spring MVC**

gateway-admin 深度依赖 MyBatis-Plus（阻塞式 ORM）。

```
强行在 WebFlux 里用 MyBatis-Plus：

  .flatMap(result -> {
      // ❌ 在 Netty 事件循环线程里做阻塞 DB 查询
      // 后果：阻塞整个事件循环，所有请求全部卡住
      return Mono.just(apiKeyService.getById(id));
  })

解决方案：换 R2DBC 响应式驱动
但 MyBatis-Plus 不支持 R2DBC
→ 要重写所有 Mapper，成本极高，完全没有必要
```

`KeyApplicationService.approve()` 还需要数据库事务（审批通过 + 创建 Key 原子完成），`@Transactional` 在 Spring MVC + MyBatis-Plus 中天然可用。

**结论**

| 维度 | gateway-core | gateway-admin |
|------|-------------|---------------|
| 框架 | Spring WebFlux | Spring MVC |
| 线程模型 | Netty 事件循环，非阻塞 | Tomcat 线程池，阻塞 |
| 数据库访问 | 无（HTTP 调 Admin） | 直连 MySQL |
| ORM | 无 | MyBatis-Plus |
| 事务支持 | 无需 | `@Transactional` |
| 适合场景 | 长连接、SSE、高并发代理 | 短时 CRUD、审批、配置 |
| 端口 | 9080（对外暴露） | 9081（内网）|

---

## 5. 为什么不用 Spring AI

Spring AI 是**应用集成框架**，定位是「让开发者在自己的应用里快速调用 AI」，不是网关平台。

```
Spring AI 的假设：
  - 只有一个用户（开发者自己）
  - API Key 写死在 application.yml
  - 无多租户、权限、配额概念
  - 开发者自己控制调用量和费用

本项目的需求：
  - 数百个业务方，每人有独立 Key
  - Key 动态创建/审批/吊销
  - 每个 Key 绑定允许的模型和配额
  - 每次调用记录谁用了什么、花了多少
```

| 功能 | Spring AI | 本项目 | Spring AI 缺失原因 |
|------|:---------:|:------:|-------------------|
| 多 LLM 统一调用 | ✅ | ✅ | — |
| SSE 流式输出 | ✅ | ✅ | — |
| API Key 动态管理 | ❌ | ✅ | Key 写死配置文件 |
| 多租户隔离 | ❌ | ✅ | 无多用户概念 |
| 配额管理 | ❌ | ✅ | 无此概念 |
| 模型权限控制 | ❌ | ✅ | 无权限体系 |
| 调用日志持久化 | ❌ | ✅ | 只有 debug log |
| 申请审批流程 | ❌ | ✅ | 无此概念 |

**Spring AI 可以用在哪**：适合网关下游的业务应用。业务团队拿到网关 Key 后，用 Spring AI 开发自己的 AI 功能，把 `base-url` 指向本网关即可：

```yaml
spring:
  ai:
    openai:
      api-key: sk-网关给的Key
      base-url: http://gateway:9080
      model: doubao-pro-32k
```

---

## 6. 数据模型设计

### 6.1 实体关系

```
admin_user
  ├──< api_key            (user_id → id)
  │       └── allowed_models ──────────> model.model_name（逗号分隔）
  │
  ├──< key_application    (user_id → id)
  │       ├── [审批通过] ──自动创建──> api_key
  │       └── [审批前] ──校验──> model_subscription.status = 1
  │
  ├──< model_subscription (user_id → id)
  │       └── model_id ────────────────> model.id
  │
  └──< usage_log          (由 gateway-core 写入，user_id 为字符串)
          └── trace_id ──贯穿──> 全请求链路
```

### 6.2 核心表字段

**api_key**

```sql
id             BIGINT PK AUTO_INCREMENT
key_value      VARCHAR(255) UNIQUE    -- sk-{uuid}，网关鉴权凭证
key_name       VARCHAR(255)           -- 可读名称
user_id        BIGINT                 -- 归属用户
tenant_id      VARCHAR(255)           -- 租户维度，用于日志分组
app_id         VARCHAR(255)           -- 应用维度，用于日志分组
status         INT DEFAULT 1          -- 1=启用 0=禁用
expire_time    DATETIME               -- NULL=永不过期
allowed_models TEXT                   -- 逗号分隔，NULL=允许所有模型
total_quota    BIGINT DEFAULT 0       -- 总 token 配额，0=不限
used_quota     BIGINT DEFAULT 0       -- 已累计使用 token
deleted        INT DEFAULT 0          -- MyBatis-Plus 逻辑删除
```

**key_application**

```sql
id               BIGINT PK
user_id          BIGINT           -- 申请人
tenant_id        VARCHAR(255)
app_id           VARCHAR(255)
key_name         VARCHAR(255)     -- 申请的 Key 名称
allowed_models   TEXT             -- 申请使用的模型
reason           TEXT             -- 申请原因
approval_status  INT DEFAULT 0    -- 0=待审 1=通过 2=拒绝
approver_id      BIGINT           -- 审批人
approval_comment TEXT             -- 审批意见
approval_time    DATETIME
```

**usage_log**

```sql
id                    BIGINT PK
timestamp             DATETIME       -- 调用时间
trace_id              VARCHAR(64)    -- 全链路追踪 ID
request_id            VARCHAR(128)   -- 单次请求 UUID
tenant_id             VARCHAR(255)   -- 来自 ApiKeyContext
app_id                VARCHAR(255)   -- 来自 ApiKeyContext
user_id               VARCHAR(255)   -- 来自 ApiKeyContext
model                 VARCHAR(255)   -- 调用的模型名
provider              VARCHAR(255)   -- 路由到的供应商
prompt_tokens         INT            -- 输入 token 数
completion_tokens     INT            -- 输出 token 数
total_tokens          INT            -- 总 token 数
cache_creation_tokens INT            -- 缓存写入 token（Anthropic）
cache_read_tokens     INT            -- 缓存命中 token（Anthropic）
status                VARCHAR(50)    -- success / error
latency_ms            BIGINT         -- 端到端延迟毫秒
ttft_ms               BIGINT         -- 首 token 延迟（规划中）
tpot_ms               BIGINT         -- 每 token 平均时间（规划中）
error_code            VARCHAR(64)
error_message         TEXT
```

**model**

```sql
id             BIGINT PK
model_name     VARCHAR(255)           -- 如 doubao-pro-32k
provider       VARCHAR(255)           -- volcano / openai / anthropic
status         INT DEFAULT 1          -- 1=可用 0=下线
input_price    DECIMAL(10,6)          -- 每千 token 输入价格（元）
output_price   DECIMAL(10,6)          -- 每千 token 输出价格（元）
max_tokens     INT                    -- 最大上下文长度
support_stream BOOLEAN DEFAULT TRUE
```

**model_subscription**

```sql
id        BIGINT PK
model_id  BIGINT         -- 关联 model.id
user_id   BIGINT         -- 订阅用户
tenant_id VARCHAR(255)
app_id    VARCHAR(255)
status    INT DEFAULT 1  -- 1=已订阅 0=已取消
-- 业务规则：申请 Key 时校验用户是否已订阅申请的模型
```

---

## 7. 核心调用链路

### 7.1 业务方调用模型（主链路）

```
业务方应用
  │
  │  POST /v1/chat/completions
  │  Authorization: Bearer sk-abc123
  │  Body: {"model":"doubao-pro-32k","messages":[...]}
  │
  ▼
─────────────────────── gateway-core ───────────────────────
  │
  ① AuthInterceptor.preHandle()
  │   ├─ extractApiKey(request)
  │   │    └─ header "Authorization" → 去掉 "Bearer " 前缀
  │   │
  │   ├─ TraceIdUtil.setTraceId()
  │   │    └─ 读 X-Trace-Id header，没有则 UUID 生成
  │   │    └─ 写入 ThreadLocal，response header 回传
  │   │
  │   ├─ AdminClient.validateApiKey("sk-abc123")
  │   │    └─ WebClient GET http://admin:9081/api/admin/keys/validate?key=sk-abc123
  │   │         │
  │   │         ▼  ─── gateway-admin ───
  │   │         ApiKeyController.validateKey()
  │   │           └─ ApiKeyService.validateKey()
  │   │                └─ SELECT * FROM api_key
  │   │                     WHERE key_value='sk-abc123'
  │   │                     AND status=1
  │   │                     AND deleted=0
  │   │           └─ 转换为 ApiKeyInfo {userId,tenantId,appId,allowedModels}
  │   │         返回 Result<ApiKeyInfo>
  │   │
  │   └─ ApiKeyContext.set(apiKeyInfo)  → ThreadLocal
  │
  ② ChatController.chatCompletions(request)
  │   └─ 调用 ChatService.chat(request)
  │
  ③ ChatService.chat()
  │   ├─ startTime = System.currentTimeMillis()
  │   ├─ requestId = UUID.randomUUID().toString()
  │   │
  │   ├─ providers.stream()
  │   │    .filter(p -> p.supports("doubao-pro-32k"))
  │   │    .findFirst()
  │   │    → VolcanoProvider（supports: model.startsWith("doubao-")）
  │   │
  │   ├─ VolcanoProvider.chat(request)
  │   │    ├─ convertToVolcanoFormat(request)
  │   │    │    └─ {model, messages, temperature, max_tokens, top_p}
  │   │    ├─ WebClient POST https://ark.cn-beijing.volces.com/api/v3/chat/completions
  │   │    │    Header: Authorization: Bearer {volcanoApiKey（配置文件中）}
  │   │    ├─ 接收火山方舟响应
  │   │    └─ convertFromVolcanoFormat(response)
  │   │         └─ 转换为统一 ChatResponse {id,model,choices,usage}
  │   │
  │   ├─ latency = now - startTime
  │   └─ UsageLogService.logUsage(request, response, latency, "volcano", "success", requestId, null, null)
  │
  ④ UsageLogService.logUsage()
  │   ├─ apiKeyInfo = ApiKeyContext.get()  ← 从 ThreadLocal 取
  │   ├─ traceId   = TraceIdUtil.getTraceId()
  │   ├─ 组装 UsageLogDTO
  │   │    {timestamp, traceId, requestId, tenantId, appId, userId,
  │   │     model, provider, promptTokens, completionTokens,
  │   │     totalTokens, status, latencyMs, errorCode, errorMessage}
  │   ├─ log.info("Usage log: {}", dto)   ← 本地日志
  │   └─ AdminClient.sendUsageLog(dto)
  │        └─ WebClient POST http://admin:9081/api/admin/logs
  │             │
  │             ▼  ─── gateway-admin ───
  │             LogController.receiveUsageLog(dto)
  │               └─ UsageLogRecord record = map(dto)
  │               └─ usageLogRecordService.save(record)
  │                    └─ INSERT INTO usage_log (...)
  │
  ⑤ AuthInterceptor.afterCompletion()
  │   ├─ TraceIdUtil.clearTraceId()   ← 清 ThreadLocal 防内存泄漏
  │   └─ ApiKeyContext.clear()
  │
  ▼
业务方收到响应
  {"id":"...","choices":[{"message":{"role":"assistant","content":"..."}}}],
   "usage":{"prompt_tokens":100,"completion_tokens":200,"total_tokens":300}}
```

### 7.2 流式调用链路（SSE）

```
业务方
  │  POST /v1/chat/completions/stream
  │
  ▼
ChatController.chatCompletionsStream()
  │  返回类型：Flux<String>
  │  Content-Type: text/event-stream
  │  （连接保持打开，逐 chunk 推送）
  │
  ▼
ChatService.chatStream()
  │  findProvider("doubao-pro-32k") → VolcanoProvider
  │
  ▼
VolcanoProvider.chatStream()
  │  volcanoRequest.put("stream", true)
  │  WebClient POST → 火山方舟
  │  .retrieve()
  │  .bodyToFlux(String.class)          ← 逐行读取 SSE
  │  .map(convertVolcanoStreamToOpenAI) ← 格式转换
  │
  ▼ 逐 chunk 推送给业务方（无需等全量响应）
data: {"id":"x","choices":[{"delta":{"content":"你好"}}]}
data: {"id":"x","choices":[{"delta":{"content":"，我是"}}]}
data: {"id":"x","choices":[{"delta":{},"finish_reason":"stop"}]}
data: [DONE]
```

### 7.3 Key 申请与审批链路

```
普通用户（前端）
  │  POST /api/admin/key-applications
  │  Body: {userId, keyName, allowedModels, reason, tenantId, appId}
  │
  ▼
KeyApplicationController.createApplication()
  └─ KeyApplicationService.submitApplication()
       └─ application.setApprovalStatus(0)  ← 待审
       └─ INSERT INTO key_application

管理员（前端）
  │  POST /api/admin/key-applications/{id}/approve
  │  Body: {approverId, comment}
  │
  ▼
KeyApplicationService.approve(id, approverId, comment)
  │
  ├─ getById(id)  → 查询申请记录
  ├─ 校验 approvalStatus == 0（防止重复审批）
  │
  ├─ 若 allowedModels 非空，校验用户订阅：
  │    modelSubscriptionService.listUserSubscriptions(userId, tenantId, appId)
  │      .anyMatch(sub -> sub.getStatus() == 1)
  │    未订阅任何模型 → 返回 null（不允许通过）
  │
  ├─ application.setApprovalStatus(1)   ← 已通过
  ├─ application.setApprovalTime(now)
  ├─ updateById(application)
  │
  └─ apiKeyService.createKey(newApiKey)
       ├─ keyValue = "sk-" + IdUtil.fastSimpleUUID()
       ├─ status   = 1（启用）
       ├─ 复制 userId/tenantId/appId/allowedModels
       └─ INSERT INTO api_key

结果：用户在"我的申请"页看到状态变为"已通过"，
      在"API Keys"页看到新生成的 Key
```

### 7.4 管理员操作链路（前端 → Admin）

```
管理员浏览器（port 5173）
  │
  ├─ localStorage.role = 'admin' → 渲染完整菜单
  │
  ├─ Dashboard
  │    GET /api/admin/dashboard/statistics
  │    → DashboardService: COUNT(api_key) + COUNT(model)
  │
  ├─ 模型管理
  │    GET  /api/admin/models          → 列表
  │    POST /api/admin/models          → 发布新模型
  │    PUT  /api/admin/models/{id}     → 更新
  │
  ├─ 调用日志
  │    GET /api/admin/logs?model=&status=&startTime=&endTime=
  │    → LogController → LambdaQueryWrapper 条件查询 usage_log
  │    → 点击行 → Drawer 展示全部字段（traceId/requestId/tokens...）
  │
  └─ Key 审批
       GET  /api/admin/key-applications          → 全部申请列表
       POST /api/admin/key-applications/{id}/approve → 审批通过
       POST /api/admin/key-applications/{id}/reject  → 审批拒绝
```

---

## 8. 核心功能原理

### 8.1 API Key 鉴权原理

```
网关收到请求
    │
    ├─ Header: "Authorization: Bearer sk-xxx"
    │    提取: auth.substring(7) = "sk-xxx"
    │
    ├─ 无 Key → throw BusinessException(401, "Missing API Key")
    │
    ├─ AdminClient.validateApiKey("sk-xxx")
    │    └─ HTTP GET → Admin /api/admin/keys/validate?key=sk-xxx
    │         SELECT WHERE key_value=? AND status=1 AND deleted=0
    │         返回 ApiKeyInfo（含 allowedModels 列表）
    │
    ├─ 返回 null → throw BusinessException(401, "Invalid API Key")
    │
    └─ ApiKeyContext.set(apiKeyInfo)
         后续 UsageLogService 从此处取 tenantId/appId/userId
```

**关键设计**：
- 供应商真实 API Key（volcanoApiKey）只存在于 gateway-core 的配置文件，业务方永远看不到
- 业务方持有的是网关虚拟 Key（`sk-uuid`），存在 Admin 的 MySQL 里
- 两层 Key 完全隔离，供应商 Key 可以随时轮换而不影响业务方

### 8.2 SSE 流式代理原理

```
传统 HTTP（同步）：
  客户端 ──请求──▶ 网关 ──请求──▶ LLM
                           ◀── 等待全部生成完 ──
         ◀── 一次性返回全部内容 ──
  用户等待 10 秒看到完整回复

SSE 流式：
  客户端 ──请求──▶ 网关 ──请求──▶ LLM
                  ◀── chunk1 ──
  ◀── chunk1 ──                ◀── chunk2 ──
  ◀── chunk2 ──                ◀── chunk3 ──
                  ◀── chunk3 ──
  用户边等边看，首字出现时间从 10 秒缩短到 0.5 秒
```

实现原理（`VolcanoProvider.chatStream()`）：

```java
return webClient
    .post()
    .uri(volcanoApiUrl + "/chat/completions")
    .header("Authorization", "Bearer " + volcanoApiKey)
    .bodyValue(volcanoRequest)         // stream: true
    .retrieve()
    .bodyToFlux(String.class)          // 不等全量，逐行读取
    .map(this::convertVolcanoStreamToOpenAI); // 格式转换
                                       // 每个 chunk 立即推给客户端
```

Spring WebFlux 的 `bodyToFlux` 会在收到每一行数据后立即触发 `map`，无需等全部数据。
Controller 声明 `produces = TEXT_EVENT_STREAM_VALUE`，框架自动维持连接，逐条写入响应流。

### 8.3 Provider 路由原理

```java
// ChatService 中，providers 是 Spring 自动注入的所有 ModelProvider 实现类的列表
private final List<ModelProvider> providers;

private ModelProvider findProvider(String model) {
    return providers.stream()
        .filter(p -> p.supports(model))
        .findFirst()
        .orElseThrow(() -> new BusinessException("不支持的模型: " + model));
}
```

路由规则（各 Provider 的 `supports()` 实现）：

| 请求 model | 匹配 Provider | 规则 |
|-----------|--------------|------|
| `doubao-pro-32k` | VolcanoProvider | `startsWith("doubao-")` |
| `ep-20250101-xxx` | VolcanoProvider | `startsWith("ep-")` |
| `gpt-4o` | OpenAIProvider | `startsWith("gpt-")` |
| `claude-3-5-sonnet` | AnthropicProvider | `startsWith("claude-")` |
| `unknown-model` | — | 抛 BusinessException |

### 8.4 TraceId 全链路追踪原理

```
请求进入 AuthInterceptor
  ├─ 读 X-Trace-Id header
  │    有 → 使用客户端传来的 ID（支持跨服务追踪）
  │    无 → UUID.randomUUID() 生成新 ID
  ├─ TraceIdUtil.setTraceId(traceId)   写入 ThreadLocal
  └─ response.setHeader(X-Trace-Id, traceId)  回传给客户端

请求处理中（同一线程）
  ChatService → UsageLogService
    └─ TraceIdUtil.getTraceId() 随时可取，写入 UsageLogDTO

日志格式配置（application.yml）：
  pattern: "%d{yyyy-MM-dd HH:mm:ss} [%X{traceId}] %-5level - %msg"
  → 所有 log.info/debug 自动带上 traceId

请求结束 afterCompletion()
  └─ TraceIdUtil.clearTraceId()  清除 ThreadLocal，防止线程池复用时污染
```


### 8.5 MyBatis-Plus 关键特性

```java
// 1. 逻辑删除：@TableLogic
// 所有 SELECT 自动加 WHERE deleted=0
// DELETE 自动变 UPDATE SET deleted=1
@TableLogic
private Integer deleted;

// 2. LambdaQueryWrapper 类型安全查询
new LambdaQueryWrapper<UsageLogRecord>()
    .ge(UsageLogRecord::getTimestamp, startTime)
    .eq(UsageLogRecord::getModel, model)
    .eq(UsageLogRecord::getStatus, status)
    .orderByDesc(UsageLogRecord::getTimestamp);

// 3. ServiceImpl 基类提供通用方法，Service 只需继承
public class ApiKeyService extends ServiceImpl<ApiKeyMapper, ApiKey> {
    // 直接用 save() / list() / getById() / updateById()
}
```

---

## 9. Q1 目标完成情况

### 9.1 后端完成情况

| 功能模块 | 状态 | 说明 |
|---------|:----:|------|
| 统一调用入口 /v1/chat/completions | 完成 | 同步 + SSE 流式均已实现 |
| Provider 适配层（Volcano/OpenAI/Anthropic） | 完成 | Volcano 真实调用，其余配置 Key 后生效 |
| API Key 鉴权 | 完成 | 已打通网关到 Admin 验证链路 |
| API Key CRUD | 完成 | 创建/列表/吊销/验证 |
| API Key 申请与审批流 | 完成 | 提交/审批通过（自动建Key）/拒绝 |
| 审批前校验模型订阅 | 完成 | approve() 中检查 model_subscription |
| 模型管理（发布/定价/下线） | 完成 | CRUD 完整 |
| 模型订阅关系 | 完成 | 订阅/取消/查询 |
| Usage 日志采集 | 完成 | 网关采集，HTTP 推送 Admin，写入 MySQL |
| Usage 日志查询 | 完成 | 时间/模型/状态条件查询 |
| Usage 日志统计 | 完成 | GET /logs/statistics |
| TraceId 全链路追踪 | 完成 | ThreadLocal + Header 传递 |
| Dashboard 统计 | 完成 | Key 数量/模型数量 |
| 渠道/用户/充值/兑换码/任务/系统设置 | 完成 | CRUD 完整 |

### 9.2 前端完成情况

| 页面 | 状态 | 说明 |
|------|:----:|------|
| Dashboard | 完成 | 统计卡片 |
| API Keys 管理 | 完成 | 创建/列表/吊销 |
| API Key 申请 | 完成 | 表单提交 + 我的申请列表 |
| 模型管理/定价 | 完成 | 列表 + 发布 |
| 模型订阅 | 完成 | 订阅/取消订阅 |
| 调用日志 | 完成 | 列表 + 筛选 + 详情 Drawer |
| 接口文档 | 完成 | iframe 内嵌 Swagger UI |
| Playground | 完成 | 实时对话测试 |
| 渠道/用户/充值/任务/设置 | 完成 | 完整页面 |
| 角色差异化菜单 | 完成 | admin/user 两套菜单 |

### 9.3 待补齐项（P1）

| 项目 | 说明 |
|------|------|
| 登录认证 | 目前 role 存 localStorage，无真正登录，需补 JWT 登录接口 |
| 配额扣减 | used_quota 字段已有，但验证后未实际扣减 token 数 |
| Dashboard token 统计 | 当前返回 0，需从 usage_log 表聚合 |
| 调用日志按用户过滤 | 普通用户应只看自己的日志 |
| Key 审批前端页面 | KeyApplicationController 列表数据目前是 mock |

### 9.4 技术选型总结

| 技术 | 选用原因 |
|------|----------|
| Spring WebFlux | LLM SSE 流式必须非阻塞，阻塞模型无法支撑高并发长连接 |
| Spring MVC | Admin CRUD 配合 MyBatis-Plus 事务，简单直接 |
| MyBatis-Plus | 零 XML，逻辑删除，类型安全查询，减少 80% 样板代码 |
| WebClient | 非阻塞 HTTP，网关调 Admin 验 Key 和上报日志均异步 |
| MySQL | 元数据结构固定，事务保证审批原子性 |
| React + Ant Design | Admin 台主流组合，组件开箱即用 |
| React Query | 服务端状态自动缓存和重试 |
| 不用 Spring AI | 无多租户/配额/审批/日志持久化，企业网关必须自研这些能力 |

---

## 10. 后续新增功能与技术栈

### 10.1 JWT 登录认证

**新增文件：**

| 文件 | 说明 |
|------|------|
| `gateway-admin/util/JwtUtil.java` | 生成/解析/验证 JWT token，基于 jjwt 0.12.5 |
| `gateway-admin/dto/LoginRequest.java` | 登录请求 DTO |
| `gateway-admin/dto/LoginResponse.java` | 登录响应 DTO（含 token/userId/username/role/expiresIn） |
| `gateway-admin/controller/AuthController.java` | POST /api/admin/auth/login、POST /logout、GET /me |
| `gateway-admin/interceptor/JwtAuthInterceptor.java` | 拦截所有 /api/** 请求，白名单放行登录/文档页 |
| `gateway-admin/config/WebMvcConfig.java` | 注册 JwtAuthInterceptor + CORS 配置 |
| `frontend/src/api/auth.ts` | 前端 login / logout / me 接口 |
| `frontend/src/pages/Login/` | 登录页（深色玻璃拟态 UI） |

**新增依赖（gateway-admin/pom.xml）：**

```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
<!-- 密码加密（无需完整 Spring Security） -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

**核心流程：**

```
POST /api/admin/auth/login {username, password}
  → AdminUserService.login() 验证密码（支持 BCrypt 和明文）
  → JwtUtil.generateToken(userId, username, role)
  → 返回 {token, userId, username, role, expiresIn}

后续请求：
  Authorization: Bearer {token}
  → JwtAuthInterceptor.preHandle()
  → JwtUtil.validateToken() 验证签名和过期时间
  → 写入 request.setAttribute(currentUserId/currentUsername/currentRole)
```

**配置项（application.yml）：**

```yaml
jwt:
  secret: aigateway-secret-key-must-be-at-least-32-chars!!
  expiration: 86400000  # 24 小时（毫秒）
```

**前端联动：**
- `request.ts` 请求拦截器自动读取 `localStorage.token` 写入 Authorization header
- 响应拦截器捕获 401，自动清除 token 并跳转 `/login`
- `router/index.tsx` 的 `RequireAuth` 组件：未登录自动跳转登录页

> 当前状态：JWT 验证代码已完整实现，拦截器暂时设置为直接放行（`return true`），
> 前端路由守卫也已禁用，方便测试阶段直接访问。恢复时还原两处即可。

---

### 10.2 配额扣减

**新增/修改文件：**

| 文件 | 变更内容 |
|------|----------|
| `ApiKeyMapper.java` | 新增 `@Update` 原子递增方法 `incrementUsedQuota(keyId, tokens)` |
| `ApiKeyService.java` | 新增 `deductQuota(keyId, tokens)` 方法，含配额余量校验 |
| `ApiKeyController.java` | 新增 `POST /api/admin/keys/{keyId}/deduct-quota?tokens=N` 端点 |
| `AdminClient.java` | 新增 `deductQuotaAsync(keyId, tokens)` 异步调用方法 |
| `ChatService.java` | 新增 `deductQuotaIfNeeded()` 调用后触发异步扣减 |

**调用链路：**

```
LLM 调用成功
  → ChatService.deductQuotaIfNeeded(response)
  → 从 ApiKeyContext 取 keyId，从 response.usage 取 totalTokens
  → AdminClient.deductQuotaAsync(keyId, totalTokens)  ← fire-and-forget，不阻塞
  → POST http://admin:9081/api/admin/keys/{keyId}/deduct-quota?tokens=N
  → ApiKeyService.deductQuota()
      ├─ totalQuota == 0 → 不限配额，跳过检查
      ├─ remaining < tokens → 配额不足，返回 false（记录警告日志）
      └─ baseMapper.incrementUsedQuota(keyId, tokens)
           UPDATE api_key SET used_quota = used_quota + #{tokens}
           WHERE id = #{keyId} AND deleted = 0
           （原子操作，防并发超用）
```

**关键设计：**
- `totalQuota = 0` 表示不限配额
- SQL UPDATE 原子递增，多实例并发安全
- 扣减异步进行（`.subscribe()`），不影响 LLM 响应延迟

---

### 10.3 Prometheus 监控接入

**新增文件：**

| 文件 | 说明 |
|------|------|
| `gateway-core/metrics/LlmMetrics.java` | 自定义 LLM 调用指标，在 UsageLogService 中调用 |

**新增依赖（gateway-admin 和 gateway-core 的 pom.xml）：**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**暴露的 Prometheus 指标：**

| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `llm_requests_total` | Counter | model / provider / status | LLM 调用总次数 |
| `llm_tokens_total` | Counter | model / provider / type(prompt/completion/total) | token 消耗量 |
| `llm_latency_seconds` | Timer | model / provider / status | P50/P95/P99 延迟分位 |

**访问端点：**

```
http://localhost:9080/actuator/prometheus   # gateway-core 指标
http://localhost:9081/actuator/prometheus   # gateway-admin 指标
```

**application.yml 配置：**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
```

**Grafana 查询示例：**

```promql
# 各模型每分钟调用量
rate(llm_requests_total[1m])

# 各模型 P99 延迟
histogram_quantile(0.99, rate(llm_latency_seconds_bucket[5m]))

# 各供应商 token 消耗速率
rate(llm_tokens_total{type="total"}[5m])

# 错误率
rate(llm_requests_total{status="error"}[5m]) / rate(llm_requests_total[5m])
```

**接入链路：**

```
每次 LLM 调用结束
  → UsageLogService.logUsage()
  → LlmMetrics.record(model, provider, status, latencyMs, promptTokens, completionTokens)
      ├─ Counter llm_requests_total.increment()
      ├─ Counter llm_tokens_total.increment(promptTokens)   type=prompt
      ├─ Counter llm_tokens_total.increment(completionTokens) type=completion
      ├─ Counter llm_tokens_total.increment(total)          type=total
      └─ Timer   llm_latency_seconds.record(latencyMs, MILLISECONDS)
```

---

### 10.4 更新后的技术选型总表

| 技术 | 模块 | 选用原因 |
|------|------|----------|
| Spring WebFlux | gateway-core | LLM SSE 流式必须非阻塞，Netty 事件循环支撑高并发长连接 |
| Spring MVC | gateway-admin | Admin CRUD 配合 MyBatis-Plus 事务，简单直接 |
| MyBatis-Plus | gateway-admin | 零 XML，逻辑删除，LambdaQueryWrapper 类型安全，`@Update` 原子 SQL |
| WebClient | gateway-core | 非阻塞 HTTP，验 Key 和上报日志异步，配额扣减 fire-and-forget |
| MySQL | gateway-admin | 结构固定，事务保证审批原子性，UPDATE 原子递增防超用 |
| React + Ant Design | frontend | Admin 台主流组合，Table/Form/Modal 开箱即用 |
| React Query | frontend | 服务端状态自动缓存/重试，配合 axios 拦截器统一错误处理 |
| jjwt 0.12.5 | gateway-admin | 轻量 JWT 库，无需引入完整 Spring Security |
| spring-security-crypto | gateway-admin | 仅用 BCryptPasswordEncoder，不引入 Spring Security 过滤器链 |
| Micrometer + Prometheus | 两个后端 | 标准 Spring Boot 监控方案，与 Grafana 无缝集成 |
| 不用 Spring AI | — | 无多租户/配额/审批/日志持久化，企业网关必须自研这些能力 |
