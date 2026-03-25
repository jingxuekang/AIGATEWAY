import { useMemo, useState } from 'react'
import { DatePicker, Descriptions, Drawer, Select, Space, Table, Tag, Button, Typography } from 'antd'
import type { Dayjs } from 'dayjs'
import { useQuery } from 'react-query'
import dayjs from 'dayjs'
import { request } from '../../api/request'
import PageHeader from '../../components/PageHeader'
import './index.css'

const { RangePicker } = DatePicker
const { Text } = Typography

interface TaskLogRow {
  id: number
  username?: string
  userId?: string

  keyId?: string | null
  model?: string
  channelId?: string | null
  channelName?: string | null

  requestUrl?: string | null
  requestMethod?: string | null
  requestBody?: string | null
  responseHeaders?: any
  responseBody?: string | null

  promptTokens?: number
  completionTokens?: number
  totalTokens?: number

  quota?: number | null
  ip?: string | null

  status?: 'success' | 'failed' | string
  error?: string | null

  createdAt?: string
  updatedAt?: string
  costTimeMs?: number | null

  traceId?: string
  requestId?: string
  latencyMs?: number | null
}

const Task = () => {
  const [params, setParams] = useState({ page: 1, pageSize: 20 })
  const [timeRange, setTimeRange] = useState<[Dayjs, Dayjs] | null>(null)
  const [modelFilter, setModelFilter] = useState<string | undefined>(undefined)
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined) // usage_log status: success/error/timeout
  const [selectedLog, setSelectedLog] = useState<TaskLogRow | null>(null)

  const queryParams = useMemo(
    () => ({
      page: params.page,
      pageSize: params.pageSize,
      model: modelFilter || undefined,
      status: statusFilter || undefined,
      startTime: timeRange?.[0]?.format('YYYY-MM-DDTHH:mm:ss'),
      endTime: timeRange?.[1]?.format('YYYY-MM-DDTHH:mm:ss'),
    }),
    [params.page, params.pageSize, modelFilter, statusFilter, timeRange]
  )

  interface TaskLogPage {
    list: TaskLogRow[]
    total: number
    page?: number
    pageSize?: number
    pages?: number
  }

  const { data, isLoading } = useQuery<TaskLogPage>(
    ['taskLogs', queryParams],
    () => request.get<any, TaskLogPage>('/api/admin/tasks', { params: queryParams })
  )

  const modelOptions = useMemo(() => {
    return Array.from(new Set((data?.list || []).map((r) => r.model).filter(Boolean))).slice(0, 50)
  }, [data])

  const statusColor = (s: string | undefined) => {
    if (!s) return 'default'
    return s === 'success' ? 'green' : 'red'
  }

  const columns = [
    { title: '日志ID', dataIndex: 'id', key: 'id', width: 120 },
    {
      title: '用户',
      dataIndex: 'username',
      key: 'username',
      width: 160,
      render: (_: any, r: TaskLogRow) => r.username || r.userId || '—',
    },
    { title: '模型', dataIndex: 'model', key: 'model', width: 200 },
    {
      title: '渠道',
      dataIndex: 'channelName',
      key: 'channelName',
      width: 160,
      render: (_: any, r: TaskLogRow) => r.channelName || r.channelId || '—',
    },
    {
      title: '调用时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 190,
      render: (t: string) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '—'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 110,
      render: (s: string) => <Tag color={statusColor(s)}>{s === 'success' ? '成功' : '失败'}</Tag>,
    },
    {
      title: '耗时(ms)',
      dataIndex: 'costTimeMs',
      key: 'costTimeMs',
      width: 120,
      align: 'right' as const,
      render: (ms: number | null | undefined) => (ms != null ? ms : '—'),
    },
    { title: '输入Tokens', dataIndex: 'promptTokens', key: 'promptTokens', width: 120, align: 'right' as const, render: (v: any) => v ?? '—' },
    { title: '输出Tokens', dataIndex: 'completionTokens', key: 'completionTokens', width: 120, align: 'right' as const, render: (v: any) => v ?? '—' },
    { title: '总Tokens', dataIndex: 'totalTokens', key: 'totalTokens', width: 110, align: 'right' as const, render: (v: any) => v ?? '—' },
    { title: '错误信息', dataIndex: 'error', key: 'error', width: 260, ellipsis: true, render: (e: any) => e || '—' },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: any, r: TaskLogRow) => (
        <Button type="link" onClick={() => setSelectedLog(r)}>
          详情
        </Button>
      ),
    },
  ]

  const onFilterChange = () => setParams((p) => ({ ...p, page: 1 }))

  return (
    <div className="task">
      <PageHeader
        title="任务日志"
        subtitle="已将任务日志页面对齐 New-API 的日志明细风格：从 `usage_log` 读取并映射为 New-API 字段（近似）。"
      />

      <Space style={{ marginBottom: 16 }} wrap>
        <RangePicker
          showTime
          value={timeRange}
          onChange={(v) => {
            setTimeRange(v as [Dayjs, Dayjs] | null)
            onFilterChange()
          }}
        />

        <Select
          placeholder="模型"
          style={{ width: 220 }}
          allowClear
          value={modelFilter}
          onChange={(v) => {
            setModelFilter(v)
            onFilterChange()
          }}
        >
          {modelOptions.map((m) => (
            <Select.Option key={m} value={m}>
              {m}
            </Select.Option>
          ))}
        </Select>

        <Select
          placeholder="状态"
          style={{ width: 140 }}
          allowClear
          value={statusFilter}
          onChange={(v) => {
            setStatusFilter(v)
            onFilterChange()
          }}
        >
          <Select.Option value="success">成功</Select.Option>
          <Select.Option value="error">失败</Select.Option>
          <Select.Option value="timeout">超时</Select.Option>
        </Select>
      </Space>

      <Table
        columns={columns}
        dataSource={data?.list || []}
        rowKey="id"
        loading={isLoading}
        pagination={{
          current: params.page,
          pageSize: params.pageSize,
          total: data?.total || 0,
          onChange: (page, pageSize) => setParams({ page, pageSize }),
        }}
        scroll={{ x: 1600 }}
      />

      <Drawer
        width={760}
        title="日志详情（New-API 风格近似）"
        open={!!selectedLog}
        onClose={() => setSelectedLog(null)}
      >
        {selectedLog ? (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="日志ID">{selectedLog.id}</Descriptions.Item>
            <Descriptions.Item label="用户">{selectedLog.username || selectedLog.userId || '—'}</Descriptions.Item>
            <Descriptions.Item label="模型">{selectedLog.model || '—'}</Descriptions.Item>
            <Descriptions.Item label="渠道">{selectedLog.channelName || selectedLog.channelId || '—'}</Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={statusColor(selectedLog.status)}>{selectedLog.status === 'success' ? '成功' : '失败'}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="调用时间">{selectedLog.createdAt ? dayjs(selectedLog.createdAt).format('YYYY-MM-DD HH:mm:ss') : '—'}</Descriptions.Item>
            <Descriptions.Item label="耗时(ms)">{selectedLog.costTimeMs ?? '—'}</Descriptions.Item>

            <Descriptions.Item label="Tokens">
              <div>
                <Text type="secondary">输入：</Text> {selectedLog.promptTokens ?? '—'}
                <br />
                <Text type="secondary">输出：</Text> {selectedLog.completionTokens ?? '—'}
                <br />
                <Text type="secondary">总计：</Text> {selectedLog.totalTokens ?? '—'}
              </div>
            </Descriptions.Item>

            <Descriptions.Item label="错误信息">{selectedLog.error || '—'}</Descriptions.Item>
            <Descriptions.Item label="请求Body">
              <pre
                style={{
                  background: '#f5f5f5',
                  padding: 10,
                  borderRadius: 6,
                  maxHeight: 260,
                  overflow: 'auto',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word',
                }}
              >
                {selectedLog.requestBody || '—'}
              </pre>
            </Descriptions.Item>

            <Descriptions.Item label="响应Body">
              <pre
                style={{
                  background: '#f5f5f5',
                  padding: 10,
                  borderRadius: 6,
                  maxHeight: 260,
                  overflow: 'auto',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word',
                }}
              >
                {selectedLog.responseBody || '—'}
              </pre>
            </Descriptions.Item>
          </Descriptions>
        ) : null}
      </Drawer>
    </div>
  )
}

export default Task
