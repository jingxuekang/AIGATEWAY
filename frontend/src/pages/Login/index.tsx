import React, { useState } from 'react'
import { Form, Input, Button, message } from 'antd'
import { UserOutlined, LockOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { authApi } from '../../api/auth'
import './index.css'

const Login: React.FC = () => {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true)
    try {
      const res = await authApi.login(values)
      localStorage.setItem('token', res.token)
      localStorage.setItem('userId', String(res.userId))
      localStorage.setItem('username', res.username)
      localStorage.setItem('role', res.role)
      message.success('登录成功')
      navigate('/')
    } catch (e) {
      // error shown by interceptor
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-logo-row">
          <div className="login-logo-icon">
            <ThunderboltOutlined />
          </div>
          <h1 className="login-title">AI Gateway</h1>
        </div>
        <p className="login-subtitle">企业级大模型统一接入平台</p>
        <Form name="login" onFinish={onFinish} size="large" autoComplete="off">
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input prefix={<UserOutlined style={{ color: '#bbb' }} />} placeholder="用户名" />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password prefix={<LockOutlined style={{ color: '#bbb' }} />} placeholder="密码" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" block loading={loading} className="login-btn">
              登 录
            </Button>
          </Form.Item>
        </Form>
        <div className="login-footer">AI Model Gateway © 2026</div>
      </div>
    </div>
  )
}

export default Login
