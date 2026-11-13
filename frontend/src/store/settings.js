import { defineStore } from 'pinia'
import {
  DEFAULT_SETTINGS,
  getAtPath,
  normalizeSettings,
  setAtPath,
  validateSetting
} from './settings-schema'
import { getUserSettings, updateUserSettings, resetUserSettings } from '../api'
import { useAuthStore } from './auth'

/**
 * 偏好设置 store（SRS 9.11 设置模块）。
 *
 * 生效机制：外观类设置（主题/字号）改完立即生效（写 CSS class），无需重启；
 * 通知/隐私类即时更新 store，业务模块订阅 store 取值。
 *
 * 持久化：
 *  1. localStorage（xg_user_settings）——改完立即写，未登录也生效；
 *  2. 服务端 user_setting 表——登录后按 800ms 防抖同步，跨设备漫游。
 * 登录后 init() 会用服务端值覆盖本地（服务端为准），同步失败静默降级为本地模式。
 */

const LS_KEY = 'xg_user_settings'
const SYNC_DELAY = 800

function safeGet() {
  try {
    return JSON.parse(localStorage.getItem(LS_KEY)) || null
  } catch (e) {
    return null // 数据损坏时按首次使用处理，normalize 会回落默认值
  }
}

function safePut(data) {
  try {
    localStorage.setItem(LS_KEY, JSON.stringify(data))
  } catch (e) {
    /* 隐私模式下写入失败，仅本次会话内生效 */
  }
}

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    data: structuredClone(DEFAULT_SETTINGS),
    /** 服务端同步状态：idle | syncing | ok | error */
    syncState: 'idle',
    /** 待同步补丁（防抖窗口内累积） */
    pendingPatch: {},
    syncTimer: null
  }),

  getters: {
    /** 实际生效的主题（system 需查 matchMedia） */
    effectiveTheme: (s) =>
      s.data.appearance.theme === 'dark' ||
      (s.data.appearance.theme === 'system' &&
        window.matchMedia?.('(prefers-color-scheme: dark)').matches)
        ? 'dark'
        : 'light',
    themeLabel: (s) => ({ light: '亮色', dark: '暗色', system: '跟随系统' }[s.data.appearance.theme]),
    fontLabel: (s) => ({ small: '小', standard: '标准', large: '大' }[s.data.appearance.fontSize])
  },

  actions: {
    /**
     * 应用启动时调用：先本地恢复（同步，避免主题闪烁），
     * 已登录再拉服务端覆盖（异步，fire-and-forget，失败静默降级本地模式）。
     */
    init() {
      this.data = normalizeSettings(safeGet())
      this.applyAppearance()
      this.watchSystemTheme()

      const auth = useAuthStore()
      if (!auth.isLoggedIn) return
      getUserSettings()
        .then((r) => {
          this.data = normalizeSettings(r.data) // 服务端为权威值
          this.applyAppearance()
          safePut(this.data)
          this.syncState = 'ok'
        })
        .catch(() => {
          this.syncState = 'error'
        })
    },

    /**
     * 修改单项设置：校验 → 立即生效 → 本地持久化 → 防抖同步服务端。
     * 校验失败 reject Error（文案可直接 toast），成功 resolve。
     */
    async set(path, value) {
      const err = validateSetting(path, value)
      if (err) throw new Error(err)

      const before = getAtPath(this.data, path)
      setAtPath(this.data, path, value)

      // 跨项校验：免打扰起止时刻不能相同（区间无法解释）
      if (path === 'notification.quietStart' || path === 'notification.quietEnd') {
        if (this.data.notification.quietStart === this.data.notification.quietEnd) {
          setAtPath(this.data, path, before)
          throw new Error('免打扰开始与结束时刻不能相同')
        }
      }

      this.applyAppearance()
      safePut(this.data)
      return this.queueSync(path, value)
    },

    /** 防抖合并同步：800ms 内的连续修改只发一次 PUT（补丁为「分组.键」扁平键值） */
    queueSync(path, value) {
      this.pendingPatch[path] = value
      clearTimeout(this.syncTimer)
      return new Promise((resolve, reject) => {
        this.syncTimer = setTimeout(async () => {
          const patch = this.pendingPatch
          this.pendingPatch = {}
          if (!useAuthStore().isLoggedIn) return resolve() // 未登录：仅本地持久化
          this.syncState = 'syncing'
          try {
            await updateUserSettings(patch)
            this.syncState = 'ok'
            resolve()
          } catch (e) {
            this.syncState = 'error'
            reject(e) // 请求已 silent，是否提示由调用方决定
          }
        }, SYNC_DELAY)
      })
    },

    /** 重置全部设置：本地立即恢复默认 + 已登录时删服务端记录 */
    async reset() {
      clearTimeout(this.syncTimer)
      this.pendingPatch = {}
      this.data = structuredClone(DEFAULT_SETTINGS)
      this.applyAppearance()
      safePut(this.data)
      if (!useAuthStore().isLoggedIn) return
      try {
        await resetUserSettings()
        this.syncState = 'ok'
      } catch (e) {
        this.syncState = 'error'
        throw e
      }
    },

    /* ------------------------- 生效机制 ------------------------- */

    /** 主题与字号立即生效：挂到 <html> 上，全局令牌随 class 切换 */
    applyAppearance() {
      const el = document.documentElement
      el.classList.toggle('theme-dark', this.effectiveTheme === 'dark')
      el.classList.toggle('theme-light', this.effectiveTheme === 'light')
      const fs = this.data.appearance.fontSize
      el.classList.toggle('fs-small', fs === 'small')
      el.classList.toggle('fs-large', fs === 'large')
    },

    /** theme=system 时跟随系统切换（监听器常驻，代价可忽略） */
    watchSystemTheme() {
      window.matchMedia?.('(prefers-color-scheme: dark)')
        .addEventListener('change', () => this.applyAppearance())
    }
  }
})
