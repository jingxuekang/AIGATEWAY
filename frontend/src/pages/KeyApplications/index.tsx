import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, Tag, Space, message, Typography } from 'antd'
import { PlusOutlined, CheckOutlined, CloseOutlined, CopyOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { request } from '../../api/request'
import { ApiKey, getUserKeys } from '../../api/apiKey'
import dayjs from 'dayjs'
import PageHeader from '../../components/PageHeader'
import './index.css'

const { Text, Paragraph } = Typography

interface KeyApplication {
  id: number
  userId: number
  username: string
  keyName: string
  /** 提交申请时由系统自动生成，审批通过后写入 API Key */
  tenantId?: string
  allowedModels: string
  reason: string
  approvalStatus: number
  approvalComment: string
  createTime: string
  approvalTime: string
}

const KeyApplications = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [isApprovalModalOpen, setIsApprovalModalOpen] = useState(false)
  const [currentApplication, setCurrentApplication] = useState<KeyApplication | null>(null)
  const [generatedKey, setGeneratedKey] = useState<string | null>(null)
  const [targetKeyId, setTargetKeyId] = useState<number | null>(null)
  const [form] = Form.useForm()
  const [approvalForm] = Form.useForm()
  const queryClient = useQueryClient()

  const role = (localStorage.getItem('role') as 'admin' | 'user') || 'admin'
  const isAdmin = role === 'admin'

  const { data: subscribedModels } = useQuery<{ id: number; modelName: string }[]>(
    'subscribedModelsForApply',
    () => request.get<any, { id: number; modelName: string }[]>('/api/admin/model-subscriptions/my'),
    { enabled: !isAdmin }
  )

  const { data, isLoading } = useQuery<KeyApplication[]>(
    'keyApplications',
    () => isAdmin
      ? request.get<any, KeyApplication[]>('/api/admin/key-applications')
      : request.get<any, KeyApplication[]>('/api/admin/key-applications/my')
  )

  const { data: userKeys, isLoading: userKeysLoading } = useQuery<ApiKey[]>(
    ['userKeysForMerge', currentApplication?.userId],
    () => {
      if (!currentApplication) return Promise.resolve([])
      return getUserKeys(currentApplication.userId)
    },
    { enabled: isAdmin && isApprovalModalOpen && !!currentApplication?.userId }
  )

  const createMutation = useMutation(
    (values: any) => request.post<any, KeyApplication>('/api/admin/key-applications', values),
    {
      onSuccess: (created) => {
        queryClient.invalidateQueries('keyApplications')
        setIsModalOpen(false)
        form.resetFields()
        Modal.success({
          title: '申请已提交',
          width: 520,
          content: (
            <div>
              <p>等待管理员审批。系统已为本次申请分配<strong>租户 ID</strong>，审批通过后会写入 API Key，调用日志「租户」列将显示该标识。</p>
              {created?.tenantId ? (
                <Paragraph style={{ marginBottom: 0 }}>
                  <Text type="secondary">租户 ID（请保存）：</Text>
                  <br />
                  <Text code copyable={{ text: created.tenantId }} style={{ fontSize: 13 }}>
                    {created.tenantId}
                  </Text>
                </Paragraph>
              ) : null}
            </div>
          ),
        })
      },
      onError: (err: any) => { message.error(err?.response?.data?.message || '提交失败') },
    }
  )

  const approveMutation = useMutation(
    ({ id, comment, totalQuota, expireTime, targetKeyId }: {
      id: number
      comment: string
      totalQuota: number
      expireTime?: string
      targetKeyId?: number | null
    }) =>
      request.put<any, { keyValue?: string; id?: number }>(
        `/api/admin/key-applications/${id}/approve`,
        { comment, totalQuota, expireTime: expireTime || null, targetKeyId: targetKeyId || null }
      ),
    {
      onSuccess: (data) => {
        queryClient.invalidateQueries('keyApplications')
        queryClient.invalidateQueries('myApiKeys')
        setIsApprovalModalOpen(false)
        approvalForm.resetFields()
        setTargetKeyId(null)
        if (data?.keyValue) {
          setGeneratedKey(data.keyValue)
        } else {
          message.success('审批通过')
        }
      },
      onError: (err: any) => { message.error(err?.response?.data?.message || '审批失败') },
    }
  )

  const rejectMutation = useMutation(
    ({ id, comment }: { id: number; comment: string }) =>
      request.put(`/api/admin/key-applications/${id}/reject`, { comment }),
    {
      onSuccess: () => {
        message.success('已拒绝')
        queryClient.invalidateQueries('keyApplications')
        setIsApprovalModalOpen(false)
        approvalForm.resetFields()
      },
      onError: (err: any) => { message.error(err?.response?.data?.message || '操作失败') },
    }
  )

  const handleApprovalSubmit = (approved: boolean) => {
    approvalForm.validateFields().then(values => {
      if (approved) {
        approveMutation.mutate({
          id: currentApplication!.id,
          comment: values.comment,
          totalQuota: Number(values.totalQuota) || 0,
          expireTime: values.expireTime || undefined,
          targetKeyId: targetKeyId || null,
        })
      } else {
        rejectMutation.mutate({ id: currentApplication!.id, comment: values.comment })
      }
    })
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
    ...(isAdmin ? [{ title: '申请人', dataIndex: 'username', key: 'username', width: 110 }] : []),
    { title: 'Key 名称', dataIndex: 'keyName', key: 'keyName', width: 130 },
    {
      title: '租户 ID',
      dataIndex: 'tenantId',
      key: 'tenantId',
      width: 200,
      ellipsis: true,
      render: (t: string) =>
        t ? (
          <Text copyable={{ text: t }} code style={{ fontSize: 11 }}>
            {t}
          </Text>
        ) : (
          <Text type="secondary">—</Text>
        ),
    },
    {
      title: '申请模型', dataIndex: 'allowedModels', key: 'allowedModels',
      render: (models: string) => models?.split(',').map(m => <Tag key={m}>{m}</Tag>),
    },
    { title: '申请理由', dataIndex: 'reason', key: 'reason', ellipsis: true },
    {
      title: '状态', dataIndex: 'approvalStatus', key: 'approvalStatus', width: 90,
      render: (s: number) => {
        const cfg: Record<number, { color: string; text: string }> = {
          0: { color: 'orange', text: '待审批' },
          1: { color: 'green', text: '已通过' },
          2: { color: 'red', text: '已拒绝' },
        }
        return <Tag color={cfg[s]?.color}>{cfg[s]?.text}</Tag>
      },
    },
    ...(isAdmin ? [{ title: '审批意见', dataIndex: 'approvalComment', key: 'approvalComment', ellipsis: true }] : []),
    {
      title: '申请时间', dataIndex: 'createTime', key: 'createTime', width: 130,
      render: (t: string) => dayjs(t).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: '操作', key: 'action', width: 160,
      render: (_: any, record: KeyApplication) =>
        isAdmin && record.approvalStatus === 0 ? (
          <Space>
            <Button type="link" icon={<CheckOutlined />}
              onClick={() => { setCurrentApplication(record); approvalForm.resetFields(); setTargetKeyId(null); setIsApprovalModalOpen(true) }}
            >通过</Button>
            <Button type="link" danger icon={<CloseOutlined />}
              onClick={() => { setCurrentApplication(record); approvalForm.resetFields(); setTargetKeyId(null); setIsApprovalModalOpen(true) }}
            >拒绝</Button>
          </Space>
        ) : null,
    },
  ]

  return (
    <div className="key-applications">
      <PageHeader
        title={isAdmin ? '申请审批' : '我的申请'}
        subtitle={
          isAdmin
            ? '审批通过后自动生成 API Key，用户可在「API Keys」页面查看'
            : '先在「可用模型」页面订阅模型 → 提交申请（系统将自动生成租户 ID）→ 等待管理员审批 → 审批通过后可在「API Keys」页面查看完整 Key'
        }
        primaryAction={
          !isAdmin ? (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => { form.resetFields(); setIsModalOpen(true) }}>
              新建申请
            </Button>
          ) : null
        }
      />

      <Table columns={columns} dataSource={data || []} rowKey="id" loading={isLoading} />

      {/* 申请表单（普通用户）*/}
      <Modal title="申请 API Key" open={isModalOpen}
        onOk={() => form.validateFields().then(v => createMutation.mutate(v))}
        onCancel={() => { setIsModalOpen(false); form.resetFields() }}
        confirmLoading={createMutation.isLoading}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="keyName" label="Key 名称" rules={[{ required: true, message: '请输入 Key 名称' }]}>
            <Input placeholder="例如：生产环境 Key" />
          </Form.Item>
          <Form.Item name="allowedModels" label="申请模型" rules={[{ required: true, message: '请选择模型' }]}>
            <Select mode="multiple" placeholder={
              (subscribedModels || []).length === 0
                ? '请先在「可用模型」页面订阅模型'
                : '选择需要使用的模型'
            }>
              {(subscribedModels || []).map(m =>
                <Select.Option key={m.modelName} value={m.modelName}>{m.modelName}</Select.Option>
              )}
            </Select>
          </Form.Item>
          <Form.Item name="reason" label="申请理由" rules={[{ required: true, message: '请输入申请理由' }]}>
            <Input.TextArea rows={4} placeholder="请说明使用场景和预计调用量" />
          </Form.Item>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            提交后系统会自动生成租户 ID，并在弹窗与列表中展示，请妥善保存。
          </Typography.Text>
        </Form>
      </Modal>

      {/* Admin 审批弹窗 */}
      <Modal title={`审批申请：${currentApplication?.keyName}`}
        open={isApprovalModalOpen}
        onCancel={() => { setIsApprovalModalOpen(false); approvalForm.resetFields(); setTargetKeyId(null) }}
        footer={[
          <Button key="reject" danger loading={rejectMutation.isLoading}
            onClick={() => handleApprovalSubmit(false)}>拒绝</Button>,
          <Button key="approve" type="primary" loading={approveMutation.isLoading}
            onClick={() => handleApprovalSubmit(true)}>
            {targetKeyId ? '通过并追加到 Key' : '通过并生成 Key'}
          </Button>,
        ]}
      >
        {currentApplication?.tenantId ? (
          <Paragraph style={{ marginBottom: 16 }}>
            <Text type="secondary">租户 ID：</Text>
            <Text code copyable={{ text: currentApplication.tenantId }}>{currentApplication.tenantId}</Text>
          </Paragraph>
        ) : null}
        <div style={{ marginBottom: 16 }}>
          <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 6 }}>
            {targetKeyId ? '将把本次申请的模型追加到该 Key' : '可选：把本次申请的模型追加到已有 Key'}
          </Typography.Text>
          <Select
            value={targetKeyId ?? undefined}
            onChange={(v) => setTargetKeyId(v ? Number(v) : null)}
            style={{ width: '100%' }}
            placeholder="不选择则新建 Key"
            loading={userKeysLoading}
            allowClear
            disabled={!userKeys || (userKeys || []).length === 0 || userKeysLoading}
          >
            {(userKeys || [])
              .filter(k => k.status === 1)
              .map(k => (
                <Select.Option key={k.id} value={k.id}>
                  {`${k.keyName} (#${k.id})`}
                </Select.Option>
              ))}
          </Select>
        </div>

        <Form form={approvalForm} layout="vertical">
          <Form.Item name="comment" label="审批意见" rules={[{ required: true, message: '请输入审批意见' }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="totalQuota" label="Token 配额（0 = 不限制）" initialValue={0}>
            <Input type="number" min={0} placeholder="例如：1000000（即 100万 tokens）" />
          </Form.Item>
          <Form.Item name="expireTime" label="有效期（不填则永久有效）">
            <Input type="datetime-local" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 审批通过后展示生成的 Key */}
      <Modal
        title="审批通过 - API Key 已生成"
        open={!!generatedKey}
        footer={[
          <Button key="copy" type="primary" icon={<CopyOutlined />}
            onClick={() => { navigator.clipboard.writeText(generatedKey || ''); message.success('已复制') }}>
            复制 Key
          </Button>,
          <Button key="close" onClick={() => setGeneratedKey(null)}>关闭</Button>,
        ]}
        onCancel={() => setGeneratedKey(null)}
      >
        <div style={{ padding: '8px 0' }}>
          <p style={{ color: '#ff4d4f', marginBottom: 12, fontWeight: 500 }}>
            ⚠️ 用户可在「API Keys」页面查看完整 Key，此处可复制后通知用户。
          </p>
          <div style={{ background: '#f5f5f5', borderRadius: 6, padding: '12px 16px', fontFamily: 'monospace', fontSize: 13, wordBreak: 'break-all', border: '1px solid #d9d9d9' }}>
            <Text copyable>{generatedKey || ''}</Text>
          </div>
        </div>
      </Modal>
    </div>
  )
}

export default KeyApplications
