import { Card, Row, Col, Statistic, Spin, Table, Tag, Progress, Badge, Typography } from 'antd'
import {
  ApiOutlined, KeyOutlined, FileTextOutlined, DatabaseOutlined,
  ThunderboltOutlined, ClockCircleOutlined, CheckCircleOutlined, RiseOutlined,
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import { getDashboardStats, getCircuitBreakerStatus, CircuitBreakerStatus } from '../../api/dashboard'
import dayjs from 'dayjs'
import PageHeader from '../../components/PageHeader'
import './index.css'

const { Text } = Typography

const stateConfig: Record<string, { color: string; badge: 'success' | 'error' | 'warning'; text: string }> = {
  CLOSED:    { color: '#52c41a', badge: 'success', text: '正常' },
  OPEN:      { color: '#ff4d4f', badge: 'error',   text: '熔断' },
  HALF_OPEN: { color: '#fa8c16', badge: 'warning', text: '恢复中' },
}

const Dashboard = () => {
  const { data: stats, isLoading } = useQuery('dashboardStats', getDashboardStats)
  const { data: cbStatus } = useQuery('circuitBreakers', getCircuitBreakerStatus, {
    refetchInterval: 10000, // 每10秒自动刷新熔断状态
  })

  if (isLoading) {
    return <Spin size="large" style={{ display: 'block', margin: '120px auto' }} />
  }

  const statCards = [
    {
      title: '总请求',
      value: stats?.totalRequests || 0,
      icon: <ApiOutlined />,
      color: '#1677ff',
      bg: '#e6f4ff',
      suffix: '次',
    },
    {
      title: '今日请求',
      value: stats?.todayRequests || 0,
      icon: <RiseOutlined />,
      color: '#52c41a',
      bg: '#f6ffed',
      suffix: '次',
    },
    {
      title: '总 Token',
      value: stats?.totalTokens || 0,
      icon: <FileTextOutlined />,
      color: '#fa8c16',
      bg: '#fff7e6',
      suffix: '',
    },
    {
      title: '今日 Token',
      value: stats?.todayTokens || 0,
      icon: <FileTextOutlined />,
      color: '#722ed1',
      bg: '#f9f0ff',
      suffix: '',
    },
    {
      title: 'API Keys',
      value: stats?.totalKeys || 0,
      icon: <KeyOutlined />,
      color: '#13c2c2',
      bg: '#e6fffb',
      suffix: '个',
    },
    {
      title: '可用模型',
      value: stats?.totalModels || 0,
      icon: <DatabaseOutlined />,
      color: '#2f54eb',
      bg: '#f0f5ff',
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
          <Col xs={24} sm={12} md={8} lg={8} xl={4} key={i}>
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
        <Col xs={24} lg={16}>
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
        <Col xs={24} lg={8}>
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

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={12}>
          <Card title="模型 Token 排行（Top10）" bordered={false}>
            <Table
              size="small"
              pagination={false}
              rowKey={(r: any) => r.model}
              dataSource={stats?.tokensByModel || []}
              columns={[
                { title: '模型', dataIndex: 'model', key: 'model', ellipsis: true },
                {
                  title: '请求',
                  dataIndex: 'requests',
                  key: 'requests',
                  width: 90,
                  align: 'right',
                },
                {
                  title: 'Token',
                  dataIndex: 'tokens',
                  key: 'tokens',
                  width: 130,
                  align: 'right',
                  render: (v: number, r: any) => (
                    <div>
                      <div>{(v || 0).toLocaleString()}</div>
                      <Progress
                        percent={Math.round(((v || 0) / ((stats?.tokensByModel?.[0]?.tokens || 1) as number)) * 100)}
                        showInfo={false}
                        strokeColor="#1677ff"
                      />
                    </div>
                  ),
                },
              ]}
            />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card
            title="渠道调用排行"
            bordered={false}
            extra={<span style={{ fontSize: 12, color: '#999' }}>与渠道里「Provider」同展示名；聚合 key 仍为日志中的厂商 code</span>}
          >
            <Table
              size="small"
              pagination={false}
              rowKey={(r: any) => r.provider}
              dataSource={stats?.statsByProvider || []}
              columns={[
                {
                  title: 'Provider',
                  key: 'provider',
                  ellipsis: true,
                  render: (_: any, r: any) => (
                    <div>
                      <div>{r.providerLabel || r.provider}</div>
                      {r.providerLabel && r.providerLabel !== r.provider && (
                        <Text type="secondary" style={{ fontSize: 11 }}>code: {r.provider}</Text>
                      )}
                    </div>
                  ),
                },
                { title: '请求', dataIndex: 'requests', key: 'requests', width: 90, align: 'right' },
                {
                  title: 'Token',
                  dataIndex: 'tokens',
                  key: 'tokens',
                  width: 120,
                  align: 'right',
                  render: (v: number) => (v || 0).toLocaleString(),
                },
              ]}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={14}>
          <Card title="近 7 天趋势" bordered={false}>
            <Table
              size="small"
              pagination={false}
              rowKey={(r: any) => r.date}
              dataSource={stats?.dailyStats || []}
              columns={[
                { title: '日期', dataIndex: 'date', key: 'date', width: 120 },
                {
                  title: '请求量',
                  dataIndex: 'requests',
                  key: 'requests',
                  render: (v: number) => (
                    <div>
                      <Text>{v || 0}</Text>
                      <Progress
                        percent={Math.round(((v || 0) / ((stats?.dailyStats?.reduce((a, b) => Math.max(a, b.requests || 0), 1)) as number)) * 100)}
                        showInfo={false}
                        strokeColor="#52c41a"
                      />
                    </div>
                  ),
                },
                {
                  title: 'Token',
                  dataIndex: 'tokens',
                  key: 'tokens',
                  width: 140,
                  align: 'right',
                  render: (v: number) => (v || 0).toLocaleString(),
                },
              ]}
            />
          </Card>
        </Col>
        <Col xs={24} lg={10}>
          <Card title="用户消耗排行（7天）" bordered={false}>
            <Table
              size="small"
              pagination={false}
              rowKey={(r: any) => `${r.userId}`}
              dataSource={stats?.topUsers || []}
              columns={[
                {
                  title: '用户',
                  dataIndex: 'username',
                  key: 'username',
                  ellipsis: true,
                  render: (v: string) => v || '—',
                },
                { title: '请求', dataIndex: 'requests', key: 'requests', width: 80, align: 'right' },
                {
                  title: 'Token',
                  dataIndex: 'tokens',
                  key: 'tokens',
                  width: 120,
                  align: 'right',
                  render: (v: number) => (v || 0).toLocaleString(),
                },
              ]}
            />
          </Card>
        </Col>
      </Row>

      {/* 熔断器状态 */}
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={24}>
          <Card
            title="Provider 熔断状态"
            bordered={false}
            extra={<span style={{ fontSize: 12, color: '#999' }}>每10秒自动刷新</span>}
          >
            <Row gutter={[12, 12]}>
              {(cbStatus || []).map((cb: CircuitBreakerStatus) => {
                const cfg = stateConfig[cb.state] || stateConfig.CLOSED
                return (
                  <Col key={cb.provider} xs={12} sm={8} md={6} lg={4}>
                    <div style={{
                      padding: '12px 16px',
                      borderRadius: 8,
                      border: `1px solid ${cfg.color}33`,
                      background: `${cfg.color}0d`,
                      textAlign: 'center',
                    }}>
                      <Badge status={cfg.badge} />
                      <span style={{ fontWeight: 600, marginLeft: 4, fontSize: 13 }}>
                        {cb.provider}
                      </span>
                      <div style={{ marginTop: 6 }}>
                        <Tag color={cb.state === 'CLOSED' ? 'success' : cb.state === 'OPEN' ? 'error' : 'warning'}>
                          {cb.label}
                        </Tag>
                      </div>
                    </div>
                  </Col>
                )
              })}
              {(!cbStatus || cbStatus.length === 0) && (
                <Col span={24}>
                  <span style={{ color: '#999', fontSize: 13 }}>暂无熔断器数据（gateway-core 未启动或未调用过）</span>
                </Col>
              )}
            </Row>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Dashboard
