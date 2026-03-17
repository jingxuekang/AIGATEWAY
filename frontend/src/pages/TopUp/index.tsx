import { Table, Tag, DatePicker, Space, Button, Modal, message } from 'antd'
import { useQuery } from 'react-query'
import { request } from '../../api/request'
import dayjs from 'dayjs'
import PageHeader from '../../components/PageHeader'
import './index.css'

const { RangePicker } = DatePicker

interface TopUpRecord {
  id: number
  userId: number
  username: string
  amount: number
  quota: number
  paymentMethod: string
  status: string
  createTime: string
}

const TopUp = () => {
  const { data, isLoading } = useQuery<TopUpRecord[]>('topup', () =>
    request.get<any, TopUpRecord[]>('/api/admin/topup')
  )

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: '用户', dataIndex: 'username', key: 'username' },
    {
      title: '充值金额', dataIndex: 'amount', key: 'amount',
      render: (amount: number) => `¥${amount?.toFixed(2) ?? '0.00'}`,
    },
    {
      title: '获得配额', dataIndex: 'quota', key: 'quota',
      render: (quota: number) => quota?.toLocaleString(),
    },
    {
      title: '支付方式', dataIndex: 'paymentMethod', key: 'paymentMethod',
      render: (method: string) => {
        const config: Record<string, { color: string; text: string }> = {
          alipay: { color: 'blue', text: '支付宝' },
          wechat: { color: 'green', text: '微信' },
          balance: { color: 'orange', text: '余额' },
        }
        return <Tag color={config[method]?.color}>{config[method]?.text || method}</Tag>
      },
    },
    {
      title: '状态', dataIndex: 'status', key: 'status',
      render: (status: string) => {
        const map: Record<string, { color: string; text: string }> = {
          pending: { color: 'orange', text: '待支付' },
          success: { color: 'green', text: '成功' },
          failed: { color: 'red', text: '失败' },
        }
        return <Tag color={map[status]?.color}>{map[status]?.text ?? status}</Tag>
      },
    },
    {
      title: '充值时间', dataIndex: 'createTime', key: 'createTime',
      render: (text: string) => dayjs(text).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '操作', key: 'action', width: 80,
      render: (_: any, record: TopUpRecord) => (
        <Space>
          <Button type="link" onClick={() => {
            Modal.info({
              title: '充值详情', width: 500,
              content: (
                <div style={{ marginTop: 16 }}>
                  <p><strong>订单 ID：</strong>{record.id}</p>
                  <p><strong>用户：</strong>{record.username}</p>
                  <p><strong>金额：</strong>¥{record.amount?.toFixed(2)}</p>
                  <p><strong>配额：</strong>{record.quota?.toLocaleString()}</p>
                  <p><strong>时间：</strong>{dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss')}</p>
                </div>
              ),
            })
          }}>详情</Button>
          {record.status === 'pending' && (
            <Button type="link" danger onClick={() => message.info('取消充值功能待实现')}>取消</Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div className="topup">
      <PageHeader title="充值记录" />
      <RangePicker style={{ marginBottom: 16 }} />
      <Table columns={columns} dataSource={data || []} rowKey="id" loading={isLoading} />
    </div>
  )
}

export default TopUp
