import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 数创星光外卖 · 商家端 H5
export default defineConfig({
  base: '/merchant',
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    port: 8086,
    host: true,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8090', changeOrigin: true }
    }
  }
})
