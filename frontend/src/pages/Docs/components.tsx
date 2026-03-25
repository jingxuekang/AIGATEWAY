import { useState, useEffect } from 'react'
import { Select, Card, Tabs, Space, Typography, Divider, Button, Tag } from 'antd'
import { CopyOutlined, CheckOutlined, ApiOutlined } from '@ant-design/icons'
import { useQuery } from 'react-query'
import { request } from '../../api/request'
import { getMyKeys } from '../../api/apiKey'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../../components/PageHeader'
import './index.css'

const { Text, Paragraph } = Typography

const GATEWAY_URL = 'http://localhost:9080'

const PROVIDER_LABELS: Record<string, { label: string; color: string }> = {
  deepseek: { label: 'DeepSeek',     color: '#4096ff' },
  azure:    { label: 'Azure OpenAI', color: '#0078d4' },
  volcano:  { label: '火山引擎',     color: '#ff6b35' },
  openai:   { label: 'OpenAI',       color: '#10a37f' },
  anthropic:{ label: 'Anthropic',    color: '#d4a853' },
}

interface ModelItem { id: number; modelName: string; provider: string; description: string; maxTokens: number }
interface ApiKeyItem { id: number; keyName: string; keyValue: string; allowedModels: string }

const CopyButton = ({ text }: { text: string }) => {
  const [copied, setCopied] = useState(false)
  return (
    <Button size="small" type="text"
      icon={copied ? <CheckOutlined style={{ color: '#52c41a' }} /> : <CopyOutlined />}
      onClick={() => { navigator.clipboard.writeText(text); setCopied(true); setTimeout(() => setCopied(false), 2000) }}
      style={{ color: copied ? '#52c41a' : '#aaa' }}
    >{copied ? '已复制' : '复制'}</Button>
  )
}

const CodeBlock = ({ code, lang }: { code: string; lang: string }) => (
  <div style={{ background: '#1a1a2e', borderRadius: 10, overflow: 'hidden', marginBottom: 16 }}>
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 16px', background: '#16213e', borderBottom: '1px solid #2a2a4a' }}>
      <Text style={{ color: '#7c7cad', fontSize: 12, fontFamily: 'monospace' }}>{lang}</Text>
      <CopyButton text={code} />
    </div>
    <pre style={{ margin: 0, padding: '16px 20px', color: '#e2e8f0', fontSize: 13, fontFamily: "'Cascadia Code','Fira Code',monospace", lineHeight: 1.7, overflowX: 'auto', whiteSpace: 'pre' }}><code>{code}</code></pre>
  </div>
)

export { CodeBlock, CopyButton, GATEWAY_URL, PROVIDER_LABELS }
export type { ModelItem, ApiKeyItem }
