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

export const authApi = {
  login: (data: LoginRequest): Promise<LoginResponse> =>
    request.post('/api/admin/auth/login', data),

  logout: () => request.post('/api/admin/auth/logout'),

  me: (): Promise<LoginResponse> => request.get('/api/admin/auth/me'),
}
