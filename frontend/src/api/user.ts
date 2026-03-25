import request from './request'

export interface AdminUser {
  id: number
  username: string
  email: string
  role: string
  status: number
  quota: number
  usedQuota: number
  createTime: string
}

export const getUsers = () => {
  return request.get<any, AdminUser[]>('/api/admin/users')
}

export const createUser = (data: Omit<AdminUser, 'id' | 'usedQuota' | 'createTime'> & { password: string }) => {
  return request.post<any, AdminUser>('/api/admin/users', data)
}

export const updateUser = (id: number, data: Partial<AdminUser>) => {
  return request.put<any, boolean>(`/api/admin/users/${id}`, data)
}

export const deleteUser = (id: number) => {
  return request.delete<any, boolean>(`/api/admin/users/${id}`)
}
