# AI Gateway 架构设计详解

## 问题1：兼容不同模型的调用格式

### 当前支持情况：✅ 已支持

我们的网关采用了 **Provider 适配器模式**，可以兼容不同的 AI 模型调用格式。

### 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                      Gateway Core                            │
│  ┌────────────────────────────────────────────────────────┐ │
│  │           统一接口层 (ChatController)                   │ │
│  │     POST /v1/chat/completions (OpenAI 兼容格式)        │ │
│  └────────────────┬───────────────────────────────────────┘ │
│                   │                                          │
│  ┌────────────────▼───────────────────────────────────────┐ │
│  │              ChatService (路由层)                       │ │
│  │  - 解析请求模型                                         │ │
│  │  - 选择对应的 Provider                                  │ │
│  │  - 转换请求格式                                         │ │
│  └────────────────┬───────────────────────────────────────┘ │
│                   │                                          │
│         ┌─────────┴─────────┬──────────────┬──────────────┐ │
│         │                   │              │              │ │
│  ┌──────▼──────┐   ┌───────▼──────┐  ┌───▼──────┐  ┌───▼──┐
│  │   OpenAI    │   │  Anthropic   │  │  Volcano │  │ 自定义│
│  │   Provider  │   │   Provider   │  │ Provider │  │Provider│
│  └──────┬──────┘   └───────┬──────┘  └───┬──────┘  └───┬──┘
└─────────┼──────────────────┼─────────────┼─────────────┼───┘
          │                  │             │             │
          ▼                  ▼             ▼             ▼
    ┌─────────┐        ┌─────────┐   ┌─────────┐   ┌─────────┐
    │ OpenAI  │        │Anthropic│   │  火山   │   │  自有   │
    │   API   │        │   API   │   │ 方舟API │   │  模型   │
    └─────────┘        └─────────┘   └─────────┘   └─────────┘
```

### 实现流程

#### 步骤1: 定义统一的 Provider 接口

```java
public interface ModelProvider {
    // 提供商名称
    String getProviderName();
    
    // 是否支持该模型
    boolean supports(String model);
    
    // 同步调用
    ChatResponse chat(ChatRequest request);
    
    // 流式调用
    Flux<String> chatStream(ChatRequest request);
}
```

#### 步骤2: 实现各个 Provider 适配器

**OpenAI Provider (已实现)**
```java
@Component
public class OpenAIProvider implements ModelProvider {
    @Override
    public boolean supports(String model) {
        return model.startsWith("gpt-") || 
               model.startsWith("o1-") || 
               model.startsWith("o3-");
    }
    
    @Override
    public ChatResponse chat(ChatRequest request) {
        // 1. 转换请求格式为 OpenAI 格式
        OpenAIChatRequest openAIRequest = convertToOpenAI(request);
        
        // 2. 调用 OpenAI API
        OpenAIChatResponse openAIResponse = callOpenAI(openAIRequest);
        
        // 3. 转换响应为统一格式
        return convertFromOpenAI(openAIResponse);
    }
}
```

**Anthropic Provider (已实现)**
```java
@Component
public class AnthropicProvider implements ModelProvider {
    @Override
    public boolean supports(String model) {
        return model.startsWith("claude-");
    }
    
    @Override
    public ChatResponse chat(ChatRequest request) {
        // 1. 转换请求格式为 Anthropic 格式
        AnthropicRequest anthropicRequest = convertToAnthropic(request);
        
        // 2. 调用 Anthropic API
        AnthropicResponse anthropicResponse = callAnthropic(anthropicRequest);
        
        // 3. 转换响应为统一格式
        return convertFromAnthropic(anthropicResponse);
    }
}
```

**火山方舟 Provider (待实现)**
```java
@Component
public class VolcanoProvider implements ModelProvider {
    @Override
    public String getProviderName() {
        return "volcano";
    }
    
    @Override
    public boolean supports(String model) {
        // 火山方舟的模型命名规则
        return model.startsWith("doubao-") || 
               model.contains("volcano");
    }
    
    @Override
    public ChatResponse chat(ChatRequest request) {
        // 1. 转换为火山方舟格式
        VolcanoRequest volcanoRequest = new VolcanoRequest();
        volcanoRequest.setModel(request.getModel());
        
        // 火山方舟使用不同的消息格式
        List<VolcanoMessage> messages = request.getMessages().stream()
            .map(msg -> {
                VolcanoMessage vm = new VolcanoMessage();
                vm.setRole(msg.getRole());
                vm.setContent(msg.getContent());
                return vm;
            })
            .collect(Collectors.toList());
        volcanoRequest.setMessages(messages);
        
        // 2. 调用火山方舟 API
        VolcanoResponse volcanoResponse = restTemplate.postForObject(
            volcanoApiUrl,
            volcanoRequest,
            VolcanoResponse.class
        );
        
        // 3. 转换响应
        ChatResponse response = new ChatResponse();
        response.setModel(volcanoResponse.getModel());
        // ... 转换其他字段
        return response;
    }
}
```

#### 步骤3: 自动路由机制

```java
@Service
public class ChatService {
    private final List<ModelProvider> providers;
    
    public ChatResponse chat(ChatRequest request) {
        // 自动选择合适的 Provider
        ModelProvider provider = providers.stream()
            .filter(p -> p.supports(request.getModel()))
            .findFirst()
            .orElseThrow(() -> new BusinessException("不支持的模型"));
        
        // 调用对应的 Provider
        return provider.chat(request);
    }
}
```

### 格式转换示例

#### OpenAI 格式
```json
{
  "model": "gpt-4",
  "messages": [
    {"role": "user", "content": "Hello"}
  ],
  "temperature": 0.7,
  "max_tokens": 1000
}
```

#### Anthropic 格式
```json
{
  "model": "claude-3-opus",
  "messages": [
    {"role": "user", "content": "Hello"}
  ],
  "max_tokens": 1000,
  "temperature": 0.7
}
```

#### 火山方舟格式
```json
{
  "model": "doubao-pro-32k",
  "messages": [
    {"role": "user", "content": "Hello"}
  ],
  "parameters": {
    "temperature": 0.7,
    "max_tokens": 1000
  }
}
```

---

## 问题2：支持流式输出（SSE）

### 当前支持情况：✅ 已支持

我们的网关已经实现了 SSE (Server-Sent Events) 流式输出。

### 架构设计

```
客户端                    Gateway Core                Provider
  │                           │                          │
  │  POST /v1/chat/          │                          │
  │  completions/stream      │                          │
  ├──────────────────────────>│                          │
  │                           │  调用 Provider           │
  │                           │  chatStream()            │
  │                           ├─────────────────────────>│
  │                           │                          │
  │                           │  返回 Flux<String>       │
  │                           │<─────────────────────────┤
  │                           │                          │
  │  data: {...chunk1...}    │                          │
  │<──────────────────────────┤                          │
  │                           │                          │
  │  data: {...chunk2...}    │                          │
  │<──────────────────────────┤                          │
  │                           │                          │
  │  data: [DONE]            │                          │
  │<──────────────────────────┤                          │
  │                           │                          │
```

### 实现流程

#### 步骤1: Controller 层支持流式响应

```java
@RestController
@RequestMapping("/v1")
public class ChatController {
    
    @PostMapping(
        value = "/chat/completions/stream",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> chatCompletionsStream(@RequestBody ChatRequest request) {
        return chatService.chatStream(request);
    }
}
```

#### 步骤2: Service 层实现流式逻辑

```java
@Service
public class ChatService {
    
    public Flux<String> chatStream(ChatRequest request) {
        // 1. 选择 Provider
        ModelProvider provider = findProvider(request.getModel());
        
        // 2. 调用 Provider 的流式接口
        Flux<String> stream = provider.chatStream(request);
        
        // 3. 添加日志记录（异步）
        return stream.doOnNext(chunk -> {
            // 记录每个 chunk
            log.debug("Stream chunk: {}", chunk);
        }).doOnComplete(() -> {
            // 流结束时记录日志
            usageLogService.logStreamComplete(request);
        });
    }
}
```

#### 步骤3: Provider 实现流式调用

**OpenAI 流式实现**
```java
@Override
public Flux<String> chatStream(ChatRequest request) {
    return webClient
        .post()
        .uri(openaiApiUrl + "/chat/completions")
        .header("Authorization", "Bearer " + apiKey)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of(
            "model", request.getModel(),
            "messages", request.getMessages(),
            "stream", true
        ))
        .retrieve()
        .bodyToFlux(String.class)
        .map(line -> {
            // 转换为 SSE 格式
            if (line.startsWith("data: ")) {
                return line;
            }
            return "data: " + line + "\n\n";
        });
}
```

**Anthropic 流式实现**
```java
@Override
public Flux<String> chatStream(ChatRequest request) {
    return webClient
        .post()
        .uri(anthropicApiUrl + "/messages")
        .header("x-api-key", apiKey)
        .header("anthropic-version", "2023-06-01")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of(
            "model", request.getModel(),
            "messages", request.getMessages(),
            "stream", true,
            "max_tokens", 1024
        ))
        .retrieve()
        .bodyToFlux(String.class)
        .map(this::convertAnthropicStreamToOpenAI);
}
```

### SSE 格式说明

#### OpenAI SSE 格式
```
data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"gpt-4","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"gpt-4","choices":[{"index":0,"delta":{"content":" world"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"gpt-4","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

data: [DONE]
```

### 客户端调用示例

#### JavaScript/TypeScript
```typescript
const response = await fetch('http://localhost:9080/v1/chat/completions/stream', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer YOUR_API_KEY'
  },
  body: JSON.stringify({
    model: 'gpt-4',
    messages: [{ role: 'user', content: 'Hello' }]
  })
});

const reader = response.body.getReader();
const decoder = new TextDecoder();

while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  
  const chunk = decoder.decode(value);
  const lines = chunk.split('\n');
  
  for (const line of lines) {
    if (line.startsWith('data: ')) {
      const data = line.slice(6);
      if (data === '[DONE]') {
        console.log('Stream completed');
        break;
      }
      const json = JSON.parse(data);
      console.log(json.choices[0].delta.content);
    }
  }
}
```

#### Python
```python
import requests
import json

response = requests.post(
    'http://localhost:9080/v1/chat/completions/stream',
    headers={
        'Content-Type': 'application/json',
        'Authorization': 'Bearer YOUR_API_KEY'
    },
    json={
        'model': 'gpt-4',
        'messages': [{'role': 'user', 'content': 'Hello'}]
    },
    stream=True
)

for line in response.iter_lines():
    if line:
        line = line.decode('utf-8')
        if line.startswith('data: '):
            data = line[6:]
            if data == '[DONE]':
                break
            chunk = json.loads(data)
            print(chunk['choices'][0]['delta'].get('content', ''), end='')
```

---

## 问题3：API Key 的统一管理和分发

### 当前支持情况：✅ 已实现

我们的网关实现了完整的 API Key 管理和分发机制。

### 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    API Key 管理流程                          │
└─────────────────────────────────────────────────────────────┘

1. 创建阶段
   ┌──────────┐      ┌──────────────┐      ┌──────────┐
   │  Admin   │─────>│ ApiKeyService│─────>│  MySQL   │
   │  创建Key │      │  生成 sk-xxx │      │  存储    │
   └──────────┘      └──────────────┘      └──────────┘

2. 认证阶段
   ┌──────────┐      ┌──────────────┐      ┌──────────┐
   │  Client  │─────>│AuthInterceptor│─────>│  Redis   │
   │ Bearer Key│      │  验证Key     │      │  缓存    │
   └──────────┘      └──────────────┘      └──────────┘
                            │
                            ▼
                      ┌──────────┐
                      │  MySQL   │
                      │  查询    │
                      └──────────┘

3. 使用阶段
   ┌──────────┐      ┌──────────────┐      ┌──────────┐
   │ChatService│─────>│ Provider API │─────>│ AI Model │
   │ 调用模型  │      │  使用真实Key │      │  返回结果│
   └──────────┘      └──────────────┘      └──────────┘
                            │
                            ▼
                      ┌──────────┐
                      │UsageLog  │
                      │记录使用量│
                      └──────────┘

4. 配额管理
   ┌──────────┐      ┌──────────────┐      ┌──────────┐
   │ Response │─────>│ QuotaService │─────>│  MySQL   │
   │ 返回后   │      │  扣减配额    │      │  更新    │
   └──────────┘      └──────────────┘      └──────────┘
```

### 实现流程

#### 步骤1: API Key 创建

```java
@Service
public class ApiKeyService extends ServiceImpl<ApiKeyMapper, ApiKey> {
    
    public ApiKey createKey(ApiKey apiKey) {
        // 1. 生成唯一的 Key 值
        String keyValue = generateKey(); // sk-xxxxx
        apiKey.setKeyValue(keyValue);
        
        // 2. 设置初始状态
        apiKey.setStatus(CommonConstants.KEY_STATUS_ENABLED);
        apiKey.setUsedQuota(0L);
        
        // 3. 保存到数据库
        save(apiKey);
        
        // 4. 预热缓存（可选）
        cacheKey(apiKey);
        
        log.info("Created API Key: {}", keyValue);
        return apiKey;
    }
    
    private String generateKey() {
        // 生成格式: sk-{32位随机字符}
        return CommonConstants.API_KEY_PREFIX + IdUtil.fastSimpleUUID();
    }
}
```

#### 步骤2: API Key 认证拦截器

```java
@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    @Autowired
    private ApiKeyService apiKeyService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        // 1. 从请求头获取 API Key
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException("Missing API Key");
        }
        
        String apiKey = authorization.substring(7);
        
        // 2. 验证 API Key（先查缓存，再查数据库）
        ApiKey key = validateKey(apiKey);
        if (key == null) {
            throw new BusinessException("Invalid API Key");
        }
        
        // 3. 检查状态
        if (key.getStatus() != CommonConstants.KEY_STATUS_ENABLED) {
            throw new BusinessException("API Key is disabled");
        }
        
        // 4. 检查配额
        if (key.getTotalQuota() != null && 
            key.getUsedQuota() >= key.getTotalQuota()) {
            throw new BusinessException("Quota exceeded");
        }
        
        // 5. 检查过期时间
        if (key.getExpireTime() != null && 
            LocalDateTime.now().isAfter(key.getExpireTime())) {
            throw new BusinessException("API Key expired");
        }
        
        // 6. 将 Key 信息存入请求上下文
        request.setAttribute("apiKey", key);
        
        return true;
    }
    
    private ApiKey validateKey(String keyValue) {
        // 1. 先从 Redis 缓存获取
        String cacheKey = "api_key:" + keyValue;
        ApiKey cached = (ApiKey) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 2. 从数据库查询
        ApiKey key = apiKeyService.validateKey(keyValue);
        if (key != null) {
            // 3. 写入缓存（5分钟过期）
            redisTemplate.opsForValue().set(
                cacheKey, 
                key, 
                5, 
                TimeUnit.MINUTES
            );
        }
        
        return key;
    }
}
```

#### 步骤3: 模型权限控制

```java
@Service
public class ChatService {
    
    public ChatResponse chat(ChatRequest request, ApiKey apiKey) {
        // 1. 检查模型权限
        if (!hasModelPermission(apiKey, request.getModel())) {
            throw new BusinessException("No permission for model: " + request.getModel());
        }
        
        // 2. 选择 Provider
        ModelProvider provider = findProvider(request.getModel());
        
        // 3. 调用模型
        ChatResponse response = provider.chat(request);
        
        // 4. 记录使用量
        recordUsage(apiKey, request, response);
        
        // 5. 扣减配额
        deductQuota(apiKey, response.getUsage().getTotalTokens());
        
        return response;
    }
    
    private boolean hasModelPermission(ApiKey apiKey, String model) {
        String allowedModels = apiKey.getAllowedModels();
        if (allowedModels == null || allowedModels.isEmpty()) {
            return true; // 允许所有模型
        }
        
        // 检查是否在允许列表中
        return Arrays.asList(allowedModels.split(","))
            .contains(model);
    }
    
    private void deductQuota(ApiKey apiKey, Integer tokens) {
        // 使用乐观锁更新配额
        apiKeyMapper.incrementUsedQuota(apiKey.getId(), tokens);
        
        // 清除缓存
        redisTemplate.delete("api_key:" + apiKey.getKeyValue());
    }
}
```

#### 步骤4: Provider API Key 映射

```java
@Configuration
public class ProviderConfig {
    
    @Bean
    public Map<String, String> providerApiKeys() {
        Map<String, String> keys = new HashMap<>();
        
        // 从配置文件或数据库加载真实的 Provider API Keys
        keys.put("openai", env.getProperty("provider.openai.api-key"));
        keys.put("anthropic", env.getProperty("provider.anthropic.api-key"));
        keys.put("volcano", env.getProperty("provider.volcano.api-key"));
        
        return keys;
    }
}

@Component
public class OpenAIProvider implements ModelProvider {
    
    @Autowired
    private Map<String, String> providerApiKeys;
    
    @Override
    public ChatResponse chat(ChatRequest request) {
        // 使用网关配置的真实 API Key
        String realApiKey = providerApiKeys.get("openai");
        
        // 调用 OpenAI API
        return webClient
            .post()
            .uri(openaiApiUrl)
            .header("Authorization", "Bearer " + realApiKey)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ChatResponse.class)
            .block();
    }
}
```

### 配额管理流程

```sql
-- 创建 API Key 时设置配额
INSERT INTO api_key (key_value, total_quota, used_quota) 
VALUES ('sk-xxx', 100000, 0);

-- 每次调用后更新已用配额
UPDATE api_key 
SET used_quota = used_quota + ? 
WHERE id = ? AND used_quota + ? <= total_quota;

-- 查询剩余配额
SELECT (total_quota - used_quota) as remaining_quota 
FROM api_key 
WHERE key_value = ?;
```

### 数据库表结构

```sql
CREATE TABLE `api_key` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `key_value` VARCHAR(128) NOT NULL COMMENT 'API Key 值',
  `key_name` VARCHAR(100) NOT NULL COMMENT 'Key 名称',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `expire_time` DATETIME COMMENT '过期时间',
  `allowed_models` TEXT COMMENT '允许的模型列表（逗号分隔）',
  `total_quota` BIGINT DEFAULT 0 COMMENT '总配额（tokens）',
  `used_quota` BIGINT DEFAULT 0 COMMENT '已用配额（tokens）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key_value` (`key_value`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 完整的调用流程

```
1. 客户端请求
   POST /v1/chat/completions
   Authorization: Bearer sk-e32e51dfe3544852acd4b7c05b051442
   
2. AuthInterceptor 拦截
   ├─ 提取 API Key
   ├─ 验证 Key（Redis缓存 -> MySQL）
   ├─ 检查状态、配额、过期时间
   └─ 检查模型权限
   
3. ChatService 处理
   ├─ 选择对应的 Provider
   ├─ 使用真实的 Provider API Key
   └─ 调用 AI 模型
   
4. 响应处理
   ├─ 记录使用日志
   ├─ 扣减配额
   └─ 返回结果给客户端
```

---

## 总结

### 三个问题的答案

1. **兼容不同模型的调用格式**
   - ✅ 已实现 Provider 适配器模式
   - ✅ 支持 OpenAI、Anthropic
   - 🔄 可扩展支持火山方舟、自有协议

2. **支持流式输出（SSE）**
   - ✅ 已实现 SSE 流式接口
   - ✅ 使用 Reactor Flux 实现
   - ✅ 兼容 OpenAI 流式格式

3. **API Key 统一管理和分发**
   - ✅ 已实现完整的 Key 管理
   - ✅ 支持创建、验证、吊销
   - ✅ 支持配额管理
   - ✅ 支持模型权限控制
   - ✅ 支持过期时间
   - 🔄 可扩展 Redis 缓存优化

### 优势

1. **统一接口** - 客户端只需对接一个网关
2. **灵活扩展** - 新增 Provider 只需实现接口
3. **安全可控** - 真实 API Key 不暴露给客户端
4. **精细管理** - 支持用户级、应用级的权限和配额控制
5. **高性能** - 支持缓存、流式输出
6. **可观测** - 完整的日志和统计

### 下一步优化建议

1. 添加 Redis 缓存提升性能
2. 实现限流控制（基于 Token Bucket）
3. 添加负载均衡和故障转移
4. 实现成本优化（自动选择最便宜的模型）
5. 添加 Webhook 通知
6. 实现 API Key 的自动轮换
