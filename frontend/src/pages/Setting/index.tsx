import { Form, Input, InputNumber, Switch, Button, Card, message, Tabs } from 'antd'
import { useMutation, useQuery, useQueryClient } from 'react-query'
import { request } from '../../api/request'
import PageHeader from '../../components/PageHeader'
import './index.css'

const Setting = () => {
  const [systemForm] = Form.useForm()
  const [securityForm] = Form.useForm()
  const queryClient = useQueryClient()

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

  const { isLoading: securityLoading } = useQuery(
    'security-settings',
    () => request.get<any, Record<string, string>>('/api/admin/security-settings'),
    {
      onSuccess: (cfg) => {
        securityForm.setFieldsValue({
          rateLimitEnabled: cfg['gateway.rate-limit.enabled'] === 'true',
          ipLimitEnabled: cfg['gateway.rate-limit.ip-limit-enabled'] === 'true',
          promptGuardEnabled: cfg['gateway.prompt-guard.enabled'] === 'true',
          sensitiveWordsEnabled: cfg['gateway.prompt-guard.sensitive-words-enabled'] === 'true',
        })
      },
    }
  )

  const saveSecurityMutation = useMutation(
    (values: any) => request.post('/api/admin/security-settings', {
      rateLimitEnabled: values.rateLimitEnabled,
      ipLimitEnabled: values.ipLimitEnabled,
      promptGuardEnabled: values.promptGuardEnabled,
      sensitiveWordsEnabled: values.sensitiveWordsEnabled,
    }),
    {
      onSuccess: () => {
        message.success('安全策略已保存')
        queryClient.invalidateQueries('security-settings')
      },
    }
  )

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
    <Card loading={securityLoading}>
      <Form
        form={securityForm}
        layout="vertical"
        onFinish={values => saveSecurityMutation.mutate(values)}
      >
        <Form.Item name="rateLimitEnabled" label="启用限流" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item name="ipLimitEnabled" label="启用 IP 限流" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item name="promptGuardEnabled" label="启用 Prompt 注入防护" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item name="sensitiveWordsEnabled" label="启用敏感词过滤" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item>
          <Button
            type="primary"
            htmlType="submit"
            loading={saveSecurityMutation.isLoading}
          >
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
