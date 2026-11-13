/**
 * 骑手端登录态（与用户端 session.js 隔离）。
 * token 原型即 rider.id，放在请求头 X-Rider-Token。
 */
const KEY = 'xg_rider_token'

export function getRiderToken() {
  try {
    return localStorage.getItem(KEY) || ''
  } catch (e) {
    return ''
  }
}

export function setRiderToken(token) {
  try {
    if (token) localStorage.setItem(KEY, String(token))
    else localStorage.removeItem(KEY)
  } catch (e) {
    /* 隐私模式忽略 */
  }
}

export function clearRiderToken() {
  setRiderToken('')
}
