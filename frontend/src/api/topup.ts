import request from './request'

export interface TopUpRecord {
  id: number
  userId: number
  amount: number
  currency: string
  status: string
  createTime: string
}

export const getTopUps = () => {
  return request.get<any, TopUpRecord[]>('/api/admin/topups')
}

