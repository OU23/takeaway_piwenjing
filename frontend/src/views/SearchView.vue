<template>
  <div class="page search">
    <!-- 吸顶头部：搜索栏 + 分类行 + 筛选行（对齐京东外卖搜索结果页） -->
    <div class="s-head">
      <div class="s-bar">
        <button class="s-bar__back" @click="cancel">
          <Icon name="back" :size="18" color="#3a3a3a" />
        </button>
        <div class="s-bar__field">
          <Icon name="search" :size="14" color="#9CA0A8" />
          <input
            ref="inputEl"
            v-model="kw"
            class="s-bar__input"
            placeholder="搜索店铺、商品"
            @keyup.enter="doSearch"
            @input="onInput"
          />
          <button v-if="kw" class="s-bar__clear" @click="clearKw">✕</button>
        </div>
        <button class="s-bar__go" @click="doSearch">搜索</button>
      </div>

      <div class="s-cats">
        <div
          v-for="(c, i) in cats"
          :key="c.id"
          class="s-cat"
          :class="{ 's-cat--on': i === 0 }"
          @click="onCat(c)"
        >
          <span class="s-cat__icon" :style="{ background: catBg(c.icon) }">
            <Icon :name="catIcon(c.icon)" :size="14" color="#fff" />
          </span>
          {{ c.name }}
        </div>
      </div>

      <div class="s-filters">
        <div class="s-filter" :class="{ 's-filter--on': sortOpen }" @click="sortOpen = !sortOpen">
          {{ sortLabel }}
          <i class="s-filter__caret" :class="{ 's-filter__caret--up': sortOpen }" />
          <div v-if="sortOpen" class="s-sort-pop">
            <div
              v-for="s in sorts"
              :key="s.key"
              class="s-sort-item"
              :class="{ 's-sort-item--on': s.key === sortKey }"
              @click.stop="pickSort(s.key)"
            >
              {{ s.label }}
            </div>
          </div>
        </div>
        <span class="s-filter" :class="{ 's-filter--picked': pickupOnly }" @click="togglePickup">自提更省</span>
        <span class="s-filter" @click="onMonthly">超级月卡折上折</span>
      </div>
    </div>

    <div class="s-body">
      <div v-if="!kw" class="s-empty">
        <div class="s-empty__title">搜你想吃的</div>
        <div class="hot">
          <button v-for="t in hot" :key="t" class="hot__tag" @click="quick(t)">{{ t }}</button>
        </div>
      </div>

      <div v-else>
        <div v-if="loading">
          <div v-for="i in 3" :key="i" class="skeleton mt-12" style="height: 160px; border-radius: 12px" />
        </div>
        <template v-else>
          <div class="s-count">找到 {{ displayed.length }} 家相关店铺</div>
          <RichShopCard
            v-for="s in displayed"
            :key="s.id"
            :shop="s"
            :dishes="dishMap[s.id] || []"
            class="mt-12"
            @click="goShop(s.id)"
            @add="onAdd"
            @coupon="onCoupon"
          />
          <EmptyState v-if="!displayed.length" icon="store" title="没有找到相关店铺" desc="换个关键词试试" />
        </template>
      </div>
    </div>

    <!-- 悬浮购物车：有加购才出现，角标为购物车总件数 -->
    <div v-if="cartCount > 0" class="s-fab" @click="goCartShop">
      <span class="s-fab__emoji">🛒</span>
      <i class="s-fab__badge">{{ cartCount > 99 ? '99+' : cartCount }}</i>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import RichShopCard from '../components/RichShopCard.vue'
import EmptyState from '../components/EmptyState.vue'
import { listShops, getShopDetail, getCategories, addToCart, getCartCount } from '../api'
import { toast } from '../api/http'

defineOptions({ name: 'SearchView' })

const router = useRouter()
const kw = ref('')
const shops = ref([])
const dishMap = ref({})
const loading = ref(false)
const inputEl = ref(null)
const hot = ['炸鸡', '奶茶', '麻辣烫', '汉堡', '咖啡', '寿司']

/* 头部分类行（真实品类数据，视觉行） */
const cats = ref([])

/* 筛选：排序 + 自提更省 */
const sortKey = ref('recommend')
const sortOpen = ref(false)
const pickupOnly = ref(false)
const sorts = [
  { key: 'recommend', label: '综合排序' },
  { key: 'sales', label: '销量最高' },
  { key: 'rating', label: '评分最高' },
  { key: 'minprice', label: '起送价最低' }
]
const sortLabel = computed(() => (sorts.find(s => s.key === sortKey.value) || sorts[0]).label)

/* 悬浮购物车 */
const cartCount = ref(0)
let lastShopId = null

const displayed = computed(() =>
  pickupOnly.value ? shops.value.filter(s => s.pickupEnabled) : shops.value
)

let timer = null
function onInput() {
  clearTimeout(timer)
  timer = setTimeout(doSearch, 350)
}

async function doSearch() {
  sortOpen.value = false
  const keyword = kw.value.trim()
  if (!keyword) {
    shops.value = []
    dishMap.value = {}
    return
  }
  loading.value = true
  try {
    const r = await listShops({ keyword, sort: sortKey.value })
    shops.value = r.data || []
    // 每家店取店内商品（按月销排序取前 6），渲染爆款商品行；单店失败不阻塞整页
    const list = displayed.value
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
  } finally {
    loading.value = false
  }
}

function pickSort(key) {
  sortKey.value = key
  sortOpen.value = false
  doSearch()
}

function togglePickup() {
  pickupOnly.value = !pickupOnly.value
}

function onMonthly() {
  toast('超级月卡折上折：活动页在 SRS 5.2.10 中列出', 'info')
}

function onCoupon() {
  toast('活动页规划中', 'info')
}

function onCat(c) {
  toast(`「${c.name}」分类筛选即将上线，先看看全部店铺`, 'info')
}

/* 爆款行 + 加购：无规格直接加，有规格引导进店选规格 */
async function onAdd(dish) {
  if (dish.hasSpec) {
    goShop(dish.shopId)
    return
  }
  try {
    await addToCart({ dishId: dish.id, quantity: 1 })
    lastShopId = dish.shopId
    await refreshCartCount()
    toast('已加入购物车', 'success')
  } catch (e) {
    toast(e.message, 'error')
  }
}

async function refreshCartCount() {
  try {
    const r = await getCartCount()
    cartCount.value = r.data || 0
  } catch {
    /* 未登录等场景静默 */
  }
}

function goCartShop() {
  if (lastShopId) goShop(lastShopId)
  else goShop(displayed.value[0]?.id)
}

function quick(t) {
  kw.value = t
  doSearch()
}

function clearKw() {
  kw.value = ''
  shops.value = []
  dishMap.value = {}
  inputEl.value && inputEl.value.focus()
}

function cancel() {
  router.back()
}

function goShop(id) {
  router.push(`/shop/${id}`)
}

/* 与首页金刚区一致的分类配色 / 图标映射 */
const CAT_ICON = {
  food: 'fork', fast: 'truck', drink: 'wallet', fresh: 'heart', market: 'store',
  medical: 'shield', cake: 'gift', pickup: 'receipt', coupon: 'ticket', all: 'chart'
}
const CAT_BG = {
  food: 'linear-gradient(135deg,#FFB03A,#F5481F)',
  drink: 'linear-gradient(135deg,#FFD9A0,#E8834A)',
  fast: 'linear-gradient(135deg,#B7D9F7,#2E6FBF)',
  fresh: 'linear-gradient(135deg,#C7E9B0,#4E9B3F)',
  market: 'linear-gradient(135deg,#C9D6FF,#3A6BF0)',
  medical: 'linear-gradient(135deg,#A8E6CF,#1F8A70)',
  cake: 'linear-gradient(135deg,#FFC1CC,#C2185B)',
  pickup: 'linear-gradient(135deg,#12BFA0,#0F8A78)',
  coupon: 'linear-gradient(135deg,#FFE39A,#F2B23C)',
  all: 'linear-gradient(135deg,#D7C4A3,#7A5B37)'
}
function catIcon(icon) {
  return CAT_ICON[icon] || 'store'
}
function catBg(icon) {
  return CAT_BG[icon] || CAT_BG.food
}

onMounted(async () => {
  nextTick(() => inputEl.value && inputEl.value.focus())
  refreshCartCount()
  try {
    const r = await getCategories()
    cats.value = r.data || []
  } catch {
    /* 分类行纯视觉，失败不阻塞 */
  }
})
</script>

<style scoped>
.search {
  min-height: 100%;
  background: var(--bg-sunken, #f4f5f7);
  padding-bottom: 80px;
}

/* ---------------- 吸顶头部 ---------------- */
.s-head {
  position: sticky;
  top: 0;
  z-index: 6;
  background: #fff;
  border-radius: 0 0 12px 12px;
  box-shadow: 0 4px 12px rgba(23, 24, 26, .04);
  padding-bottom: 8px;
}

.s-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: calc(8px + env(safe-area-inset-top, 0px)) var(--sp-page, 12px) 8px;
}

.s-bar__back {
  flex-shrink: 0;
  background: none;
  border: none;
  padding: 2px;
  display: flex;
}

.s-bar__field {
  flex: 1;
  min-width: 0;
  height: 34px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  border-radius: var(--r-btn, 17px);
  background: var(--bg-sunken, #f2f3f5);
}

.s-bar__input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: var(--t-1, #1a1a1a);
}

.s-bar__clear {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: none;
  background: #c8c9cc;
  color: #fff;
  font-size: 10px;
  line-height: 1;
}

.s-bar__go {
  flex-shrink: 0;
  height: 30px;
  padding: 0 14px;
  border: none;
  border-radius: 15px;
  background: linear-gradient(135deg, #FF5B4A, #E02E24);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}
.s-bar__go:active {
  opacity: .85;
}

/* ---------------- 分类行 ---------------- */
.s-cats {
  display: flex;
  gap: 18px;
  overflow-x: auto;
  padding: 4px var(--sp-page, 12px) 8px;
}

.s-cat {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--t-2, #555);
  white-space: nowrap;
  -webkit-user-select: none;
  user-select: none;
}
.s-cat:active {
  opacity: .6;
}

.s-cat--on {
  color: #1a1a1a;
  font-weight: 700;
}

.s-cat__icon {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ---------------- 筛选行 ---------------- */
.s-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 var(--sp-page, 12px);
}

.s-filter {
  position: relative;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  height: 28px;
  padding: 0 12px;
  border-radius: 14px;
  background: var(--bg-sunken, #f2f3f5);
  color: var(--t-2, #555);
  font-size: 13px;
  -webkit-user-select: none;
  user-select: none;
}
.s-filter:active {
  opacity: .7;
}

.s-filter--picked {
  color: #B9760F;
  background: #FFF5E6;
  box-shadow: inset 0 0 0 1px #FFE1B3;
}

.s-filter__caret {
  width: 0;
  height: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 5px solid currentColor;
  transition: transform .15s ease;
}
.s-filter__caret--up {
  transform: rotate(180deg);
}

.s-sort-pop {
  position: absolute;
  left: 0;
  top: 34px;
  min-width: 120px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 6px 20px rgba(23, 24, 26, .12);
  padding: 4px;
  z-index: 7;
}

.s-sort-item {
  padding: 9px 12px;
  font-size: 13px;
  color: var(--t-2, #555);
  border-radius: 7px;
  white-space: nowrap;
}
.s-sort-item:active {
  background: var(--bg-sunken, #f2f3f5);
}
.s-sort-item--on {
  color: #E02E24;
  font-weight: 700;
}

/* ---------------- 结果区 ---------------- */
.s-body {
  padding: 10px var(--sp-page, 12px) 0;
}

.s-count {
  font-size: 12px;
  color: var(--t-3, #999);
}

.s-empty {
  padding: 28px var(--sp-page, 12px);
}
.s-empty__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--t-1, #1a1a1a);
  margin-bottom: 14px;
}
.hot {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.hot__tag {
  padding: 6px 14px;
  border-radius: 16px;
  border: none;
  background: #fff;
  color: var(--t-2, #555);
  font-size: 13px;
  box-shadow: var(--sh-light);
}

/* ---------------- 悬浮购物车 ---------------- */
.s-fab {
  position: fixed;
  right: 16px;
  bottom: calc(24px + env(safe-area-inset-bottom, 0px));
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 6px 20px rgba(23, 24, 26, .18);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 8;
}
.s-fab:active {
  transform: scale(.94);
}

.s-fab__emoji {
  font-size: 22px;
  line-height: 1;
}

.s-fab__badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  border-radius: 9px;
  background: #E02E24;
  color: #fff;
  font-size: 10px;
  font-style: normal;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid #fff;
}
</style>
