/**
 * 移动端交互基础工具：滚动锁 / rAF 节流 / 可见性轮询 / 轻触反馈。
 *
 * 这些能力在移动端属于「不做就明显不对劲」的一类：
 * 弹层不锁背景会滚动穿透、滚动回调不节流会掉帧、
 * 页面切后台还在轮询会白白耗电。
 */

/* ------------------------------------------------------------------ */
/* 滚动锁：弹层打开时冻结背景，支持嵌套计数                            */
/* ------------------------------------------------------------------ */
let lockCount = 0
let savedOverflow = ''
let savedPaddingRight = ''

export function lockScroll() {
  if (lockCount === 0) {
    // 桌面端（含 App 内的宽屏 WebView）补回滚动条宽度，避免锁定时页面横向跳动
    const gap = window.innerWidth - document.documentElement.clientWidth
    savedOverflow = document.body.style.overflow
    savedPaddingRight = document.body.style.paddingRight
    document.body.style.overflow = 'hidden'
    if (gap > 0) document.body.style.paddingRight = `${gap}px`
    document.body.classList.add('is-scroll-locked')
  }
  lockCount += 1
}

export function unlockScroll() {
  lockCount = Math.max(0, lockCount - 1)
  if (lockCount === 0) {
    document.body.style.overflow = savedOverflow
    document.body.style.paddingRight = savedPaddingRight
    document.body.classList.remove('is-scroll-locked')
  }
}

/* ------------------------------------------------------------------ */
/* rAF 节流：滚动 / 尺寸类回调统一走这里，一帧最多一次                  */
/* ------------------------------------------------------------------ */
export function rafThrottle(fn) {
  let ticking = false
  let lastArgs = null
  return function throttled(...args) {
    lastArgs = args
    if (ticking) return
    ticking = true
    requestAnimationFrame(() => {
      ticking = false
      fn.apply(this, lastArgs)
    })
  }
}

/* ------------------------------------------------------------------ */
/* 可见性轮询：页面切到后台自动暂停，失败指数退避                       */
/* ------------------------------------------------------------------ */
/**
 * @param {() => Promise<any>} task 每次轮询执行的任务
 * @param {object} opts
 * @param {number} opts.interval 基础间隔（毫秒）
 * @param {number} opts.maxInterval 退避上限（毫秒）
 * @param {boolean} opts.immediate 是否立即执行一次
 * @returns {{ start: () => void, stop: () => void }}
 */
export function createVisiblePoller(task, opts = {}) {
  const { interval = 15000, maxInterval = 60000, immediate = false } = opts
  let timer = null
  let stopped = true
  let delay = interval
  let running = false

  const clear = () => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  const schedule = (wait) => {
    clear()
    if (stopped || document.hidden) return
    timer = setTimeout(tick, wait)
  }

  async function tick() {
    if (stopped) return
    if (document.hidden) {
      // 后台不再轮询，回到前台由 visibilitychange 重新拉起
      return
    }
    if (running) {
      schedule(delay)
      return
    }
    running = true
    try {
      await task()
      delay = interval
    } catch (e) {
      // 网络抖动时逐步拉长间隔，避免弱网下持续打满请求
      delay = Math.min(delay * 2, maxInterval)
    } finally {
      running = false
      schedule(delay)
    }
  }

  function onVisible() {
    if (document.hidden) {
      clear()
    } else if (!stopped) {
      delay = interval
      // 回到前台立刻补一次，保证看到的是最新状态
      schedule(0)
    }
  }

  return {
    start() {
      if (!stopped) return
      stopped = false
      document.addEventListener('visibilitychange', onVisible)
      schedule(immediate ? 0 : interval)
    },
    stop() {
      stopped = true
      clear()
      document.removeEventListener('visibilitychange', onVisible)
    }
  }
}

/* ------------------------------------------------------------------ */
/* 轻触反馈：Android WebView 支持 vibrate，iOS 静默降级                 */
/* ------------------------------------------------------------------ */
let lastHaptic = 0

export function tapFeedback(duration = 8) {
  const now = Date.now()
  // 60ms 内不重复触发，避免连点时马达「哒哒哒」拖慢手感
  if (now - lastHaptic < 60) return
  lastHaptic = now
  try {
    navigator.vibrate?.(duration)
  } catch (e) {
    /* 不支持则忽略 */
  }
}
