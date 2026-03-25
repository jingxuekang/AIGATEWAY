import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, Switch, Space, Tag, message, Tooltip, Typography } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined, EyeInvisibleOutlined, CopyOutlined, ThunderboltOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { request } from '../../api/request'
import { getProviders, Provider } from '../../api/provider'
import PageHeader from '../../components/PageHeader'
import './index.css'

const { Text } = Typography

const PROVIDER_COLORS: Record<string, string> = {
  deepseek: '#4096ff', azure: '#0078d4', volcano: '#ff6b35', openai: '#10a37f', anthropic: '#d4a853',
  qwen: '#ff6a00', moonshot: '#7c3aed', glm: '#0ea5e9', minimax: '#ec4899', baichuan: '#f59e0b',
  hunyuan: '#10b981', yi: '#6366f1',
}

interface Channel {
  id: number
  name: string
  provider: string
  baseUrl: string
  apiKey: string
  models: string
  weight: number
  maxConcurrency: number
  timeout: number
  status: number
}

const Channel = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingChannel, setEditingChannel] = useState<Channel | null>(null)
  const [visibleKeys, setVisibleKeys] = useState<Record<number, boolean>>({})
  const [testResults, setTestResults] = useState<Record<number, { success: boolean; latencyMs: number; error?: string } | 'testing'>>({})
  const [form] = Form.useForm()
  const queryClient = useQueryClient()

  const { data: providers = [], isLoading: providersLoading } = useQuery<Provider[]>('providers', getProviders)
  const enabledProviders = providers.filter(p => p.status === 1)

  const { data: channels, isLoading } = useQuery<Channel[]>('channels', () =>
    request.get<any, Channel[]>('/api/admin/channels')
  )

  const createMutation = useMutation(
    (values: any) => request.post<any, Channel>('/api/admin/channels', values),
    { onSuccess: () => { message.success('创建成功'); queryClient.invalidateQueries('channels'); handleClose() } }
  )
  const updateMutation = useMutation(
    ({ id, ...values }: any) => request.put(`/api/admin/channels/${id}`, values),
    { onSuccess: () => { message.success('更新成功'); queryClient.invalidateQueries('channels'); handleClose() } }
  )
  const deleteMutation = useMutation(
    (id: number) => request.delete(`/api/admin/channels/${id}`),
    { onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries('channels') } }
  )
  const toggleMutation = useMutation(
    ({ id, status }: { id: number; status: number }) => request.put(`/api/admin/channels/${id}/status`, { status }),
    { onSuccess: () => queryClient.invalidateQueries('channels') }
  )

  const handleClose = () => { setIsModalOpen(false); setEditingChannel(null); form.resetFields() }

  const handleEdit = (record: Channel) => {
    setEditingChannel(record)
    form.setFieldsValue({ ...record, status: record.status === 1, apiKey: '' })
    setIsModalOpen(true)
  }

  const handleSubmit = () => {
    form.validateFields().then(values => {
      const payload = { ...values, status: values.status ? 1 : 0 }
      if (editingChannel) {
        if (!payload.apiKey) delete payload.apiKey
        updateMutation.mutate({ id: editingChannel.id, ...payload })
      } else {
        createMutation.mutate(payload)
      }
    })
  }

  const handleTest = async (id: number) => {
    setTestResults(prev => ({ ...prev, [id]: 'testing' }))
    try {
      const res = await request.post<any, { success: boolean; latencyMs: number; error?: string }>(`/api/admin/channels/${id}/test`, {})
      setTestResults(prev => ({ ...prev, [id]: res }))
      res.success ? message.success(`连通成功，延迟 ${res.latencyMs}ms`) : message.error(`连通失败：${res.error}`)
    } catch (e: any) {
      setTestResults(prev => ({ ...prev, [id]: { success: false, latencyMs: 0, error: e?.message } }))
      message.error('测试失败')
    }
  }

  const handleProviderChange = (val: string) => {
    const opt = enabledProviders.find(p => p.code === val)
    if (opt) form.setFieldValue('baseUrl', opt.baseUrl)
  }

  const renderApiKey = (apiKey: string, id: number) => {
    if (!apiKey) return <Text type="secondary">未配置</Text>
    const isVisible = visibleKeys[id]
    const masked = apiKey.length > 12
      ? apiKey.substring(0, 8) + '••••••••' + apiKey.substring(apiKey.length - 4)
      : '••••••••••••'
    return (
      <Space size={4}>
        <Text code style={{ fontSize: 11 }}>{isVisible ? apiKey : masked}</Text>
        <Tooltip title={isVisible ? '隐藏' : '显示'}>
          <Button type="text" size="small" icon={isVisible ? <EyeInvisibleOutlined /> : <EyeOutlined />}
            onClick={() => setVisibleKeys(prev => ({ ...prev, [id]: !prev[id] }))} />
        </Tooltip>
        <Tooltip title="复制">
          <Button type="text" size="small" icon={<CopyOutlined />}
            onClick={() => { navigator.clipboard.writeText(apiKey); message.success('已复制') }} />
        </Tooltip>
      </Space>
    )
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    {
      title: '渠道名称', dataIndex: 'name', key: 'name', width: 150,
      render: (name: string, r: Channel) => (
        <Space><span style={{ fontWeight: 500 }}>{name}</span>
          {testResults[r.id] && testResults[r.id] !== 'testing' && (
            (testResults[r.id] as any).success
              ? <Tooltip title={`延迟 ${(testResults[r.id] as any).latencyMs}ms`}><CheckCircleOutlined style={{ color: '#52c41a' }} /></Tooltip>
              : <Tooltip title={(testResults[r.id] as any).error}><CloseCircleOutlined style={{ color: '#ff4d4f' }} /></Tooltip>
          )}
        </Space>
      ),
    },
    {
      title: 'Provider', dataIndex: 'provider', key: 'provider', width: 130,
      render: (p: string) => <Tag color={PROVIDER_COLORS[p] || 'default'}>{enabledProviders.find(o => o.code === p)?.name || p}</Tag>,
    },
    { title: 'Base URL', dataIndex: 'baseUrl', key: 'baseUrl', ellipsis: true, width: 200 },
    {
      title: '官方 API Key', dataIndex: 'apiKey', key: 'apiKey', width: 280,
      render: (apiKey: string, r: Channel) => renderApiKey(apiKey, r.id),
    },
    {
      title: '支持模型', dataIndex: 'models', key: 'models', width: 180,
      render: (m: string) => m
        ? m.split(',').map(v => <Tag key={v} style={{ fontSize: 11 }}>{v.trim()}</Tag>)
        : <Tag>按 Provider 匹配</Tag>,
    },
    { title: '权重', dataIndex: 'weight', key: 'weight', width: 70, align: 'center' as const },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 90,
      render: (s: number, r: Channel) => (
        <Switch checked={s === 1} size="small" loading={toggleMutation.isLoading}
          onChange={(checked) => toggleMutation.mutate({ id: r.id, status: checked ? 1 : 0 })}
          checkedChildren="启用" unCheckedChildren="禁用" />
      ),
    },
    {
      title: '操作', key: 'action', width: 180, fixed: 'right' as const,
      render: (_: any, r: Channel) => (
        <Space size={0}>
          <Button type="link" size="small" icon={testResults[r.id] === 'testing' ? undefined : <ThunderboltOutlined />}
            loading={testResults[r.id] === 'testing'} onClick={() => handleTest(r.id)}>测试</Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(r)}>编辑</Button>
          <Button type="link" danger size="small" icon={<DeleteOutlined />}
            onClick={() => Modal.confirm({ title: '确认删除', content: `确定删除渠道「${r.name}」吗？`, onOk: () => deleteMutation.mutate(r.id) })}>删除</Button>
        </Space>
      ),
    },
  ]

  return (
    <div className="channel">
      <PageHeader title="渠道管理" subtitle="配置上游 AI Provider 的官方 API Key，网关通过渠道路由请求到对应大模型"
        primaryAction={
          <Button type="primary" icon={<PlusOutlined />}
            onClick={() => { setEditingChannel(null); form.resetFields(); form.setFieldsValue({ weight: 100, maxConcurrency: 100, timeout: 30000, status: true }); setIsModalOpen(true) }}>
            新建渠道
          </Button>
        }
      />
      <Table columns={columns} dataSource={channels || []} rowKey="id" loading={isLoading} scroll={{ x: 1400 }}
        locale={{ emptyText: <div style={{ padding: 24, color: '#999' }}>暂无渠道，请点击「新建渠道」配置官方 API Key</div> }}
      />
      <Modal title={editingChannel ? `编辑渠道：${editingChannel.name}` : '新建渠道 - 配置官方 API Key'}
        open={isModalOpen} onOk={handleSubmit} onCancel={handleClose} width={620}
        confirmLoading={createMutation.isLoading || updateMutation.isLoading}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="渠道名称" rules={[{ required: true, message: '请输入渠道名称' }]}>
            <Input placeholder="例如：DeepSeek-主渠道" />
          </Form.Item>
          <Form.Item name="provider" label="Provider" rules={[{ required: true, message: '请选择 Provider' }]}>
            <Select placeholder="选择后自动填充 Base URL" onChange={handleProviderChange} loading={providersLoading}>
              {enabledProviders.map(o => (
                <Select.Option key={o.code} value={o.code}>
                  <Tag color={PROVIDER_COLORS[o.code] || 'default'} style={{ marginRight: 6 }}>{o.code}</Tag>{o.name}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="baseUrl" label="Base URL" rules={[{ required: true, message: '请输入 Base URL' }]}>
            <Input placeholder="https://api.deepseek.com/v1" />
          </Form.Item>
          <Form.Item name="apiKey" label={editingChannel ? '官方 API Key（留空则保留原值）' : '官方 API Key'}
            rules={editingChannel ? [] : [{ required: true, message: '请输入官方 API Key' }]}
            extra="从对应 AI 开放平台申请的密钥，例如 DeepSeek 开放平台的 sk-xxx">
            <Input.Password placeholder={editingChannel ? '留空则保留原 Key' : 'sk-xxx'} />
          </Form.Item>
          <Form.Item name="models" label="支持模型（逗号分隔，留空=按 Provider 前缀匹配）">
            <Input placeholder="deepseek-chat,deepseek-reasoner" />
          </Form.Item>
          <Space style={{ width: '100%' }} size={12}>
            <Form.Item name="weight" label="权重" style={{ flex: 1 }} initialValue={100}>
              <Input type="number" min={1} />
            </Form.Item>
            <Form.Item name="maxConcurrency" label="最大并发" style={{ flex: 1 }} initialValue={100}>
              <Input type="number" min={1} />
            </Form.Item>
            <Form.Item name="timeout" label="超时(ms)" style={{ flex: 1 }} initialValue={30000}>
              <Input type="number" min={1000} />
            </Form.Item>
          </Space>
          <Form.Item name="status" label="状态" valuePropName="checked" initialValue={true}>
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default Channel
