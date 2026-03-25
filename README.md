# AI Model Gateway

基于 Spring Boot + React 的 AI 模型网关平台，参考 [new-api](https://github.com/QuantumNous/new-api) 项目架构，使用 Java 技术栈实现。

## 项目简介

AI Model Gateway 是一个企业级 AI 模型管理平台，提供统一的模型接入、调用、监控和管理能力。

### 核心功能

- ✅ **统一网关**: 所有模型调用统一入口，OpenAI 兼容格式
- ✅ **API Key 管理**: 自助申请、审批、吊销
- ✅ **模型管理**: 模型发布、订阅、配置
- ✅ **调用日志**: 自动采集调用日志（Token、延迟、状态）
- ✅ **流式输出**: 支持 SSE 流式响应
- ✅ **多提供商**: 支持 DeepSeek、Azure OpenAI、OpenAI、Claude、火山方舟（豆包）等
- ✅ **限流熔断**: 基于 Resilience4j 的限流和熔断保护
- ✅ **监控告警**: Prometheus + Grafana 实时监控

## 技术栈

### 后端
- Spring Boot 3.1.5
- MyBatis Plus 3.5.5
- MySQL 9.5
- Elasticsearch 8.x
- Redis 6.x
- Resilience4j (限流熔断)
- Prometheus (监控指标)

### 前端
- React 18
- TypeScript
- Ant Design 5
- React Query
- Vite

### 监控
- Prometheus (指标采集)
- Grafana (可视化)
- Elasticsearch (日志存储)

## 项目结构

```
ai-model-gateway/
├── gateway-common/          # 公共模块
├── gateway-core/            # 网关核心服务
├── gateway-admin/           # 管理后台服务
├── gateway-provider/        # 模型提供商适配层
├── frontend/                # 前端项目
└── docs/                    # 文档
    └── database/            # 数据库脚本
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Elasticsearch 8.x
- Redis 6.x
- Node.js 18+

### 数据库初始化

```bash
mysql -u root -p < docs/database/schema.sql
```

### 启动后端服务

#### 1. 启动网关核心服务

```bash
cd gateway-core
mvn spring-boot:run
```

访问: http://localhost:8080

#### 2. 启动管理后台服务

```bash
cd gateway-admin
mvn spring-boot:run
```

访问: http://localhost:8081

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问: http://localhost:3000

## API 文档

启动服务后访问 Swagger UI:

- 网关服务: http://localhost:8080/doc.html
- 管理服务: http://localhost:8081/doc.html

## 核心接口

### 聊天接口 (OpenAI 兼容)

```bash
POST /v1/chat/completions
Authorization: Bearer sk-xxx

{
  "model": "gpt-4",
  "messages": [
    {"role": "user", "content": "Hello"}
  ],
  "stream": false
}
```

### API Key 管理

```bash
# 创建 API Key
POST /api/admin/keys

# 获取用户 Keys
GET /api/admin/keys/user/{userId}

# 吊销 Key
PUT /api/admin/keys/{keyId}/revoke
```

### 模型管理

```bash
# 获取模型列表
GET /api/admin/models

# 发布模型
POST /api/admin/models

# 更新模型
PUT /api/admin/models/{id}
```

## 配置说明

### 网关核心服务配置

编辑 `gateway-core/src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  redis:
    host: localhost
    port: 6379
  
  elasticsearch:
    uris: http://localhost:9200
```

### 管理服务配置

编辑 `gateway-admin/src/main/resources/application.yml`:

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_gateway
    username: root
    password: root
```

## 开发计划

### Q1 MVP (已完成)
- ✅ 统一网关接口
- ✅ API Key 管理
- ✅ 模型管理
- ✅ 调用日志采集
- ✅ 流式输出支持

### Q2 计划
- ⏳ 限流与熔断
- ⏳ PII 脱敏
- ⏳ 语义缓存
- ⏳ 智能路由
- ⏳ 成本统计

## 参考项目

- [new-api](https://github.com/QuantumNous/new-api) - Go 版本的 AI 模型网关

## License

MIT License
