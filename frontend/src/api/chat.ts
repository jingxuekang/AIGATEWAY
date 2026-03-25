import { chatRequest } from './request'

export interface ChatMessage {
  role: 'system' | 'user' | 'assistant'
  content: string | Array<{ type: string; text?: string; image_url?: { url: string; detail?: string } }>
}

export interface ChatRequest {
  model: string
  messages: ChatMessage[]
  stream?: boolean
  temperature?: number
  maxTokens?: number
}

export interface ChatResponse {
  id: string
  model: string
  choices: Array<{
    index: number
    message: ChatMessage
    finishReason: string
  }>
  usage: {
    promptTokens: number
    completionTokens: number
    totalTokens: number
  }
}

/**
 * 发送聊天消息
 * @param data 请求体
 * @param endpoint 接口路径，默认 /v1/chat/completions，多模态时传 /v1/responses
 */
export const sendChatMessage = (data: ChatRequest, endpoint = '/v1/chat/completions') => {
  return chatRequest.post<any, ChatResponse>(endpoint, data)
}
