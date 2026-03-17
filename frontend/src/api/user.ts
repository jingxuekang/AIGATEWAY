import request from './request'

export interface AdminUser {
  id: number
  username: string
  email: string
  status: number
  createTime: string
}

export const getUsers = () => {
  return request.get<any, AdminUser[]>('/api/admin/users')
}

