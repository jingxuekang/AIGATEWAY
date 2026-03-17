import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, Tag, Space, message } from 'antd'
import { PlusOutlined, EditOutlined, KeyOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { request } from '../../api/request'
import PageHeader from '../../components/PageHeader'
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

const User = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery<User[]>('users', () =>
    request.get<any, User[]>('/api/admin/users')
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
    }
  )

  const handleCreate = () => {
    setEditingUser(null)
    form.resetFields()
    setIsModalOpen(true)
  }

  const handleEdit = (record: User) => {
    setEditingUser(record)
    form.setFieldsValue(record)
    setIsModalOpen(true)
  }

  const handleSubmit = () => {
    form.validateFields().then(values => {
      if (editingUser) {
        updateMutation.mutate({ id: editingUser.id, ...values })
      } else {
        createMutation.mutate(values)
      }
    })
  }

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: (role: string) => (
        <Tag color={role === 'admin' ? 'red' : 'blue'}>
          {role === 'admin' ? '管理员' : '普通用户'}
        </Tag>
      ),
    },
    {
      title: '配额',
      key: 'quota',
      render: (_: any, record: User) => (
        <span>
          {record.usedQuota.toLocaleString()} / {record.quota.toLocaleString()}
        </span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>
          {status === 1 ? '正常' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: User) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            icon={<KeyOutlined />}
            onClick={() => message.info('查看 API Keys')}
          >
            API Keys
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div className="user">
      <PageHeader
        title="用户管理"
        primaryAction={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新建用户
          </Button>
        }
      />

      <Table
        columns={columns}
        dataSource={data || []}
        rowKey="id"
        loading={isLoading}
      />

      <Modal
        title={editingUser ? '编辑用户' : '新建用户'}
        open={isModalOpen}
        onOk={handleSubmit}
        onCancel={() => {
          setIsModalOpen(false)
          setEditingUser(null)
          form.resetFields()
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="username"
            label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input />
          </Form.Item>

          <Form.Item
            name="email"
            label="邮箱"
            rules={[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入有效的邮箱' },
            ]}
          >
            <Input />
          </Form.Item>

          {!editingUser && (
            <Form.Item
              name="password"
              label="密码"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password />
            </Form.Item>
          )}

          <Form.Item
            name="role"
            label="角色"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select>
              <Select.Option value="user">普通用户</Select.Option>
              <Select.Option value="admin">管理员</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="quota"
            label="配额"
            rules={[{ required: true, message: '请输入配额' }]}
          >
            <Input type="number" />
          </Form.Item>

          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select>
              <Select.Option value={1}>正常</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default User
