import { useState, useMemo, useEffect, useRef } from 'react'
import { Card, Input, Button, Select, Space, List, message, Spin, Tag, Alert, Upload, Modal } from 'antd'
import { SendOutlined, PictureOutlined, KeyOutlined, ApiOutlined } from '@ant-design/icons'
import { useQuery } from 'react-query'
import { request } from '../../api/request'
import './index.css'

const { TextArea } = Input

interface ModelOption {
  id: number
  modelName: string
  provider: string
  description: string
}

interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: any
}

// /api/admin/keys/validate 返回数据结构（用于前端校验模型可用范围）
interface ApiKeyInfo {
  id: number
  keyValue?: string
  keyName?: string
  userId: number
  tenantId?: string
  appId?: string
  status: number
  expireTime?: string
  allowedModels?: string[]
  totalQuota?: number
  usedQuota?: number
}

const renderContent = (content: any) => {
  if (typeof content === 'string') return <span>{content}</span>
  if (Array.isArray(content)) {
    return (
      <div>
        {content.map((part: any, i: number) => {
          if (part.type === 'text') return <span key={i}>{part.text}</span>
          if (part.type === 'input_text') return <span key={i}>{part.text}</span>
          if (part.type === 'image_url') return (
            <img key={i} src={part.image_url?.url} alt="attached"
              style={{ maxWidth: 300, maxHeight: 300, display: 'block', marginTop: 8, borderRadius: 8 }} />
          )
          if (part.type === 'input_image') {
            // Volcano/豆包：image_url 字段是字符串（可能是 https URL 或 dataURL）
            const src = typeof part.image_url === 'string' ? part.image_url : part.image_url?.url
            return (
              <img
                key={i}
                src={src}
                alt="attached"
                style={{ maxWidth: 300, maxHeight: 300, display: 'block', marginTop: 8, borderRadius: 8 }}
              />
            )
          }
          return null
        })}
      </div>
    )
  }
  return <span>{String(content)}</span>
}

const Chat = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [model, setModel] = useState<string>('')
  const [selectedKeyId, setSelectedKeyId] = useState<number | null>(null)
  const [manualKeyValue, setManualKeyValue] = useState<string>('')
  const [manualKeyInfo, setManualKeyInfo] = useState<ApiKeyInfo | null>(null)
  const [validatedKeyValue, setValidatedKeyValue] = useState<string>('')
  const [loading, setLoading] = useState(false)
  const [validatingKey, setValidatingKey] = useState(false)
  const [imageUrl, setImageUrl] = useState('')
  const [imageUploading, setImageUploading] = useState(false)

  const textareaRef = useRef<any>(null)

  const role = localStorage.getItem('role') || 'user'
  const isAdmin = role === 'admin'

  const { data: modelOptions, isLoading: modelsLoading } = useQuery<ModelOption[]>(
    'mySubscribedModels',
    () => isAdmin
      ? request.get<any, ModelOption[]>('/api/admin/model-subscriptions')
      : request.get<any, ModelOption[]>('/api/admin/model-subscriptions/my'),
    { staleTime: 0, cacheTime: 0 }
  )

  const hasKey = isAdmin || !!manualKeyInfo

  /** 普通用户：取该手动 Key 的 allowedModels（不再和已订阅做交集），用于决定下拉展示范围 */
  const allowedModelsArr = useMemo(() => {
    if (isAdmin) return null
    const allowed = manualKeyInfo?.allowedModels
    if (!allowed || allowed.length === 0) return null
    return allowed.map(s => String(s).trim()).filter(Boolean)
  }, [isAdmin, manualKeyInfo])

  const selectOptions = useMemo(() => {
    const base = modelOptions || []
    const mappedFromModelOptions = (list: ModelOption[]) =>
      list.map(m => ({ value: m.modelName, label: `${m.modelName} (${m.provider})` }))

    if (isAdmin) return mappedFromModelOptions(base)
    if (!allowedModelsArr) return mappedFromModelOptions(base)

    // 按 allowedModelsArr 展示（只有 1 个时也只展示它；多于 1 个时展示全部）
    const byName = new Map<string, ModelOption>(base.map(m => [m.modelName, m]))
    return allowedModelsArr.map(name => {
      const opt = byName.get(name)
      return { value: name, label: opt ? `${opt.modelName} (${opt.provider})` : name }
    })
  }, [modelOptions, isAdmin, allowedModelsArr])

  const singleAllowedModel = !isAdmin && allowedModelsArr && allowedModelsArr.length === 1

  useEffect(() => {
    if (selectOptions.length === 0) {
      setModel('')
      return
    }
    const ok = selectOptions.some(o => o.value === model)
    if (!ok) setModel(selectOptions[0].value)
  }, [selectOptions, model])

  const isMultiModal = (() => {
    const m = (model || '').trim().toLowerCase()
    return (
      m.startsWith('doubao-') ||
      m.startsWith('ep-') ||
      m.startsWith('volcano-') ||
      m.startsWith('glm-4.6v') ||
      m.startsWith('gpt-4-vision') ||
      m.startsWith('claude-3')
    )
  })()
  // 注意：gateway-core 的 /v1/responses 期望的多模态格式是 OpenAI 风格（content: image_url + text）
  const isDataUrl = !!imageUrl && imageUrl.startsWith('data:')

  const fileToDataUrl = (file: File) => new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = (e) => reject(e)
    reader.readAsDataURL(file)
  })

  const handleTextareaPaste = async (e: React.ClipboardEvent<HTMLTextAreaElement>) => {
    const cd = e.clipboardData
    const files = Array.from(cd.files || [])
    const imageFile = files.find((f) => f.type && f.type.startsWith('image/'))
    if (!imageFile) return

    if (!model) return

    if (!isMultiModal) {
      e.preventDefault()
      Modal.warning({
        title: '当前模型不支持图片多模态',
        content: '请选择支持图片的多模态模型后再粘贴图片。',
      })
      return
    }

    // Base64 数据会显著增大体积；限制一下避免超出网关/内存
    const maxMb = 5
    if (imageFile.size > maxMb * 1024 * 1024) {
      e.preventDefault()
      message.error(`图片过大（>${maxMb}MB），请换小一点的图。`)
      return
    }

    e.preventDefault()
    try {
      setImageUploading(true)
      const url = await fileToDataUrl(imageFile)
      setImageUrl(url)
      const clipboardText = cd.getData('text/plain') || ''
      if (clipboardText.trim()) setInput(prev => (prev.trim() ? prev : clipboardText))
      message.success('图片已粘贴（将以 Base64 发送）')
    } catch {
      message.error('图片粘贴失败，请重试。')
    } finally {
      setImageUploading(false)
    }
  }

  const validateAndUseManualKey = async () => {
    const k = manualKeyValue.trim()
    if (!k) {
      message.warning('请输入 API Key（sk-...）')
      return
    }
    setValidatingKey(true)
    try {
      const info = await request.get<any, ApiKeyInfo>('/api/admin/keys/validate', {
        params: { key: k }
      })
      setManualKeyInfo(info)
      setSelectedKeyId(Number(info.id))
      setValidatedKeyValue(k)
      message.success('API Key 验证成功，可开始对话')
    } catch (e: any) {
      setManualKeyInfo(null)
      setSelectedKeyId(null)
      setValidatedKeyValue('')
      message.error(e?.message || 'API Key 验证失败')
    } finally {
      setValidatingKey(false)
    }
  }

  const handleSend = async () => {
    if (!input.trim() && !imageUrl) return
    if (!model) { message.warning('请先选择模型'); return }
    if (!isAdmin && !selectedKeyId) { message.warning('请先输入并验证 API Key'); return }
    if (imageUrl && !isMultiModal) {
      Modal.warning({
        title: '当前模型不支持图片多模态',
        content: '请切换到支持图片的多模态模型后，再粘贴/发送图片。',
      })
      return
    }

    let userContent: any
    if (isMultiModal) {
      if (imageUrl) {
        const textValue = input.trim() ? input.trim() : '请描述图片内容'
        // 统一使用 OpenAI 多模态格式：image_url + text
        userContent = [
          { type: 'image_url', image_url: { url: imageUrl } },
          { type: 'text', text: textValue },
        ]
      } else {
        // 仅文本
        userContent = input.trim()
      }
    } else {
      userContent = input
    }

    const userMessage: ChatMessage = { role: 'user', content: userContent }
    const newMessages = [...messages, userMessage]
    setMessages(newMessages)
    setInput('')
    setImageUrl('')
    setLoading(true)

    try {
      if (isAdmin) {
        // 管理员走后端代理，不需要网关 Key
        const data = await request.post<any, any>('/api/admin/channels/chat', {
          model,
          messages: newMessages,
        })
        if (data?.choices?.length > 0) {
          setMessages(prev => [...prev, data.choices[0].message])
        } else {
          message.error('响应格式错误')
        }
      } else {
        // 普通用户走后端代理，后端用真实 Key 调网关，前端不接触真实 Key
        const data = await request.post<any, any>(`/api/admin/keys/${selectedKeyId}/chat`, {
          model,
          messages: newMessages,
        })
        const chatResponse = data?.data || data
        if (chatResponse?.choices?.length > 0) {
          setMessages(prev => [...prev, chatResponse.choices[0].message])
        } else {
          message.error('响应格式错误')
        }
      }
    } catch (error: any) {
      message.error('发送失败: ' + (error?.response?.data?.message || error?.message || String(error)))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="chat">
      <Card
        title={
          <Space>
            Playground
            {isAdmin
              ? <Tag color="blue" icon={<ApiOutlined />}>管理员模式·直连渠道</Tag>
              : <Tag color="green">用户模式·按所选 Key 可用模型</Tag>
            }
          </Space>
        }
        extra={
          <Space wrap>
            {/* 普通用户：手动输入 Key */}
            {!isAdmin && (
              <Space.Compact>
                <Input.Password
                  value={manualKeyValue}
                  onChange={(e) => {
                    const next = e.target.value
                    setManualKeyValue(next)
                    const trimmed = next.trim()
                    if (!trimmed) {
                      setManualKeyInfo(null)
                      setSelectedKeyId(null)
                      setValidatedKeyValue('')
                      return
                    }
                    if (validatedKeyValue && trimmed !== validatedKeyValue) {
                      // 用户修改了输入框内容但还没重新验证，则清空已验证结果
                      setManualKeyInfo(null)
                      setSelectedKeyId(null)
                    }
                  }}
                  placeholder="输入 API Key（sk-...）"
                  style={{ width: 240 }}
                />
                <Button
                  type="primary"
                  icon={<KeyOutlined />}
                  loading={validatingKey}
                  onClick={validateAndUseManualKey}
                >
                  验证并使用
                </Button>
              </Space.Compact>
            )}
            {/* 模型选择 */}
            {modelsLoading ? <Spin size="small" /> : (
              <Select
                value={model || undefined}
                onChange={(value) => {
                  setModel(value)
                  setMessages([])
                  setInput('')
                  setImageUrl('')
                }}
                style={{ width: 280 }}
                options={selectOptions}
                placeholder={selectOptions.length === 0 ? '暂无可用模型' : '选择模型'}
                disabled={selectOptions.length === 0 || !!singleAllowedModel}
              />
            )}
          </Space>
        }
      >
        {/* 普通用户无 Key 提示 */}
        {!isAdmin && !manualKeyInfo && (
          <Alert
            type="warning"
            showIcon
            message="请先输入并验证你的 API Key，然后选择模型开始对话。"
            style={{ marginBottom: 16 }}
          />
        )}

        {/* 无模型提示 */}
        {selectOptions.length === 0 && !modelsLoading && (
          <div style={{ textAlign: 'center', padding: '24px 0', color: '#999' }}>
            {isAdmin
              ? '平台暂无模型，请先在「模型管理」页面发布模型。'
              : !hasKey
                ? '请先输入并验证 API Key。'
                : allowedModelsArr && allowedModelsArr.length > 0
                  ? '当前 Key 绑定的模型无法在列表中展示，请检查 Key 的 allowedModels 或先订阅对应模型。'
                  : '暂无已订阅模型，请先到「可用模型」订阅并申请 Key。'}
          </div>
        )}

        <div className="chat-messages">
          <List
            dataSource={messages}
            renderItem={(item) => (
              <div className={`message ${item.role}`}>
                <div className="role">{item.role === 'user' ? '用户' : 'AI'}</div>
                <div className="content">{renderContent(item.content)}</div>
              </div>
            )}
          />
          {loading && (
            <div className="message assistant">
              <div className="role">AI</div>
              <div className="content"><Spin size="small" /> 思考中...</div>
            </div>
          )}
        </div>

        <div className="chat-composer">
          {isMultiModal && (
            <div className="chat-composer-multimodal">
              <Space.Compact className="chat-image-uploader">
                <Upload
                  accept="image/*"
                  showUploadList={false}
                  disabled={imageUploading}
                  beforeUpload={async (file) => {
                    const f = file as File
                    // Base64 dataUrl 会显著增大体积；限制一下避免超出网关/内存限制
                    const maxMb = 5
                    if (f.size > maxMb * 1024 * 1024) {
                      message.error(`图片过大（>${maxMb}MB），请换小一点的图。`)
                      return Upload.LIST_IGNORE
                    }
                    try {
                      setImageUploading(true)
                      const url = await fileToDataUrl(f)
                      setImageUrl(url)
                      message.success('图片已载入（将以 Base64 发送）')
                    } catch (e) {
                      message.error('图片读取失败，请重试。')
                    } finally {
                      setImageUploading(false)
                    }
                    return Upload.LIST_IGNORE
                  }}
                >
                  <Button loading={imageUploading} disabled={imageUploading}>
                    上传图片
                  </Button>
                </Upload>

                <Input
                  prefix={<PictureOutlined />}
                  placeholder="图片 URL（可选）/ DataURL（可选）"
                  value={isDataUrl ? '已上传图片（dataURL）' : imageUrl}
                  onChange={(e) => setImageUrl(e.target.value)}
                  allowClear
                  readOnly={isDataUrl}
                />
              </Space.Compact>

              {imageUrl && (
                <img src={imageUrl} alt="preview"
                  style={{ maxHeight: 120, borderRadius: 8, marginBottom: 0, display: 'block' }}
                  onError={() => setImageUrl('')}
                />
              )}
            </div>
          )}

          <Space.Compact style={{ width: '100%' }}>
            <TextArea
              ref={textareaRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onPaste={handleTextareaPaste}
              placeholder={!hasKey ? '请先输入并验证 API Key' : (isMultiModal ? '输入文本消息...' : '输入消息...（Shift+Enter 换行，Enter 发送）')}
              autoSize={{ minRows: 2, maxRows: 6 }}
              disabled={!model}
              onPressEnter={(e) => { if (!e.shiftKey) { e.preventDefault(); handleSend() } }}
            />
            <Button type="primary" icon={<SendOutlined />} onClick={handleSend}
              loading={loading} disabled={!model || !hasKey}>发送</Button>
          </Space.Compact>
        </div>

        {messages.length > 0 && (
          <Button type="link" danger size="small" style={{ marginTop: 8 }}
            onClick={() => setMessages([])}>清空对话</Button>
        )}
      </Card>
    </div>
  )
}

export default Chat
