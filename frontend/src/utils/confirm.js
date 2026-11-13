import { reactive } from 'vue'

/**
 * 全局确认弹层状态。
 *
 * 原生 window.confirm 在移动端有两个硬伤：样式不可控、且会阻塞主线程，
 * 在 WebView 里表现为「整个界面卡住 + 弹出系统级丑陋对话框」。
 * 这里统一换成 App 内自绘弹层。
 */
export const confirmState = reactive({
  open: false,
  title: '',
  message: '',
  confirmText: '确认',
  cancelText: '取消',
  danger: false,
  resolve: null
})

/**
 * @param {object|string} options 字符串视为 message
 * @returns {Promise<boolean>}
 */
export function confirmDialog(options) {
  const opts = typeof options === 'string' ? { message: options } : (options || {})

  // 已有未决弹层时先按「取消」结清，避免 Promise 悬挂
  if (confirmState.resolve) {
    confirmState.resolve(false)
    confirmState.resolve = null
  }

  confirmState.title = opts.title || '操作确认'
  confirmState.message = opts.message || ''
  confirmState.confirmText = opts.confirmText || '确认'
  confirmState.cancelText = opts.cancelText || '取消'
  confirmState.danger = !!opts.danger
  confirmState.open = true

  return new Promise((resolve) => {
    confirmState.resolve = resolve
  })
}

export function settleConfirm(ok) {
  const resolve = confirmState.resolve
  confirmState.resolve = null
  confirmState.open = false
  if (resolve) resolve(!!ok)
}
