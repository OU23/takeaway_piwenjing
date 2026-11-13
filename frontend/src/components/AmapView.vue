<template>
  <div class="amap" :style="{ height }">
    <div ref="el" class="amap__canvas" />
    <slot v-if="!ready" name="fallback">
      <div class="amap__fallback">地图加载中…（未配置 Key 或不可用时为占位）</div>
    </slot>
  </div>
</template>

<script setup>
/**
 * 高德地图容器（复用）。
 *
 * - markers: [{ id?, lng, lat, title? }]，点标记时 emit('marker-click', id)
 * - center: [lng, lat] 可选；不传则按 markers 自动适配视野
 * - locate: 是否用浏览器定位打「我的位置」点
 * - 加载失败（离线 / Key 无 JS API 权限）时显示 fallback 槽位，不阻塞页面
 */
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { loadAMap } from '../utils/amap'

const props = defineProps({
  markers: { type: Array, default: () => [] },
  center: { type: Array, default: null },
  height: { type: String, default: '150px' },
  locate: { type: Boolean, default: true }
})
const emit = defineEmits(['ready', 'fail', 'marker-click'])

/** 滨海新区（与种子数据一致） */
const DEFAULT_CENTER = [117.7021, 39.0031]

const el = ref(null)
const ready = ref(false)
let map = null
let AMapLib = null
let markerObjs = []
let meMarker = null

onMounted(init)
onBeforeUnmount(() => {
  if (map) map.destroy()
  map = null
})

async function init() {
  try {
    AMapLib = await loadAMap()
    await nextTick()
    if (!el.value) return
    map = new AMapLib.Map(el.value, {
      zoom: 15,
      center: props.center || centerOf(props.markers) || DEFAULT_CENTER,
      viewMode: '2D'
    })
    ready.value = true
    render()
    if (props.locate) locateMe()
    emit('ready', map)
  } catch (e) {
    ready.value = false
    emit('fail', e)
  }
}

function centerOf(list) {
  const m = (list || []).find(x => x.lng != null && x.lat != null)
  return m ? [Number(m.lng), Number(m.lat)] : null
}

function render() {
  if (!map || !AMapLib) return
  if (markerObjs.length) {
    map.remove(markerObjs)
    markerObjs = []
  }
  ;(props.markers || []).forEach(mk => {
    if (mk.lat == null || mk.lng == null) return
    const marker = new AMapLib.Marker({
      position: [Number(mk.lng), Number(mk.lat)],
      title: mk.title || '',
      label: mk.title
        ? { content: mk.title, direction: 'top', offset: new AMapLib.Pixel(0, -2) }
        : undefined
    })
    if (mk.id != null) marker.on('click', () => emit('marker-click', mk.id))
    map.add(marker)
    markerObjs.push(marker)
  })
  if (markerObjs.length && !props.center) map.setFitView(markerObjs)
}

function locateMe() {
  if (!map || !AMapLib) return
  try {
    const geo = new AMapLib.Geolocation({ enableHighAccuracy: true, timeout: 5000, showButton: false })
    geo.getCurrentPosition((status, result) => {
      if (status !== 'complete' || !result || !result.position || !map) return
      const pos = [result.position.lng, result.position.lat]
      if (meMarker) map.remove(meMarker)
      meMarker = new AMapLib.Marker({
        position: pos,
        title: '我的位置',
        content:
          '<div style="width:14px;height:14px;border-radius:50%;background:#3A6BF0;border:2px solid #fff;box-shadow:0 0 0 4px rgba(58,107,240,.25)"></div>'
      })
      map.add(meMarker)
      if (props.center) map.setCenter(pos)
    })
  } catch (e) {
    /* 定位不可用，忽略 */
  }
}

watch(
  () => props.markers,
  () => {
    if (ready.value) render()
  },
  { deep: true }
)

watch(
  () => props.center,
  (c) => {
    if (ready.value && c && map) map.setCenter(c)
  }
)
</script>

<style scoped>
.amap {
  position: relative;
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(135deg, #e9f2ff, #d6e4f5);
}
.amap__canvas {
  position: absolute;
  inset: 0;
}
.amap__fallback {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  text-align: center;
  font-size: 12px;
  color: #5c6169;
  background: linear-gradient(135deg, #e9f2ff, #d6e4f5);
}
</style>
