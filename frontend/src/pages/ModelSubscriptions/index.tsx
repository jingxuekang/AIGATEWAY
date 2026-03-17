import { Table, Button, Tag, Space, message, Modal } from 'antd'
import { PlusOutlined, MinusOutlined, EyeOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
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

const ModelSubscriptions = () => {
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery<ModelSubscription[]>('modelSubscriptions', () =>
    request.get<any, ModelSubscription[]>('/api/admin/model-subscriptions')
  )

  const subscribeMutation = useMutation(
    (modelId: number) => request.post(`/api/admin/model-subscriptions/${modelId}/subscribe`),
    {
      onSuccess: () => {
        message.success('订阅成功')
        queryClient.invalidateQueries('modelSubscriptions')
      },
    }
  )

  const unsubscribeMutation = useMutation(
    (modelId: number) => request.delete(`/api/admin/model-subscriptions/${modelId}/subscribe`),
    {
      onSuccess: () => {
        message.success('取消订阅成功')
        queryClient.invalidateQueries('modelSubscriptions')
      },
    }
  )

  const columns = [
    {
      title: '模型名称',
      dataIndex: 'modelName',
      key: 'modelName',
      width: 200,
      align: 'center' as const,
    },
    {
      title: 'Provider',
      dataIndex: 'provider',
      key: 'provider',
      width: 140,
      align: 'center' as const,
      render: (provider: string) => <Tag color="blue">{provider}</Tag>,
    },
    {
      title: '输入价格',
      dataIndex: 'inputPrice',
      key: 'inputPrice',
      width: 160,
      align: 'center' as const,
      render: (price: number) => `$${price}/1M tokens`,
    },
    {
      title: '输出价格',
      dataIndex: 'outputPrice',
      key: 'outputPrice',
      width: 160,
      align: 'center' as const,
      render: (price: number) => `$${price}/1M tokens`,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      width: 320,
      align: 'left' as const,
    },
    {
      title: '状态',
      dataIndex: 'subscribed',
      key: 'subscribed',
      width: 120,
      align: 'center' as const,
      render: (subscribed: boolean) => (
        <Tag color={subscribed ? 'green' : 'default'}>
          {subscribed ? '已订阅' : '未订阅'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      fixed: 'right' as const,
      align: 'center' as const,
      render: (_: any, record: ModelSubscription) => (
        <Space size={0} wrap={false} className="table-action">
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => {
              Modal.info({
                title: '模型详情',
                width: 600,
                content: (
                  <div style={{ marginTop: 16 }}>
                    <p><strong>模型名称：</strong>{record.modelName}</p>
                    <p><strong>Provider：</strong>{record.provider}</p>
                    <p><strong>输入价格：</strong>{record.inputPrice}/1M tokens</p>
                    <p><strong>输出价格：</strong>{record.outputPrice}/1M tokens</p>
                    <p><strong>订阅状态：</strong>{record.subscribed ? '已订阅' : '未订阅'}</p>
                    <p><strong>描述：</strong></p>
                    <p>{record.description}</p>
                  </div>
                ),
              })
            }}
          >
            详情
          </Button>
          {record.subscribed ? (
            <Button
              type="link"
              danger
              icon={<MinusOutlined />}
              onClick={() => unsubscribeMutation.mutate(record.id)}
              className="table-action-btn"
            >
              取消订阅
            </Button>
          ) : (
            <Button
              type="link"
              icon={<PlusOutlined />}
              onClick={() => subscribeMutation.mutate(record.id)}
              className="table-action-btn"
            >
              订阅
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div className="model-subscriptions">
      <PageHeader title="模型订阅" />
      <Table
        columns={columns}
        dataSource={data || []}
        rowKey="id"
        loading={isLoading}
        scroll={{ x: 1300 }}
      />
    </div>
  )
}

export default ModelSubscriptions
