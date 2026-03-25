# AI Model Gateway 技术架构文档

> 版本：v1.0 | 更新时间：2025-05

---

## 一、项目概述

AI Model Gateway 是一个面向企业的 AI 模型统一接入平台，提供 OpenAI 兼容协议的网关层，支持多 Provider 路由、API Key 管理、配额控制、调用日志等完整生命周期管理。

系统分为两个运行时服务：
- **gateway-core**（端口 9080）：对外暴露 OpenAI 兼容接口，处理鉴权、路由、限流、熔断
- **gateway-admin**（端口 9082）：管理后台 REST API，供前端和内部调用

---

## 二、现有技术栈（自研实现）

### 2.1 模块划分

```
ai-model-gateway/
├── gateway-common      # 公共模块：Result、DTO、Exception、工具类
├── gateway-core        # 网关核心：请求路由、认证、限流、熔断、日志
├── gateway-admin       # 管理后台：用户、Key、渠道、审批、日志查询
├── gateway-provider    # Provider 抽象：ModelProvider 接口 + DynamicChannelProvider
└── frontend            # 管理前端：React + Ant Design
```

### 2.2 后端技术栈

| 层次 | 技术/框架 | 版本 | 用途 |
|------|----------|------|------|
| 运行时 | Java | 21 | 虚拟线程、Record、模式匹配 |
| 基础框架 | Spring Boot | 3.3.6 | Web、自动配置 |
| 响应式 HTTP | Spring WebFlux + WebClient | 3.3.6 | SSE 流式、上游 HTTP 调用 |
| ORM | MyBatis-Plus | 3.5.16 | 数据访问、逻辑删除、分页 |
| 数据库 | MySQL 8.x | — | 业务数据持久化 |
| 熔断/限流 | Resilience4j | 2.x | CircuitBreaker、RateLimiter |
| 认证 | JWT (jjwt) + 自研拦截器 | — | 网关鉴权、Admin JWT |
| 加密 | Jasypt | — | 配置文件敏感信息加密 |
| API 文档 | Knife4j (OpenAPI 3) | 4.5.0 | Swagger UI |
| 工具库 | Hutool | 5.8.26 | UUID、加解密 |
| 监控指标 | Micrometer + Prometheus | — | QPS、延迟、Token 消耗 |
| 日志聚合 | Logback + Loki + Grafana | — | 结构化日志采集展示 |
| 容器化 | Docker Compose | — | 本地一键启动 |

### 2.3 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| React | 18.x | UI 框架 |
| TypeScript | 5.x | 类型安全 |
| Vite | 5.x | 构建工具 |
| Ant Design | 5.x | 组件库 |
| React Query | 3.x | 服务端状态管理 |
| React Router | 6.x | 前端路由 |
| Axios | — | HTTP 客户端 |
| dayjs | — | 时间处理 |

### 2.4 核心请求链路

```
外部调用方（Bearer API Key）
        │
        ▼
[gateway-core :9080]
  ├── AuthInterceptor          # 调 Admin /validate 验证 Key，注入 userId/role
  ├── RateLimitInterceptor     # Resilience4j 限流（全局/Key/IP 三级）
  │
  └── ChatController
        POST /v1/chat/completions        # 同步
        POST /v1/chat/completions/stream # SSE 流式
        POST /v1/responses               # 多模态
              │
              ▼
          ChatService（10 步流水线）
            1. PromptGuardService     正则 + 敏感词注入检测
            2. 模型权限校验            Key.allowedModels 白名单
            3. BudgetGuardService     全局/Key/模型 Token 预算
            4. SemanticCacheService   精确语义缓存（Caffeine，TTL 3600s）
            5. SmartRoutingService    动态渠道权重随机 → 静态 Provider 轮询
            6. CircuitBreakerService  Resilience4j 熔断（失败率>50% 触发）
            7. DynamicChannelProvider OpenAI 兼容协议 WebClient 调上游
            8. SemanticCacheService   写入缓存
            9. UsageLogService        异步 HTTP 上报日志到 Admin
           10. AdminClient            异步扣减配额
```

### 2.5 Provider 路由策略

- **动态渠道优先**：从 `channel` 表加载，按 `weight` 字段加权随机选择，30s 轮询刷新
- **静态 Provider 回退**：`application-providers.yml` 配置，按模型前缀匹配（`deepseek-*`、`gpt-*`、`claude-*` 等）
- **熔断保护**：失败率 > 50% 触发熔断，30s 后 HALF_OPEN 自动探测恢复
- **协议支持**：全部 OpenAI 兼容协议；Azure 额外加 `api-key` header

### 2.6 Admin 功能模块

| 模块 | 接口前缀 | 说明 |
|------|---------|------|
| 用户管理 | `/api/admin/users` | CRUD，角色/配额管理 |
| API Key 管理 | `/api/admin/keys` | 创建/删除/脱敏展示，批量填充用户名 |
| Key 申请审批 | `/api/admin/key-applications` | 用户申请 → 管理员审批 → 自动生成 Key |
| 渠道管理 | `/api/admin/channels` | 配置上游 Provider 渠道，连通性测试 |
| 模型管理 | `/api/admin/models` | 发布/下线模型，订阅管理 |
| 调用日志 | `/api/admin/logs` | 分页查询，批量填充用户名 |
| 仪表盘 | `/api/admin/dashboard` | 统计、熔断器状态、最近调用 |
| 系统设置 | `/api/admin/settings` | 限流、敏感词动态配置 |
| Playground 代理 | `/api/admin/keys/{id}/chat` | 后端代理调网关，Key 不离开服务器 |

### 2.7 安全设计

- API Key 全程脱敏展示（`sk-abc123****xyz`），完整 Key 仅创建时一次性返回
- Playground 连通性测试走后端代理，前端不接触真实 Key
- 内部接口（网关→Admin）通过 `X-Internal-Secret` 固定密钥认证
- JWT 有效期 7 天，Admin 接口全部需要 JWT
- 用户只能操作自己的资源，Admin 可操作全部资源

---

## 三、Spring AI 替代方案

### 3.1 Spring AI 核心抽象

| 抽象 | 说明 |
|------|------|
| `ChatClient` | 统一多 LLM 调用的 Fluent API，支持同步/流式 |
| `ChatModel` | 底层模型接口，各 Provider Starter 自动注入 |
| `StreamingChatModel` | SSE 流式输出，返回 `Flux<ChatResponse>` |
| `EmbeddingModel` | 向量化接口 |
| `VectorStore` | 向量数据库集成（Redis、ES、Pinecone 等）|
| `Advisor` | AOP 式请求/响应拦截（缓存、RAG、日志、安全）|
| `PromptTemplate` | 提示词模板管理 |
| `@Tool` / `ToolCallback` | 函数调用标准化 |
| `Media` | 多模态消息中的图片/音频对象 |

### 3.2 Spring AI 替代技术栈对比

#### 后端核心变化

| 现有实现 | Spring AI 替代 | 迁移成本 |
|---------|---------------|----------|
| 自研 `ModelProvider` 接口 + `DynamicChannelProvider` | `ChatModel` 接口 + 各 Provider Starter | 中：需重写 Provider 适配层 |
| 自研 `SmartRoutingService`（权重路由）| 自定义 `RouterChatModel` 包装多个 `ChatModel` Bean | 低：路由逻辑保留，底层换标准接口 |
| `provider.chatStream()` → `Flux<String>` | `ChatClient.stream()` → `Flux<ChatResponse>` | 低：格式更标准，SSE 序列化简化 |
| `SemanticCacheService`（Caffeine 精确匹配）| `VectorStoreChatMemoryAdvisor` + `SimpleVectorStore`（向量相似度缓存）| 高：需引入向量化，真正语义缓存 |
| `PromptGuardService`（正则 + 关键词）| 自定义 `RequestResponseAdvisor` + `SafeGuardAdvisor` | 低：Advisor 方式更优雅 |
| `UsageLogService`（自研上报）| 自定义 `Advisor` 拦截 `usage` 字段上报 | 低：标准 usage 结构更易获取 |
| 手动构造多模态 messages 数组 | `UserMessage` + `Media` 对象 | 低：标准化多模态消息构造 |
| 无 RAG / 知识库 | `QuestionAnswerAdvisor` + `VectorStore` | 新增能力 |
| 无函数调用 | `@Tool` 注解 / `ToolCallbackProvider` | 新增能力 |

#### 不变部分

| 组件 | 说明 |
|------|------|
| Spring Boot 3.3.6 | 保留 |
| MyBatis-Plus + MySQL | 保留，Admin 数据层不变 |
| Resilience4j | 熔断/限流保留 |
| JWT + 拦截器 | 认证层保留 |
| React + Ant Design 前端 | 完全保留 |
| Prometheus + Grafana + Loki | 监控层保留 |
| Jasypt | 配置加密保留 |

### 3.3 Spring AI 方案依赖配置

```xml
<!-- Spring AI BOM -->
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-bom</artifactId>
    <version>1.0.4</version>
  <type>pom</type>
  <scope>import</scope>
</dependency>

<!-- OpenAI Provider -->
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- Anthropic Claude -->
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
</dependency>

<!-- Redis VectorStore -->
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-redis-store-spring-boot-starter</artifactId>
</dependency>
```

### 3.4 Spring AI 核心代码结构

```
gateway-core (Spring AI)
  advisor/
    PromptGuardAdvisor.java   # RequestResponseAdvisor 实现
    BudgetAdvisor.java
    UsageLogAdvisor.java
  config/
    RouterChatModel.java      # 多 ChatModel Bean 权重路由
  service/
    ChatService.java          # ChatClient.builder().defaultAdvisors().build()
```

---

## 四、两种方案对比

| 维度 | 自研方案 | Spring AI 方案 |
|------|---------|---------------|
| Provider 接入 | 自研 ModelProvider + WebClient | Spring AI Starter 自动配置 |
| SSE 流式 | Flux<String> 手动拼接 | ChatClient.stream() 原生支持 |
| 语义缓存 | Caffeine 精确匹配 | VectorStore 向量相似度缓存 |
| Prompt 安全 | 自研正则检测 | Advisor AOP 可插拔 |
| RAG 知识库 | 不支持 | QuestionAnswerAdvisor 原生支持 |
| 函数调用 | 不支持 | @Tool 注解原生支持 |
| 熔断/限流 | Resilience4j（相同）| Resilience4j（相同）|
| 灵活性 | 极高（完全自控）| 中（框架约束）|
| 学习成本 | 高 | 低 |

---

## 五、总体架构图

```
前端 React :3000
       |
 gateway-admin :9082  <--JDBC-->  MySQL
       |  X-Internal-Secret
 gateway-core :9080
  AuthInterceptor / RateLimiter
  ChatService 10步流水线
  PromptGuard / Budget / Cache / Routing / CircuitBreaker
       |
DeepSeek / OpenAI / Azure / Anthropic / 火山引擎

监控：Prometheus + Grafana + Loki
```

---

## 六、迁移路线图

1. 阶段一：引入 Spring AI BOM，替换 DynamicChannelProvider WebClient 调用
2. 阶段二：PromptGuard / UsageLog 重构为 Advisor
3. 阶段三：引入 VectorStore 实现语义缓存 + RAG
4. 阶段四：全部 Provider 使用 Spring AI Starter

---

## 七、数据库主要表

| 表名 | 说明 |
|------|------|
| user | 用户：username/role/quota |
| api_key | API Key：keyValue/userId/allowedModels |
| key_application | 申请审批流 |
| channel | 渠道：provider/baseUrl/apiKey/weight |
| model_info | 模型信息：名称/价格/provider |
| model_subscription | 用户模型订阅关系 |
| usage_log_record | 调用日志：tokens/latencyMs/status |