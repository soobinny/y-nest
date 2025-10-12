import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // "/users"로 시작하는 모든 요청을 백엔드(8080)으로 프록시
      "/users": "http://localhost:8080",
    },
  },
})