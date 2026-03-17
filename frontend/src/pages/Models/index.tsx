import { useState } from 'react'
import { Table, Button, Modal, Form, Input, InputNumber, Switch, Tag, message } from 'antd'
import { PlusOutlined, EditOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { getModels, createModel, updateModel, Model } from '../../api/model'
import PageHeader from '../../components/PageHeader'
import './index.css'

const Models = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingModel, setEditingModel] = useState<Model | null>(null)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()

  const { data: models, isLoading } = useQuery('models', getModels)

  const createMutation = useMutation(createModel, {
    onSuccess: () => {
      message.success('创建成功')
      handleCloseModal()
      queryClient.invalidateQueries('models')
    },
  })

  const updateMutation = useMutation(
    ({ id, data }: { id: number; data: Partial<Model> }) => updateModel(id, data),
    {
      onSuccess: () => {
        message.success('更新成功')
        handleCloseModal()
        queryClient.invalidateQueries('models')
      },
    }
  )

  const handleCloseModal = () => {
    setIsModalOpen(false)
    setEditingModel(null)
    form.resetFields()
  }

  const handleSubmit = () => {
    form.validateFields().then((values) => {
      if (editingModel) {
        updateMutation.mutate({ id: editingModel.id, data: values })
      } else {
        createMutation.mutate(values)
      }
    })
  }

  const handleEdit = (record: Model) => {
    setEditingModel(record)
    form.setFieldsValue(record)
    setIsModalOpen(true)
  }

  const columns = [
    {
      title: '模型名称',
      dataIndex: 'modelName',
      key: 'modelName',
      width: 200,
      align: 'center' as const,
    },
    {
      title: '版本',
      dataIndex: 'modelVersion',
      key: 'modelVersion',
      width: 120,
      align: 'center' as const,
    },
    {
      title: '提供商',
      dataIndex: 'provider',
      key: 'provider',
      width: 140,
      align: 'center' as const,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      align: 'center' as const,
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>
          {status === 1 ? '可用' : '不可用'}
        </Tag>
      ),
    },
    {
      title: '输入价格',
      dataIndex: 'inputPrice',
      key: 'inputPrice',
      width: 160,
      align: 'center' as const,
      render: (price: number) => `¥${price}/1K tokens`,
    },
    {
      title: '输出价格',
      dataIndex: 'outputPrice',
      key: 'outputPrice',
      width: 160,
      align: 'center' as const,
      render: (price: number) => `¥${price}/1K tokens`,
    },
    {
      title: '流式输出',
      dataIndex: 'supportStream',
      key: 'supportStream',
      width: 120,
      align: 'center' as const,
      render: (support: boolean) => (
        <Tag color={support ? 'blue' : 'default'}>
          {support ? '支持' : '不支持'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      fixed: 'right' as const,
      align: 'center' as const,
      render: (record: Model) => (
        <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
          编辑
        </Button>
      ),
    },
  ]

  return (
    <div className="models">
      <PageHeader
        title="模型管理"
        primaryAction={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsModalOpen(true)}>
            发布模型
          </Button>
        }
      />

      <Table
        columns={columns}
        dataSource={models}
        rowKey="id"
        loading={isLoading}
        scroll={{ x: 1100 }}
      />

      <Modal
        title={editingModel ? '编辑模型' : '发布模型'}
        open={isModalOpen}
        onOk={handleSubmit}
        onCancel={handleCloseModal}
        confirmLoading={createMutation.isLoading || updateMutation.isLoading}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="modelName"
            label="模型名称"
            rules={[{ required: true, message: '请输入模型名称' }]}
          >
            <Input placeholder="例如: gpt-4" />
          </Form.Item>
          <Form.Item name="modelVersion" label="版本">
            <Input placeholder="例如: 1.0.0" />
          </Form.Item>
          <Form.Item
            name="provider"
            label="提供商"
            rules={[{ required: true, message: '请输入提供商' }]}
          >
            <Input placeholder="例如: openai" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="模型描述" />
          </Form.Item>
          <Form.Item name="inputPrice" label="输入价格 (¥/1K tokens)">
            <InputNumber min={0} step={0.001} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="outputPrice" label="输出价格 (¥/1K tokens)">
            <InputNumber min={0} step={0.001} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="maxTokens" label="最大 Tokens">
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="supportStream" label="支持流式输出" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="status" label="状态" initialValue={1}>
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default Models
