import { BrowserRouter } from 'react-router-dom'
import { ConfigProvider, App as AntdApp } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { QueryClient, QueryClientProvider } from 'react-query'
import Router from './router'
import './App.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 30000,
    },
  },
})

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider
        locale={zhCN}
        theme={{
          token: {
            colorPrimary: '#1677ff',
            borderRadius: 8,
            fontFamily: `-apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif`,
          },
          components: {
            Table: {
              borderRadius: 10,
              headerBg: '#fafafa',
            },
            Card: {
              borderRadius: 12,
            },
            Button: {
              borderRadius: 8,
            },
            Input: {
              borderRadius: 8,
            },
            Select: {
              borderRadius: 8,
            },
            Modal: {
              borderRadius: 12,
            },
          },
        }}
      >
        <AntdApp>
          <BrowserRouter>
            <Router />
          </BrowserRouter>
        </AntdApp>
      </ConfigProvider>
    </QueryClientProvider>
  )
}

export default App
