import request from './request'

export interface TaskLog {
  id: number
  username?: string
  userId?: string
  keyId?: string | null
  model?: string
  channelId?: string | null
  channelName?: string | null
  createdAt?: string
  status?: string
  error?: string | null
  costTimeMs?: number | null
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  requestBody?: string | null
  responseBody?: string | null
}

export const getTaskLogs = () => {
  return request.get<any, TaskLog[]>('/api/admin/tasks')
}

