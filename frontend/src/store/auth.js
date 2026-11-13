import { defineStore } from 'pinia'
import { getSession, loginAccount, loginBySms, logoutAccount, registerAccount } from '../api'
import { getToken, getUserSnapshot, setToken, setUserSnapshot } from '../utils/session'
import { useAppStore } from './app'

/**
 * 登录会话。
 *
 * 令牌与用户快照都落 localStorage：刷新页面、杀掉 WebView 重进都能恢复登录态，
 * 恢复时再用 /auth/me 校验一次，避免拿着一张过期令牌渲染出「假登录」界面。
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: getUserSnapshot(),
    /** 是否已完成一次会话恢复（路由守卫据此决定要不要等） */
    ready: false,
    /** 登录/注册请求进行中，用于禁用按钮防重复提交 */
    pending: false
  }),

  getters: {
    isLoggedIn: (s) => !!s.token,
    displayName: (s) => s.user?.nickname || s.user?.username || '未登录',
    /** 「我的」页展示的次要标识：优先邮箱，其次用户名 */
    accountLabel: (s) => s.user?.email || s.user?.username || ''
  },

  actions: {
    apply(payload) {
      this.token = payload?.token || ''
      this.user = payload?.user || null
      setToken(this.token)
      setUserSnapshot(this.user)
      this.syncAppState()
    },

    clear() {
      const had = !!this.token
      this.token = ''
      this.user = null
      setToken('')
      setUserSnapshot(null)
      // 启动时本来就没登录，不必再触发一轮刷新（App.bootstrap 已经拉过）
      if (had) this.syncAppState()
    },

    /**
     * 身份变了，app store 里缓存的资料与购物车角标就过期了。
     * 不刷新的话：登录后「我的」仍显示匿名演示用户，登出后仍显示上一个人的余额。
     */
    syncAppState() {
      const app = useAppStore()
      app.loadUser()
      app.refreshCartCount()
    },

    async login(account, password) {
      this.pending = true
      try {
        const r = await loginAccount({ account, password })
        this.apply(r.data)
        return r.data
      } finally {
        this.pending = false
      }
    },

    /** 手机验证码登录：号码未注册时后端会自动建号 */
    async loginSms(phone, code) {
      this.pending = true
      try {
        const r = await loginBySms({ phone, code })
        this.apply(r.data)
        return r.data
      } finally {
        this.pending = false
      }
    },

    async register({ username, email, phone, code, password }) {
      this.pending = true
      try {
        const r = await registerAccount({ username, email, phone, code, password })
        this.apply(r.data)
        return r.data
      } finally {
        this.pending = false
      }
    },

    /** 应用启动时校验本地令牌；只在首次导航前调用一次 */
    async restore() {
      if (this.ready) return
      if (!this.token) {
        this.clear()
        this.ready = true
        return
      }
      try {
        const r = await getSession()
        this.user = r.data?.user || null
        setUserSnapshot(this.user)
      } catch (e) {
        // 只有「令牌确实无效」才清会话；后端没启动等网络问题不清，
        // 否则一次断网就会把用户踢下线。
        if (e?.code === 11001) this.clear()
      } finally {
        this.ready = true
      }
    },

    async logout() {
      try {
        await logoutAccount()
      } catch (e) {
        /* 无状态令牌：服务端无需配合，本地清理即可 */
      }
      this.clear()
    }
  }
})
