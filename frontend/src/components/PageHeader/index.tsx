import React, { createContext, useContext } from 'react'
import { Breadcrumb, Button, Dropdown, Typography } from 'antd'
import { EllipsisOutlined } from '@ant-design/icons'
import './index.css'

export type PageBreadcrumbItem = {
  title: React.ReactNode
  href?: string
  onClick?: () => void
}

export type PageHeaderActionItem = {
  key: string
  label: React.ReactNode
  icon?: React.ReactNode
  danger?: boolean
  disabled?: boolean
  onClick?: () => void
}

export interface PageHeaderProps {
  title: React.ReactNode
  subtitle?: React.ReactNode
  breadcrumb?: PageBreadcrumbItem[] // 可覆盖自动面包屑
  primaryAction?: React.ReactNode // 主按钮：永远在最右
  moreActions?: PageHeaderActionItem[] // 次操作：收进更多菜单
  actions?: React.ReactNode // 兼容旧用法（不推荐）
}

type PageHeaderContextValue = {
  breadcrumb?: PageBreadcrumbItem[]
}

const PageHeaderContext = createContext<PageHeaderContextValue>({})

export const PageHeaderProvider: React.FC<React.PropsWithChildren<PageHeaderContextValue>> = ({ breadcrumb, children }) => {
  return <PageHeaderContext.Provider value={{ breadcrumb }}>{children}</PageHeaderContext.Provider>
}

function useAutoBreadcrumb() {
  return useContext(PageHeaderContext).breadcrumb
}

const PageHeader: React.FC<PageHeaderProps> = ({ title, subtitle, breadcrumb, primaryAction, moreActions, actions }) => {
  const autoBreadcrumb = useAutoBreadcrumb()
  const finalBreadcrumb = breadcrumb ?? autoBreadcrumb

  const dropdownItems = (moreActions ?? []).map((a) => ({
    key: a.key,
    label: a.label,
    icon: a.icon,
    danger: a.danger,
    disabled: a.disabled,
    onClick: () => a.onClick?.(),
  }))

  return (
    <div className="ag-page-header">
      <div className="ag-page-header-row">
        <div className="ag-page-header-left">
          {finalBreadcrumb?.length ? (
            <Breadcrumb
              items={finalBreadcrumb.map((b) => ({
                title: b.href ? (
                  <a href={b.href} onClick={(e) => (b.onClick ? (e.preventDefault(), b.onClick()) : undefined)}>
                    {b.title}
                  </a>
                ) : (
                  b.title
                ),
              }))}
            />
          ) : null}
        </div>

        <div className="ag-page-header-center">
          <Typography.Title level={2} className="ag-page-header-title">
            {title}
          </Typography.Title>
          {subtitle ? <div className="ag-page-header-subtitle">{subtitle}</div> : null}
        </div>

        <div className="ag-page-header-right">
          {actions}
          {dropdownItems.length ? (
            <Dropdown
              menu={{ items: dropdownItems }}
              placement="bottomRight"
              trigger={['click']}
            >
              <Button className="ag-page-header-more" icon={<EllipsisOutlined />} />
            </Dropdown>
          ) : null}
          {primaryAction}
        </div>
      </div>
    </div>
  )
}

export default PageHeader

