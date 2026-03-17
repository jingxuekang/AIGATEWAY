import { chatRequest } from './request'

export interface ChatMessage {
  role: 'system' | 'user' | 'assistant'
  content: string
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

export const sendChatMessage = (data: ChatRequest) => {
  return chatRequest.post<any, ChatResponse>('/v1/chat/completions', data)
}
