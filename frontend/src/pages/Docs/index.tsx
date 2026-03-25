import { useState, useEffect } from 'react'
import { Select, Card, Tabs, Space, Typography, Divider, Button, Tag } from 'antd'
import { ApiOutlined } from '@ant-design/icons'
import { useQuery } from 'react-query'
import { request } from '../../api/request'
import { getMyKeys } from '../../api/apiKey'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../../components/PageHeader'
import { CodeBlock, CopyButton, GATEWAY_URL, PROVIDER_LABELS, ModelItem, ApiKeyItem } from './components'
import './index.css'

const { Text, Paragraph } = Typography

const Docs = () => {
  const navigate = useNavigate()
  const { data: models } = useQuery<ModelItem[]>('docsModels', () => request.get<any, ModelItem[]>('/api/admin/model-subscriptions'))
  const { data: keys }   = useQuery<ApiKeyItem[]>('myApiKeys', getMyKeys)

  const [selectedModel, setSelectedModel] = useState<string>('')
  const [selectedKey,   setSelectedKey]   = useState<ApiKeyItem | null>(null)

  useEffect(() => { if (models?.length && !selectedModel) setSelectedModel(models[0].modelName) }, [models])
  useEffect(() => { if (keys?.length   && !selectedKey)   setSelectedKey(keys[0]) },   [keys])

  const apiKey  = selectedKey?.keyValue || 'YOUR_API_KEY'
  const model   = selectedModel || 'YOUR_MODEL'
  const baseUrl = `${GATEWAY_URL}/v1`

  const curl    = `curl ${baseUrl}/chat/completions \\\n  -H "Content-Type: application/json" \\\n  -H "Authorization: Bearer ${apiKey}" \\\n  -d '{"model":"${model}","messages":[{"role":"user","content":"你好！"}]}'`
  // 流式：调用显式 stream 端点，避免 /v1/chat/completions 因 Accept 选择到非 SSE handler
  const curlS   = `curl -N ${baseUrl}/chat/completions/stream \\\n  -H "Content-Type: application/json" \\\n  -H "Authorization: Bearer ${apiKey}" \\\n  -H "Accept: text/event-stream" \\\n  -d '{"model":"${model}","messages":[{"role":"user","content":"你好！"}]}'`
  // Windows PowerShell：PowerShell 的 curl 通常是别名（Invoke-WebRequest），
  // 使用 -H @{...} + -Body + -Method Post 便于直接复制运行。
  const curlWin = `curl ${baseUrl}/chat/completions -H @{\n  "Content-Type" = "application/json"\n  "Authorization" = "Bearer ${apiKey}"\n} -Body '{ "model":"${model}","messages":[{"role":"user","content":"你好！"}] }' -Method Post`
  const curlWinS = `curl ${baseUrl}/chat/completions/stream -H @{\n  "Content-Type" = "application/json"\n  "Authorization" = "Bearer ${apiKey}"\n  "Accept" = "text/event-stream"\n} -Body '{ "model":"${model}","messages":[{"role":"user","content":"你好！"}] }' -Method Post`
  // Windows CMD（cmd.exe）
  // 注意：这里使用 curl.exe（避免 PowerShell/系统 alias 干扰）+ JSON 内部双引号使用 \" 转义。
  const curlCmd = `curl.exe -X POST ${baseUrl}/chat/completions -H "Content-Type: application/json" -H "Authorization: Bearer ${apiKey}" -d "{\\"model\\":\\"${model}\\",\\"messages\\":[{\\"role\\":\\"user\\",\\"content\\":\\"你好！\\"}]}" `
  const curlCmdS = `curl.exe -N -X POST ${baseUrl}/chat/completions/stream -H "Content-Type: application/json" -H "Authorization: Bearer ${apiKey}" -H "Accept: text/event-stream" -d "{\\"model\\":\\"${model}\\",\\"messages\\":[{\\"role\\":\\"user\\",\\"content\\":\\"你好！\\"}]}" `
  const py      = `from openai import OpenAI\nclient = OpenAI(api_key="${apiKey}", base_url="${baseUrl}")\nrsp = client.chat.completions.create(model="${model}", messages=[{"role":"user","content":"你好！"}])\nprint(rsp.choices[0].message.content)`
  const pyS     = `from openai import OpenAI\nclient = OpenAI(api_key="${apiKey}", base_url="${baseUrl}")\nstream = client.chat.completions.create(model="${model}", messages=[{"role":"user","content":"你好！"}], stream=True)\nfor chunk in stream:\n    if chunk.choices[0].delta.content: print(chunk.choices[0].delta.content, end="", flush=True)`
  const node    = `import OpenAI from 'openai';\nconst client = new OpenAI({ apiKey: '${apiKey}', baseURL: '${baseUrl}' });\nconst rsp = await client.chat.completions.create({ model: '${model}', messages: [{ role: 'user', content: '你好！' }] });\nconsole.log(rsp.choices[0].message.content);`
  const nodeS   = `import OpenAI from 'openai';\nconst client = new OpenAI({ apiKey: '${apiKey}', baseURL: '${baseUrl}' });\nconst stream = await client.chat.completions.create({ model: '${model}', messages: [{ role: 'user', content: '你好！' }], stream: true });\nfor await (const chunk of stream) process.stdout.write(chunk.choices[0]?.delta?.content || '');`

  const currentModel = (models || []).find(m => m.modelName === selectedModel)
  const pi = PROVIDER_LABELS[currentModel?.provider || '']

  const isWindows = typeof navigator !== 'undefined' && /Win/i.test(navigator.userAgent)

  const langItems = [
    {
      key: 'curl',
      label: <b style={{ fontFamily: 'monospace' }}>cURL</b>,
      children: (
        <Tabs
          defaultActiveKey={isWindows ? 'winps' : 'unix'}
          items={[
            {
              key: 'unix',
              label: 'Linux / macOS (bash)',
              children: (
                <>
                  <p style={{ color: '#666', marginBottom: 8 }}>标准请求</p>
                  <CodeBlock code={curl} lang="bash" />
                  <p style={{ color: '#666', marginBottom: 8 }}>流式输出</p>
                  <CodeBlock code={curlS} lang="bash (stream)" />
                </>
              ),
            },
            {
              key: 'winps',
              label: 'Windows (PowerShell)',
              children: (
                <>
                  <p style={{ color: '#666', marginBottom: 8 }}>标准请求</p>
                  <CodeBlock code={curlWin} lang="powershell" />
                  <p style={{ color: '#666', marginBottom: 8 }}>流式输出</p>
                  <CodeBlock code={curlWinS} lang="powershell (stream)" />
                </>
              ),
            },
            {
              key: 'cmd',
              label: 'Windows (CMD)',
              children: (
                <>
                  <p style={{ color: '#666', marginBottom: 8 }}>标准请求</p>
                  <CodeBlock code={curlCmd} lang="shell (cmd)" />
                  <p style={{ color: '#666', marginBottom: 8 }}>流式输出</p>
                  <CodeBlock code={curlCmdS} lang="shell (cmd) (stream)" />
                </>
              ),
            },
          ]}
        />
      ),
    },
    { key: 'python', label: <b style={{fontFamily:'monospace'}}>Python</b>,  children: <><p style={{color:'#666',marginBottom:8}}>安装：<Text code>pip install openai</Text></p><CodeBlock code={py} lang="python" /><p style={{color:'#666',marginBottom:8}}>流式输出</p><CodeBlock code={pyS} lang="python (stream)" /></> },
    { key: 'node',   label: <b style={{fontFamily:'monospace'}}>Node.js</b>, children: <><p style={{color:'#666',marginBottom:8}}>安装：<Text code>npm install openai</Text></p><CodeBlock code={node} lang="typescript" /><p style={{color:'#666',marginBottom:8}}>流式输出</p><CodeBlock code={nodeS} lang="typescript (stream)" /></> },
  ]

  return (
    <div className="docs">
      <PageHeader title="快捷接入" subtitle="选择模型和 API Key，自动生成可直接运行的代码示例" />

      {/* 步骤条 */}
      <div className="docs-steps">
        {['选择模型', '选择 API Key', '复制代码', '开始调用'].map((s, i) => (
          <div key={i} className="docs-step">
            <div className="docs-step-num">{i+1}</div>
            <span className="docs-step-label">{s}</span>
            {i < 3 && <div className="docs-step-line" />}
          </div>
        ))}
      </div>

      <div className="docs-layout">
        {/* 左侧配置面板 */}
        <div className="docs-sidebar">
          <Card className="docs-config-card" title={<Space><ApiOutlined />接入配置</Space>}>
            <div className="docs-field">
              <label className="docs-label">网关地址</label>
              <div className="docs-value-row">
                <Text code style={{flex:1,fontSize:12}}>{baseUrl}</Text>
                <CopyButton text={baseUrl} />
              </div>
            </div>
            <Divider style={{margin:'14px 0'}} />
            <div className="docs-field">
              <label className="docs-label"><span style={{color:'#ff4d4f'}}>*</span> 选择模型</label>
              <Select style={{width:'100%'}} value={selectedModel||undefined} onChange={setSelectedModel}
                placeholder={models?.length===0 ? '暂无模型' : '请选择模型'} optionLabelProp="label">
                {(models||[]).map(m => {
                  const p = PROVIDER_LABELS[m.provider]
                  return (
                    <Select.Option key={m.modelName} value={m.modelName} label={m.modelName}>
                      <div style={{display:'flex',justifyContent:'space-between',alignItems:'center'}}>
                        <span style={{fontFamily:'monospace',fontSize:13}}>{m.modelName}</span>
                        <Tag color={p?.color||'default'} style={{fontSize:11,margin:0}}>{p?.label||m.provider}</Tag>
                      </div>
                    </Select.Option>
                  )
                })}
              </Select>
              {currentModel && (
                <div className="docs-model-info">
                  {pi && <Tag color={pi.color}>{pi.label}</Tag>}
                  {currentModel.maxTokens && <Text type="secondary" style={{fontSize:12}}> 最大 {currentModel.maxTokens.toLocaleString()} tokens</Text>}
                  {currentModel.description && <Paragraph type="secondary" style={{fontSize:12,marginBottom:0,marginTop:4}}>{currentModel.description}</Paragraph>}
                </div>
              )}
              {!models?.length && <Button size="small" type="link" onClick={()=>navigate('/model-subscriptions')}>去订阅模型 →</Button>}
            </div>
            <Divider style={{margin:'14px 0'}} />
            <div className="docs-field">
              <label className="docs-label"><span style={{color:'#ff4d4f'}}>*</span> 选择 API Key</label>
              <Select style={{width:'100%'}} value={selectedKey?.id} disabled={!keys?.length}
                onChange={(id)=>setSelectedKey((keys||[]).find(k=>k.id===id)||null)}
                placeholder={keys?.length===0 ? '暂无 Key，请先申请' : '选择 API Key'}>
                {(keys||[]).map(k => (
                  <Select.Option key={k.id} value={k.id}>
                    <div style={{display:'flex',justifyContent:'space-between'}}>
                      <span>{k.keyName}</span>
                      <Text type="secondary" style={{fontSize:11,fontFamily:'monospace'}}>{k.keyValue?.substring(0,12)}...</Text>
                    </div>
                  </Select.Option>
                ))}
              </Select>
              {selectedKey && (
                <div className="docs-key-row">
                  <Text code style={{fontSize:11,flex:1,wordBreak:'break-all'}}>{selectedKey.keyValue?.substring(0,28)}...</Text>
                  <CopyButton text={selectedKey.keyValue} />
                </div>
              )}
              {!keys?.length && <Button size="small" type="link" onClick={()=>navigate('/key-applications')}>去申请 Key →</Button>}
            </div>
          </Card>
        </div>

        {/* 右侧代码面板 */}
        <div className="docs-main">
          <div className="docs-info-cards">
            <div className="docs-info-card">
              <div className="docs-info-card-label">Base URL</div>
              <div className="docs-info-card-value">{baseUrl}</div>
            </div>
            <div className="docs-info-card">
              <div className="docs-info-card-label">Model</div>
              <div className="docs-info-card-value">{model}</div>
            </div>
          </div>
          <Card title={<Space><span style={{fontFamily:'monospace',fontSize:15}}>{'</>'}</span>代码示例</Space>} bordered={false}
            style={{borderRadius:12,boxShadow:'0 2px 12px rgba(0,0,0,0.06)'}}>
            <Tabs items={langItems} />
          </Card>
        </div>
      </div>
    </div>
  )
}

export default Docs
