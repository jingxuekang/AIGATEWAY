import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout as AntdLayout, Menu, Avatar, Dropdown, Tag } from 'antd'
import {
  DashboardOutlined, KeyOutlined, ApiOutlined, FileTextOutlined,
  MessageOutlined, UserOutlined, SettingOutlined, DollarOutlined,
  HistoryOutlined, ApartmentOutlined, BookOutlined, LogoutOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import { PageHeaderProvider, PageBreadcrumbItem } from '../PageHeader'
import './index.css'

const { Header, Sider, Content } = AntdLayout

const Layout = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const role = (localStorage.getItem('role') as 'admin' | 'user') || 'admin'
  const username = localStorage.getItem('username') || 'Admin'

  const handleLogout = () => {
    localStorage.clear()
    navigate('/login')
  }

  const adminMenuItems = [
    { key: '/', icon: <DashboardOutlined />, label: '仪表盘' },
    { key: '/account', icon: <UserOutlined />, label: '用户中心' },
    {
      type: 'group' as const, label: '访问与鉴权', children: [
        { key: '/channel', icon: <ApiOutlined />, label: '渠道管理' },
        { key: '/keys', icon: <KeyOutlined />, label: 'API Keys' },
        { key: '/key-applications', icon: <KeyOutlined />, label: '申请审批' },
      ]
    },
    {
      type: 'group' as const, label: '用户与计费', children: [
        { key: '/user', icon: <UserOutlined />, label: '用户管理' },
        { key: '/topup', icon: <DollarOutlined />, label: '充值记录' },
        { key: '/redemption', icon: <DollarOutlined />, label: '兑换码管理' },
      ]
    },
    {
      type: 'group' as const, label: '模型管理', children: [
        { key: '/providers', icon: <ApiOutlined />, label: 'Provider 管理' },
        { key: '/models', icon: <ApartmentOutlined />, label: '模型管理' },
      ]
    },
    {
      type: 'group' as const, label: '运维与观测', children: [
        { key: '/logs', icon: <FileTextOutlined />, label: '调用日志' },
        { key: '/task', icon: <HistoryOutlined />, label: '任务日志' },
      ]
    },
    {
      type: 'group' as const, label: '工具', children: [
        { key: '/chat', icon: <MessageOutlined />, label: 'Playground' },
        { key: '/docs', icon: <BookOutlined />, label: '接口文档' },
        { key: '/setting', icon: <SettingOutlined />, label: '系统设置' },
      ]
    },
  ]

  const userMenuItems = [
    { key: '/account', icon: <UserOutlined />, label: '用户中心' },
    {
      type: 'group' as const, label: '我的密钥', children: [
        { key: '/keys', icon: <KeyOutlined />, label: 'API Keys' },
        { key: '/key-applications', icon: <KeyOutlined />, label: '我的申请' },
      ]
    },
    {
      type: 'group' as const, label: '模型', children: [
        { key: '/models', icon: <ApartmentOutlined />, label: '可用模型' },
      ]
    },
    {
      type: 'group' as const, label: '使用记录', children: [
        { key: '/logs', icon: <FileTextOutlined />, label: '调用日志' },
      ]
    },
    {
      type: 'group' as const, label: '工具', children: [
        { key: '/chat', icon: <MessageOutlined />, label: 'Playground' },
        { key: '/docs', icon: <BookOutlined />, label: '接口文档' },
      ]
    },
  ]

  const menuItems = role === 'admin' ? adminMenuItems : userMenuItems

  const buildRouteMeta = (items: any[]) => {
    const map: Record<string, { label: string; group?: string }> = {}
    const groupDefaultRoute: Record<string, string> = {}
    const walk = (arr: any[], currentGroup?: string) => {
      for (const it of arr) {
        if (!it) continue
        if (it.type === 'group' && typeof it.label === 'string' && Array.isArray(it.children)) {
          if (groupDefaultRoute[it.label] == null) {
            const first = (it.children as any[]).find((c) => c && typeof c.key === 'string')
            if (first?.key) groupDefaultRoute[it.label] = first.key
          }
          walk(it.children, it.label)
          continue
        }
        if (it.key && typeof it.label === 'string') map[it.key] = { label: it.label, group: currentGroup }
        if (Array.isArray(it.children)) walk(it.children, currentGroup)
      }
    }
    walk(items)
    return { map, groupDefaultRoute }
  }

  const pathname = location.pathname || '/'
  const { map: routeMetaMap, groupDefaultRoute } = buildRouteMeta(menuItems)
  const meta = routeMetaMap[pathname]
  const currentLabel = meta?.label || (pathname === '/' ? '仪表盘' : '')
  const currentGroup = meta?.group

  const groupCrumb = currentGroup
    ? ({
        title: currentGroup,
        href: groupDefaultRoute[currentGroup] || undefined,
        onClick: groupDefaultRoute[currentGroup] ? () => navigate(groupDefaultRoute[currentGroup]) : undefined,
      } as PageBreadcrumbItem)
    : null

  const breadcrumb: PageBreadcrumbItem[] = [
    { title: '首页', href: '/', onClick: () => navigate('/') },
    ...(groupCrumb ? [groupCrumb] : []),
    ...(currentLabel && currentLabel !== '仪表盘' ? [{ title: currentLabel }] : []),
  ]

  const userDropdownItems = {
    items: [
      { key: 'profile', icon: <UserOutlined />, label: '个人信息' },
      { type: 'divider' as const },
      { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', danger: true },
    ],
    onClick: ({ key }: { key: string }) => { if (key === 'logout') handleLogout() },
  }

  return (
    <AntdLayout className="app-container">
      <Header className="app-header">
        <div className="logo-wrap" onClick={() => navigate('/')}>
          <div className="logo-icon"><ThunderboltOutlined /></div>
          <span className="logo-text">AI Gateway</span>
        </div>
        <div className="header-right">
          <Tag color={role === 'admin' ? 'blue' : 'green'} style={{ margin: 0 }}>
            {role === 'admin' ? '管理员' : '用户'}
          </Tag>
          <Dropdown menu={userDropdownItems} placement="bottomRight">
            <div className="header-user">
              <Avatar size={32} icon={<UserOutlined />} style={{ background: '#0077fa' }} />
              <span className="header-user-name">{username}</span>
            </div>
          </Dropdown>
        </div>
      </Header>
      <AntdLayout>
        <Sider width={200} className="app-sider" theme="light">
          <Menu
            mode="inline"
            selectedKeys={[location.pathname]}
            items={menuItems}
            onClick={({ key }) => navigate(key)}
          />
        </Sider>
        <Content className="app-content">
          <PageHeaderProvider breadcrumb={breadcrumb}>
            <Outlet />
          </PageHeaderProvider>
        </Content>
      </AntdLayout>
    </AntdLayout>
  )
}

export default Layout