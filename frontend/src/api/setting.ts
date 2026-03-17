import request from './request'

export interface SystemSetting {
  siteName: string
  defaultModel: string
  language: string
}

export const getSettings = () => {
  return request.get<any, SystemSetting>('/api/admin/settings')
}

export const saveSettings = (data: Partial<SystemSetting>) => {
  return request.post<any, boolean>('/api/admin/settings', data)
}

