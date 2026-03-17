# AI Model Gateway Frontend

基于 React + TypeScript + Ant Design 的 AI 模型网关管理前端。

## 技术栈

- React 18
- TypeScript
- Ant Design 5
- React Router 6
- React Query
- Axios
- Vite

## 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

## 功能模块

- **仪表盘**: 总览统计数据
- **API Keys 管理**: 创建、查看、吊销 API Keys
- **模型管理**: 发布和管理 AI 模型
- **调用日志**: 查看模型调用记录
- **聊天测试**: 测试模型调用功能

## 项目结构

```
frontend/
├── src/
│   ├── api/           # API 接口
│   ├── components/    # 公共组件
│   ├── pages/         # 页面组件
│   ├── router/        # 路由配置
│   ├── App.tsx        # 应用入口
│   └── main.tsx       # 主入口
├── index.html
├── package.json
└── vite.config.ts
```
