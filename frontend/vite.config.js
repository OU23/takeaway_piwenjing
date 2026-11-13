import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import viteCompression from 'vite-plugin-compression'

// 数创星光外卖 · 用户端 H5：构建优化（分块 / 压缩 / 按需）
export default defineConfig(({ mode }) => ({
  plugins: [
    vue(),
    // 预压缩：构建期生成 .gz / .br，由 Nginx 的 gzip_static / brotli_static 直接吐，零运行时开销
    viteCompression({ algorithm: 'gzip', ext: '.gz', threshold: 1024 }),
    viteCompression({ algorithm: 'brotliCompress', ext: '.br', threshold: 1024 })
  ],
  // 生产包剔除调试语句：保留 warn/error，去掉 log/debug/info 与 debugger
  esbuild:
    mode === 'production'
      ? { drop: ['debugger'], pure: ['console.log', 'console.debug', 'console.info'] }
      : {},
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
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
    // 首屏必需资源（gzip）控制在预算内；echarts 不在此工程
    chunkSizeWarningLimit: 600,
    // 构建日志里不再重复计算 gzip 体积，构建更快
    reportCompressedSize: false,
    // 小于 4KB 的静态资源直接内联，省掉一轮请求
    assetsInlineLimit: 4096,
    rollupOptions: {
      output: {
        // 稳定 vendor 分块：业务改动不影响 vue/axios 缓存命中
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (/[\\/]node_modules[\\/](vue|vue-router|pinia)[\\/]/.test(id)) return 'vue-vendor'
            if (id.includes('axios')) return 'axios'
          }
          return undefined
        }
      }
    },
    // 默认开启：为动态 import 的路由 chunk 注入 modulepreload，缩短关键路径
    modulePreload: { polyfill: true }
  }
}))
