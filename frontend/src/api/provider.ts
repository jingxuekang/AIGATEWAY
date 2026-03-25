import request from './request'

export interface Provider {
  id: number
  code: string
  name: string
  baseUrl: string
  apiKey: string
  status: number
  description?: string
  createTime: string
  updateTime: string
}

export const getProviders = () => {
  return request.get<any, Provider[]>('/api/admin/providers')
}

export const createProvider = (data: Partial<Provider>) => {
  return request.post<any, Provider>('/api/admin/providers', data)
}

export const updateProvider = (id: number, data: Partial<Provider>) => {
  return request.put<any, boolean>(`/api/admin/providers/${id}`, data)
}

export const deleteProvider = (id: number) => {
  return request.delete<any, boolean>(`/api/admin/providers/${id}`)
}

