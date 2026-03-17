import request from './request'

export interface KeyApplication {
  id?: number
  userId: number
  tenantId?: string
  appId?: string
  keyName: string
  allowedModels?: string
  reason?: string
  approvalStatus?: number
  approverId?: number
  approvalComment?: string
  approvalTime?: string
  createTime?: string
}

export const submitKeyApplication = (data: Omit<KeyApplication, 'id' | 'approvalStatus'>) => {
  return request.post<any, KeyApplication>('/api/admin/key-applications', data)
}

export const getMyKeyApplications = (userId: number) => {
  return request.get<any, KeyApplication[]>('/api/admin/key-applications/my', {
    params: { userId },
  })
}

export const getAllKeyApplications = (status?: number) => {
  return request.get<any, KeyApplication[]>('/api/admin/key-applications', {
    params: { status },
  })
}

export const approveKeyApplication = (id: number, approverId: number, comment?: string) => {
  return request.post<any, any>(`/api/admin/key-applications/${id}/approve`, null, {
    params: { approverId, comment },
  })
}

export const rejectKeyApplication = (id: number, approverId: number, comment?: string) => {
  return request.post<any, boolean>(`/api/admin/key-applications/${id}/reject`, null, {
    params: { approverId, comment },
  })
}

