<!--
  轻量下拉刷新（无第三方依赖）
  - 仅在页面滚动到顶部且向下拖拽时介入，不抢正常滚动手势
  - 拖拽期间才给内容加 transform，松手后立即移除，避免破坏子元素 position: sticky
-->
<template>
  <div class="pr" @touchstart="onStart" @touchmove="onMove" @touchend="onEnd" @touchcancel="onEnd">
    <div class="pr__indicator" :class="{ 'pr__indicator--active': pulling || state === 'loading' }" :style="indicatorStyle">
      <span v-if="state === 'loading'" class="pr__spinner" />
      <span v-else class="pr__arrow" :class="{ 'pr__arrow--up': distance > 0 && distance >= threshold }">↓</span>
      <span class="pr__text">{{ text }}</span>
    </div>

    <div class="pr__content" :style="contentStyle">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { computed, onUnmounted, ref } from 'vue'

const props = defineProps({
  /** 触发刷新的回调，返回 Promise 时会在其完成后收起 */
  onRefresh: { type: Function, required: true },
  /** 触发阈值（px） */
  threshold: { type: Number, default: 64 },
  disabled: { type: Boolean, default: false }
})

const distance = ref(0)
const pulling = ref(false)
const state = ref('idle') // idle | loading
const dragging = ref(false)

let startY = 0
let startX = 0
let axis = '' // '' | 'x' | 'y'
let restoreTimer = null

const text = computed(() => {
  if (state.value === 'loading') return '正在刷新…'
  if (distance.value >= props.threshold) return '松手刷新'
  return '下拉刷新'
})

const indicatorStyle = computed(() => ({
  // 指示器跟随下拉位移，超过阈值后阻尼
  transform: `translate3d(0, ${Math.min(distance.value, props.threshold + 24)}px, 0)`,
  opacity: Math.min(1, distance.value / 40)
}))

const contentStyle = computed(() => {
  // 仅在拖拽中位移内容；松手后（含回弹动画）交还浏览器，sticky 正常工作
  if (!dragging.value) return {}
  const d = Math.min(distance.value, props.threshold + 24)
  return { transform: `translate3d(0, ${d * 0.55}px, 0)` }
})

function topReached() {
  return (window.scrollY || document.documentElement.scrollTop || 0) <= 0
}

function onStart(e) {
  if (props.disabled || state.value === 'loading') return
  if (!topReached()) return
  const t = e.touches[0]
  if (!t) return
  startY = t.clientY
  startX = t.clientX
  axis = ''
  pulling.value = true
}

function onMove(e) {
  if (!pulling.value) return
  const t = e.touches[0]
  if (!t) return
  const dy = t.clientY - startY
  const dx = t.clientX - startX

  if (!axis) {
    if (Math.abs(dx) > 8 && Math.abs(dx) > Math.abs(dy)) {
      // 横向手势（如首页横滑模块）交还给浏览器
      pulling.value = false
      return
    }
    if (Math.abs(dy) < 6) return
    axis = 'y'
  }

  if (dy <= 0) {
    distance.value = 0
    dragging.value = false
    return
  }

  // 阻尼：越拉越沉，避免一拽到底
  distance.value = Math.min(dy * 0.5, 120)
  dragging.value = true

  if (distance.value > 0 && e.cancelable) {
    // 阻断 WebView 自带的橡皮筋，让手势完全由我们接管
    e.preventDefault()
  }
}

async function onEnd() {
  if (!pulling.value) return
  pulling.value = false
  const shouldRefresh = distance.value >= props.threshold
  dragging.value = false

  if (!shouldRefresh) {
    distance.value = 0
    return
  }

  state.value = 'loading'
  distance.value = props.threshold
  try {
    await props.onRefresh()
  } catch (e) {
    /* 错误由请求层统一提示 */
  } finally {
    state.value = 'idle'
    distance.value = 0
  }
}

onUnmounted(() => {
  if (restoreTimer) clearTimeout(restoreTimer)
})
</script>

<style scoped>
.pr {
  position: relative;
  min-height: 100%;
}

.pr__indicator {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 12px;
  color: var(--t-3);
  opacity: 0;
  pointer-events: none;
  will-change: transform, opacity;
}

.pr__indicator--active {
  opacity: 1;
}

.pr__arrow {
  font-size: 13px;
  transition: transform .18s ease;
}

.pr__arrow--up {
  transform: rotate(180deg);
}

.pr__spinner {
  width: 13px;
  height: 13px;
  border: 1.5px solid var(--line-strong);
  border-top-color: var(--brand-2);
  border-radius: 50%;
  animation: prSpin .7s linear infinite;
}

.pr__content {
  will-change: transform;
}

@keyframes prSpin {
  to { transform: rotate(360deg); }
}
</style>
