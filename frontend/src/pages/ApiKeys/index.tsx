import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, Tag, Space, message, Typography, Progress, Tooltip, Alert } from 'antd'
import { PlusOutlined, DeleteOutlined, CopyOutlined, EditOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { request } from '../../api/request'
import { useNavigate } from 'react-router-dom'
import { getMyKeys, createApiKey, deleteApiKey, renameApiKey, ApiKey } from '../../api/apiKey'
import dayjs from 'dayjs'
import PageHeader from '../../components/PageHeader'
import './index.css'

const { Text } = Typography

const ApiKeys = () => {
  const navigate = useNavigate()
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [renameRecord, setRenameRecord] = useState<ApiKey | null>(null)
  const [newKeyValue, setNewKeyValue] = useState<string | null>(null)
  const [form] = Form.useForm()
  const [renameForm] = Form.useForm()
  const queryClient = useQueryClient()
  const isAdmin = (localStorage.getItem('role') || 'user') === 'admin'

  const { data: keys, isLoading } = useQuery(
    isAdmin ? 'allApiKeys' : 'myApiKeys',
    () => isAdmin
      ? request.get<any, ApiKey[]>('/api/admin/keys')
      : getMyKeys()
  )

  const { data: allModels } = useQuery<{ modelName: string }[]>(
    'allModelsForKey',
    () => request.get<any, { modelName: string }[]>('/api/admin/model-subscriptions'),
    { enabled: isAdmin }
  )

  const createMutation = useMutation(createApiKey, {
    onSuccess: (created: any) => {
      if (created?.keyValue) setNewKeyValue(created.keyValue)
      setIsModalOpen(false)
      form.resetFields()
      queryClient.invalidateQueries(isAdmin ? 'allApiKeys' : 'myApiKeys')
    },
    onError: (err: any) => message.error(err?.response?.data?.message || '创建失败'),
  })

  const revokeMutation = useMutation(deleteApiKey, {
    onSuccess: () => {
      message.success('删除成功')
      queryClient.invalidateQueries(isAdmin ? 'allApiKeys' : 'myApiKeys')
    },
  })

  const renameMutation = useMutation(
    ({ id, keyName }: { id: number; keyName: string }) => renameApiKey(id, keyName),
    {
      onSuccess: () => {
        message.success('名称已更新')
        setRenameRecord(null)
        renameForm.resetFields()
        queryClient.invalidateQueries(isAdmin ? 'allApiKeys' : 'myApiKeys')
        queryClient.invalidateQueries('myApiKeys')
      },
      onError: (err: any) => message.error(err?.response?.data?.message || '重命名失败'),
    }
  )

  const quotaRender = (_: any, r: ApiKey) => !r.totalQuota
    ? <Tag color="green">不限</Tag>
    : <Tooltip title={`${r.usedQuota} / ${r.totalQuota} tokens`}>
        <Progress percent={Math.min(100, Math.round((r.usedQuota || 0) / r.totalQuota * 100))}
          size="small" status={(r.usedQuota || 0) / r.totalQuota >= 0.9 ? 'exception' : 'normal'} />
      </Tooltip>

  const expireRender = (t: string) => t
    ? <Tag color={dayjs(t).isBefore(dayjs()) ? 'red' : 'default'}>{dayjs(t).format('YYYY-MM-DD')}</Tag>
    : <Tag color="green">永久</Tag>

  const modelRender = (m: string) => m
    ? m.split(',').map(v => <Tag key={v} color="geekblue" style={{ fontSize: 11 }}>{v}</Tag>)
    : <Tag color="green">不限</Tag>

  const actionRender = (_: any, r: ApiKey) => (
    <Space size={0}>
      <Button type="link" size="small" icon={<EditOutlined />} disabled={r.status === 0}
        onClick={() => { setRenameRecord(r); renameForm.setFieldsValue({ keyName: r.keyName }) }}
      >改名</Button>
      <Button type="link" danger size="small" icon={<DeleteOutlined />} disabled={r.status === 0}
        onClick={() => Modal.confirm({
          title: '确认删除？',
          content: `删除 ${r.keyName} 后无法恢复`,
          okType: 'danger',
          onOk: () => revokeMutation.mutate(r.id),
        })}
      >删除</Button>
    </Space>
  )

  const baseColumns = [
    { title: 'Key 名称', dataIndex: 'keyName', key: 'keyName', width: 140 },
    { title: 'API Key', dataIndex: 'keyValue', key: 'keyValue', width: 220,
      render: (v: string) => <Text code style={{ fontSize: 11 }}>{v}</Text> },
    { title: '绑定模型', dataIndex: 'allowedModels', key: 'allowedModels', width: 200, render: modelRender },
    { title: '配额', key: 'quota', width: 130, render: quotaRender },
    { title: '有效期', dataIndex: 'expireTime', key: 'expireTime', width: 110, render: expireRender },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 150,
      render: (t: string) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-' },
    { title: '状态', dataIndex: 'status', key: 'status', width: 75,
      render: (s: number) => <Tag color={s === 1 ? 'success' : 'error'}>{s === 1 ? '启用' : '禁用'}</Tag> },
    { title: '操作', key: 'action', width: 130, fixed: 'right' as const, render: actionRender },
  ]

  const adminColumns = [
    { title: '用户', dataIndex: 'username', key: 'username', width: 110,
      render: (v: string) => <Tag color="blue">{v || '-'}</Tag> },
    ...baseColumns,
  ]

  const emptyText = isAdmin
    ? <div style={{ padding: 24, textAlign: 'center', color: '#999' }}>暂无 API Key 数据</div>
    : (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <p style={{ color: '#999' }}>暂无 API Key，请先订阅模型后提交申请</p>
        <Space>
          <Button onClick={() => navigate('/models')}>浏览可用模型</Button>
          <Button type="primary" onClick={() => navigate('/key-applications')}>去申请 Key</Button>
        </Space>
      </div>
    )

  return (
    <div className="api-keys">
      <PageHeader
        title="API Keys"
        subtitle={isAdmin ? '查看平台所有用户的 API Key' : '通过申请流程获取 API Key'}
        primaryAction={
          isAdmin
            ? <Button type="primary" icon={<PlusOutlined />} onClick={() => { form.resetFields(); setIsModalOpen(true) }}>新建 Key</Button>
            : <Button type="primary" onClick={() => navigate('/key-applications')}>申请 API Key</Button>
        }
      />

      {newKeyValue && (
        <Alert
          style={{ marginBottom: 16 }}
          type="success"
          showIcon
          message="API Key 创建成功！请立即复制保存，关闭后将无法再次查看"
          description={
            <Space>
              <Text code style={{ fontSize: 13 }}>{newKeyValue}</Text>
              <Button size="small" icon={<CopyOutlined />}
                onClick={() => { navigator.clipboard.writeText(newKeyValue); message.success('已复制') }}
              >复制</Button>
            </Space>
          }
          closable
          onClose={() => setNewKeyValue(null)}
        />
      )}

      <Table
        columns={isAdmin ? adminColumns : baseColumns}
        dataSource={keys || []}
        rowKey="id"
        loading={isLoading}
        pagination={{ pageSize: 20, showSizeChanger: false }}
        size="middle"
        scroll={{ x: isAdmin ? 1300 : 1100 }}
        locale={{ emptyText }}
      />

      <Modal
        title="修改 Key 名称"
        open={!!renameRecord}
        onCancel={() => { setRenameRecord(null); renameForm.resetFields() }}
        onOk={() => renameForm.validateFields().then(v => renameMutation.mutate({
          id: renameRecord!.id,
          keyName: v.keyName,
        }))}
        confirmLoading={renameMutation.isLoading}
        destroyOnClose
      >
        <Form form={renameForm} layout="vertical">
          <Form.Item name="keyName" label="显示名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input maxLength={255} showCount placeholder="仅改展示名，不影响 sk- 密钥" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="新建 API Key"
        open={isModalOpen}
        onOk={() => form.validateFields().then(v => createMutation.mutate({
          ...v,
          allowedModels: Array.isArray(v.allowedModels) ? v.allowedModels.join(',') : v.allowedModels,
        }))}
        onCancel={() => setIsModalOpen(false)}
        confirmLoading={createMutation.isLoading}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="keyName" label="Key 名称" rules={[{ required: true, message: '请输入 Key 名称' }]}>
            <Input placeholder="例如：测试环境 Key" />
          </Form.Item>
          <Form.Item name="allowedModels" label="绑定模型（不选则不限）">
            <Select mode="multiple" placeholder="选择允许调用的模型">
              {(allModels || []).map(m =>
                <Select.Option key={m.modelName} value={m.modelName}>{m.modelName}</Select.Option>
              )}
            </Select>
          </Form.Item>
          <Form.Item name="totalQuota" label="总配额 Token 数（0=不限）" initialValue={0}>
            <Input type="number" min={0} />
          </Form.Item>
          <Form.Item name="tenantId" label="租户 ID"><Input placeholder="可选" /></Form.Item>
          <Form.Item name="appId" label="应用 ID"><Input placeholder="可选" /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default ApiKeys