import request from './request'

export interface TaskLog {
  id: number
  type: string
  status: string
  startTime: string
  endTime: string
}

export const getTaskLogs = () => {
  return request.get<any, TaskLog[]>('/api/admin/tasks')
}

