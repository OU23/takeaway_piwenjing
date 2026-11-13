/**
 * 后端地址解析（Web / Android App 双形态）。
 *
 * - Web（dev server）：走 Vite 代理，base = /api/v1
 * - Android App（真机 / 模拟器）：页面由本地 WebView 承载，没有代理，
 *   必须直连局域网内的后端；因此 App 内提供"服务器地址"设置项。
 *
 * 三种取值优先级：
 *   1) 用户在「我的 → 服务器地址」中手动设置（localStorage 持久化）
 *   2) 原生 App 默认值：Android 模拟器用 10.0.2.2 映射宿主机
 *   3) Web 默认值：/api/v1
 */

const STORAGE_KEY = 'xg_api_base'

/** Android 模拟器访问宿主机的固定地址 */
export const EMULATOR_BASE = 'http://10.0.2.2:8090/api/v1'

export const WEB_BASE = '/api/v1'

let nativeCache = null

/** 是否运行在 Capacitor 原生容器中 */
export function isNative() {
  if (nativeCache !== null) return nativeCache
  try {
    const cap = globalThis.Capacitor
    nativeCache = !!(cap && typeof cap.isNativePlatform === 'function' && cap.isNativePlatform())
  } catch (e) {
    nativeCache = false
  }
  return nativeCache
}

function normalize(base) {
  if (!base) return ''
  return String(base).trim().replace(/\/+$/, '')
}

/** 当前生效的后端地址 */
export function getApiBase() {
  const saved = normalize(localStorage.getItem(STORAGE_KEY))
  if (saved) return saved
  return isNative() ? EMULATOR_BASE : WEB_BASE
}

/** 是否由用户手动指定 */
export function isCustomApiBase() {
  return !!normalize(localStorage.getItem(STORAGE_KEY))
}

/** 设置后端地址；传空则恢复默认 */
export function setApiBase(base) {
  const v = normalize(base)
  if (v) localStorage.setItem(STORAGE_KEY, v)
  else localStorage.removeItem(STORAGE_KEY)
  return getApiBase()
}

/** 默认值（用于设置面板的占位提示） */
export function defaultApiBase() {
  return isNative() ? EMULATOR_BASE : WEB_BASE
}

/** 由 Base 推导健康检查地址 */
export function healthUrl(base) {
  return normalize(base || getApiBase()) + '/meta/dict'
}
