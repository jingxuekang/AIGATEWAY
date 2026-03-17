import request from './request'

export interface ModelSubscription {
  id: number
  modelId: number
  userId: number
  tenantId?: string
  appId?: string
  status: number
  createTime?: string
  updateTime?: string
}

export const subscribeModel = (modelId: number, userId: number, tenantId?: string, appId?: string) => {
  return request.post<any, boolean>('/api/admin/model-subscriptions/subscribe', null, {
    params: { modelId, userId, tenantId, appId },
  })
}

export const unsubscribeModel = (modelId: number, userId: number, tenantId?: string, appId?: string) => {
  return request.post<any, boolean>('/api/admin/model-subscriptions/unsubscribe', null, {
    params: { modelId, userId, tenantId, appId },
  })
}

export const getMySubscriptions = (userId: number, tenantId?: string, appId?: string) => {
  return request.get<any, ModelSubscription[]>('/api/admin/model-subscriptions/my', {
    params: { userId, tenantId, appId },
  })
}

