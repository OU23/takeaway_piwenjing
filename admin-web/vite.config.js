import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import viteCompression from 'vite-plugin-compression'

// 平台运营后台：构建优化（分块 / 压缩 / 按需）
export default defineConfig({
  base: '/admin',
  plugins: [
    vue(),
    viteCompression({ algorithm: 'gzip', ext: '.gz', threshold: 1024 }),
    viteCompression({ algorithm: 'brotliCompress', ext: '.br', threshold: 1024 })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 8081,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8090',
        changeOrigin: true
      }
    }
  },
  build: {
    target: 'es2018',
    cssCodeSplit: true,
    // 已按需引入 echarts，单块仍可能 200KB+，放宽告警阈值
    chunkSizeWarningLimit: 600,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('echarts') || id.includes('zrender')) return 'echarts-vendor'
            if (/[\\/]node_modules[\\/](vue|vue-router)[\\/]/.test(id)) return 'vue-vendor'
            if (id.includes('axios')) return 'axios'
          }
          return undefined
        }
      }
    },
    modulePreload: { polyfill: true }
  }
})
