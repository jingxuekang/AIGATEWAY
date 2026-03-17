import { Tabs, Card, Typography } from 'antd'
import PageHeader from '../../components/PageHeader'
import './index.css'

const { Paragraph, Title, Text } = Typography

const Docs = () => {
  const quickStart = (
    <Card>
      <Title level={3}>快速开始</Title>
      <Paragraph>
        <Title level={4}>1. 获取 API Key</Title>
        <Text>在 API Keys 页面创建或申请一个 API Key</Text>
      </Paragraph>

      <Paragraph>
        <Title level={4}>2. 发起请求</Title>
        <Text code>POST https://your-gateway.com/v1/chat/completions</Text>
        <pre style={{ background: '#f5f5f5', padding: '16px', borderRadius: '4px' }}>
{`curl https://your-gateway.com/v1/chat/completions \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer YOUR_API_KEY" \\
  -d '{
    "model": "gpt-4",
    "messages": [
      {"role": "user", "content": "Hello!"}
    ]
  }'`}
        </pre>
      </Paragraph>

      <Paragraph>
        <Title level={4}>3. 流式输出</Title>
        <pre style={{ background: '#f5f5f5', padding: '16px', borderRadius: '4px' }}>
{`curl https://your-gateway.com/v1/chat/completions \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer YOUR_API_KEY" \\
  -d '{
    "model": "gpt-4",
    "messages": [
      {"role": "user", "content": "Hello!"}
    ],
    "stream": true
  }'`}
        </pre>
      </Paragraph>
    </Card>
  )

  const apiReference = (
    <Card>
      <Title level={3}>API 参考</Title>
      
      <Title level={4}>Chat Completions</Title>
      <Paragraph>
        <Text strong>Endpoint:</Text> <Text code>POST /v1/chat/completions</Text>
      </Paragraph>
      <Paragraph>
        <Text strong>Headers:</Text>
        <ul>
          <li><Text code>Authorization: Bearer YOUR_API_KEY</Text></li>
          <li><Text code>Content-Type: application/json</Text></li>
        </ul>
      </Paragraph>
      <Paragraph>
        <Text strong>Request Body:</Text>
        <pre style={{ background: '#f5f5f5', padding: '16px', borderRadius: '4px' }}>
{`{
  "model": "gpt-4",           // 必填：模型名称
  "messages": [               // 必填：对话消息
    {
      "role": "system",       // system | user | assistant
      "content": "You are a helpful assistant."
    },
    {
      "role": "user",
      "content": "Hello!"
    }
  ],
  "temperature": 0.7,         // 可选：0-2，默认 1
  "max_tokens": 1000,         // 可选：最大生成 token 数
  "stream": false             // 可选：是否流式输出
}`}
        </pre>
      </Paragraph>
    </Card>
  )

  const sdkExamples = (
    <Card>
      <Title level={3}>SDK 示例</Title>
      
      <Title level={4}>Python</Title>
      <pre style={{ background: '#f5f5f5', padding: '16px', borderRadius: '4px' }}>
{`from openai import OpenAI

client = OpenAI(
    api_key="YOUR_API_KEY",
    base_url="https://your-gateway.com/v1"
)

response = client.chat.completions.create(
    model="gpt-4",
    messages=[
        {"role": "user", "content": "Hello!"}
    ]
)

print(response.choices[0].message.content)`}
      </pre>

      <Title level={4}>Node.js</Title>
      <pre style={{ background: '#f5f5f5', padding: '16px', borderRadius: '4px' }}>
{`import OpenAI from 'openai';

const client = new OpenAI({
  apiKey: 'YOUR_API_KEY',
  baseURL: 'https://your-gateway.com/v1',
});

const response = await client.chat.completions.create({
  model: 'gpt-4',
  messages: [
    { role: 'user', content: 'Hello!' }
  ],
});

console.log(response.choices[0].message.content);`}
      </pre>
    </Card>
  )

  const items = [
    {
      key: 'quickstart',
      label: '快速开始',
      children: quickStart,
    },
    {
      key: 'api',
      label: 'API 参考',
      children: apiReference,
    },
    {
      key: 'sdk',
      label: 'SDK 示例',
      children: sdkExamples,
    },
  ]

  return (
    <div className="docs">
      <PageHeader title="接口文档" />
      <Tabs items={items} />
    </div>
  )
}

export default Docs
