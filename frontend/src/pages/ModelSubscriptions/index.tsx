import { Table, Button, Tag, Space, message, Modal, Alert } from 'antd'
import { PlusOutlined, MinusOutlined, EyeOutlined, KeyOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { useNavigate } from 'react-router-dom'
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

const ModelSubscriptions = () => {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const role = localStorage.getItem('role') || 'user'
  const isAdmin = role === 'admin'

  const { data, isLoading } = useQuery<ModelSubscription[]>('modelSubscriptions', () =>
    request.get<any, ModelSubscription[]>('/api/admin/model-subscriptions')
  )

  const subscribeMutation = useMutation(
    (modelId: number) => request.post(`/api/admin/model-subscriptions/${modelId}/subscribe`),
    {
      onSuccess: () => { message.success('订阅成功，现在可以申请该模型的 API Key'); queryClient.invalidateQueries('modelSubscriptions') },
      onError: () => { message.error('订阅失败') },
    }
  )

  const unsubscribeMutation = useMutation(
    (modelId: number) => request.delete(`/api/admin/model-subscriptions/${modelId}/subscribe`),
    {
      onSuccess: () => { message.success('已取消订阅'); queryClient.invalidateQueries('modelSubscriptions') },
      onError: () => { message.error('取消订阅失败') },
    }
  )

  const subscribedModels = (data || []).filter(m => m.subscribed).map(m => m.modelName)

  const columns = [
    {
      title: '模型名称', dataIndex: 'modelName', key: 'modelName', width: 220,
      render: (name: string, record: ModelSubscription) => (
        <Space>
          <span style={{ fontWeight: 600 }}>{name}</span>
          {record.subscribed && <Tag color="success" style={{ fontSize: 11 }}>已订阅</Tag>}
        </Space>
      ),
    },
    {
      title: 'Provider', dataIndex: 'provider', key: 'provider', width: 130,
      render: (p: string) => <Tag color={PROVIDER_COLOR[p] || 'default'}>{p}</Tag>,
    },
    {
      title: '输入价格', dataIndex: 'inputPrice', key: 'inputPrice', width: 150, align: 'center' as const,
      render: (price: number) => <span style={{ color: '#1677ff' }}>${price}/1M tokens</span>,
    },
    {
      title: '输出价格', dataIndex: 'outputPrice', key: 'outputPrice', width: 150, align: 'center' as const,
      render: (price: number) => <span style={{ color: '#fa8c16' }}>${price}/1M tokens</span>,
    },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
    {
      title: '操作', key: 'action', width: 240, fixed: 'right' as const,
      render: (_: any, record: ModelSubscription) => (
        <Space>
          <Button type="link" size="small" icon={<EyeOutlined />}
            onClick={() => Modal.info({
              title: record.modelName,
              width: 480,
              content: (
                <div style={{ marginTop: 12 }}>
                  <p><b>Provider：</b>{record.provider}</p>
                  <p><b>输入价格：</b>${record.inputPrice}/1M tokens</p>
                  <p><b>输出价格：</b>${record.outputPrice}/1M tokens</p>
                  <p><b>描述：</b>{record.description}</p>
                  <p><b>状态：</b>{record.subscribed ? '✅ 已订阅' : '未订阅'}</p>
                  {record.subscribed && !isAdmin && (
                    <Button type="primary" icon={<KeyOutlined />}
                      onClick={() => { Modal.destroyAll(); navigate('/key-applications') }}>
                      申请此模型的 API Key
                    </Button>
                  )}
                </div>
              ),
            })}
          >详情</Button>

          {isAdmin ? (
            <Tag color="green">管理员可直接使用</Tag>
          ) : record.subscribed ? (
            <>
              <Button type="link" size="small" icon={<KeyOutlined />}
                onClick={() => navigate('/key-applications')}>
                申请 Key
              </Button>
              <Button type="link" danger size="small" icon={<MinusOutlined />}
                onClick={() => unsubscribeMutation.mutate(record.id)}>
                取消订阅
              </Button>
            </>
          ) : (
            <Button type="link" size="small" icon={<PlusOutlined />}
              onClick={() => subscribeMutation.mutate(record.id)}>
              订阅
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div className="model-subscriptions">
      <PageHeader
        title={isAdmin ? '模型管理' : '可用模型'}
        subtitle={isAdmin ? '管理平台上发布的模型，业务方订阅后方可申请 Key' : '订阅模型后，前往申请页面申请对应的 API Key'}
        primaryAction={isAdmin && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/models')}>
            发布新模型
          </Button>
        )}
      />

      {!isAdmin && subscribedModels.length > 0 && (
        <Alert
          style={{ marginBottom: 16 }}
          type="success"
          showIcon
          message={
            <Space>
              <span>你已订阅 {subscribedModels.length} 个模型：{subscribedModels.map(m => <Tag key={m} color="blue" style={{ marginLeft: 4 }}>{m}</Tag>)}</span>
              <Button size="small" type="primary" icon={<KeyOutlined />} onClick={() => navigate('/key-applications')}>
                去申请 API Key
              </Button>
            </Space>
          }
        />
      )}

      <Table
        columns={columns}
        dataSource={data || []}
        rowKey="id"
        loading={isLoading}
        scroll={{ x: 1100 }}
        rowClassName={(r) => r.subscribed ? 'subscribed-row' : ''}
      />
    </div>
  )
}

export default ModelSubscriptions
