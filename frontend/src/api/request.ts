import axios from 'axios'
import { message } from 'antd'

const request = axios.create({
  baseURL: (import.meta as any).env?.VITE_API_BASE_URL || '',
  timeout: 30000,
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const { code, message: msg, data } = response.data
    if (code === 200) {
      return data
    } else {
      message.error(msg || '请求失败')
      return Promise.reject(new Error(msg))
    }
  },
  (error) => {
    if (error.response?.status === 401) {
      const errMsg = error.response?.data?.message || 'Authentication failed'
      // API Key 过期/配额耗尽等业务 401，提示后不跳登录
      const isApiKeyError = errMsg.includes('expired') ||
          errMsg.includes('quota') ||
          errMsg.includes('API Key') ||
          errMsg.includes('Invalid or disabled')
      if (isApiKeyError) {
        message.error(errMsg, 5)
      } else {
        // JWT 失效等认证 401，提示后跳登录
        message.error(errMsg || '登录已过期，请重新登录', 3)
        setTimeout(() => {
          localStorage.removeItem('token')
          localStorage.removeItem('userId')
          localStorage.removeItem('username')
          localStorage.removeItem('role')
          window.location.href = '/login'
        }, 1500)
      }
      return Promise.reject(error)
    }
    message.error(error.response?.data?.message || error.message || '网络错误')
    return Promise.reject(error)
  }
)

// Chat API 单独实例（直接调用 gateway-core，9080 端口）
const chatRequest = axios.create({
  baseURL: (import.meta as any).env?.VITE_CHAT_BASE_URL || 'http://localhost:9080',
  timeout: 60000,
})

chatRequest.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

chatRequest.interceptors.response.use(
  (response) => {
    // Gateway Core 返回的是 Result<ChatResponse> 格式: { code, message, data }
    const { code, message: msg, data } = response.data
    if (code === 200) {
      return data  // 返回实际的 ChatResponse
    } else {
      message.error(msg || '请求失败')
      return Promise.reject(new Error(msg))
    }
  },
  (error) => {
    const errMsg = error.response?.data?.message || error.message || '网络错误'
    if (error.response?.status === 401) {
      // Gateway Key 鉴权失败，直接展示具体原因，不跳登录
      message.error(errMsg, 5)
    } else if (error.response?.status === 429) {
      message.error(errMsg || '请求过于频繁，请稍后再试', 5)
    } else if (error.response?.status === 502 || error.response?.status === 504) {
      message.error(errMsg || '上游服务暂时不可用，请稍后重试', 5)
    } else {
      message.error(errMsg)
    }
    return Promise.reject(new Error(errMsg))
  }
)

export { request, chatRequest }
export default request
