import request from './request'

export interface Channel {
  id: number
  name: string
  provider: string
  status: number
  createTime: string
}

export const getChannels = () => {
  return request.get<any, Channel[]>('/api/admin/channels')
}

