/**
 * 会话存储与失效广播。
 *
 * 单独抽成不依赖 vue / pinia / axios 的模块，是为了打断循环引用：
 * http.js 需要读令牌，store 需要读写令牌，router 需要跳转，
 * 三者互相 import 会成环。这里只保留「存 + 事件」两件事。
 */

const TOKEN_KEY = 'xg_auth_token'
const USER_KEY = 'xg_auth_user'

/** 内存缓存：避免每次请求都读 localStorage（同步 API，高频调用会掉帧） */
let tokenCache = null

function safeLocalStorage() {
  try {
    return window.localStorage
  } catch (e) {
    // 隐私模式下 localStorage 可能抛异常
    return null
  }
}

export function getToken() {
  if (tokenCache !== null) return tokenCache
  const ls = safeLocalStorage()
  tokenCache = (ls && ls.getItem(TOKEN_KEY)) || ''
  return tokenCache
}

export function setToken(token) {
  tokenCache = token || ''
  const ls = safeLocalStorage()
  if (!ls) return
  if (tokenCache) ls.setItem(TOKEN_KEY, tokenCache)
  else ls.removeItem(TOKEN_KEY)
}

/** 用户资料快照：刷新页面时先渲染这个，避免头像/昵称闪一下「未登录」 */
export function getUserSnapshot() {
  const ls = safeLocalStorage()
  if (!ls) return null
  try {
    const raw = ls.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

export function setUserSnapshot(user) {
  const ls = safeLocalStorage()
  if (!ls) return
  if (user) ls.setItem(USER_KEY, JSON.stringify(user))
  else ls.removeItem(USER_KEY)
}

/* ---------------------------- 会话失效广播 ---------------------------- */

let expiredHandler = null

/** 由 main.js 注册（避免 http.js 直接依赖 router 造成循环引用） */
export function onSessionExpired(fn) {
  expiredHandler = fn
}

export function notifySessionExpired() {
  if (expiredHandler) expiredHandler()
}
