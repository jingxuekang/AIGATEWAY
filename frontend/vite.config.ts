import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        // gateway-admin 默认端口在 gateway-admin/src/main/resources/application.yml
        // 这里需要与后端保持一致，否则前端会出现大量 404/500
        target: 'http://localhost:9082',
        changeOrigin: true,
      },
    },
  },
})