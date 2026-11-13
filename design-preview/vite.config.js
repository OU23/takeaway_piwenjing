import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 数创星光外卖 · 用户端 H5（独立工程，桌面浏览器以手机壳视口演示）
export default defineConfig({
  base: '/user',
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: { port: 8082, host: true }
})
