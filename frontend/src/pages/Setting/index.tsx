import { Form, Input, InputNumber, Switch, Button, Card, message, Tabs } from 'antd'
import { useMutation } from 'react-query'
import { request } from '../../api/request'
import PageHeader from '../../components/PageHeader'
import './index.css'

const Setting = () => {
  const [systemForm] = Form.useForm()
  const [securityForm] = Form.useForm()

  const updateMutation = useMutation(
    (values: any) => request.put('/api/admin/settings', values),
    {
      onSuccess: () => {
        message.success('保存成功')
      },
    }
  )

  const handleSystemSubmit = () => {
    systemForm.validateFields().then(values => {
      updateMutation.mutate({ ...values, category: 'system' })
    })
  }

  const handleSecuritySubmit = () => {
    securityForm.validateFields().then(values => {
      updateMutation.mutate({ ...values, category: 'security' })
    })
  }

  const systemSettings = (
    <Card>
      <Form form={systemForm} layout="vertical">
        <Form.Item name="siteName" label="站点名称" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item name="siteUrl" label="站点 URL" rules={[{ required: true, type: 'url' }]}>
          <Input />
        </Form.Item>
        <Form.Item name="defaultQuota" label="新用户默认配额" rules={[{ required: true }]}>
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="enableRegistration" label="允许用户注册" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item name="enableApiKeyApplication" label="允许申请 API Key" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item>
          <Button type="primary" onClick={handleSystemSubmit} loading={updateMutation.isLoading}>
            保存系统设置
          </Button>
        </Form.Item>
      </Form>
    </Card>
  )

  const securitySettings = (
    <Card>
      <Form form={securityForm} layout="vertical">
        <Form.Item name="maxRequestsPerMinute" label="每分钟最大请求数" rules={[{ required: true }]}>
          <InputNumber min={1} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="maxRequestsPerDay" label="每天最大请求数" rules={[{ required: true }]}>
          <InputNumber min={1} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="enableIpWhitelist" label="启用 IP 白名单" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item name="ipWhitelist" label="IP 白名单（每行一个）">
          <Input.TextArea rows={4} placeholder="192.168.1.1" />
        </Form.Item>
        <Form.Item name="enableAuditLog" label="启用审计日志" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item>
          <Button type="primary" onClick={handleSecuritySubmit} loading={updateMutation.isLoading}>
            保存安全设置
          </Button>
        </Form.Item>
      </Form>
    </Card>
  )

  return (
    <div className="setting">
      <PageHeader title="系统设置" />
      <Tabs items={[
        { key: 'system', label: '系统设置', children: systemSettings },
        { key: 'security', label: '安全设置', children: securitySettings },
      ]} />
    </div>
  )
}

export default Setting
