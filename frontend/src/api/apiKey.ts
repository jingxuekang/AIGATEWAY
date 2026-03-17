import request from './request'

export interface ApiKey {
  id: number
  keyValue: string
  keyName: string
  userId: number
  tenantId: string
  appId: string
  status: number
  expireTime: string
  allowedModels: string
  totalQuota: number
  usedQuota: number
  createTime: string
  updateTime: string
}

export const createApiKey = (data: Partial<ApiKey>) => {
  return request.post<any, ApiKey>('/api/admin/keys', data)
}

export const getUserKeys = (userId: number) => {
  return request.get<any, ApiKey[]>(`/api/admin/keys/user/${userId}`)
}

export const revokeApiKey = (keyId: number) => {
  return request.put<any, boolean>(`/api/admin/keys/${keyId}/revoke`)
}
