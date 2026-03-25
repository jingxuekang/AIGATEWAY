import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Switch, Tag, Space, message } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { getProviders, createProvider, updateProvider, deleteProvider, Provider } from '../../api/provider'
import PageHeader from '../../components/PageHeader'

const Providers = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editing, setEditing] = useState<Provider | null>(null)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()
  
  const { data: providers, isLoading } = useQuery('providers', getProviders)

  const createMutation = useMutation(createProvider, {
    onSuccess: () => {
      message.success('创建成功')
      handleClose()
      queryClient.invalidateQueries('providers')
    },
  })

  const updateMutation = useMutation(
    ({ id, data }: { id: number; data: Partial<Provider> }) => updateProvider(id, data),
    {
      onSuccess: () => {
        message.success('更新成功')
        handleClose()
        queryClient.invalidateQueries('providers')
      },
    }
  )

  const deleteMutation = useMutation(deleteProvider, {
    onSuccess: () => {
      message.success('删除成功')
      queryClient.invalidateQueries('providers')
    },
  })

  const handleClose = () => {
    setIsModalOpen(false)
    setEditing(null)
    form.resetFields()
  }

  const handleSubmit = () => {
    form.validateFields().then(values => {
      const payload = {
        ...values,
        status: values.status ? 1 : 0,
      }
      if (editing) {
        updateMutation.mutate({ id: editing.id, data: payload })
      } else {
        createMutation.mutate(payload)
      }
    })
  }

  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name', width: 180 },
    { title: '标识(code)', dataIndex: 'code', key: 'code', width: 140 },
    { title: 'Base URL', dataIndex: 'baseUrl', key: 'baseUrl', ellipsis: true },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (s: number) => (
        <Tag color={s === 1 ? 'green' : 'red'}>{s === 1 ? '启用' : '禁用'}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      render: (record: Provider) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => {
              setEditing(record)
              form.setFieldsValue({
                ...record,
                status: record.status === 1,
              })
              setIsModalOpen(true)
            }}
          >
            编辑
          </Button>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() =>
              Modal.confirm({
                title: '确认删除',
                content: `确定要删除 Provider ${record.name} (${record.code}) 吗？`,
                onOk: () => deleteMutation.mutate(record.id),
              })
            }
          >
            删除
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div className="providers">
      <PageHeader
        title="Provider 管理"
        primaryAction={
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditing(null)
              form.resetFields()
              form.setFieldsValue({ status: true })
              setIsModalOpen(true)
            }}
          >
            新建 Provider
          </Button>
        }
      />

      <Table
        columns={columns}
        dataSource={providers || []}
        rowKey="id"
        loading={isLoading}
      />

      <Modal
        title={editing ? '编辑 Provider' : '新建 Provider'}
        open={isModalOpen}
        onOk={handleSubmit}
        onCancel={handleClose}
        confirmLoading={createMutation.isLoading || updateMutation.isLoading}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="例如：OpenAI" />
          </Form.Item>
          <Form.Item name="code" label="标识(code)" rules={[{ required: true, message: '请输入唯一标识' }]}>
            <Input placeholder="例如：openai / deepseek / volcano" />
          </Form.Item>
          <Form.Item name="baseUrl" label="Base URL" rules={[{ required: true, message: '请输入 Base URL' }]}>
            <Input placeholder="例如：https://api.openai.com/v1" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="备注说明" />
          </Form.Item>
          <Form.Item name="status" label="状态" valuePropName="checked" initialValue>
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default Providers

