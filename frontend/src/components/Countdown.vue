<!--
  秒杀倒计时
  独立成组件的原因：倒计时每秒都要更新，如果写在首页里，
  首页（含几十张门店卡片）会跟着每秒重渲染一次。
  这里把秒级更新收敛到一个叶子节点，首页只渲染一次。
-->
<template>
  <span class="countdown num">
    <Icon name="clock" :size="11" color="#fff" />
    {{ text }}
  </span>
</template>

<script setup>
import { computed, onActivated, onDeactivated, onMounted, onUnmounted, ref, watch } from 'vue'
import Icon from './Icon.vue'
import { fmtDuration } from '../utils/format'

const props = defineProps({
  seconds: { type: Number, default: 3600 }
})

const remain = ref(props.seconds)
let timer = null

/**
 * 上次结算时刻。
 * 这里按真实时间差扣减，而不是「每秒减一」，原因有二：
 * ① 后台标签页的 setInterval 会被浏览器节流到约 1 次/分钟，逐次减一会明显走慢；
 * ② 页面切走再回来时，能一次性补上期间流逝的秒数，而不是停在那里。
 */
let lastTs = Date.now()

function tick() {
  if (document.hidden) return
  const now = Date.now()
  const elapsed = Math.round((now - lastTs) / 1000)
  if (elapsed < 1) return
  lastTs = now
  const next = remain.value - elapsed
  // 演示数据没有真实结束时间，归零后进入下一轮
  remain.value = next > 0 ? next : 3600
}

function start() {
  if (timer) return
  // 先补算一次，覆盖「停表期间」流逝的时间
  tick()
  timer = setInterval(tick, 1000)
}

function stop() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

watch(
  () => props.seconds,
  (v) => {
    remain.value = v
    lastTs = Date.now()
  }
)

onMounted(start)
/*
 * 首页被 keep-alive 缓存：切到订单/我的等 tab 时组件不会卸载，
 * onUnmounted 不触发，定时器会一直在后台每秒 setState。
 * 必须靠 deactivated 停表，activated 恢复。
 */
onActivated(start)
onDeactivated(stop)
onUnmounted(stop)

const text = computed(() => fmtDuration(remain.value))
</script>

<style scoped>
.countdown {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  background: var(--c-price);
  color: #fff;
  font-size: 10px;
  border-radius: var(--r-tag);
  padding: 2px 5px;
  font-weight: var(--fw-body);
  font-variant-numeric: tabular-nums;
}
</style>
