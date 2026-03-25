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
  breadcrumb?: PageBreadcrumbItem[]
  primaryAction?: React.ReactNode
  moreActions?: PageHeaderActionItem[]
  actions?: React.ReactNode
}

type PageHeaderContextValue = {
  breadcrumb?: PageBreadcrumbItem[]
}

const PageHeaderContext = createContext<PageHeaderContextValue>({})

export const PageHeaderProvider: React.FC<React.PropsWithChildren<PageHeaderContextValue>> = ({
  breadcrumb,
  children,
}) => {
  return <PageHeaderContext.Provider value={{ breadcrumb }}>{children}</PageHeaderContext.Provider>
}

function useAutoBreadcrumb() {
  return useContext(PageHeaderContext).breadcrumb
}

const PageHeader: React.FC<PageHeaderProps> = ({
  title,
  subtitle,
  breadcrumb,
  primaryAction,
  moreActions,
  actions,
}) => {
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
    <header className="ag-page-header">
      {finalBreadcrumb?.length ? (
        <Breadcrumb
          className="ag-page-header-breadcrumb"
          items={finalBreadcrumb.map((b) => ({
            title: b.href ? (
              <a
                href={b.href}
                onClick={(e) => (b.onClick ? (e.preventDefault(), b.onClick()) : undefined)}
              >
                {b.title}
              </a>
            ) : (
              b.title
            ),
          }))}
        />
      ) : null}

      <div className="ag-page-header-main">
        <div className="ag-page-header-titles">
          <Typography.Title level={4} className="ag-page-header-title">
            {title}
          </Typography.Title>
          {subtitle ? <div className="ag-page-header-subtitle">{subtitle}</div> : null}
        </div>
        <div className="ag-page-header-actions">
          {actions}
          {dropdownItems.length ? (
            <Dropdown menu={{ items: dropdownItems }} placement="bottomRight" trigger={['click']}>
              <Button type="text" className="ag-page-header-more" icon={<EllipsisOutlined />} />
            </Dropdown>
          ) : null}
          {primaryAction}
        </div>
      </div>
    </header>
  )
}

export default PageHeader
