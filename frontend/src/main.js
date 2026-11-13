import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './store/auth'
import { useSettingsStore } from './store/settings'
import { onSessionExpired } from './utils/session'
import { hasDraftFor } from './utils/draft'
import { toast } from './api/http'
import './styles/global.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia).use(router)

// 偏好设置在挂载前恢复：主题/字号同步应用，避免首帧闪白
useSettingsStore(pinia).init()

/**
 * 令牌失效的统一处理。
 *
 * 放在 main.js 而不是 http.js，是为了避免 http → router → store → api → http 的循环引用。
 * 只有「当前页需要登录」时才跳转：用户只是在首页浏览时令牌过期，不该被强行弹到登录页。
 */
let lastNotify = 0
onSessionExpired(() => {
  const auth = useAuthStore(pinia)
  const wasLoggedIn = auth.isLoggedIn
  auth.clear()

  const current = router.currentRoute.value
  if (!current.meta?.requiresAuth) return

  // 当前页若已暂存未提交的修改（地址表单、评价内容），提示里明确告诉用户不会丢
  const hadDraft = hasDraftFor(current.path)

  // 并发请求同时失效时只提示一次
  const now = Date.now()
  if (wasLoggedIn && now - lastNotify > 3000) {
    lastNotify = now
    toast(
      hadDraft
        ? '登录状态已过期，已暂存您填写的修改，重新登录后可继续'
        : '登录状态已过期，请重新登录',
      'error'
    )
  }
  router.replace({ path: '/login', query: { redirect: current.fullPath } })
})

app.mount('#app')
