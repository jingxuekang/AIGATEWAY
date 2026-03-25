import { useState } from 'react'
import { Table, Button, Modal, Form, Input, InputNumber, Switch, Tag, Space, message, Alert, Tabs } from 'antd'
import { PlusOutlined, EditOutlined, MinusOutlined, EyeOutlined, KeyOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { useNavigate } from 'react-router-dom'
import { getModels, createModel, updateModel, Model } from '../../api/model'
import { getProviders } from '../../api/provider'
import { request } from '../../api/request'
import PageHeader from '../../components/PageHeader'
import './index.css'

interface ModelSubscription {
  id: number
  modelName: string
  provider: string
  subscribed: boolean
  inputPrice: number
  outputPrice: number
  description: string
}

const PROVIDER_COLOR: Record<string, string> = {
  openai: 'blue', anthropic: 'purple', deepseek: 'cyan',
  azure: 'geekblue', volcano: 'orange',
}

// ==================== 模型列表 Tab（Admin）====================
const ModelListTab = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingModel, setEditingModel] = useState<Model | null>(null)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()

  const { data: models, isLoading } = useQuery('models', getModels)
  const { data: providers } = useQuery('providers', getProviders)
  const providerOptions = (providers || []).map(p => ({ label: `${p.name} (${p.code})`, value: p.code }))

  const createMutation = useMutation(createModel, {
    onSuccess: () => { message.success('创建成功'); handleClose(); queryClient.invalidateQueries('models') },
  })
  const updateMutation = useMutation(
    ({ id, data }: { id: number; data: Partial<Model> }) => updateModel(id, data),
    { onSuccess: () => { message.success('更新成功'); handleClose(); queryClient.invalidateQueries('models') } }
  )

  const handleClose = () => { setIsModalOpen(false); setEditingModel(null); form.resetFields() }
  const handleEdit = (record: Model) => { setEditingModel(record); form.setFieldsValue(record); setIsModalOpen(true) }
  const handleSubmit = () => form.validateFields().then(values =>
    editingModel ? updateMutation.mutate({ id: editingModel.id, data: values }) : createMutation.mutate(values)
  )

  const columns = [
    { title: '模型名称', dataIndex: 'modelName', key: 'modelName', width: 200 },
    { title: '版本', dataIndex: 'modelVersion', key: 'modelVersion', width: 120 },
    { title: 'Provider', dataIndex: 'provider', key: 'provider', width: 140,
      render: (p: string) => <Tag color={PROVIDER_COLOR[p] || 'default'}>{p}</Tag> },
    { title: '状态', dataIndex: 'status', key: 'status', width: 90,
      render: (s: number) => <Tag color={s === 1 ? 'green' : 'red'}>{s === 1 ? '可用' : '不可用'}</Tag> },
    { title: '输入价格', dataIndex: 'inputPrice', key: 'inputPrice', width: 160,
      render: (p: number) => `¥${p}/1K tokens` },
    { title: '输出价格', dataIndex: 'outputPrice', key: 'outputPrice', width: 160,
      render: (p: number) => `¥${p}/1K tokens` },
    { title: '流式', dataIndex: 'supportStream', key: 'supportStream', width: 80,
      render: (s: boolean) => <Tag color={s ? 'blue' : 'default'}>{s ? '支持' : '不支持'}</Tag> },
    { title: '操作', key: 'action', width: 80, fixed: 'right' as const,
      render: (_: any, record: Model) => <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button> },
  ]

  return (
    <>
      <div style={{ marginBottom: 16, textAlign: 'right' }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsModalOpen(true)}>发布模型</Button>
      </div>
      <Table columns={columns} dataSource={models} rowKey="id" loading={isLoading} scroll={{ x: 1000 }} />
      <Modal title={editingModel ? '编辑模型' : '发布模型'} open={isModalOpen}
        onOk={handleSubmit} onCancel={handleClose} width={600}
        confirmLoading={createMutation.isLoading || updateMutation.isLoading}>
        <Form form={form} layout="vertical">
          <Form.Item name="modelName" label="模型名称" rules={[{ required: true, message: '请输入模型名称' }]}>
            <Input placeholder="例如: gpt-4o-mini" />
          </Form.Item>
          <Form.Item name="modelVersion" label="版本"><Input placeholder="例如: 1.0.0" /></Form.Item>
          <Form.Item name="provider" label="Provider" rules={[{ required: true, message: '请选择 Provider' }]}>
            <select style={{ width: '100%', padding: '4px 8px', borderRadius: 6, border: '1px solid #d9d9d9' }}
              onChange={e => form.setFieldValue('provider', e.target.value)}>
              <option value="">请选择</option>
              {providerOptions.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="inputPrice" label="输入价格 (¥/1K tokens)">
            <InputNumber min={0} step={0.001} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="outputPrice" label="输出价格 (¥/1K tokens)">
            <InputNumber min={0} step={0.001} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="maxTokens" label="最大 Tokens"><InputNumber min={1} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="supportStream" label="支持流式输出" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="status" label="状态" initialValue={1}>
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}

// ==================== 模型订阅 Tab ====================
const ModelSubscriptionTab = () => {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const role = localStorage.getItem('role') || 'user'
  const isAdmin = role === 'admin'

  const { data, isLoading } = useQuery<ModelSubscription[]>('modelSubscriptions', () =>
    request.get<any, ModelSubscription[]>('/api/admin/model-subscriptions')
  )

  const subscribeMutation = useMutation(
    (modelId: number) => request.post(`/api/admin/model-subscriptions/${modelId}/subscribe`),
    { onSuccess: () => { message.success('订阅成功'); queryClient.invalidateQueries('modelSubscriptions') } }
  )
  const unsubscribeMutation = useMutation(
    (modelId: number) => request.delete(`/api/admin/model-subscriptions/${modelId}/subscribe`),
    { onSuccess: () => { message.success('已取消订阅'); queryClient.invalidateQueries('modelSubscriptions') } }
  )

  const subscribedModels = (data || []).filter(m => m.subscribed).map(m => m.modelName)

  const columns = [
    { title: '模型名称', dataIndex: 'modelName', key: 'modelName', width: 220,
      render: (name: string, record: ModelSubscription) => (
        <Space><span style={{ fontWeight: 600 }}>{name}</span>
          {record.subscribed && <Tag color="success">已订阅</Tag>}
        </Space>
      ) },
    { title: 'Provider', dataIndex: 'provider', key: 'provider', width: 130,
      render: (p: string) => <Tag color={PROVIDER_COLOR[p] || 'default'}>{p}</Tag> },
    { title: '输入价格', dataIndex: 'inputPrice', key: 'inputPrice', width: 160, align: 'center' as const,
      render: (p: number) => <span style={{ color: '#1677ff' }}>${p}/1M tokens</span> },
    { title: '输出价格', dataIndex: 'outputPrice', key: 'outputPrice', width: 160, align: 'center' as const,
      render: (p: number) => <span style={{ color: '#fa8c16' }}>${p}/1M tokens</span> },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
    { title: '操作', key: 'action', width: 200, fixed: 'right' as const,
      render: (_: any, record: ModelSubscription) => (
        <Space>
          <Button type="link" size="small" icon={<EyeOutlined />}
            onClick={() => Modal.info({
              title: record.modelName, width: 480,
              content: (<div style={{ marginTop: 12 }}>
                <p><b>Provider：</b>{record.provider}</p>
                <p><b>输入价格：</b>${record.inputPrice}/1M tokens</p>
                <p><b>输出价格：</b>${record.outputPrice}/1M tokens</p>
                <p><b>描述：</b>{record.description}</p>
                <p><b>状态：</b>{record.subscribed ? '✅ 已订阅' : '未订阅'}</p>
              </div>),
            })}>详情</Button>
          {record.subscribed ? (
            <>
              <Button type="link" size="small" icon={<KeyOutlined />} onClick={() => navigate('/key-applications')}>申请 Key</Button>
              {!isAdmin && <Button type="link" danger size="small" icon={<MinusOutlined />}
                onClick={() => unsubscribeMutation.mutate(record.id)}>取消订阅</Button>}
            </>
          ) : (
            <Button type="link" size="small" icon={<PlusOutlined />}
              onClick={() => subscribeMutation.mutate(record.id)}>订阅</Button>
          )}
        </Space>
      ) },
  ]

  return (
    <>
      {subscribedModels.length > 0 && (
        <Alert style={{ marginBottom: 16 }} type="success" showIcon
          message={<Space>
            <span>已订阅 {subscribedModels.length} 个模型：{subscribedModels.map(m => <Tag key={m} color="blue" style={{ marginLeft: 4 }}>{m}</Tag>)}</span>
            <Button size="small" type="primary" icon={<KeyOutlined />} onClick={() => navigate('/key-applications')}>去申请 API Key</Button>
          </Space>}
        />
      )}
      <Table columns={columns} dataSource={data || []} rowKey="id" loading={isLoading}
        scroll={{ x: 1000 }} rowClassName={(r) => r.subscribed ? 'subscribed-row' : ''} />
    </>
  )
}

// ==================== 主页面 ====================
const Models = () => {
  const role = localStorage.getItem('role') || 'user'
  const isAdmin = role === 'admin'

  const tabItems = [
    ...(isAdmin ? [{ key: 'list', label: '模型列表', children: <ModelListTab /> }] : []),
    { key: 'subscription', label: isAdmin ? '订阅管理' : '可用模型', children: <ModelSubscriptionTab /> },
  ]

  return (
    <div className="models">
      <PageHeader
        title="模型管理"
        subtitle={isAdmin ? '发布模型供用户订阅，用户订阅后可申请 API Key' : '订阅模型后前往申请页面申请 API Key'}
      />
      <Tabs defaultActiveKey={isAdmin ? 'list' : 'subscription'} items={tabItems} />
    </div>
  )
}

export default Models
