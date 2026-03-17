import { useState } from 'react'
import { Card, Input, Button, Select, Space, List, message } from 'antd'
import { SendOutlined } from '@ant-design/icons'
import { sendChatMessage, ChatMessage } from '../../api/chat'
import './index.css'

const { TextArea } = Input

const Chat = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [model, setModel] = useState('deepseek-chat')
  const [loading, setLoading] = useState(false)

  const handleSend = async () => {
    if (!input.trim()) return

    const userMessage: ChatMessage = {
      role: 'user',
      content: input,
    }

    setMessages([...messages, userMessage])
    setInput('')
    setLoading(true)

    try {
      console.log('Sending message:', { model, messages: [...messages, userMessage] })
      const response = await sendChatMessage({
        model,
        messages: [...messages, userMessage],
      })
      
      console.log('Received response:', response)

      if (response && response.choices && response.choices.length > 0) {
        const assistantMessage = response.choices[0].message
        console.log('Assistant message:', assistantMessage)
        setMessages((prev) => [...prev, assistantMessage])
      } else {
        console.error('Invalid response format:', response)
        message.error('响应格式错误')
      }
    } catch (error) {
      console.error('Send message error:', error)
      message.error('发送失败: ' + (error as Error).message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="chat">
      <Card title="聊天测试" extra={
        <Select value={model} onChange={setModel} style={{ width: 200 }}>
          <Select.Option value="gpt-4">gpt-4</Select.Option>
          <Select.Option value="gpt-3.5-turbo">gpt-3.5-turbo</Select.Option>
          <Select.Option value="claude-3-opus">claude-3-opus</Select.Option>
          <Select.Option value="claude-3-sonnet">claude-3-sonnet</Select.Option>
          <Select.Option value="deepseek-chat">deepseek-chat</Select.Option>
          <Select.Option value="deepseek-coder">deepseek-coder</Select.Option>
          <Select.Option value="azure-gpt-4o-mini">azure-gpt-4o-mini</Select.Option>
        </Select>
      }>
        <div className="chat-messages">
          <List
            dataSource={messages}
            renderItem={(item) => (
              <div className={`message ${item.role}`}>
                <div className="role">{item.role === 'user' ? '用户' : 'AI'}</div>
                <div className="content">{item.content}</div>
              </div>
            )}
          />
        </div>
        <Space.Compact style={{ width: '100%', marginTop: 16 }}>
          <TextArea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="输入消息..."
            autoSize={{ minRows: 2, maxRows: 6 }}
            onPressEnter={(e) => {
              if (!e.shiftKey) {
                e.preventDefault()
                handleSend()
              }
            }}
          />
          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={handleSend}
            loading={loading}
          >
            发送
          </Button>
        </Space.Compact>
      </Card>
    </div>
  )
}

export default Chat
