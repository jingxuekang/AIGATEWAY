import { Card, Row, Col, Statistic, Spin, Table, Tag, Progress } from 'antd'
import {
  ApiOutlined, KeyOutlined, FileTextOutlined, DatabaseOutlined,
  ThunderboltOutlined, ClockCircleOutlined, CheckCircleOutlined,
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import { getDashboardStats } from '../../api/dashboard'
import dayjs from 'dayjs'
import PageHeader from '../../components/PageHeader'
import './index.css'

const Dashboard = () => {
  const { data: stats, isLoading } = useQuery('dashboardStats', getDashboardStats)

  if (isLoading) {
    return <Spin size="large" style={{ display: 'block', margin: '120px auto' }} />
  }

  const statCards = [
    {
      title: '总调用次数',
      value: stats?.totalRequests || 0,
      icon: <ApiOutlined />,
      color: '#1677ff',
      bg: '#e6f4ff',
      suffix: '次',
    },
    {
      title: 'API Keys',
      value: stats?.totalKeys || 0,
      icon: <KeyOutlined />,
      color: '#52c41a',
      bg: '#f6ffed',
      suffix: '个',
    },
    {
      title: 'Token 消耗',
      value: stats?.totalTokens || 0,
      icon: <FileTextOutlined />,
      color: '#fa8c16',
      bg: '#fff7e6',
      suffix: '',
    },
    {
      title: '可用模型',
      value: stats?.totalModels || 0,
      icon: <DatabaseOutlined />,
      color: '#722ed1',
      bg: '#f9f0ff',
      suffix: '个',
    },
  ]

  const recentLogs = stats?.recentLogs || []

  const logColumns = [
    {
      title: '时间',
      dataIndex: 'timestamp',
      width: 160,
      render: (t: string) => dayjs(t).format('MM-DD HH:mm:ss'),
    },
    { title: '模型', dataIndex: 'model', width: 150 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (s: string) => (
        <Tag color={s === 'success' ? 'success' : 'error'}>{s === 'success' ? '成功' : '失败'}</Tag>
      ),
    },
    {
      title: 'Tokens',
      dataIndex: 'totalTokens',
      width: 90,
      align: 'right' as const,
    },
    {
      title: '延迟(ms)',
      dataIndex: 'latencyMs',
      width: 90,
      align: 'right' as const,
      render: (v: number) => (
        <span style={{ color: v > 3000 ? '#ff4d4f' : v > 1000 ? '#fa8c16' : '#52c41a' }}>{v}</span>
      ),
    },
  ]

  return (
    <div className="dashboard">
      <PageHeader
        title="仪表盘"
        subtitle={dayjs().format('YYYY年MM月DD日 dddd')}
      />

      <Row gutter={[16, 16]}>
        {statCards.map((card, i) => (
          <Col span={6} key={i}>
            <Card className="stat-card" bordered={false}>
              <div className="stat-card-inner">
                <div className="stat-icon" style={{ background: card.bg, color: card.color }}>
                  {card.icon}
                </div>
                <div className="stat-info">
                  <Statistic
                    title={card.title}
                    value={card.value}
                    suffix={card.suffix}
                    valueStyle={{ color: card.color, fontSize: 24, fontWeight: 700 }}
                  />
                </div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={16}>
          <Card title="最近调用记录" bordered={false} className="recent-card">
            <Table
              columns={logColumns}
              dataSource={recentLogs}
              rowKey="id"
              size="small"
              pagination={false}
              locale={{ emptyText: '暂无调用记录' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card title="成功率" bordered={false} className="rate-card">
            <div className="rate-display">
              <Progress
                type="circle"
                percent={stats?.successRate || 100}
                strokeColor={{ '0%': '#1677ff', '100%': '#52c41a' }}
                size={120}
              />
              <div className="rate-label">调用成功率</div>
            </div>
            <div className="rate-stats">
              <div className="rate-stat-item">
                <CheckCircleOutlined style={{ color: '#52c41a' }} />
                <span>成功 {stats?.successRequests || 0} 次</span>
              </div>
              <div className="rate-stat-item">
                <ClockCircleOutlined style={{ color: '#fa8c16' }} />
                <span>平均延迟 {stats?.avgLatencyMs || 0} ms</span>
              </div>
              <div className="rate-stat-item">
                <ThunderboltOutlined style={{ color: '#1677ff' }} />
                <span>首Token {stats?.avgTtftMs || 0} ms</span>
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Dashboard
