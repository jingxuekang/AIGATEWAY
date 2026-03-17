import { Table, Tag } from 'antd'
import { useQuery } from 'react-query'
import { getRedemptions, RedemptionCode } from '../../api/redemption'
import dayjs from 'dayjs'
import PageHeader from '../../components/PageHeader'

const RedemptionPage = () => {
  const { data, isLoading } = useQuery('redemptions', getRedemptions)

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id' },
    { title: '兑换码', dataIndex: 'code', key: 'code' },
    { title: '面额', dataIndex: 'amount', key: 'amount' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        let color = 'default'
        if (status === 'unused') color = 'green'
        if (status === 'used') color = 'blue'
        if (status === 'expired') color = 'red'
        return <Tag color={color}>{status}</Tag>
      },
    },
    {
      title: '过期时间',
      dataIndex: 'expireTime',
      key: 'expireTime',
      render: (text: string) => dayjs(text).format('YYYY-MM-DD HH:mm:ss'),
    },
  ]

  return (
    <div>
      <PageHeader title="兑换码管理" />
      <Table<RedemptionCode> rowKey="id" loading={isLoading} columns={columns} dataSource={data || []} />
    </div>
  )
}

export default RedemptionPage

