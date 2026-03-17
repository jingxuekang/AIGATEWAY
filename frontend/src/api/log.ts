import request from './request'

export interface LogEntry {
  id: string
  timestamp: string
  model: string
  status: string
  latencyMs: number
  promptTokens: number
  completionTokens: number
  totalTokens: number
}

export interface LogQueryParams {
  startTime?: string
  endTime?: string
  model?: string
  status?: string
  page?: number
  pageSize?: number
}

export interface LogResponse {
  list: LogEntry[]
  total: number
  page: number
  pageSize: number
}

export const queryLogs = (params: LogQueryParams) => {
  return request.get<any, LogResponse>('/api/admin/logs', { params })
}

export const getLogStatistics = (startTime?: string, endTime?: string) => {
  return request.get<any, any>('/api/admin/logs/statistics', {
    params: { startTime, endTime }
  })
}
