import { BrowserRouter } from 'react-router-dom'
import { ConfigProvider, App as AntdApp, theme } from 'antd'
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

/** 主色与圆角：对齐 New-API / Semi 风格 */
const agTheme = {
  token: {
    colorPrimary: '#0077fa',
    colorSuccess: '#00b42a',
    colorWarning: '#ff7d00',
    colorError: '#f53f3f',
    colorInfo: '#0077fa',
    colorTextBase: '#1c1f23',
    colorText: 'rgba(28, 31, 35, 0.88)',
    colorTextSecondary: 'rgba(28, 31, 35, 0.6)',
    colorTextTertiary: 'rgba(28, 31, 35, 0.45)',
    colorTextQuaternary: 'rgba(28, 31, 35, 0.35)',
    colorBgLayout: '#f7f8fa',
    colorBgContainer: '#ffffff',
    colorBorder: 'rgba(28, 31, 35, 0.08)',
    colorBorderSecondary: 'rgba(28, 31, 35, 0.06)',
    borderRadius: 6,
    borderRadiusLG: 8,
    borderRadiusSM: 4,
    fontFamily: `'Inter', 'Lato', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif`,
    fontSize: 14,
    fontSizeSM: 12,
    fontSizeLG: 16,
    fontSizeXL: 18,
    lineHeight: 1.5715,
    lineHeightLG: 1.5,
    lineHeightSM: 1.66,
    controlHeight: 36,
    controlHeightLG: 40,
    motionDurationFast: '0.12s',
    motionDurationMid: '0.18s',
  },
  components: {
    Layout: {
      headerBg: '#ffffff',
      bodyBg: '#f7f8fa',
      siderBg: '#ffffff',
      triggerBg: '#f7f8fa',
    },
    Menu: {
      itemBorderRadius: 6,
      itemHeight: 40,
      itemMarginInline: 8,
      itemMarginBlock: 4,
      itemPaddingInline: 12,
      iconSize: 18,
      collapsedIconSize: 18,
      groupTitleFontSize: 12,
      fontSize: 14,
      itemColor: 'rgba(28, 31, 35, 0.75)',
      itemHoverColor: '#0077fa',
      itemHoverBg: 'rgba(0, 119, 250, 0.08)',
      itemSelectedColor: '#0077fa',
      itemSelectedBg: 'rgba(0, 119, 250, 0.12)',
      subMenuItemBorderRadius: 6,
    },
    Table: {
      borderRadius: 8,
      headerBg: '#f2f3f5',
      headerColor: 'rgba(28, 31, 35, 0.8)',
      headerSplitColor: 'rgba(28, 31, 35, 0.06)',
      fontSize: 13,
      cellPaddingBlock: 10,
      cellPaddingInline: 14,
      cellFontSize: 13,
    },
    Card: {
      borderRadiusLG: 10,
      paddingLG: 20,
      headerFontSize: 15,
      headerFontSizeSM: 14,
    },
    Button: {
      borderRadius: 6,
      fontWeight: 500,
      controlHeight: 36,
      paddingContentHorizontal: 16,
    },
    Input: {
      borderRadius: 6,
      paddingBlock: 8,
      paddingInline: 12,
    },
    Select: {
      borderRadius: 6,
    },
    Modal: {
      borderRadiusLG: 10,
      titleFontSize: 16,
    },
    Form: {
      labelFontSize: 13,
      verticalLabelPadding: '0 0 6px',
    },
    Breadcrumb: {
      fontSize: 12,
      itemColor: 'rgba(28, 31, 35, 0.45)',
      linkColor: 'rgba(28, 31, 35, 0.6)',
      linkHoverColor: '#0077fa',
      separatorColor: 'rgba(28, 31, 35, 0.35)',
    },
    Tag: {
      borderRadiusSM: 4,
    },
    Tabs: {
      titleFontSize: 14,
    },
  },
  algorithm: theme.defaultAlgorithm,
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={zhCN} theme={agTheme}>
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
