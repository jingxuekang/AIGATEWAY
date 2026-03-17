# AI Gateway 完整技术栈文档

## 一、技术栈总览

### 1.1 后端技术栈

| 技术 | 版本 | 用途 | 选型原因 |
|------|------|------|---------|
| **Spring Boot** | 3.1.5 | 应用框架 | 成熟稳定，生态完善，企业级标准 |
| **Spring WebFlux** | 3.1.5 | 响应式 Web | 非阻塞 I/O，支持高并发，SSE 流式输出 |
| **WebClient** | 3.1.5 | HTTP 客户端 | 响应式非阻塞，与 WebFlux 配合，支持流式响应 |
| **MyBatis Plus** | 3.5.5 | ORM 框架 | 简化 CRUD，代码生成，强大的条件构造器 |
| **MySQL** | 9.5 | 关系数据库 | 存储 API Key、模型配置、用户数据 |
| **Elasticsearch** | 8.x | 日志存储 | 高性能日志查询，聚合统计，时序数据 |
| **Redis** | 7.x | 缓存/限流 | API Key 缓存，速率限制，分布式锁 |
| **Maven** | 3.x | 构建工具 | 多模块管理，依赖管理 |
| **Lombok** | - | 代码简化 | 减少样板代码，提高开发效率 |
| **Swagger/OpenAPI** | 3.x | API 文档 | 自动生成 API 文档，接口测试 |

### 1.2 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **React** | 18.x | UI 框架 |
| **TypeScript** | 5.x | 类型安全 |
| **Vite** | 5.x | 构建工具 |
| **Ant Design** | 5.x | UI 组件库 |
| **React Router** | 6.x | 路由管理 |
| **React Query** | 3.x | 数据获取 |
| **Axios** | 1.x | HTTP 客户端 |

### 1.3 架构模式

- **多模块 Maven 项目**：gateway-common、gateway-provider、gateway-core、gateway-admin
- **Provider 适配器模式**：统一不同 AI 模型的调用接口
- **响应式编程**：WebFlux + Reactor 实现非阻塞 I/O
- **前后端分离**：React SPA + RESTful API

---

## 二、核心流程调用链

### 2.1 AI 模型调用完整流程

```
客户端请求
    ↓
[1] gateway-core: ChatController.chat()
    ├─ 接收 HTTP POST /v1/chat/completions
    ├─ 提取 Authorization Header 中的 API Key
    └─ 调用 AuthService.validateApiKey()
    
    ↓
[2] gateway-core: AuthService.validateApiKey()
    ├─ 从 Redis 缓存查询 API Key
    ├─ 缓存未命中则查询 MySQL
    ├─ 验证 Key 状态（是否过期、是否吊销）
    ├─ 验证配额（已用配额 < 总配额）
    ├─ 验证模型权限（请求的模型在 allowedModels 中）
    └─ 返回验证结果 + 租户信息
    
    ↓
[3] gateway-core: RoutingService.selectProvider()
    ├─ 根据模型名称选择 Provider
    ├─ 负载均衡（如果有多个 Provider 实例）
    └─ 返回 Provider 实例
    
    ↓
[4] gateway-provider: OpenAIProvider.chat() / AnthropicProvider.chat()
    ├─ 转换请求格式（统一格式 → Provider 特定格式）
    ├─ 使用 WebClient 发起非阻塞 HTTP 请求
    ├─ 处理流式响应（SSE）或普通响应
    ├─ 转换响应格式（Provider 格式 → 统一格式）
    └─ 返回 Flux<ServerSentEvent> 或 Mono<ChatResponse>
    
    ↓
[5] gateway-core: UsageTracker.recordUsage()
    ├─ 计算 Token 使用量
    ├─ 更新 API Key 已用配额（MySQL）
    ├─ 记录详细日志到 Elasticsearch
    └─ 异步执行，不阻塞响应
    
    ↓
[6] 返回响应给客户端
    ├─ 流式：SSE 格式，逐块返回
    └─ 非流式：JSON 格式，一次性返回
```

**为什么这么设计？**

1. **认证前置**：在路由前验证，避免无效请求消耗资源
2. **缓存优化**：Redis 缓存 API Key，减少数据库查询
3. **适配器模式**：Provider 层解耦，易于扩展新模型
4. **响应式编程**：WebFlux 非阻塞，支持高并发
5. **异步日志**：不阻塞主流程，提高响应速度

---

### 2.2 API Key 管理流程

#### 2.2.1 创建 API Key

```
前端提交申请
    ↓
[1] gateway-admin: ApiKeyController.createKey()
    ├─ 接收 POST /api/admin/keys
    ├─ 验证管理员权限
    └─ 调用 ApiKeyService.createKey()
    
    ↓
[2] gateway-admin: ApiKeyService.createKey()
    ├─ 生成唯一 Key（UUID + 前缀）
    ├─ 设置默认配额和过期时间
    ├─ 保存到 MySQL（api_key 表）
    ├─ 写入 Redis 缓存
    └─ 返回 API Key 对象
    
    ↓
[3] 返回给前端展示
```

#### 2.2.2 吊销 API Key

```
管理员操作
    ↓
[1] gateway-admin: ApiKeyController.revokeKey()
    ├─ 接收 PUT /api/admin/keys/{keyId}/revoke
    └─ 调用 ApiKeyService.revokeKey()
    
    ↓
[2] gateway-admin: ApiKeyService.revokeKey()
    ├─ 更新 MySQL 状态为 0（禁用）
    ├─ 删除 Redis 缓存
    └─ 返回操作结果
```

**为什么这么设计？**

1. **双写策略**：MySQL 持久化 + Redis 缓存，兼顾性能和可靠性
2. **缓存失效**：吊销时立即删除缓存，确保实时生效
3. **权限控制**：管理接口独立，与核心网关分离

---

### 2.3 日志查询流程

```
前端查询请求
    ↓
[1] gateway-admin: LogController.queryLogs()
    ├─ 接收 GET /api/admin/logs
    ├─ 解析查询参数（时间范围、模型、状态）
    └─ 调用 ElasticsearchService.search()
    
    ↓
[2] gateway-admin: ElasticsearchService.search()
    ├─ 构建 ES 查询 DSL
    ├─ 执行查询（RestHighLevelClient）
    ├─ 解析结果
    └─ 返回分页数据
    
    ↓
[3] 返回给前端展示
```

**为什么用 Elasticsearch？**

1. **时序数据**：日志按时间顺序，ES 擅长时序查询
2. **全文检索**：支持复杂条件过滤
3. **聚合统计**：Dashboard 统计数据（按模型、按时间聚合）
4. **高性能**：亿级数据查询毫秒级响应

---

## 三、为什么这么做？场景分析

### 3.1 为什么用 WebFlux 而不是传统 Spring MVC？

**场景：高并发 AI 模型调用**

- **问题**：AI 模型响应慢（1-10 秒），传统阻塞 I/O 会占用线程
- **Spring MVC**：每个请求占用一个线程，1000 并发需要 1000 线程
- **WebFlux**：非阻塞，少量线程处理大量并发（事件循环）

**实际效果**：
- Spring MVC：200 线程，支持 200 并发
- WebFlux：8 线程（CPU 核心数），支持 10000+ 并发

### 3.2 为什么用 Provider 适配器模式？

**场景：对接多个 AI 模型提供商**

- **OpenAI**：`{"model": "gpt-4", "messages": [...]}`
- **Anthropic**：`{"model": "claude-3-opus", "messages": [...]}`
- **火山方舟**：`{"req_id": "xxx", "model": {...}}`

**不用适配器的问题**：
- 每个模型写一套代码，重复劳动
- 新增模型需要修改核心代码，违反开闭原则

**使用适配器的好处**：
- 统一接口：`Provider.chat(request) → Flux<Response>`
- 新增模型只需实现 Provider 接口
- 核心代码不变，扩展性强

### 3.3 为什么日志用 Elasticsearch 而不是 MySQL？

**场景：每天百万级调用日志**

| 需求 | MySQL | Elasticsearch |
|------|-------|---------------|
| 写入性能 | 1000 TPS | 10000+ TPS |
| 查询性能 | 慢（全表扫描） | 快（倒排索引） |
| 聚合统计 | 慢（GROUP BY） | 快（聚合桶） |
| 存储成本 | 高（索引多） | 低（压缩） |

**实际场景**：
- 查询"过去 7 天 gpt-4 的平均延迟"
- MySQL：扫描百万行，GROUP BY，耗时 10 秒
- ES：聚合查询，耗时 100 毫秒

### 3.4 为什么 API Key 用 Redis 缓存？

**场景：每秒 1000 次 API Key 验证**

- **不用缓存**：每次查询 MySQL，数据库压力大
- **用缓存**：99% 命中 Redis，MySQL 只处理缓存未命中

**效果**：
- MySQL QPS：10（缓存未命中）
- Redis QPS：990（缓存命中）
- 响应时间：从 10ms 降到 1ms

---

## 四、可能遇到的场景和解决方案

### 4.1 场景：API Key 泄露

**问题**：用户 API Key 被盗用，产生大量费用

**解决方案**：
1. **IP 白名单**：限制 API Key 只能从特定 IP 调用
2. **速率限制**：每分钟最多 100 次调用
3. **异常检测**：调用量突增时告警
4. **快速吊销**：管理后台一键吊销

**实现**：
```java
// Redis 限流
String key = "rate_limit:" + apiKey;
Long count = redisTemplate.opsForValue().increment(key);
if (count == 1) {
    redisTemplate.expire(key, 60, TimeUnit.SECONDS);
}
if (count > 100) {
    throw new BusinessException("Rate limit exceeded");
}
```

### 4.2 场景：某个模型提供商故障

**问题**：OpenAI API 宕机，所有 GPT-4 请求失败

**解决方案**：
1. **多 Provider 配置**：同一模型配置多个 Provider
2. **自动故障转移**：主 Provider 失败，切换到备用
3. **熔断降级**：连续失败后熔断，避免雪崩

**实现**：
```java
@Override
public Mono<ChatResponse> chat(ChatRequest request) {
    return primaryProvider.chat(request)
        .onErrorResume(e -> {
            log.warn("Primary provider failed, fallback to secondary");
            return secondaryProvider.chat(request);
        })
        .timeout(Duration.ofSeconds(30))
        .retry(2);
}
```

### 4.3 场景：日志数据量爆炸

**问题**：每天 1000 万条日志，ES 存储成本高

**解决方案**：
1. **冷热分离**：7 天内热数据（SSD），7 天外冷数据（HDD）
2. **定期清理**：30 天外数据归档到 S3
3. **采样存储**：成功请求采样 10%，失败请求全量

**实现**：
```java
// 采样逻辑
if ("success".equals(status) && Math.random() > 0.1) {
    return; // 90% 成功请求不记录
}
elasticsearchService.save(log);
```

### 4.4 场景：流式输出中断

**问题**：SSE 流式输出到一半，客户端断开连接

**解决方案**：
1. **检测断开**：监听 `onCancel()` 事件
2. **停止转发**：取消上游 Provider 请求
3. **记录日志**：标记为"部分成功"

**实现**：
```java
return webClient.post()
    .retrieve()
    .bodyToFlux(ServerSentEvent.class)
    .doOnCancel(() -> {
        log.info("Client disconnected, cancel upstream request");
    })
    .doFinally(signal -> {
        recordUsage(signal == SignalType.CANCEL ? "partial" : "success");
    });
```

---

## 五、模块职责划分

### 5.1 gateway-common
- **职责**：公共工具类、常量、异常定义
- **内容**：Result、BusinessException、TraceIdUtil
- **依赖**：无（被其他模块依赖）

### 5.2 gateway-provider
- **职责**：AI 模型 Provider 适配器
- **内容**：OpenAIProvider、AnthropicProvider、VolcanoProvider
- **依赖**：gateway-common、WebClient

### 5.3 gateway-core
- **职责**：核心网关逻辑（认证、路由、转发）
- **内容**：ChatController、AuthService、RoutingService
- **依赖**：gateway-common、gateway-provider、Redis

### 5.4 gateway-admin
- **职责**：管理后台 API（API Key、模型、日志）
- **内容**：ApiKeyController、ModelController、LogController
- **依赖**：gateway-common、MySQL、Elasticsearch

---

## 六、数据流向图

```
┌─────────────┐
│   客户端     │
└──────┬──────┘
       │ HTTP POST /v1/chat/completions
       │ Authorization: Bearer sk-xxx
       ↓
┌─────────────────────────────────────┐
│         gateway-core                │
│  ┌──────────────────────────────┐  │
│  │  ChatController              │  │
│  └────────┬─────────────────────┘  │
│           │                         │
│  ┌────────↓─────────────────────┐  │
│  │  AuthService                 │  │
│  │  - 验证 API Key (Redis/MySQL)│  │
│  │  - 验证配额                  │  │
│  │  - 验证模型权限              │  │
│  └────────┬─────────────────────┘  │
│           │                         │
│  ┌────────↓─────────────────────┐  │
│  │  RoutingService              │  │
│  │  - 选择 Provider             │  │
│  │  - 负载均衡                  │  │
│  └────────┬─────────────────────┘  │
└───────────┼─────────────────────────┘
            │
            ↓
┌─────────────────────────────────────┐
│       gateway-provider              │
│  ┌──────────────────────────────┐  │
│  │  OpenAIProvider              │  │
│  │  - 转换请求格式              │  │
│  │  - WebClient 调用 OpenAI API │  │
│  │  - 转换响应格式              │  │
│  └────────┬─────────────────────┘  │
└───────────┼─────────────────────────┘
            │
            ↓
┌─────────────────────────────────────┐
│       OpenAI API                    │
│  https://api.openai.com             │
└─────────────────────────────────────┘
```

---

## 七、总结

这个 AI Gateway 项目采用了现代化的技术栈和架构设计：

1. **响应式编程**：WebFlux + WebClient，支持高并发和流式输出
2. **适配器模式**：Provider 层解耦，易于扩展新模型
3. **多层缓存**：Redis 缓存 + MySQL 持久化，兼顾性能和可靠性
4. **日志分离**：Elasticsearch 存储日志，MySQL 存储配置
5. **前后端分离**：React SPA + RESTful API，开发效率高

这些设计选择都是基于实际场景需求，确保系统的高性能、高可用和可扩展性。
