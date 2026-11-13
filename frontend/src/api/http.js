import axios from 'axios'
import { reactive } from 'vue'
import { getApiBase } from './base'
import { getToken, notifySessionExpired } from '../utils/session'
import { getRiderToken } from '../utils/rider'

/**
 * 全局提示（由 App.vue 渲染）
 * 必须是响应式对象：普通对象 push 不会触发模板更新，提示会静默丢失。
 */
export const toastState = reactive({ list: [] })

export function toast(msg, type = 'info', duration = 2000) {
  const id = Date.now() + Math.random()
  toastState.list.push({ id, msg, type })
  // 最多同时展示 3 条，避免连续失败时刷屏挡住页面
  if (toastState.list.length > 3) toastState.list.splice(0, toastState.list.length - 3)
  setTimeout(() => {
    const i = toastState.list.findIndex(t => t.id === id)
    if (i > -1) toastState.list.splice(i, 1)
  }, duration)
}

/** 业务错误码：未登录 / 令牌失效（与后端 AuthService.CODE_UNAUTHORIZED 一致） */
export const CODE_UNAUTHORIZED = 11001

/** 带业务码的请求异常，调用方可据此把错误落到具体字段上 */
export class ApiError extends Error {
  constructor(message, code) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

const http = axios.create({
  // 具体值在请求拦截器中按「Web / App」动态解析（见 api/base.js）
  baseURL: getApiBase(),
  timeout: 15000
})

// 每次请求都重新解析，保证用户在 App 内改完服务器地址后立即生效
http.interceptors.request.use((config) => {
  config.baseURL = getApiBase()
  // 骑手端：登录后签发的签名令牌（与用户端令牌受众隔离）
  const riderToken = getRiderToken()
  if (riderToken) config.headers['X-Rider-Token'] = riderToken
  else delete config.headers['X-Rider-Token']
  // 用户端：登录后签发的签名令牌；未登录时不带任何身份头（后端自行处理匿名浏览）
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  } else {
    delete config.headers.Authorization
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body.code !== 'undefined' && body.code !== 0) {
      const code = body.code
      const message = body.message || '请求失败'

      // 令牌失效：清理会话并跳登录页，不弹 toast（跳转本身就是反馈）
      if (code === CODE_UNAUTHORIZED) {
        notifySessionExpired()
        return Promise.reject(new ApiError(message, code))
      }
      // silent：调用方自己把错误渲染到表单上，避免 toast 与内联提示重复
      if (!resp.config?.silent) {
        toast(message, 'error')
      }
      return Promise.reject(new ApiError(message, code))
    }
    return body
  },
  (err) => {
    // 调用方主动取消（如路由切换）不提示
    if (axios.isCancel(err)) return Promise.reject(err)

    const silent = err?.config?.silent
    let msg = err?.response?.data?.message || err.message || '网络异常'
    if (err.code === 'ERR_NETWORK') {
      msg = `无法连接后端（${getApiBase()}），请在「我的 → 服务器地址」检查地址与网络`
    }
    if (!silent) toast(msg, 'error')
    return Promise.reject(new ApiError(msg, err?.response?.data?.code))
  }
)

export default http
