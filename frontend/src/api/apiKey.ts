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

/** 获取当前登录用户自己的 Key（走 /my 接口，不需要传 userId）*/
export const getMyKeys = () => {
  return request.get<any, ApiKey[]>('/api/admin/keys/my')
}

/** admin 查看指定用户的 Key */
export const getUserKeys = (userId: number) => {
  return request.get<any, ApiKey[]>(`/api/admin/keys/user/${userId}`)
}

export const revokeApiKey = (keyId: number) => {
  return request.put<any, boolean>(`/api/admin/keys/${keyId}/revoke`)
}

export const deleteApiKey = (keyId: number) => {
  return request.delete<any, boolean>(`/api/admin/keys/${keyId}`)
}

/** 修改 Key 显示名称 */
export const renameApiKey = (keyId: number, keyName: string) => {
  return request.patch<any, boolean>(`/api/admin/keys/${keyId}/key-name`, { keyName })
}
