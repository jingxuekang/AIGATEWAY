import { useEffect, useState } from 'react'
import { Card, Table, Button, message } from 'antd'
import { useQuery } from 'react-query'
import { getModels, Model } from '../../api/model'
import { getMySubscriptions, subscribeModel, unsubscribeModel } from '../../api/modelSubscription'

const ModelSubscriptionPage = () => {
  const userId = 1 // TODO: 替换为登录用户 ID
  const [subscribedIds, setSubscribedIds] = useState<number[]>([])

  const { data: models, isLoading } = useQuery('models', getModels)
  const { data: subs } = useQuery(['modelSubscriptions', userId], () => getMySubscriptions(userId))

  useEffect(() => {
    if (subs) {
      setSubscribedIds(subs.map((s) => s.modelId))
    }
  }, [subs])

  const handleToggle = async (record: Model) => {
    try {
      if (subscribedIds.includes(record.id)) {
        await unsubscribeModel(record.id, userId)
        message.success('已取消订阅')
        setSubscribedIds(subscribedIds.filter((id) => id !== record.id))
      } else {
        await subscribeModel(record.id, userId)
        message.success('订阅成功')
        setSubscribedIds([...subscribedIds, record.id])
      }
    } catch (e) {
      message.error('操作失败')
    }
  }

  const columns = [
    { title: '模型名称', dataIndex: 'modelName', key: 'modelName' },
    { title: '版本', dataIndex: 'modelVersion', key: 'modelVersion' },
    { title: '提供商', dataIndex: 'provider', key: 'provider' },
    {
      title: '订阅状态',
      key: 'subscribed',
      render: (record: Model) => (subscribedIds.includes(record.id) ? '已订阅' : '未订阅'),
    },
    {
      title: '操作',
      key: 'action',
      render: (record: Model) => (
        <Button type="link" onClick={() => handleToggle(record)}>
          {subscribedIds.includes(record.id) ? '取消订阅' : '订阅'}
        </Button>
      ),
    },
  ]

  return (
    <Card title="模型发布 & 订阅" loading={isLoading}>
      <Table<Model> rowKey="id" columns={columns} dataSource={models || []} />
    </Card>
  )
}

export default ModelSubscriptionPage

