import request from './request'
import { chatRequest } from './request'

export interface DashboardStats {
  totalKeys: number
  totalModels: number
  totalRequests: number
  totalTokens: number
  todayRequests: number
  todayTokens: number
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
  dailyStats: Array<{ date: string; requests: number; tokens: number }>
  tokensByModel: Array<{ model: string; tokens: number; requests: number }>
  statsByProvider: Array<{ provider: string; providerLabel?: string; requests: number; tokens: number }>
  topUsers: Array<{ userId: string; username: string; requests: number; tokens: number }>
}

export interface CircuitBreakerStatus {
  provider: string
  state: 'CLOSED' | 'OPEN' | 'HALF_OPEN'
  healthy: boolean
  label: string
}

export const getDashboardStats = (): Promise<DashboardStats> => {
  return request.get<any, DashboardStats>('/api/admin/dashboard/statistics')
}

export const getCircuitBreakerStatus = (): Promise<CircuitBreakerStatus[]> => {
  return chatRequest.get<any, CircuitBreakerStatus[]>('/v1/status/circuit-breakers')
}
