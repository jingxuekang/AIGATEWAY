import request from './request'

export interface DashboardStats {
  totalKeys: number
  totalModels: number
  totalRequests: number
  totalTokens: number
  successRequests: number
  successRate: number
  avgLatencyMs: number
  avgTtftMs: number
  recentLogs: Array<{
    id: number
    timestamp: string
    model: string
    status: string
    totalTokens: number
    latencyMs: number
  }>
}

export const getDashboardStats = (): Promise<DashboardStats> => {
  return request.get<any, DashboardStats>('/api/admin/dashboard/statistics')
}
