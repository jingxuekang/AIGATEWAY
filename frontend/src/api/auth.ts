import request from './request'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  role: string
  expiresIn: number
}

/** /api/admin/auth/me 返回结构 */
export interface MeResponse {
  userId: number
  role: string
  user?: {
    id: number
    username: string
    email?: string
    quota: number
    usedQuota: number
    status: number
    createTime?: string
  }
  admin?: {
    id: number
    username: string
    email?: string
    role: string
    status?: number
  }
}

export const authApi = {
  login: (data: LoginRequest): Promise<LoginResponse> =>
    request.post('/api/admin/auth/login', data),

  logout: () => request.post('/api/admin/auth/logout'),

  me: (): Promise<MeResponse> => request.get('/api/admin/auth/me'),
}
