<template>
  <div class="page">
    <PullRefresh :on-refresh="onPullRefresh">
    <!-- 品牌头部：定位 + 搜索 + 频道（视觉对齐京东外卖式布局） -->
    <div class="home-head">
      <div class="home-head__row">
        <span class="home-head__scene">
          <Icon name="nearby" :size="15" color="#fff" />
          外卖
        </span>
        <span class="home-head__addr">
          <Icon name="nearby" :size="11" color="rgba(255,255,255,.85)" />
          {{ home.city || '天津' }}
        </span>
        <div class="home-head__search" @click="goSearch">
          <Icon name="search" :size="14" color="#9CA0A8" />
          <span class="home-head__search-txt">搜索店铺、商品</span>
          <span class="home-head__search-btn">搜索</span>
        </div>
      </div>
      <div class="home-head__tabs">
        <span
          v-for="(t, i) in channels"
          :key="t"
          class="home-head__tab"
          :class="{ 'home-head__tab--on': i === 0 }"
        >
          {{ t }}
        </span>
      </div>
    </div>

    <div class="page-body">
      <!-- 轮播 -->
      <div v-if="!ready" class="skeleton" style="height: 96px; border-radius: 10px" />
      <template v-else>
        <div ref="bannersEl" class="banners" @scroll="onBannerScroll">
          <div
            v-for="(b, i) in home.banners"
            :key="i"
            class="banner"
            :class="'banner--' + b.gradient"
            @click="onBanner(b)"
          >
            <div class="banner__title">{{ b.title }}</div>
            <div class="banner__sub">{{ b.subtitle }}</div>
            <span class="banner__btn">立即查看</span>
          </div>
        </div>
        <!-- 位置指示：只有一张时不显示，避免无意义的装饰 -->
        <div v-if="(home.banners || []).length > 1" class="banners__dots">
          <i
            v-for="(b, i) in home.banners"
            :key="i"
            class="banners__dot"
            :class="{ 'banners__dot--on': i === bannerIndex }"
          />
        </div>
      </template>

      <!-- 金刚区 -->
      <div class="card mt-12">
        <div class="grid">
          <div v-for="c in home.categories" :key="c.id" class="grid__item" @click="onCategory(c)">
            <div class="grid__icon" :class="'grid__icon--' + c.icon">
              <Icon :name="catIcon(c.icon)" :size="19" color="#fff" />
            </div>
            <span>{{ c.name }}</span>
          </div>
        </div>
      </div>

      <!-- 超级补贴（秒杀数据驱动的爆品横滑） -->
      <div v-if="home.seckills && home.seckills.length" class="card mt-12">
        <div class="card__title">
          <span class="subsidy-title">超级补贴</span>
          <small class="link-more" @click="goNearby">查看更多 ›</small>
        </div>
        <div class="hscroll">
          <div v-for="s in home.seckills" :key="s.dishId" class="seckill" @click="goShop(s.shopId)">
            <div class="seckill__pic">
              <PhImage style="width: 78px; height: 78px" :image="s.image" :label="s.name.slice(0, 2)" />
              <i class="seckill__corner">爆品一口价</i>
            </div>
            <div class="seckill__name ellipsis">{{ s.name }}</div>
            <div class="seckill__price">
              <!--
                后端 Seckill 只有 price（现价）与 originPrice（原价）。
                补贴角签 = 原价 - 现价，仅在原价更高时展示，不凭空造折扣。
              -->
              <span class="price"><span class="price__sym">¥</span>{{ yuan(s.price) }}</span>
              <span
                v-if="s.originPrice && s.originPrice > s.price"
                class="seckill__subsidy"
              >补{{ ((s.originPrice - s.price) / 100).toFixed(1) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 神券 / 到店自取 -->
      <div class="duo mt-12">
        <div class="duo__card duo__card--coupon" @click="goNearby">
          <div class="duo__title">天天神券</div>
          <div class="duo__sub">每日限领 · 满 30 减 8</div>
          <span class="duo__btn">去领券</span>
        </div>
        <div class="duo__card duo__card--pickup" @click="goPickupShops">
          <div class="duo__title">到店自取</div>
          <div class="duo__sub">免配送费 · 再减 3 元</div>
          <span class="duo__btn duo__btn--pickup">去自取</span>
        </div>
      </div>

      <!-- 附近好店 -->
      <div class="section-title">
        附近好店
        <small>{{ shops.length }} 家</small>
      </div>

      <div class="sorts">
        <span
          v-for="s in sorts"
          :key="s.key"
          class="sorts__item"
          :class="{ 'sorts__item--active': sort === s.key }"
          @click="switchSort(s.key)"
        >
          {{ s.label }}
        </span>
        <span class="sorts__item sorts__item--pickup" @click="goPickupShops">自提更省</span>
      </div>

      <div v-if="!ready">
        <div v-for="i in 3" :key="i" class="skeleton mt-12" style="height: 96px; border-radius: 10px" />
      </div>
      <template v-else>
        <div :class="{ 'is-switching': sorting }">
          <RichShopCard
            v-for="s in shops"
            :key="s.id"
            :shop="s"
            :dishes="dishMap[s.id] || []"
            class="mt-12 tap-card"
            @click="goShop(s.id)"
            @add="onAdd"
            @coupon="onCoupon"
          />
        </div>
        <EmptyState
          v-if="!shops.length"
          icon="store"
          title="附近暂无门店"
          desc="接口未连通或稍后再试"
          action="重新加载"
          @action="reload"
        />
      </template>

      <div class="text-aux mt-16" style="text-align: center">
        — 演示原型 · 数据与图片均为示例 —
      </div>
    </div>
    </PullRefresh>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import PhImage from '../components/PhImage.vue'
import RichShopCard from '../components/RichShopCard.vue'
import EmptyState from '../components/EmptyState.vue'
import PullRefresh from '../components/PullRefresh.vue'
import { getHome, listShops, getShopDetail, addToCart } from '../api'
import { yuan } from '../utils/format'
import { toast } from '../api/http'
import { rafThrottle } from '../utils/ui'
import { useAppStore } from '../store/app'

defineOptions({ name: 'HomeView' })

const router = useRouter()

/** 头部频道页签（视觉对齐京东外卖式首页） */
const channels = ['星光外卖', '品质堂食', '热菜现炒']

const home = ref({})
const shops = ref([])
const ready = ref(false)
const sorting = ref(false)
const sort = ref('recommend')
/** 每家店的热销商品（富卡片底部横滑用），key = shopId */
const dishMap = ref({})
const app = useAppStore()

const bannersEl = ref(null)
const bannerIndex = ref(0)

/** 轮播位置指示：按「卡片宽度 + 间距」换算当前页 */
const onBannerScroll = rafThrottle(() => {
  const el = bannersEl.value
  if (!el) return
  const card = el.firstElementChild
  if (!card) return
  const step = card.offsetWidth + 10
  bannerIndex.value = Math.max(0, Math.round(el.scrollLeft / step))
})

const sorts = [
  { key: 'recommend', label: '综合排序' },
  { key: 'distance', label: '距离最近' },
  { key: 'sales', label: '销量最高' },
  { key: 'rating', label: '评分最高' },
  { key: 'minprice', label: '起送价最低' }
]

const CAT_ICON = {
  food: 'fork',
  fast: 'truck',
  drink: 'wallet',
  fresh: 'heart',
  market: 'store',
  medical: 'shield',
  cake: 'gift',
  pickup: 'receipt',
  coupon: 'ticket',
  all: 'chart'
}

function catIcon(icon) {
  return CAT_ICON[icon] || 'store'
}

/** 拉取首页聚合数据；silent=true 时不闪骨架（下拉刷新场景） */
async function fetchHome(silent = false) {
  if (!silent) ready.value = false
  try {
    const [h, s] = await Promise.all([getHome(), listShops({ sort: sort.value })])
    home.value = h.data || {}
    shops.value = s.data || []
    await loadDishes(shops.value)
  } finally {
    ready.value = true
  }
}

onMounted(() => {
  fetchHome()
})

/** 下拉刷新：不闪骨架，直接静默替换数据 */
async function onPullRefresh() {
  await fetchHome(true)
}

async function reload() {
  await fetchHome()
  if (!shops.value.length) toast('仍未获取到门店，请确认后端 8090 已启动', 'info')
}

async function switchSort(key) {
  if (sort.value === key) return
  sort.value = key
  sorting.value = true
  try {
    const s = await listShops({ sort: key })
    shops.value = s.data || []
    await loadDishes(shops.value)
  } finally {
    sorting.value = false
  }
}

/**
 * 富卡片底部「店内热销」横滑：按店取详情，取未售罄、月销前 6 的商品。
 * 单店失败不影响整页（Promise.all 内已 catch）。
 */
async function loadDishes(list) {
  const details = await Promise.all(list.map(s => getShopDetail(s.id).catch(() => null)))
  const map = {}
  details.forEach(d => {
    if (!d || !d.data) return
    map[d.data.shop.id] = (d.data.dishes || [])
      .filter(x => !x.soldOut)
      .sort((a, b) => (b.monthSales || 0) - (a.monthSales || 0))
      .slice(0, 6)
  })
  dishMap.value = map
}

/** 富卡片「＋」：无规格直接加购，有规格进店选规格（与搜索页一致） */
async function onAdd(dish) {
  if (dish.hasSpec) {
    goShop(dish.shopId)
    return
  }
  try {
    await addToCart({ dishId: dish.id, quantity: 1 })
    await app.refreshCartCount()
    toast('已加入购物车', 'success')
  } catch (e) {
    toast(e.message, 'error')
  }
}

function onCoupon() {
  toast('活动页规划中', 'info')
}

function goShop(id) {
  router.push(`/shop/${id}`)
}

function goNearby() {
  router.push('/nearby')
}

function goPickupShops() {
  router.push('/nearby?pickup=1')
}

function goSearch() {
  router.push('/search')
}

function onBanner(b) {
  if (b.targetType === 'SHOP') router.push(`/shop/${b.targetValue}`)
  else if (b.targetType === 'CATEGORY') router.push('/nearby')
  else toast(b.title + '：活动页在 SRS 5.2.10 中列出', 'info')
}

function onCategory(c) {
  if (c.name === '到店自取') router.push('/nearby?pickup=1')
  else if (c.name === '天天神券') router.push('/nearby')
  else router.push('/nearby')
}
</script>

<style scoped>
.home-head {
  /* 京东式深棕头部（参考图配色），顶部叠加状态栏安全区 */
  background: linear-gradient(180deg, #7A4A28 0%, #5C3517 100%);
  padding: calc(12px + env(safe-area-inset-top, 0px)) var(--sp-page) 12px;
}

.home-head__row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.home-head__scene {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #fff;
  font-size: 17px;
  font-weight: var(--fw-title);
  white-space: nowrap;
}

.home-head__addr {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-right: 10px;
  color: rgba(255, 255, 255, .92);
  font-size: var(--fs-aux);
  white-space: nowrap;
}

.home-head__search {
  flex: 1;
  height: 34px;
  min-width: 0;
  border-radius: var(--r-btn);
  background: rgba(255, 255, 255, .96);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 4px 0 10px;
  color: var(--t-3);
  font-size: var(--fs-body);
}

.home-head__search-txt {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-head__search-btn {
  flex-shrink: 0;
  height: 26px;
  padding: 0 13px;
  border-radius: 13px;
  background: linear-gradient(135deg, #A0432A, #7E3120);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
}

.home-head__tabs {
  display: flex;
  gap: 20px;
  margin-top: 11px;
}

.home-head__tab {
  position: relative;
  padding-bottom: 4px;
  color: rgba(255, 255, 255, .78);
  font-size: var(--fs-body);
}

.home-head__tab--on {
  color: #fff;
  font-weight: var(--fw-title);
}

.home-head__tab--on::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 16px;
  height: 2px;
  border-radius: 1px;
  background: #FFD9A0;
}

.page-body {
  padding: 0 var(--sp-page) 16px;
  margin-top: -4px;
}

/* ---------------- 轮播 ---------------- */
.banners {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 4px 0 2px;
  scroll-snap-type: x mandatory;
}

/* ---------------- 轮播指示 ---------------- */
.banners__dots {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-top: 7px;
}

/*
 * 轮播指示点：未选中态原来用 --line-strong（描边色）+ 4px，
 * 在白底上几乎看不见，用户无法感知「还有第二张」。
 * 这里加大到 5px 并改用正文辅助色降透明度，保证可辨识但不抢焦点。
 */
.banners__dot {
  width: 5px;
  height: 5px;
  border-radius: 3px;
  background: var(--t-3);
  opacity: .32;
  transition: width .22s ease, opacity .22s ease, background-color .22s ease;
}

.banners__dot--on {
  width: 14px;
  opacity: 1;
  background: var(--brand-2);
}

.banner {
  min-width: 84%;
  height: 96px;
  border-radius: var(--r-card);
  padding: 14px;
  color: #fff;
  position: relative;
  scroll-snap-align: start;
  box-shadow: var(--sh-light);
}

.banner--sunrise { background: linear-gradient(135deg, #FFB03A, #F5481F); }
.banner--ember { background: linear-gradient(135deg, #FF8A3D, #C0280F); }
.banner--mint { background: linear-gradient(135deg, #2FD0AE, #0F8A78); }

.banner__title {
  font-size: 17px;
  font-weight: var(--fw-title);
}

.banner__sub {
  font-size: var(--fs-aux);
  opacity: .92;
  margin-top: 4px;
}

.banner__btn {
  position: absolute;
  right: 12px;
  bottom: 12px;
  font-size: var(--fs-aux);
  background: rgba(255, 255, 255, .22);
  border-radius: var(--r-tag);
  padding: 3px 8px;
}

/* ---------------- 金刚区 ---------------- */
.grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px 4px;
}

.grid__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  font-size: var(--fs-aux);
  color: var(--t-2);
}

.grid__icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FFB03A, #F5481F);
}

.grid__icon--drink { background: linear-gradient(135deg, #FFD9A0, #E8834A); }
.grid__icon--fresh { background: linear-gradient(135deg, #C7E9B0, #4E9B3F); }
.grid__icon--market { background: linear-gradient(135deg, #C9D6FF, #3A6BF0); }
.grid__icon--medical { background: linear-gradient(135deg, #A8E6CF, #1F8A70); }
.grid__icon--cake { background: linear-gradient(135deg, #FFC1CC, #C2185B); }
.grid__icon--pickup { background: linear-gradient(135deg, #12BFA0, #0F8A78); }
.grid__icon--coupon { background: linear-gradient(135deg, #FFE39A, #F2B23C); }
.grid__icon--all { background: linear-gradient(135deg, #D7C4A3, #7A5B37); }
.grid__icon--fast { background: linear-gradient(135deg, #B7D9F7, #2E6FBF); }

/* ---------------- 超级补贴 ---------------- */
.subsidy-title {
  color: #E02E24;
  font-weight: 700;
}

.link-more {
  cursor: pointer;
}

.link-more:active {
  opacity: .6;
}

.hscroll {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.seckill {
  position: relative;
  flex-shrink: 0;
  width: 78px;
}

.seckill__pic {
  position: relative;
  width: 78px;
  height: 78px;
}

.seckill__corner {
  position: absolute;
  left: 0;
  top: 0;
  z-index: 1;
  font-style: normal;
  font-size: 9px;
  line-height: 14px;
  padding: 0 4px;
  border-radius: 6px 0 6px 0;
  color: #fff;
  background: linear-gradient(135deg, #FF7A1A, #E02E24);
}

.seckill__name {
  font-size: var(--fs-aux);
  margin: 5px 0 2px;
}

.seckill__subsidy {
  display: inline-block;
  margin-left: 4px;
  vertical-align: 1px;
  font-size: 9px;
  line-height: 15px;
  padding: 0 5px;
  border-radius: 4px 4px 4px 0;
  color: #fff;
  background: linear-gradient(135deg, #FF5B4A, #E02E24);
}

/* ---------------- 双卡 ---------------- */
.duo {
  display: flex;
  gap: 10px;
}

.duo__card {
  flex: 1;
  border-radius: var(--r-card);
  padding: 12px;
  color: #fff;
  position: relative;
  min-height: 78px;
  box-shadow: var(--sh-light);
}

.duo__card--coupon {
  background: linear-gradient(135deg, #FFB03A, #F5481F);
}

.duo__card--pickup {
  background: linear-gradient(135deg, #2FD0AE, #0F8A78);
}

.duo__title {
  font-size: var(--fs-title);
  font-weight: var(--fw-title);
}

.duo__sub {
  font-size: var(--fs-aux);
  opacity: .92;
  margin-top: 3px;
}

.duo__btn {
  position: absolute;
  right: 10px;
  bottom: 10px;
  background: rgba(255, 255, 255, .24);
  border-radius: var(--r-tag);
  padding: 2px 7px;
  font-size: 10px;
}

/* ---------------- 排序 ---------------- */
.sorts {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.sorts__item {
  flex-shrink: 0;
  height: 27px;
  padding: 0 12px;
  border-radius: var(--r-btn);
  background: var(--bg-card);
  color: var(--t-2);
  font-size: var(--fs-aux);
  display: inline-flex;
  align-items: center;
  box-shadow: var(--sh-light);
}

.sorts__item--active {
  background: #FFF1E6;
  color: var(--brand-3);
  font-weight: var(--fw-title);
  box-shadow: inset 0 0 0 1px var(--brand-2);
}

.sorts__item--pickup {
  color: #B9760F;
  background: #FFF5E6;
  box-shadow: inset 0 0 0 1px #FFE1B3;
}

/* ---------------- 交互反馈 ---------------- */
/* 全局关掉了 tap-highlight，可点区域必须自己给按压反馈 */
.banner,
.grid__item,
.sorts__item,
.seckill,
.duo__card,
.home-head__search {
  -webkit-user-select: none;
  user-select: none;
  transition: transform .12s ease, opacity .12s ease;
}

.banner:active,
.seckill:active,
.duo__card:active {
  transform: scale(.98);
  opacity: .9;
}

.grid__item:active,
.sorts__item:active,
.home-head__search:active {
  opacity: .6;
}

/* 切换排序时列表轻微淡出，给出「正在刷新」的感知，而不是数据直接跳变 */
.is-switching {
  opacity: .5;
  transition: opacity .15s ease;
}
</style>
