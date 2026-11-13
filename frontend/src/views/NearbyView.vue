<template>
  <div class="page">
    <PullRefresh :on-refresh="onPullRefresh">
    <NavBar title="附近" :back="false" />

    <div class="nearby__search" @click="goSearch">
      <Icon name="search" :size="14" color="#9CA0A8" />
      <span>搜索店铺、商品</span>
    </div>

    <div class="nearby">
      <!-- 筛选 -->
      <div class="nearby__filters">
        <button
          v-for="s in filters"
          :key="s.key"
          class="sorts__item"
          :class="{ 'sorts__item--active': sort === s.key }"
          @click="switchSort(s.key)"
        >
          {{ s.label }}
        </button>
        <button
          class="sorts__item"
          :class="{ 'sorts__item--active': onlyPickup }"
          @click="togglePickup"
        >
          仅看自取
        </button>
      </div>

      <!-- 地图图层（高德 JS API；未配置 Key / 加载失败时回退占位） -->
      <div class="map">
        <div ref="mapEl" class="map__canvas" />
        <template v-if="!amapReady">
          <div class="map__grid" />
          <div
            v-for="(s, i) in visibleShops"
            :key="s.id"
            class="map__bubble"
            :style="bubbleStyle(s, i, visibleShops.length)"
            @click="goShop(s.id)"
          >
            <div class="map__bubble-inner">
              <span class="ellipsis">{{ s.name }}</span>
              <small class="num">¥{{ yuan(s.minPrice) }} 起</small>
            </div>
          </div>
          <div class="map__me">
            <Icon name="nearby" :size="13" color="#fff" />
          </div>
        </template>
        <div class="map__legend">
          <Icon name="info" :size="11" color="#5C6169" />
          {{ amapReady ? '高德地图 · 实时定位' : '地图占位（未配置或加载失败）' }}
        </div>
      </div>

      <!-- 底部上滑列表 -->
      <div class="nearby__list">
        <div class="nearby__list-title">
          附近好店
          <small>{{ visibleShops.length }} 家 · 按{{ currentSortLabel }}</small>
        </div>

        <div v-if="!ready">
          <div v-for="i in 3" :key="i" class="skeleton mt-12" style="height: 96px; border-radius: 10px" />
        </div>
        <div v-else :class="{ 'is-switching': sorting }">
          <ShopCard
            v-for="s in visibleShops"
            :key="s.id"
            :shop="s"
            class="mt-12 tap-card"
            @click="goShop(s.id)"
          />
        </div>

        <EmptyState v-if="ready && !visibleShops.length" icon="store" title="附近暂无门店" desc="试试切换排序或关闭自取筛选" />
      </div>
    </div>
    </PullRefresh>
  </div>
</template>

<script setup>
import { computed, onActivated, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import Icon from '../components/Icon.vue'
import ShopCard from '../components/ShopCard.vue'
import EmptyState from '../components/EmptyState.vue'
import PullRefresh from '../components/PullRefresh.vue'
import { listShops } from '../api'
import { useAppStore } from '../store/app'
import { yuan } from '../utils/format'
import { loadAMap } from '../utils/amap'

defineOptions({ name: 'NearbyView' })

const route = useRoute()
const router = useRouter()
const app = useAppStore()

const filters = [
  { key: 'recommend', label: '综合排序' },
  { key: 'distance', label: '距离最近' },
  { key: 'rating', label: '评分最高' },
  { key: 'sales', label: '销量最高' }
]

const sort = ref('distance')
const onlyPickup = ref(route.query.pickup === '1')
const shops = ref([])
const ready = ref(false)
const sorting = ref(false)

const visibleShops = computed(() =>
  onlyPickup.value ? shops.value.filter(s => s.pickupEnabled) : shops.value
)

const currentSortLabel = computed(
  () => (filters.find(f => f.key === sort.value) || { label: '综合' }).label
)

const mapEl = ref(null)
const amapReady = ref(false)
let map = null
let AMapLib = null
let markers = []

onMounted(async () => {
  await load()
  initMap()
})

/**
 * 该页被 keep-alive 缓存，从首页「到店自取」再次进入时
 * query 变了但组件不会重新挂载，需要手动同步一次筛选态。
 */
onActivated(() => {
  if (route.query.pickup === '1') onlyPickup.value = true
  if (map) map.resize()
})

async function load() {
  try {
    const r = await listShops({ sort: sort.value })
    shops.value = r.data || []
  } finally {
    // 无论成功失败都要收起骨架屏，否则请求异常时页面会一直停在占位状态
    ready.value = true
  }
}

/** 下拉刷新：保留当前列表，静默替换 */
async function onPullRefresh() {
  try {
    const r = await listShops({ sort: sort.value })
    shops.value = r.data || []
  } catch (e) {
    /* 拦截器已提示 */
  }
}

async function switchSort(key) {
  if (sort.value === key) return
  sort.value = key
  sorting.value = true
  try {
    await load()
    if (amapReady.value) renderMarkers()
  } finally {
    sorting.value = false
  }
}

function togglePickup() {
  onlyPickup.value = !onlyPickup.value
}

function goShop(id) {
  router.push(`/shop/${id}`)
}

function goSearch() {
  router.push('/search')
}

/** 把门店按顺序铺到地图上（原型用固定位置示意，非真实经纬度投影） */
function bubbleStyle(shop, i, total) {
  const cols = Math.min(2, Math.max(1, total))
  const col = i % cols
  const row = Math.floor(i / cols)
  return {
    left: `${12 + col * 44 + (row % 2) * 6}%`,
    top: `${14 + row * 26}%`
  }
}

/* ---------------- 高德地图 ---------------- */
/** 滨海新区（与种子数据一致） */
const DEFAULT_CENTER = [117.7021, 39.0031]

async function initMap() {
  if (amapReady.value || !mapEl.value) return
  try {
    AMapLib = await loadAMap()
    if (!mapEl.value) return
    map = new AMapLib.Map(mapEl.value, {
      zoom: 15,
      center: shopCenter(),
      viewMode: '2D',
      showLabel: true
    })
    amapReady.value = true
    renderMarkers()
    locateMe()
  } catch (e) {
    // 离线 / Key 类型不符 / 未配置：回退到占位地图，不影响列表
    amapReady.value = false
  }
}

function shopCenter() {
  const s = shops.value.find(x => x.lng != null && x.lat != null)
  return s ? [Number(s.lng), Number(s.lat)] : DEFAULT_CENTER
}

function renderMarkers() {
  if (!map || !AMapLib) return
  if (markers.length) {
    map.remove(markers)
    markers = []
  }
  shops.value.forEach(s => {
    if (s.lat == null || s.lng == null) return
    const marker = new AMapLib.Marker({
      position: [Number(s.lng), Number(s.lat)],
      title: s.name,
      label: { content: s.name, direction: 'top', offset: new AMapLib.Pixel(0, -2) }
    })
    marker.on('click', () => goShop(s.id))
    map.add(marker)
    markers.push(marker)
  })
  if (markers.length) map.setFitView(markers)
}

/** 浏览器定位（需 HTTPS 或 localhost）；失败则保持门店视野 */
function locateMe() {
  if (!map || !AMapLib) return
  try {
    const geo = new AMapLib.Geolocation({ enableHighAccuracy: true, timeout: 5000, showButton: false })
    geo.getCurrentPosition((status, result) => {
      if (status !== 'complete' || !result || !result.position) return
      const pos = [result.position.lng, result.position.lat]
      const me = new AMapLib.Marker({
        position: pos,
        title: '我的位置',
        content:
          '<div style="width:14px;height:14px;border-radius:50%;background:#3A6BF0;border:2px solid #fff;box-shadow:0 0 0 4px rgba(58,107,240,.25)"></div>'
      })
      map.add(me)
      map.setCenter(pos)
    })
  } catch (e) {
    /* 定位不可用，忽略 */
  }
}
</script>

<style scoped>
.nearby {
  padding-bottom: calc(var(--tabbar-h) + env(safe-area-inset-bottom, 0px));
}

.nearby__search {
  display: flex; align-items: center; gap: 6px;
  margin: 10px var(--sp-page, 12px);
  height: 34px; padding: 0 12px; border-radius: var(--r-btn, 17px);
  background: var(--bg-sunken, #f2f3f5); color: var(--t-3, #999); font-size: 13px;
  -webkit-user-select: none; user-select: none;
}
.nearby__search:active { opacity: .7; }

.nearby__filters {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 10px var(--sp-page);
  background: var(--bg-card);
  border-bottom: 1px solid var(--line);
}

.sorts__item {
  flex-shrink: 0;
  height: 28px;
  padding: 0 12px;
  border-radius: var(--r-btn);
  background: var(--bg-sunken);
  color: var(--t-2);
  font-size: var(--fs-aux);
  display: inline-flex;
  align-items: center;
}

.sorts__item--active {
  background: #FFF1E6;
  color: var(--brand-3);
  font-weight: var(--fw-title);
  box-shadow: inset 0 0 0 1px var(--brand-2);
}

/* ---------------- 地图 ---------------- */
.map {
  position: relative;
  height: 230px;
  overflow: hidden;
  background: linear-gradient(160deg, #EAF0FE, #F7F8FA 60%, #EFFAF7);
}

.map__grid {
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(0deg, rgba(58, 107, 240, .07) 0 1px, transparent 1px 26px),
    repeating-linear-gradient(90deg, rgba(58, 107, 240, .07) 0 1px, transparent 1px 26px);
}

.map__canvas {
  position: absolute;
  inset: 0;
}

.map__bubble {
  position: absolute;
  max-width: 46%;
}

.map__bubble-inner {
  background: var(--bg-card);
  border-radius: var(--r-tag);
  padding: 4px 7px;
  box-shadow: var(--sh-float);
  font-size: 10px;
  color: var(--t-1);
  display: flex;
  flex-direction: column;
  line-height: 1.35;
  border-left: 2px solid var(--brand-2);
}

.map__bubble-inner small {
  color: var(--c-price);
  font-weight: var(--fw-title);
}

.map__me {
  position: absolute;
  left: 8%;
  top: 68%;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #3A6BF0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 0 6px rgba(58, 107, 240, .16);
}

.map__legend {
  position: absolute;
  left: 12px;
  bottom: 10px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, .92);
  border-radius: var(--r-tag);
  padding: 4px 7px;
  font-size: 10px;
  color: var(--t-2);
}

/* ---------------- 列表 ---------------- */
.nearby__list {
  padding: 12px;
}

.nearby__list-title {
  font-size: var(--fs-title);
  font-weight: var(--fw-title);
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.nearby__list-title small {
  font-size: var(--fs-aux);
  font-weight: var(--fw-body);
  color: var(--t-3);
}

.is-switching {
  opacity: .5;
  transition: opacity .15s ease;
}

/* 筛选按钮按压反馈（全局已关闭 tap-highlight） */
.sorts__item {
  transition: transform .12s ease, opacity .12s ease;
  -webkit-user-select: none;
  user-select: none;
}

.sorts__item:active {
  transform: scale(.96);
  opacity: .8;
}

.map__bubble {
  transition: transform .12s ease;
  -webkit-user-select: none;
  user-select: none;
}

.map__bubble:active {
  transform: scale(.94);
}
</style>
