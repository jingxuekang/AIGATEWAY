# DeepSeek API 集成指南

## 概述

本项目已集成 DeepSeek API，支持调用 DeepSeek 的 AI 模型。DeepSeek API 兼容 OpenAI 格式，使用简单。

## 支持的模型

- `deepseek-chat`：DeepSeek 对话模型
- `deepseek-coder`：DeepSeek 代码模型
- 其他以 `deepseek-` 开头的模型

## 配置步骤

### 1. 获取 API Key

1. 访问 [DeepSeek 平台](https://platform.deepseek.com/)
2. 注册/登录账号
3. 进入 [API Keys 页面](https://platform.deepseek.com/api_keys)
4. 创建新的 API Key 并复制

### 2. 配置 API Key

有两种方式配置 API Key：

#### 方式一：环境变量（推荐）

在启动应用前设置环境变量：

**Windows (PowerShell):**
```powershell
$env:DEEPSEEK_API_KEY="sk-your-api-key-here"
```

**Windows (CMD):**
```cmd
set DEEPSEEK_API_KEY=sk-your-api-key-here
```

**Linux/Mac:**
```bash
export DEEPSEEK_API_KEY=sk-your-api-key-here
```

#### 方式二：修改配置文件

编辑 `gateway-core/src/main/resources/application.yml`：

```yaml
deepseek:
  api:
    key: sk-your-api-key-here
    base-url: https://api.deepseek.com
```

⚠️ **注意**：不要将 API Key 提交到 Git 仓库！

### 3. 在管理后台添加渠道

1. 启动服务后，访问管理后台：http://localhost:3001
2. 进入"渠道管理"页面
3. 点击"新建渠道"
4. 填写以下信息：
   - 渠道名称：`DeepSeek-Primary`
   - Provider：选择 `deepseek`
   - Base URL：`https://api.deepseek.com`
   - API Key：你的 DeepSeek API Key
   - 权重：`100`
   - 最大并发数：`100`
   - 超时时间：`30000`（毫秒）
   - 状态：启用

### 4. 添加模型配置

在"模型管理/定价"页面添加 DeepSeek 模型：

1. 点击"新建模型"
2. 填写信息：
   - 模型名称：`deepseek-chat`
   - Provider：`deepseek`
   - 输入价格：`1.0`（元/1M tokens）
   - 输出价格：`2.0`（元/1M tokens）
   - 状态：启用

## 使用示例

### 通过 Playground 测试

1. 访问管理后台的 "Playground" 页面
2. 选择模型：`deepseek-chat`
3. 输入消息进行测试

### 通过 API 调用

#### 非流式调用

```bash
curl http://localhost:9080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-gateway-api-key" \
  -d '{
    "model": "deepseek-chat",
    "messages": [
      {
        "role": "user",
        "content": "你好，请介绍一下你自己"
      }
    ]
  }'
```

#### 流式调用

```bash
curl http://localhost:9080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-gateway-api-key" \
  -d '{
    "model": "deepseek-chat",
    "messages": [
      {
        "role": "user",
        "content": "写一个 Python 快速排序"
      }
    ],
    "stream": true
  }'
```

### Python 示例

```python
import requests

url = "http://localhost:9080/v1/chat/completions"
headers = {
    "Content-Type": "application/json",
    "Authorization": "Bearer your-gateway-api-key"
}

data = {
    "model": "deepseek-chat",
    "messages": [
        {"role": "user", "content": "你好"}
    ]
}

response = requests.post(url, headers=headers, json=data)
print(response.json())
```

### JavaScript 示例

```javascript
const response = await fetch('http://localhost:9080/v1/chat/completions', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer your-gateway-api-key'
  },
  body: JSON.stringify({
    model: 'deepseek-chat',
    messages: [
      { role: 'user', content: '你好' }
    ]
  })
});

const data = await response.json();
console.log(data);
```

## 支持的参数

DeepSeek API 支持以下参数：

- `model`：模型名称（必填）
- `messages`：对话消息列表（必填）
- `temperature`：温度参数，控制随机性（0-2，默认 1）
- `max_tokens`：最大生成 token 数
- `top_p`：核采样参数（0-1）
- `frequency_penalty`：频率惩罚（-2 到 2）
- `presence_penalty`：存在惩罚（-2 到 2）
- `stop`：停止序列
- `stream`：是否流式返回（true/false）

## 价格说明

DeepSeek 的定价（截至文档编写时）：

| 模型 | 输入价格 | 输出价格 |
|------|----------|----------|
| deepseek-chat | ¥1/1M tokens | ¥2/1M tokens |
| deepseek-coder | ¥1/1M tokens | ¥2/1M tokens |

具体价格请参考 [DeepSeek 官方定价](https://platform.deepseek.com/pricing)

## 故障排查

### 问题 1：返回 Mock 响应

**原因**：API Key 未配置或配置错误

**解决**：
1. 检查环境变量 `DEEPSEEK_API_KEY` 是否设置
2. 检查 `application.yml` 中的配置
3. 重启服务

### 问题 2：API 调用超时

**原因**：网络问题或 DeepSeek 服务响应慢

**解决**：
1. 检查网络连接
2. 增加超时时间配置
3. 检查 DeepSeek 服务状态

### 问题 3：401 Unauthorized

**原因**：API Key 无效或过期

**解决**：
1. 检查 API Key 是否正确
2. 在 DeepSeek 平台检查 API Key 状态
3. 重新生成 API Key

### 问题 4：429 Too Many Requests

**原因**：超过速率限制

**解决**：
1. 降低请求频率
2. 升级 DeepSeek 账号套餐
3. 配置多个渠道进行负载均衡

## 高级配置

### 配置多个 DeepSeek 渠道

可以配置多个 DeepSeek 渠道实现负载均衡：

1. 创建多个渠道，使用不同的 API Key
2. 设置不同的权重
3. 系统会自动根据权重分配请求

### 自定义 Base URL

如果使用 DeepSeek 的代理服务，可以修改 Base URL：

```yaml
deepseek:
  api:
    key: sk-your-api-key
    base-url: https://your-proxy-url.com
```

## 相关链接

- [DeepSeek 官网](https://www.deepseek.com/)
- [DeepSeek 平台](https://platform.deepseek.com/)
- [DeepSeek API 文档](https://platform.deepseek.com/api-docs/)
- [DeepSeek 定价](https://platform.deepseek.com/pricing)

## 技术实现

### Provider 实现

DeepSeek Provider 位于：
```
gateway-provider/src/main/java/com/aigateway/provider/adapter/DeepSeekProvider.java
```

主要特性：
- 兼容 OpenAI API 格式
- 支持同步和流式调用
- 自动处理请求/响应转换
- 完善的错误处理
- 未配置 API Key 时返回 Mock 响应

### 配置文件

配置位于：
```
gateway-core/src/main/resources/application.yml
```

支持环境变量覆盖，便于部署。
