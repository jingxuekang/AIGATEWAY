import request from './request'

export interface RedemptionCode {
  id: number
  code: string
  amount: number
  status: string
  expireTime: string
}

export const getRedemptions = () => {
  return request.get<any, RedemptionCode[]>('/api/admin/redemptions')
}

