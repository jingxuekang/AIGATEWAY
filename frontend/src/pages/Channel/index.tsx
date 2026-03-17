import { useMemo, useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, Switch, Space, Tag, message } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { request } from '../../api/request'
import PageHeader from '../../components/PageHeader'
import './index.css'

interface Channel {
  id: number
  name: string
  provider: string
  baseUrl: string
  apiKey: string
  status: number
  weight: number
  maxConcurrency: number
  timeout: number
  createTime: string
}

const Channel = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingChannel, setEditingChannel] = useState<Channel | null>(null)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery<Channel[]>('channels', () =>
    request.get<any, Channel[]>('/api/admin/channels')
  )

  const createMutation = useMutation(
    (values: any) => request.post('/api/admin/channels', values),
    {
      onSuccess: () => {
        message.success('创建成功')
        queryClient.invalidateQueries('channels')
        setIsModalOpen(false)
        form.resetFields()
      },
    }
  )

  const updateMutation = useMutation(
    ({ id, ...values }: any) => request.put(`/api/admin/channels/${id}`, values),
    {
      onSuccess: () => {
        message.success('更新成功')
        queryClient.invalidateQueries('channels')
        setIsModalOpen(false)
        setEditingChannel(null)
        form.resetFields()
      },
    }
  )

  const deleteMutation = useMutation(
    (id: number) => request.delete(`/api/admin/channels/${id}`),
    {
      onSuccess: () => {
        message.success('删除成功')
        queryClient.invalidateQueries('channels')
      },
    }
  )

  const handleCreate = () => {
    setEditingChannel(null)
    form.resetFields()
    setIsModalOpen(true)
  }

  const handleEdit = (record: Channel) => {
    setEditingChannel(record)
    form.setFieldsValue(record)
    setIsModalOpen(true)
  }

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这个渠道吗？',
      onOk: () => deleteMutation.mutate(id),
    })
  }

  const handleSubmit = () => {
    form.validateFields().then(values => {
      if (editingChannel) {
        updateMutation.mutate({ id: editingChannel.id, ...values })
      } else {
        createMutation.mutate(values)
      }
    })
  }

  const columns = useMemo(() => [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
      fixed: 'left' as const,
      align: 'center' as const,
    },
    {
      title: '渠道名称',
      dataIndex: 'name',
      key: 'name',
      width: 180,
      align: 'center' as const,
    },
    {
      title: 'Provider',
      dataIndex: 'provider',
      key: 'provider',
      width: 140,
      align: 'center' as const,
      render: (provider: string) => (
        <Tag color="blue">{provider}</Tag>
      ),
    },
    {
      title: 'Base URL',
      dataIndex: 'baseUrl',
      key: 'baseUrl',
      ellipsis: true,
      width: 260,
      align: 'left' as const,
    },
    {
      title: '权重',
      dataIndex: 'weight',
      key: 'weight',
      width: 80,
      align: 'center' as const,
    },
    {
      title: '最大并发',
      dataIndex: 'maxConcurrency',
      key: 'maxConcurrency',
      width: 100,
      align: 'center' as const,
    },
    {
      title: '超时(ms)',
      dataIndex: 'timeout',
      key: 'timeout',
      width: 100,
      align: 'center' as const,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      align: 'center' as const,
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 140,
      fixed: 'right' as const,
      align: 'center' as const,
      render: (_: any, record: Channel) => (
        <Space size={0} wrap={false} className="table-action">
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
            className="table-action-btn"
          >
            编辑
          </Button>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
            className="table-action-btn"
          >
            删除
          </Button>
        </Space>
      ),
    },
  ], [deleteMutation.isLoading, updateMutation.isLoading])

  return (
    <div className="channel">
      <PageHeader
        title="渠道管理"
        primaryAction={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新建渠道
          </Button>
        }
      />

      <Table
        columns={columns}
        dataSource={data || []}
        rowKey="id"
        loading={isLoading}
        scroll={{ x: 1100 }}
      />

      <Modal
        title={editingChannel ? '编辑渠道' : '新建渠道'}
        open={isModalOpen}
        onOk={handleSubmit}
        onCancel={() => {
          setIsModalOpen(false)
          setEditingChannel(null)
          form.resetFields()
        }}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="渠道名称"
            rules={[{ required: true, message: '请输入渠道名称' }]}
          >
            <Input placeholder="例如：OpenAI-Primary" />
          </Form.Item>

          <Form.Item
            name="provider"
            label="Provider"
            rules={[{ required: true, message: '请选择 Provider' }]}
          >
            <Select>
              <Select.Option value="openai">OpenAI</Select.Option>
              <Select.Option value="azure-openai">Azure OpenAI</Select.Option>
              <Select.Option value="anthropic">Anthropic</Select.Option>
              <Select.Option value="deepseek">DeepSeek</Select.Option>
              <Select.Option value="volcano">火山方舟</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="baseUrl"
            label="Base URL"
            rules={[{ required: true, message: '请输入 Base URL' }]}
          >
            <Input placeholder="https://api.openai.com/v1" />
          </Form.Item>

          <Form.Item
            name="apiKey"
            label="API Key"
            rules={[{ required: true, message: '请输入 API Key' }]}
          >
            <Input.Password placeholder="sk-..." />
          </Form.Item>

          <Form.Item
            name="weight"
            label="权重"
            initialValue={100}
            rules={[{ required: true, message: '请输入权重' }]}
          >
            <Input type="number" />
          </Form.Item>

          <Form.Item
            name="maxConcurrency"
            label="最大并发数"
            initialValue={100}
            rules={[{ required: true, message: '请输入最大并发数' }]}
          >
            <Input type="number" />
          </Form.Item>

          <Form.Item
            name="timeout"
            label="超时时间(ms)"
            initialValue={30000}
            rules={[{ required: true, message: '请输入超时时间' }]}
          >
            <Input type="number" />
          </Form.Item>

          <Form.Item
            name="status"
            label="状态"
            valuePropName="checked"
            initialValue={true}
          >
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default Channel
