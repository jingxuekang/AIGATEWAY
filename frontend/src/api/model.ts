import request from './request'

export interface Model {
  id: number
  modelName: string
  modelVersion: string
  provider: string
  description: string
  status: number
  inputPrice: number
  outputPrice: number
  maxTokens: number
  supportStream: boolean
  createTime: string
  updateTime: string
}

export const getModels = () => {
  return request.get<any, Model[]>('/api/admin/models')
}

export const createModel = (data: Partial<Model>) => {
  return request.post<any, Model>('/api/admin/models', data)
}

export const updateModel = (id: number, data: Partial<Model>) => {
  return request.put<any, boolean>(`/api/admin/models/${id}`, data)
}
