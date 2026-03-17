# API 使用示例

本文档提供了 AI Gateway 的完整使用示例，包括不同模型的调用方式、流式输出、API Key 管理等。

## 目录

1. [API Key 管理](#api-key-管理)
2. [OpenAI 模型调用](#openai-模型调用)
3. [Anthropic Claude 调用](#anthropic-claude-调用)
4. [火山方舟调用](#火山方舟调用)
5. [流式输出](#流式输出)
6. [错误处理](#错误处理)

---

## API Key 管理

### 1. 创建 API Key

```bash
curl -X POST http://localhost:9081/api/admin/keys \
  -H "Content-Type: application/json" \
  -d '{
    "keyName": "生产环境Key",
    "userId": 1,
    "tenantId": "tenant-prod",
    "appId": "app-001",
    "allowedModels": "gpt-4,gpt-3.5-turbo,claude-3-opus,doubao-pro-32k",
    "totalQuota": 1000000,
    "expireTime": "2025-12-31T23:59:59"
  }'
```

响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "keyValue": "sk-e32e51dfe3544852acd4b7c05b051442",
    "keyName": "生产环境Key",
    "userId": 1,
    "status": 1,
    "totalQuota": 1000000,
    "usedQuota": 0,
    "createTime": "2026-03-13T10:00:00"
  }
}
```

### 2. 查询用户的 API Keys

```bash
curl http://localhost:9081/api/admin/keys/user/1
```

### 3. 吊销 API Key

```bash
curl -X PUT http://localhost:9081/api/admin/keys/1/revoke
```

---

## OpenAI 模型调用

### 1. 基础调用（gpt-3.5-turbo）

```bash
curl -X POST http://localhost:9080/v1/chat/completions \
  -H "Authorization: Bearer sk-e32e51dfe3544852acd4b7c05b051442" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [
      {
        "role": "system",
        "content": "你是一个有帮助的助手。"
      },
      {
        "role": "user",
        "content": "介绍一下人工智能"
      }
    ],
    "temperature": 0.7,
    "max_tokens": 1000
  }'
```

响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "chatcmpl-123",
    "object": "chat.completion",
    "created": 1677652288,
    "model": "gpt-3.5-turbo",
    "choices": [
      {
        "index": 0,
        "message": {
          "role": "assistant",
          "content": "人工智能（AI）是计算机科学的一个分支..."
        },
        "finishReason": "stop"
      }
    ],
    "usage": {
      "promptTokens": 20,
      "completionTokens": 150,
      "totalTokens": 170
    }
  }
}
```

### 2. GPT-4 调用

```bash
curl -X POST http://localhost:9080/v1/chat/completions \
  -H "Authorization: Bearer sk-e32e51dfe3544852acd4b7c05b051442" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4",
    "messages": [
      {
        "role": "user",
        "content": "解释量子计算的基本原理"
      }
    ],
    "temperature": 0.5,
    "max_tokens": 2000
  }'
```

### 3. 多轮对话

```bash
curl -X POST http://localhost:9080/v1/chat/completions \
  -H "Authorization: Bearer sk-e32e51dfe3544852acd4b7c05b051442" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [
      {
        "role": "user",
        "content": "什么是机器学习？"
      },
      {
        "role": "assistant",
        "content": "机器学习是人工智能的一个子领域..."
      },
      {
        "role": "user",
        "content": "它有哪些应用场景？"
      }
    ]
  }'
```

---

## Anthropic Claude 调用

### 1. Claude 3 Opus 调用

```bash
curl -X POST http://localhost:9080/v1/chat/completions \
  -H "Authorization: Bearer sk-e32e51dfe3544852acd4b7c05b051442" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "claude-3-opus",
    "messages": [
      {
        "role": "user",
        "content": "写一首关于春天的诗"
      }
    ],
    "max_tokens": 1024,
    "temperature": 0.8
  }'
```

### 2. Claude 3 Sonnet 调用

```bash
curl -X POST http://localhost:9080/v1/chat/completions \
  -H "Authorization: Bearer sk-e32e51dfe3544852acd4b7c05b051442" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "claude-3-sonnet",
    "messages": [
      {
        "role": "user",
        "content": "分析这段代码的时间复杂度：\n\nfor i in range(n):\n    for j in range(n):\n        print(i, j)"
      }
    ]
  }'
```

---

## 火山方舟调用

### 1. 豆包 Pro 调用

```bash
curl -X POST http://localhost:9080/v1/chat/completions \
  -H "Authorization: Bearer sk-e32e51dfe3544852acd4b7c05b051442" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "doubao-pro-32k",
    "messages": [
      {
        "role": "user",
        "content": "介绍一下北京的旅游景点"
      }
    ],
    "temperature": 0.7,
    "max_tokens": 2000
  }'
```

### 2. 豆包 Lite 调用

```bash
curl -X POST http://localhost:9080/v1/chat/completions \
  -H "Authorization: Bearer sk-e32e51dfe3544852acd4b7c05b051442" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "doubao-lite-32k",
    "messages": [
      {
        "role": "user",
        "content": "今天天气怎么样？"
      }
    ]
  }'
```

---

## 流式输出

### 1. OpenAI 流式调用

```bash
curl -X POST http://localhost:9080/v1/chat/completions/stream \
  -H "Authorization: Bearer sk-e32e51dfe3544852acd4b7c05b051442" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [
      {
        "role": "user",
        "content": "讲一个有趣的故事"
      }
    ]
  }'
```

响应（SSE 格式）：
```
data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{"content":"从前"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{"content":"有一个"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{"content":"小村庄"},"finish_reason":null}]}

...

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

data: [DONE]
```

### 2. JavaScript 客户端示例

```javascript
async function streamChat() {
  const response = await fetch('http://localhost:9080/v1/chat/completions/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer sk-e32e51dfe3544852acd4b7c05b051442'
    },
    body: JSON.stringify({
      model: 'gpt-3.5-turbo',
      messages: [
        { role: 'user', content: '讲一个笑话' }
      ]
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
          return;
        }
        
        try {
          const json = JSON.parse(data);
          const content = json.choices[0].delta.content;
          if (content) {
            process.stdout.write(content);
          }
        } catch (e) {
          // 忽略解析错误
        }
      }
    }
  }
}

streamChat();
```

### 3. Python 客户端示例

```python
import requests
import json

def stream_chat():
    response = requests.post(
        'http://localhost:9080/v1/chat/completions/stream',
        headers={
            'Content-Type': 'application/json',
            'Authorization': 'Bearer sk-e32e51dfe3544852acd4b7c05b051442'
        },
        json={
            'model': 'gpt-3.5-turbo',
            'messages': [
                {'role': 'user', 'content': '写一首诗'}
            ]
        },
        stream=True
    )

    for line in response.iter_lines():
        if line:
            line = line.decode('utf-8')
            if line.startswith('data: '):
                data = line[6:]
                if data == '[DONE]':
                    print('\nStream completed')
                    break
                
                try:
                    chunk = json.loads(data)
                    content = chunk['choices'][0]['delta'].get('content', '')
                    if content:
                        print(content, end='', flush=True)
                except json.JSONDecodeError:
                    pass

stream_chat()
```

---

## 错误处理

### 1. API Key 无效

请求：
```bash
curl -X POST http://localhost:9080/v1/chat/completions \
  -H "Authorization: Bearer invalid-key" \
  -H "Content-Type: application/json" \
  -d '{"model":"gpt-3.5-turbo","messages":[{"role":"user","content":"Hello"}]}'
```

响应：
```json
{
  "code": 401,
  "message": "Invalid API Key",
  "timestamp": 1773368857116
}
```

### 2. 配额不足

响应：
```json
{
  "code": 429,
  "message": "Quota exceeded",
  "data": {
    "totalQuota": 100000,
    "usedQuota": 100000,
    "remainingQuota": 0
  },
  "timestamp": 1773368857116
}
```

### 3. 模型不支持

请求：
```bash
curl -X POST http://localhost:9080/v1/chat/completions \
  -H "Authorization: Bearer sk-e32e51dfe3544852acd4b7c05b051442" \
  -H "Content-Type: application/json" \
  -d '{"model":"unsupported-model","messages":[{"role":"user","content":"Hello"}]}'
```

响应：
```json
{
  "code": 400,
  "message": "不支持的模型: unsupported-model",
  "timestamp": 1773368857116
}
```

### 4. 没有模型权限

响应：
```json
{
  "code": 403,
  "message": "No permission for model: gpt-4",
  "data": {
    "allowedModels": ["gpt-3.5-turbo", "claude-3-sonnet"]
  },
  "timestamp": 1773368857116
}
```

### 5. API Key 已过期

响应：
```json
{
  "code": 401,
  "message": "API Key expired",
  "data": {
    "expireTime": "2024-12-31T23:59:59"
  },
  "timestamp": 1773368857116
}
```

---

## 高级用法

### 1. 设置超时时间

```bash
curl -X POST http://localhost:9080/v1/chat/completions \
  -H "Authorization: Bearer sk-e32e51dfe3544852acd4b7c05b051442" \
  -H "Content-Type: application/json" \
  -H "X-Request-Timeout: 30000" \
  -d '{
    "model": "gpt-4",
    "messages": [{"role": "user", "content": "复杂问题"}]
  }'
```

### 2. 自定义请求 ID

```bash
curl -X POST http://localhost:9080/v1/chat/completions \
  -H "Authorization: Bearer sk-e32e51dfe3544852acd4b7c05b051442" \
  -H "Content-Type: application/json" \
  -H "X-Request-ID: req-12345" \
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [{"role": "user", "content": "Hello"}]
  }'
```

### 3. 批量请求

```bash
# 使用 GNU Parallel 或 xargs 进行并发请求
seq 1 10 | parallel -j 5 'curl -X POST http://localhost:9080/v1/chat/completions \
  -H "Authorization: Bearer sk-e32e51dfe3544852acd4b7c05b051442" \
  -H "Content-Type: application/json" \
  -d "{\"model\":\"gpt-3.5-turbo\",\"messages\":[{\"role\":\"user\",\"content\":\"Request {}\"}]}"'
```

---

## 统计和监控

### 1. 查询使用统计

```bash
curl http://localhost:9081/api/admin/dashboard/statistics
```

响应：
```json
{
  "code": 200,
  "data": {
    "totalModels": 4,
    "totalKeys": 5,
    "totalRequests": 1250,
    "totalTokens": 125000
  }
}
```

### 2. 查询调用日志

```bash
curl "http://localhost:9081/api/admin/logs?page=1&pageSize=20&model=gpt-4"
```

### 3. 查询 API Key 使用情况

```bash
curl http://localhost:9081/api/admin/keys/user/1
```

响应：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "keyName": "生产环境Key",
      "totalQuota": 1000000,
      "usedQuota": 125000,
      "remainingQuota": 875000,
      "usagePercentage": 12.5
    }
  ]
}
```

---

## 最佳实践

1. **API Key 安全**
   - 不要在客户端代码中硬编码 API Key
   - 使用环境变量存储 API Key
   - 定期轮换 API Key

2. **错误重试**
   - 实现指数退避重试策略
   - 对 429 (配额不足) 和 503 (服务不可用) 进行重试

3. **流式输出**
   - 对于长文本生成，优先使用流式接口
   - 提升用户体验

4. **配额管理**
   - 监控配额使用情况
   - 设置告警阈值
   - 及时充值或调整配额

5. **模型选择**
   - 根据任务复杂度选择合适的模型
   - 简单任务使用 gpt-3.5-turbo 或 doubao-lite
   - 复杂任务使用 gpt-4 或 claude-3-opus
