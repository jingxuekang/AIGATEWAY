import { Card, Form, Input, Button, Table, Tag, message } from 'antd'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import {
  submitKeyApplication,
  getMyKeyApplications,
  getAllKeyApplications,
  approveKeyApplication,
  rejectKeyApplication,
  KeyApplication,
} from '../../api/keyApplication'

const KeyApplicationPage = () => {
  const [form] = Form.useForm()
  const queryClient = useQueryClient()
  const userId = 1 // TODO: 替换为登录用户 ID
  const approverId = 1 // TODO: 替换为当前管理员 ID

  const { data: applications, isLoading } = useQuery(['keyApplications', userId], () =>
    getMyKeyApplications(userId)
  )
  const { data: allApplications, isLoading: isLoadingAll } = useQuery(
    ['allKeyApplications'],
    () => getAllKeyApplications(),
  )

  const submitMutation = useMutation(submitKeyApplication, {
    onSuccess: () => {
      message.success('提交成功')
      form.resetFields()
      queryClient.invalidateQueries(['keyApplications', userId])
    },
  })

  const approveMutation = useMutation(
    ({ id, comment }: { id: number; comment?: string }) =>
      approveKeyApplication(id, approverId, comment),
    {
      onSuccess: () => {
        message.success('已通过')
        queryClient.invalidateQueries(['allKeyApplications'])
        queryClient.invalidateQueries(['keyApplications', userId])
      },
    },
  )

  const rejectMutation = useMutation(
    ({ id, comment }: { id: number; comment?: string }) =>
      rejectKeyApplication(id, approverId, comment),
    {
      onSuccess: () => {
        message.success('已拒绝')
        queryClient.invalidateQueries(['allKeyApplications'])
        queryClient.invalidateQueries(['keyApplications', userId])
      },
    },
  )

  const handleSubmit = () => {
    form.validateFields().then((values) => {
      submitMutation.mutate({
        ...values,
        userId,
      })
    })
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id' },
    { title: 'Key 名称', dataIndex: 'keyName', key: 'keyName' },
    { title: '允许模型', dataIndex: 'allowedModels', key: 'allowedModels' },
    { title: '申请原因', dataIndex: 'reason', key: 'reason' },
    {
      title: '审批状态',
      dataIndex: 'approvalStatus',
      key: 'approvalStatus',
      render: (status: number) => {
        let color = 'default'
        let text = '待审批'
        if (status === 1) {
          color = 'green'
          text = '已通过'
        } else if (status === 2) {
          color = 'red'
          text = '已拒绝'
        }
        return <Tag color={color}>{text}</Tag>
      },
    },
  ]

  const adminColumns = [
    ...columns,
    {
      title: '操作',
      key: 'action',
      render: (record: KeyApplication) => (
        <>
          <Button
            type="link"
            onClick={() => approveMutation.mutate({ id: record.id!, comment: '' })}
            disabled={record.approvalStatus !== 0}
          >
            通过
          </Button>
          <Button
            type="link"
            danger
            onClick={() => rejectMutation.mutate({ id: record.id!, comment: '' })}
            disabled={record.approvalStatus !== 0}
          >
            拒绝
          </Button>
        </>
      ),
    },
  ]

  return (
    <div>
      <Card title="申请 API Key" style={{ marginBottom: 24 }}>
        <Form form={form} layout="vertical">
          <Form.Item
            name="keyName"
            label="Key 名称"
            rules={[{ required: true, message: '请输入 Key 名称' }]}
          >
            <Input placeholder="例如：业务A-测试环境" />
          </Form.Item>
          <Form.Item name="tenantId" label="租户 ID">
            <Input placeholder="可选" />
          </Form.Item>
          <Form.Item name="appId" label="应用 ID">
            <Input placeholder="可选" />
          </Form.Item>
          <Form.Item name="allowedModels" label="申请使用的模型（逗号分隔）">
            <Input placeholder="例如：gpt-4,gpt-3.5-turbo" />
          </Form.Item>
          <Form.Item name="reason" label="申请原因">
            <Input.TextArea rows={3} placeholder="请描述用途、预估用量、关联项目等" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" onClick={handleSubmit} loading={submitMutation.isLoading}>
              提交申请
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card title="我的申请" loading={isLoading} style={{ marginBottom: 24 }}>
        <Table<KeyApplication> rowKey="id" columns={columns} dataSource={applications || []} />
      </Card>

      <Card title="审批列表（管理员）" loading={isLoadingAll}>
        <Table<KeyApplication>
          rowKey="id"
          columns={adminColumns}
          dataSource={allApplications || []}
        />
      </Card>
    </div>
  )
}

export default KeyApplicationPage

