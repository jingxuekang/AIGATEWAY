import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, Tag, Space, message, Drawer, Badge, Tooltip, Progress } from 'antd'
import { PlusOutlined, EditOutlined, KeyOutlined, DeleteOutlined, CopyOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { request } from '../../api/request'
import PageHeader from '../../components/PageHeader'
import dayjs from 'dayjs'
import './index.css'

interface User {
  id: number
  username: string
  email: string
  role: string
  status: number
  quota: number
  usedQuota: number
  createTime: string
}

interface ApiKey {
  id: number
  keyName: string
  keyValue: string
  allowedModels: string
  totalQuota: number
  usedQuota: number
  status: number
  expireTime: string
  createTime: string
}

const UserPage = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)
  const [keyDrawerUser, setKeyDrawerUser] = useState<User | null>(null)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery<User[]>('users', () =>
    request.get<any, User[]>('/api/admin/users')
  )

  // 查询选中用户的 API Keys
  const { data: userKeys, isLoading: keysLoading } = useQuery<ApiKey[]>(
    ['userKeys', keyDrawerUser?.id],
    () => request.get<any, ApiKey[]>(`/api/admin/keys/user/${keyDrawerUser!.id}`),
    { enabled: !!keyDrawerUser }
  )

  const createMutation = useMutation(
    (values: any) => request.post('/api/admin/users', values),
    {
      onSuccess: () => {
        message.success('创建成功')
        queryClient.invalidateQueries('users')
        setIsModalOpen(false)
        form.resetFields()
      },
      onError: (err: any) => message.error(err?.response?.data?.message || '创建失败'),
    }
  )

  const updateMutation = useMutation(
    ({ id, ...values }: any) => request.put(`/api/admin/users/${id}`, values),
    {
      onSuccess: () => {
        message.success('更新成功')
        queryClient.invalidateQueries('users')
        setIsModalOpen(false)
        setEditingUser(null)
        form.resetFields()
      },
      onError: (err: any) => message.error(err?.response?.data?.message || '更新失败'),
    }
  )

  const deleteMutation = useMutation(
    (id: number) => request.delete(`/api/admin/users/${id}`),
    {
      onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries('users') },
    }
  )

  const handleCreate = () => { setEditingUser(null); form.resetFields(); setIsModalOpen(true) }
  const handleEdit = (record: User) => { setEditingUser(record); form.setFieldsValue(record); setIsModalOpen(true) }
  const handleSubmit = () => {
    form.validateFields().then(values => {
      if (editingUser) updateMutation.mutate({ id: editingUser.id, ...values })
      else createMutation.mutate(values)
    })
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '邮箱', dataIndex: 'email', key: 'email' },
    {
      title: '角色', dataIndex: 'role', key: 'role', width: 100,
      render: (role: string) => (
        <Tag color={role === 'admin' ? 'red' : 'blue'}>{role === 'admin' ? '管理员' : '普通用户'}</Tag>
      ),
    },
    {
      title: '配额', key: 'quota', width: 150,
      render: (_: any, record: User) =>
        !record.quota ? <Tag color="green">不限</Tag> : (
          <Tooltip title={`已用 ${record.usedQuota?.toLocaleString()} / 总量 ${record.quota?.toLocaleString()}`}>
            <Progress percent={Math.min(100, Math.round((record.usedQuota / record.quota) * 100))}
              size="small" status={record.usedQuota / record.quota >= 0.9 ? 'exception' : 'normal'} />
          </Tooltip>
        ),
    },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 80,
      render: (s: number) => <Tag color={s === 1 ? 'green' : 'red'}>{s === 1 ? '正常' : '禁用'}</Tag>,
    },
    {
      title: '注册时间', dataIndex: 'createTime', key: 'createTime', width: 160,
      render: (t: string) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-',
    },
    {
      title: '操作', key: 'action', width: 200,
      render: (_: any, record: User) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>
          {/* 管理员账号不显示 API Keys 按钮 */}
          {record.role !== 'admin' && (
            <Button type="link" icon={<KeyOutlined />} onClick={() => setKeyDrawerUser(record)}>API Keys</Button>
          )}
          <Button type="link" danger icon={<DeleteOutlined />}
            onClick={() => Modal.confirm({
              title: '确认删除',
              content: `确定要删除用户 ${record.username} 吗？`,
              onOk: () => deleteMutation.mutate(record.id),
            })}
          >删除</Button>
        </Space>
      ),
    },
  ]

  const keyColumns = [
    {
      title: 'Key 名称', dataIndex: 'keyName', key: 'keyName', width: 120,
    },
    {
      title: 'API Key', dataIndex: 'keyValue', key: 'keyValue',
      render: (v: string) => (
        <Space>
          <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{v?.substring(0, 20)}...</span>
          <Button type="text" size="small" icon={<CopyOutlined />}
            onClick={() => { navigator.clipboard.writeText(v); message.success('已复制') }} />
        </Space>
      ),
    },
    {
      title: '绑定模型', dataIndex: 'allowedModels', key: 'allowedModels',
      render: (m: string) => m
        ? m.split(',').map(v => <Tag key={v} color="blue" style={{ fontSize: 11 }}>{v.trim()}</Tag>)
        : <Tag>不限模型</Tag>,
    },
    {
      title: 'Token 配额', key: 'quota', width: 150,
      render: (_: any, r: ApiKey) => !r.totalQuota ? <Tag color="green">不限</Tag> : (
        <Tooltip title={`${r.usedQuota?.toLocaleString()} / ${r.totalQuota?.toLocaleString()}`}>
          <Progress percent={Math.min(100, Math.round((r.usedQuota / r.totalQuota) * 100))}
            size="small" status={r.usedQuota / r.totalQuota >= 0.9 ? 'exception' : 'normal'} />
        </Tooltip>
      ),
    },
    {
      title: '有效期', dataIndex: 'expireTime', key: 'expireTime', width: 110,
      render: (t: string) => t
        ? <Tag color={dayjs(t).isBefore(dayjs()) ? 'red' : 'default'}>{dayjs(t).format('YYYY-MM-DD')}</Tag>
        : <Tag color="green">永久</Tag>,
    },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 80,
      render: (s: number) => <Tag color={s === 1 ? 'success' : 'error'}>{s === 1 ? '启用' : '已吊销'}</Tag>,
    },
  ]

  return (
    <div className="user">
      <PageHeader
        title="用户管理"
        primaryAction={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>新建用户</Button>
        }
      />

      <Table columns={columns} dataSource={data || []} rowKey="id" loading={isLoading} />

      {/* 新建/编辑用户弹窗 */}
      <Modal
        title={editingUser ? '编辑用户' : '新建用户'}
        open={isModalOpen}
        onOk={handleSubmit}
        onCancel={() => { setIsModalOpen(false); setEditingUser(null); form.resetFields() }}
        confirmLoading={createMutation.isLoading || updateMutation.isLoading}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="email" label="邮箱" rules={[{ required: true, message: '请输入邮箱' }, { type: 'email', message: '请输入有效邮箱' }]}>
            <Input />
          </Form.Item>
          {!editingUser && (
            <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
              <Input.Password />
            </Form.Item>
          )}
          <Form.Item name="role" label="角色" rules={[{ required: true, message: '请选择角色' }]}>
            <Select>
              <Select.Option value="user">普通用户</Select.Option>
              <Select.Option value="admin">管理员</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="quota" label="Token 配额（0=不限）" initialValue={0}>
            <Input type="number" min={0} />
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true, message: '请选择状态' }]} initialValue={1}>
            <Select>
              <Select.Option value={1}>正常</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      {/* 用户 API Keys 抽屉 */}
      <Drawer
        title={
          <Space>
            <KeyOutlined />
            <span>{keyDrawerUser?.username} 的 API Keys</span>
            {userKeys && <Badge count={userKeys.length} style={{ backgroundColor: '#1677ff' }} />}
          </Space>
        }
        open={!!keyDrawerUser}
        onClose={() => setKeyDrawerUser(null)}
        width={900}
        placement="right"
      >
        {userKeys?.length === 0 && !keysLoading && (
          <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
            该用户暂无 API Key
          </div>
        )}
        <Table
          columns={keyColumns}
          dataSource={userKeys || []}
          rowKey="id"
          loading={keysLoading}
          pagination={false}
          size="small"
          scroll={{ x: 800 }}
        />
      </Drawer>
    </div>
  )
}

export default UserPage
