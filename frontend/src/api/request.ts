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
    const errMsg = error.response?.data?.message || error.message || '网络错误'
    message.error(errMsg, 5)
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
    message.error(errMsg, 5)
    return Promise.reject(new Error(errMsg))
  }
)

export { request, chatRequest }
export default request
