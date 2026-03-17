import { useState } from 'react'
import { Table, DatePicker, Select, Tag, Space, Button, Modal, message } from 'antd'
import { useQuery } from 'react-query'
import { request } from '../../api/request'
import dayjs from 'dayjs'
import PageHeader from '../../components/PageHeader'
import './index.css'

const { RangePicker } = DatePicker

interface Task {
  id: string
  taskName: string
  taskType: string
  status: string
  startTime: string
  endTime: string
  duration: number
  result: string
}

const Task = () => {
  const [params, setParams] = useState({
    page: 1,
    pageSize: 20,
  })

  interface TaskPage { list: Task[]; total: number }
  const { data, isLoading } = useQuery<TaskPage>(['tasks', params], () =>
    request.get<any, TaskPage>('/api/admin/tasks', { params })
  )

  const columns = [
    {
      title: '任务 ID',
      dataIndex: 'id',
      key: 'id',
      width: 200,
      ellipsis: true,
    },
    {
      title: '任务名称',
      dataIndex: 'taskName',
      key: 'taskName',
    },
    {
      title: '任务类型',
      dataIndex: 'taskType',
      key: 'taskType',
      render: (type: string) => {
        const colorMap: Record<string, string> = {
          quota_reset: 'blue',
          log_cleanup: 'green',
          model_sync: 'orange',
        }
        return <Tag color={colorMap[type] || 'default'}>{type}</Tag>
      },
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      render: (text: string) => dayjs(text).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      key: 'endTime',
      render: (text: string) => text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: '耗时(s)',
      dataIndex: 'duration',
      key: 'duration',
      render: (duration: number) => duration ? (duration / 1000).toFixed(2) : '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const colorMap: Record<string, string> = {
          running: 'blue',
          success: 'green',
          failed: 'red',
        }
        const textMap: Record<string, string> = {
          running: '运行中',
          success: '成功',
          failed: '失败',
        }
        return <Tag color={colorMap[status]}>{textMap[status]}</Tag>
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: Task) => (
        <Space>
          <Button
            type="link"
            onClick={() => {
              Modal.info({
                title: '任务详情',
                width: 600,
                content: (
                  <div style={{ marginTop: 16 }}>
                    <p><strong>任务 ID：</strong>{record.id}</p>
                    <p><strong>任务名称：</strong>{record.taskName}</p>
                    <p><strong>任务类型：</strong>{record.taskType}</p>
                    <p><strong>状态：</strong>{record.status}</p>
                    <p><strong>开始时间：</strong>{dayjs(record.startTime).format('YYYY-MM-DD HH:mm:ss')}</p>
                    <p><strong>结束时间：</strong>{record.endTime ? dayjs(record.endTime).format('YYYY-MM-DD HH:mm:ss') : '-'}</p>
                    <p><strong>耗时：</strong>{record.duration ? (record.duration / 1000).toFixed(2) + 's' : '-'}</p>
                    {record.result && (
                      <>
                        <p><strong>执行结果：</strong></p>
                        <pre style={{ background: '#f5f5f5', padding: 8, borderRadius: 4, maxHeight: 300, overflow: 'auto' }}>
                          {record.result}
                        </pre>
                      </>
                    )}
                  </div>
                ),
              })
            }}
          >
            详情
          </Button>
          {record.status === 'failed' && (
            <Button
              type="link"
              onClick={() => {
                Modal.confirm({
                  title: '重新执行',
                  content: '确定要重新执行这个任务吗？',
                  onOk: () => message.info('重新执行功能待实现'),
                })
              }}
            >
              重试
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div className="task">
      <PageHeader title="任务日志" />
      <Space style={{ marginBottom: 16 }} wrap>
        <RangePicker showTime />
        <Select placeholder="任务类型" style={{ width: 150 }} allowClear>
          <Select.Option value="quota_reset">配额重置</Select.Option>
          <Select.Option value="log_cleanup">日志清理</Select.Option>
          <Select.Option value="model_sync">模型同步</Select.Option>
        </Select>
        <Select placeholder="状态" style={{ width: 120 }} allowClear>
          <Select.Option value="running">运行中</Select.Option>
          <Select.Option value="success">成功</Select.Option>
          <Select.Option value="failed">失败</Select.Option>
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
      />
    </div>
  )
}

export default Task
