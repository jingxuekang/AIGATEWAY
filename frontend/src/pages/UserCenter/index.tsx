import { Card, Row, Col, Statistic, Spin, Table, Tag, Progress, Typography, Alert } from 'antd'
import {
  UserOutlined, KeyOutlined, ApiOutlined, FileTextOutlined, RiseOutlined, ThunderboltOutlined,
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import { authApi } from '../../api/auth'
import { getDashboardStats } from '../../api/dashboard'
import dayjs from 'dayjs'
import PageHeader from '../../components/PageHeader'
import './index.css'

const { Text } = Typography

const UserCenter = () => {
  const role = localStorage.getItem('role') || 'user'

  const { data: me, isLoading: meLoading } = useQuery('authMe', () => authApi.me())
  const { data: stats, isLoading: statsLoading } = useQuery('userCenterStats', getDashboardStats)

  const loading = meLoading || statsLoading
  const bu = me?.user
  const ad = me?.admin

  if (loading) {
    return <Spin size="large" style={{ display: 'block', margin: '120px auto' }} />
  }

  if (role === 'admin' && ad) {
    return (
      <div className="user-center">
        <PageHeader title="用户中心" subtitle="管理员账号" />
        <Alert type="info" showIcon message={`当前登录：${ad.username}（管理员）`} description="全局数据请在「仪表盘」查看；此处不展示业务用户配额。" />
      </div>
    )
  }

  if (!bu) {
    return (
      <div className="user-center">
        <PageHeader title="用户中心" />
        <Alert type="warning" showIcon message="未加载到业务用户信息" />
      </div>
    )
  }

  const quota = bu.quota || 0
  const used = bu.usedQuota || 0
  const pct = quota > 0 ? Math.min(100, Math.round((used / quota) * 100)) : 0

  const recentLogs = stats?.recentLogs || []
  const logColumns = [
    { title: '时间', dataIndex: 'timestamp', width: 150, render: (t: string) => dayjs(t).format('MM-DD HH:mm:ss') },
    { title: '模型', dataIndex: 'model', width: 140, ellipsis: true },
    {
      title: '状态', dataIndex: 'status', width: 72,
      render: (s: string) => <Tag color={s === 'success' ? 'success' : 'error'}>{s === 'success' ? '成功' : '失败'}</Tag>,
    },
    { title: 'Tokens', dataIndex: 'totalTokens', width: 80, align: 'right' as const },
  ]

  return (
    <div className="user-center">
      <PageHeader
        title="用户中心"
        subtitle={`${bu.username} · 账户与用量概览`}
      />

      <Row gutter={[16, 16]}>
        <Col xs={24} md={12}>
          <Card title={<><UserOutlined /> 账户信息</>} bordered={false}>
            <p><Text type="secondary">邮箱</Text> {bu.email || '—'}</p>
            <p><Text type="secondary">注册时间</Text> {bu.createTime ? dayjs(bu.createTime).format('YYYY-MM-DD') : '—'}</p>
            <p>
              <Text type="secondary">账户状态</Text>{' '}
              <Tag color={bu.status === 1 ? 'success' : 'error'}>{bu.status === 1 ? '正常' : '禁用'}</Tag>
            </p>
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card title={<><FileTextOutlined /> Token 配额（余额）</>} bordered={false}>
            {!quota ? (
              <Alert type="success" showIcon message="当前为「不限配额」模式（quota=0）" />
            ) : (
              <>
                <div style={{ marginBottom: 8 }}>
                  <Text>已用 {used.toLocaleString()} / 总量 {quota.toLocaleString()} tokens</Text>
                </div>
                <Progress percent={pct} status={pct >= 90 ? 'exception' : 'active'} />
              </>
            )}
            <Row gutter={16} style={{ marginTop: 16 }}>
              <Col span={12}>
                <Statistic title="历史总消耗（日志汇总）" value={stats?.totalTokens || 0} />
              </Col>
              <Col span={12}>
                <Statistic title="今日消耗" value={stats?.todayTokens || 0} />
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={12} sm={8} md={6}>
          <Card size="small" bordered={false}>
            <Statistic title="我的总请求" value={stats?.totalRequests || 0} prefix={<ApiOutlined />} />
          </Card>
        </Col>
        <Col xs={12} sm={8} md={6}>
          <Card size="small" bordered={false}>
            <Statistic title="今日请求" value={stats?.todayRequests || 0} prefix={<RiseOutlined />} />
          </Card>
        </Col>
        <Col xs={12} sm={8} md={6}>
          <Card size="small" bordered={false}>
            <Statistic title="我的 API Keys" value={stats?.totalKeys || 0} prefix={<KeyOutlined />} />
          </Card>
        </Col>
        <Col xs={12} sm={8} md={6}>
          <Card size="small" bordered={false}>
            <Statistic title="平均延迟(ms)" value={stats?.avgLatencyMs || 0} prefix={<ThunderboltOutlined />} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={14}>
          <Card title="我的最近调用" bordered={false}>
            <Table
              size="small"
              rowKey="id"
              pagination={false}
              dataSource={recentLogs}
              columns={logColumns}
              locale={{ emptyText: '暂无调用记录' }}
            />
          </Card>
        </Col>
        <Col xs={24} lg={10}>
          <Card title="我的模型用量（已发布模型）" bordered={false}>
            <Table
              size="small"
              rowKey={(r: any) => r.model}
              pagination={false}
              dataSource={stats?.tokensByModel || []}
              columns={[
                { title: '模型', dataIndex: 'model', ellipsis: true },
                { title: '请求', dataIndex: 'requests', width: 70, align: 'right' as const },
                { title: 'Token', dataIndex: 'tokens', width: 90, align: 'right' as const,
                  render: (v: number) => (v || 0).toLocaleString() },
              ]}
              locale={{ emptyText: '暂无数据' }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default UserCenter
