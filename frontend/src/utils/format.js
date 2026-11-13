/** 金额与时间格式化。金额统一以「分」传输与计算，仅在展示层转元。 */

export function yuan(fen) {
  if (fen === null || fen === undefined) return '0.00'
  return (fen / 100).toFixed(2)
}

/** 拆成整数与小数部分，便于「数字加大、￥ 符号缩小」的排版 */
export function yuanParts(fen) {
  const s = yuan(fen)
  const [int, dec] = s.split('.')
  return { int, dec }
}

export function fmtTime(value) {
  if (!value) return ''
  const s = String(value).replace('T', ' ')
  return s.length > 16 ? s.slice(0, 16) : s
}

export function fmtDate(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 10)
}

/** 相对时间：刚刚 / N 分钟前 / N 小时前 / MM-DD HH:mm */
export function fromNow(value) {
  if (!value) return ''
  const t = new Date(String(value).replace(' ', 'T')).getTime()
  if (Number.isNaN(t)) return fmtTime(value)
  const diff = Date.now() - t
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 3600 * 1000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400 * 1000) return `${Math.floor(diff / 3600000)} 小时前`
  const d = new Date(t)
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

/** 秒 → mm:ss / hh:mm:ss */
export function fmtDuration(sec) {
  const s = Math.max(0, Math.floor(sec || 0))
  const p = (n) => String(n).padStart(2, '0')
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const ss = s % 60
  return h > 0 ? `${p(h)}:${p(m)}:${p(ss)}` : `${p(m)}:${p(ss)}`
}

/** 订单状态 → 标签样式 */
export function statusTagClass(status) {
  switch (status) {
    case 'UNPAID':
      return 'tag--warn'
    case 'COMPLETED':
    case 'PICKED':
      return 'tag--success'
    case 'CANCELED':
      return 'tag--plain'
    case 'REFUNDED':
    case 'PART_REFUNDED':
    case 'EXCEPTION':
      return 'tag--error'
    case 'DELIVERING':
    case 'WAIT_PICKUP':
      return 'tag--info'
    default:
      return 'tag--promo'
  }
}

/** 生成幂等键（下单 / 支付必带，SRS 9.6） */
export function uuid() {
  if (window.crypto && window.crypto.randomUUID) return window.crypto.randomUUID()
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

/** 履约方式标签 */
export function fulfillLabel(code) {
  return code === 'PICKUP' ? '到店自取' : '平台配送'
}
