import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, Tag, Space, message } from 'antd'
import { PlusOutlined, CheckOutlined, CloseOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { request } from '../../api/request'
import dayjs from 'dayjs'
import PageHeader from '../../components/PageHeader'
import './index.css'

interface KeyApplication {
  id: number
  userId: number
  username: string
  keyName: string
  allowedModels: string
  reason: string
  approvalStatus: number
  approverComment: string
  createTime: string
  approvalTime: string
}

const KeyApplications = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [isApprovalModalOpen, setIsApprovalModalOpen] = useState(false)
  const [currentApplication, setCurrentApplication] = useState<KeyApplication | null>(null)
  const [form] = Form.useForm()
  const [approvalForm] = Form.useForm()
  const queryClient = useQueryClient()

  // 获取当前用户角色
  const role = (localStorage.getItem('role') as 'admin' | 'user') || 'admin'
  const isAdmin = role === 'admin'

  const { data, isLoading } = useQuery<KeyApplication[]>('keyApplications', () =>
    request.get<any, KeyApplication[]>('/api/admin/key-applications')
  )

  const createMutation = useMutation(
    (values: any) => request.post('/api/admin/key-applications', values),
    {
      onSuccess: () => {
        message.success('申请已提交')
        queryClient.invalidateQueries('keyApplications')
        setIsModalOpen(false)
        form.resetFields()
      },
    }
  )

  const approveMutation = useMutation(
    ({ id, ...values }: any) => request.put(`/api/admin/key-applications/${id}/approve`, values),
    {
      onSuccess: () => {
        message.success('审批成功')
        queryClient.invalidateQueries('keyApplications')
        setIsApprovalModalOpen(false)
        approvalForm.resetFields()
      },
    }
  )

  const handleApply = () => {
    form.resetFields()
    setIsModalOpen(true)
  }

  const handleSubmit = () => {
    form.validateFields().then(values => {
      createMutation.mutate(values)
    })
  }

  const handleApprove = (record: KeyApplication, approved: boolean) => {
    setCurrentApplication(record)
    approvalForm.setFieldsValue({ approved })
    setIsApprovalModalOpen(true)
  }

  const handleApprovalSubmit = () => {
    approvalForm.validateFields().then(values => {
      approveMutation.mutate({ id: currentApplication!.id, ...values })
    })
  }

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    ...(isAdmin ? [{
      title: '申请人',
      dataIndex: 'username',
      key: 'username',
    }] : []),
    {
      title: 'Key 名称',
      dataIndex: 'keyName',
      key: 'keyName',
    },
    {
      title: '申请模型',
      dataIndex: 'allowedModels',
      key: 'allowedModels',
      render: (models: string) => (
        <div>
          {models.split(',').map(m => (
            <Tag key={m}>{m}</Tag>
          ))}
        </div>
      ),
    },
    {
      title: '申请理由',
      dataIndex: 'reason',
      key: 'reason',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'approvalStatus',
      key: 'approvalStatus',
      render: (status: number) => {
        const config: Record<number, { color: string; text: string }> = {
          0: { color: 'orange', text: '待审批' },
          1: { color: 'green', text: '已通过' },
          2: { color: 'red', text: '已拒绝' },
        }
        return <Tag color={config[status].color}>{config[status].text}</Tag>
      },
    },
    ...(isAdmin ? [{
      title: '审批意见',
      dataIndex: 'approverComment',
      key: 'approverComment',
      ellipsis: true,
    }] : []),
    {
      title: '申请时间',
      dataIndex: 'createTime',
      key: 'createTime',
      render: (text: string) => dayjs(text).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: KeyApplication) => (
        isAdmin && record.approvalStatus === 0 ? (
          <Space>
            <Button
              type="link"
              icon={<CheckOutlined />}
              onClick={() => handleApprove(record, true)}
            >
              通过
            </Button>
            <Button
              type="link"
              danger
              icon={<CloseOutlined />}
              onClick={() => handleApprove(record, false)}
            >
              拒绝
            </Button>
          </Space>
        ) : null
      ),
    },
  ]

  return (
    <div className="key-applications">
      <PageHeader
        title={isAdmin ? '申请审批' : '我的申请'}
        primaryAction={
          !isAdmin ? (
            <Button type="primary" icon={<PlusOutlined />} onClick={handleApply}>
              新建申请
            </Button>
          ) : null
        }
      />

      <Table
        columns={columns}
        dataSource={data || []}
        rowKey="id"
        loading={isLoading}
      />

      <Modal
        title="申请 API Key"
        open={isModalOpen}
        onOk={handleSubmit}
        onCancel={() => {
          setIsModalOpen(false)
          form.resetFields()
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="keyName"
            label="Key 名称"
            rules={[{ required: true, message: '请输入 Key 名称' }]}
          >
            <Input placeholder="例如：生产环境 Key" />
          </Form.Item>

          <Form.Item
            name="allowedModels"
            label="申请模型"
            rules={[{ required: true, message: '请选择模型' }]}
          >
            <Select mode="multiple">
              <Select.Option value="gpt-4">gpt-4</Select.Option>
              <Select.Option value="gpt-3.5-turbo">gpt-3.5-turbo</Select.Option>
              <Select.Option value="claude-3-opus">claude-3-opus</Select.Option>
              <Select.Option value="claude-3-sonnet">claude-3-sonnet</Select.Option>
              <Select.Option value="deepseek-chat">deepseek-chat</Select.Option>
              <Select.Option value="deepseek-coder">deepseek-coder</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="reason"
            label="申请理由"
            rules={[{ required: true, message: '请输入申请理由' }]}
          >
            <Input.TextArea rows={4} placeholder="请说明使用场景和预计调用量" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="审批 API Key 申请"
        open={isApprovalModalOpen}
        onOk={handleApprovalSubmit}
        onCancel={() => {
          setIsApprovalModalOpen(false)
          approvalForm.resetFields()
        }}
      >
        <Form form={approvalForm} layout="vertical">
          <Form.Item name="approved" label="审批结果" hidden>
            <Input />
          </Form.Item>

          <Form.Item
            name="comment"
            label="审批意见"
            rules={[{ required: true, message: '请输入审批意见' }]}
          >
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default KeyApplications
