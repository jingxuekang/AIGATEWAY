import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, Tag, Space, message } from 'antd'
import { PlusOutlined, DeleteOutlined, CopyOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { getUserKeys, createApiKey, revokeApiKey, ApiKey } from '../../api/apiKey'
import dayjs from 'dayjs'
import PageHeader from '../../components/PageHeader'
import './index.css'

const ApiKeys = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()
  const userId = 1 // TODO: 从用户上下文获取

  const { data: keys, isLoading } = useQuery(['apiKeys', userId], () => getUserKeys(userId))

  const createMutation = useMutation(createApiKey, {
    onSuccess: () => {
      message.success('创建成功')
      setIsModalOpen(false)
      form.resetFields()
      queryClient.invalidateQueries(['apiKeys'])
    },
  })

  const revokeMutation = useMutation(revokeApiKey, {
    onSuccess: () => {
      message.success('吊销成功')
      queryClient.invalidateQueries(['apiKeys'])
    },
  })

  const handleCreate = () => {
    form.validateFields().then((values) => {
      // 将allowedModels数组转换为逗号分隔的字符串
      const formData = {
        ...values,
        userId,
        allowedModels: Array.isArray(values.allowedModels) 
          ? values.allowedModels.join(',') 
          : values.allowedModels
      }
      createMutation.mutate(formData)
    })
  }

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text)
    message.success('已复制到剪贴板')
  }

  const columns = [
    {
      title: 'Key 名称',
      dataIndex: 'keyName',
      key: 'keyName',
    },
    {
      title: 'API Key',
      dataIndex: 'keyValue',
      key: 'keyValue',
      render: (text: string) => (
        <Space>
          <span>{text.substring(0, 20)}...</span>
          <Button
            type="link"
            size="small"
            icon={<CopyOutlined />}
            onClick={() => handleCopy(text)}
          />
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '已用配额',
      key: 'quota',
      render: (record: ApiKey) => `${record.usedQuota} / ${record.totalQuota}`,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      render: (text: string) => dayjs(text).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '操作',
      key: 'action',
      render: (record: ApiKey) => (
        <Button
          type="link"
          danger
          icon={<DeleteOutlined />}
          onClick={() => revokeMutation.mutate(record.id)}
          disabled={record.status === 0}
        >
          吊销
        </Button>
      ),
    },
  ]

  return (
    <div className="api-keys">
      <PageHeader
        title="API Keys 管理"
        primaryAction={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsModalOpen(true)}>
            创建 API Key
          </Button>
        }
      />

      <Table
        columns={columns}
        dataSource={keys}
        rowKey="id"
        loading={isLoading}
      />

      <Modal
        title="创建 API Key"
        open={isModalOpen}
        onOk={handleCreate}
        onCancel={() => setIsModalOpen(false)}
        confirmLoading={createMutation.isLoading}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="keyName"
            label="Key 名称"
            rules={[{ required: true, message: '请输入 Key 名称' }]}
          >
            <Input placeholder="请输入 Key 名称" />
          </Form.Item>
          <Form.Item name="tenantId" label="租户 ID">
            <Input placeholder="请输入租户 ID" />
          </Form.Item>
          <Form.Item name="appId" label="应用 ID">
            <Input placeholder="请输入应用 ID" />
          </Form.Item>
          <Form.Item name="allowedModels" label="允许的模型">
            <Select mode="tags" placeholder="选择允许的模型">
              <Select.Option value="gpt-4">gpt-4</Select.Option>
              <Select.Option value="gpt-3.5-turbo">gpt-3.5-turbo</Select.Option>
              <Select.Option value="claude-3-opus">claude-3-opus</Select.Option>
              <Select.Option value="claude-3-sonnet">claude-3-sonnet</Select.Option>
              <Select.Option value="deepseek-chat">deepseek-chat</Select.Option>
              <Select.Option value="deepseek-coder">deepseek-coder</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="totalQuota" label="总配额">
            <Input type="number" placeholder="请输入总配额" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default ApiKeys
