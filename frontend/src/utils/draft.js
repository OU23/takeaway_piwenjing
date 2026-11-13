/**
 * 表单草稿暂存（sessionStorage）。
 *
 * 解决的问题：用户在编辑页面（收货地址、订单评价等）填到一半时登录态失效，
 * 被强制跳到登录页；如果登录失败或用户暂时放弃登录，返回后已填内容就没了。
 *
 * 约定：
 * - 每个草稿带 `path`（产生草稿的页面路由）与 `ts`（写入时间），便于登录页/会话失效
 *   处理器判断「当前页有没有待恢复的修改」，也便于过期清理（24 小时）。
 * - 只存在 sessionStorage：关掉 WebView/标签即失效，不污染长期存储，也不跨账号串数据。
 * - 提交成功必须调用 clearDraft，避免下次进来恢复出已提交的旧内容。
 */

const PREFIX = 'xg_draft:'
const MAX_AGE_MS = 24 * 3600 * 1000

function store() {
  try {
    return window.sessionStorage
  } catch (e) {
    // 隐私模式可能不可用，降级为不暂存（不影响主流程）
    return null
  }
}

/** 保存草稿；data 为空对象/空值时视为清除 */
export function saveDraft(key, data, path = '') {
  const ss = store()
  if (!ss) return
  if (!data || (typeof data === 'object' && !Array.isArray(data) && !Object.keys(data).length)) {
    ss.removeItem(PREFIX + key)
    return
  }
  try {
    ss.setItem(PREFIX + key, JSON.stringify({ path, ts: Date.now(), data }))
  } catch (e) {
    /* 存储写满等异常：草稿是增强能力，失败不应打断用户操作 */
  }
}

/** 读取草稿；不存在或已过期返回 null */
export function loadDraft(key) {
  const ss = store()
  if (!ss) return null
  try {
    const raw = ss.getItem(PREFIX + key)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') return null
    if (parsed.ts && Date.now() - parsed.ts > MAX_AGE_MS) {
      ss.removeItem(PREFIX + key)
      return null
    }
    return parsed.data ?? null
  } catch (e) {
    return null
  }
}

export function clearDraft(key) {
  const ss = store()
  if (!ss) return
  try {
    ss.removeItem(PREFIX + key)
  } catch (e) {
    /* ignore */
  }
}

/**
 * 指定路由下是否存在未提交的修改。
 * 供「登录状态已过期」提示判断要不要告诉用户「修改已暂存」。
 */
export function hasDraftFor(path) {
  const ss = store()
  if (!ss || !path) return false
  try {
    for (let i = 0; i < ss.length; i += 1) {
      const k = ss.key(i)
      if (!k || !k.startsWith(PREFIX)) continue
      const raw = ss.getItem(k)
      if (!raw) continue
      const parsed = JSON.parse(raw)
      if (parsed?.path === path && parsed?.ts && Date.now() - parsed.ts <= MAX_AGE_MS) {
        return true
      }
    }
  } catch (e) {
    return false
  }
  return false
}
