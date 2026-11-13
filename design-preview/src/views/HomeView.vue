<template>
  <TabPage>
    <!-- 顶部：品牌渐变头部（定位 / 搜索） -->
    <template #header>
      <header class="home-head">
        <StatusBar />
        <div class="home-head__row">
          <button class="home-city" @click="toast('演示版：多城市运营将在后续版本开放')">
            <span>{{ city }}</span>
            <svg width="10" height="6" viewBox="0 0 10 6" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><path d="M1 1l4 4 4-4" /></svg>
          </button>
          <div class="home-head__ops">
            <button class="home-op" @click="toast('演示版：扫码功能后续开放')">
              <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="butt" stroke-linejoin="miter"><path d="M4 8V4h4M16 4h4v4M20 16v4h-4M8 20H4v-4"/><path d="M4 12h16"/></svg>
            </button>
            <button class="home-op" @click="toast('暂无新通知')">
              <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="butt" stroke-linejoin="miter"><path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6"/><path d="M10 19a2 2 0 0 0 4 0"/></svg>
            </button>
          </div>
        </div>
        <div class="home-search">
          <svg class="home-search__icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="butt" stroke-linejoin="miter"><circle cx="11" cy="11" r="6.5" /><path d="M15.8 15.8 20.5 20.5" /></svg>
          <input v-model="keyword" placeholder="搜索店铺 / 商品" @keyup.enter="onSearch" />
          <button class="home-search__btn" @click="onSearch">搜索</button>
        </div>
      </header>
    </template>

    <!-- 新客立减 Banner -->
    <section class="gift">
      <div class="gift__tag">{{ banner.tag }}</div>
      <div class="gift__title">{{ banner.title }}</div>
      <div class="gift__actions">
        <button v-for="a in banner.actions" :key="a.label" class="gift__btn" :class="{ 'gift__btn--ghost': !a.primary }" @click="toast(`演示版：${a.label}`)">
          {{ a.label }}
        </button>
      </div>
      <div class="gift__dots"><i v-for="i in 3" :key="i" :class="{ on: i === 1 }" /></div>
    </section>

    <!-- 金刚区分类 -->
    <section class="card cats">
      <button v-for="c in categories" :key="c.name" class="cats__item" @click="onCategory(c)">
        <span class="cats__icon" :style="{ background: c.bg }">{{ c.emoji }}</span>
        <span class="cats__label">{{ c.name }}</span>
      </button>
    </section>

    <!-- 限时秒杀 -->
    <section class="card seckill">
      <div class="seckill__head">
        <div class="seckill__title"><span class="seckill__fire">🔥</span>限时秒杀</div>
        <span class="seckill__count"><b class="num">{{ countdown }}</b> 后结束</span>
        <button class="seckill__more" @click="toast('演示版：更多秒杀场次后续开放')">更多 ›</button>
      </div>
      <div class="seckill__list">
        <button v-for="s in seckill.items" :key="s.name" class="seckill__item" @click="toast(`演示版：${s.name} 详情后续开放`)">
          <span class="seckill__img" :style="{ background: s.bg }">{{ s.emoji }}</span>
          <span class="seckill__name">{{ s.name }}</span>
          <span class="seckill__price"><b>¥{{ price(s.price) }}</b><s>¥{{ price(s.origin) }}</s></span>
        </button>
      </div>
    </section>

    <!-- 优惠券入口 -->
    <section class="coupons">
      <button v-for="c in coupons" :key="c.key" class="coupon" @click="toast(`演示版：领取「${c.desc}」后续开放`)">
        <span class="coupon__left">
          <span class="coupon__title">{{ c.title }}</span>
          <span class="coupon__amount"><b>¥{{ c.amount }}</b> {{ c.desc }}</span>
          <span class="coupon__note">{{ c.note }}</span>
        </span>
        <span class="coupon__cta">去领取</span>
      </button>
    </section>

    <!-- 店铺流 -->
    <section class="shops">
      <div class="sortbar">
        <button
          v-for="s in sorts"
          :key="s.key"
          class="sortbar__item"
          :class="{ active: sortKey === s.key }"
          @click="sortKey = s.key"
        >
          {{ s.label }}
        </button>
        <button class="sortbar__filter" @click="sheetOpen = true">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="butt" stroke-linejoin="miter"><path d="M4 6h16M7 12h10M10 18h4" /></svg>
          筛选
        </button>
      </div>

      <button v-for="s in visibleShops" :key="s.id" class="shop" @click="toast(`演示版：${s.name} 店铺详情后续开放`)">
        <span class="shop__img" :style="{ background: s.bg }">{{ s.emoji }}</span>
        <span class="shop__main">
          <span class="shop__name">{{ s.name }}</span>
          <span class="shop__meta">
            <b class="num">★ {{ s.rating }}</b>
            <span class="muted">月售{{ s.monthSales }}</span>
            <span class="muted">起送 ¥{{ price(s.minPrice) }}</span>
            <span class="muted num">{{ s.distance }}km</span>
          </span>
          <span class="shop__tags">
            <span v-for="t in s.tags" :key="t" class="tag" :class="t === '支持自取' ? 'tag--green' : 'tag--orange'">{{ t }}</span>
          </span>
          <span class="shop__bottom">
            <span v-if="s.deliveryFee === 0" class="shop__fee">免配送费</span>
            <span v-else class="shop__fee muted">配送 ¥{{ price(s.deliveryFee) }}</span>
            <span class="shop__eta">预计 {{ s.etaMinutes }}分钟 送达</span>
          </span>
        </span>
      </button>

      <div v-if="!visibleShops.length" class="shops__empty">没有符合条件的店铺，换个筛选条件试试</div>
      <div class="shops__end">— 已展示全部演示店铺 —</div>
    </section>

    <!-- 筛选弹层 -->
    <template #sheet>
      <div v-if="sheetOpen" class="mask" @click.self="sheetOpen = false">
        <div class="sheet">
          <div class="sheet__title">筛选</div>
          <label class="sheet__opt"><input v-model="filters.selfPickup" type="checkbox" /><span>仅看支持到店自取</span></label>
          <label class="sheet__opt"><input v-model="filters.promo" type="checkbox" /><span>仅看有优惠活动</span></label>
          <div class="sheet__btns">
            <button class="btn" @click="resetFilters">重置</button>
            <button class="btn btn--brand" @click="sheetOpen = false">查看 {{ visibleShops.length }} 家店铺</button>
          </div>
        </div>
      </div>
    </template>
  </TabPage>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import TabPage from '../components/TabPage.vue'
import StatusBar from '../components/StatusBar.vue'
import { useToast } from '../store/toast'
import { banner, categories, city, coupons, seckill, shops, sorts } from '../mock/home'

const { show } = useToast()
const toast = show

const keyword = ref('')
const sortKey = ref('default')
const sheetOpen = ref(false)
const filters = ref({ selfPickup: false, promo: false })

function price(cents) {
  return (cents / 100).toFixed(2).replace(/\.?0+$/, '')
}

function onSearch() {
  const kw = keyword.value.trim()
  toast(kw ? `演示版：搜索「${kw}」后续接入真实检索` : '请输入店铺或商品名称')
}

function onCategory(c) {
  toast(c.name === '更多' ? '演示版：全部分类后续开放' : `演示版：进入「${c.name}」分类列表`)
}

function resetFilters() {
  filters.value = { selfPickup: false, promo: false }
}

const visibleShops = computed(() => {
  let list = shops.slice()
  const kw = keyword.value.trim()
  if (kw) list = list.filter((s) => s.name.includes(kw))
  if (filters.value.selfPickup) list = list.filter((s) => s.selfPickup)
  if (filters.value.promo) list = list.filter((s) => s.tags.some((t) => t.includes('减')))
  if (sortKey.value === 'distance') list.sort((a, b) => a.distance - b.distance)
  if (sortKey.value === 'sales') list.sort((a, b) => b.monthSales - a.monthSales)
  if (sortKey.value === 'rating') list.sort((a, b) => b.rating - a.rating)
  return list
})

/* 秒杀倒计时 */
const countdown = ref('')
let timer = null
function tick() {
  const remain = Math.max(0, seckill.endAt - Date.now())
  const h = String(Math.floor(remain / 3600000)).padStart(2, '0')
  const m = String(Math.floor((remain % 3600000) / 60000)).padStart(2, '0')
  const s = String(Math.floor((remain % 60000) / 1000)).padStart(2, '0')
  countdown.value = `${h}:${m}:${s}`
}
onMounted(() => {
  tick()
  timer = setInterval(tick, 1000)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
/* 顶部渐变头部 */
.home-head {
  background: var(--brand-grad);
  padding-bottom: 36px;
  border-radius: 0 0 18px 18px;
  box-shadow: 0 8px 18px rgba(245, 72, 31, .18);
}
.home-head__row { display: flex; align-items: center; justify-content: space-between; padding: 6px 14px 10px; }
.home-city { display: inline-flex; align-items: center; gap: 4px; color: #fff; font-size: 16px; font-weight: 600; }
.home-head__ops { display: flex; gap: 16px; }
.home-op { color: #fff; display: inline-flex; }
.home-search { margin: 0 14px; background: #fff; border-radius: var(--r-btn); height: 38px; display: flex; align-items: center; padding: 0 4px 0 12px; gap: 8px; }
.home-search__icon { color: var(--text-3); flex-shrink: 0; }
.home-search input { flex: 1; border: none; outline: none; font-size: 13px; color: var(--text-1); min-width: 0; }
.home-search input::placeholder { color: var(--text-3); }
.home-search__btn { height: 30px; padding: 0 14px; border-radius: 6px; background: var(--brand-grad); color: #fff; font-size: 13px; font-weight: 600; }

/* 新客 Banner */
.gift {
  position: relative; margin: -26px 12px 12px; padding: 16px 16px 18px;
  border-radius: var(--r-card); color: #fff; overflow: hidden;
  background: linear-gradient(120deg, #ff8a2b, #f5481f 70%);
  box-shadow: var(--shadow-brand);
}
.gift::after {
  content: ''; position: absolute; right: -30px; top: -40px; width: 140px; height: 140px;
  border-radius: 50%; background: rgba(255, 255, 255, .12);
}
.gift__tag { font-size: 10px; letter-spacing: 1px; opacity: .9; }
.gift__title { font-size: 24px; font-weight: 700; margin-top: 4px; letter-spacing: .5px; }
.gift__actions { display: flex; gap: 10px; margin-top: 12px; }
.gift__btn { height: 30px; padding: 0 16px; border-radius: var(--r-btn); background: #fff; color: #f5481f; font-size: 12px; font-weight: 600; }
.gift__btn--ghost { background: rgba(255, 255, 255, .18); color: #fff; border: 1px solid rgba(255, 255, 255, .55); }
.gift__dots { display: flex; gap: 4px; margin-top: 12px; }
.gift__dots i { width: 5px; height: 5px; border-radius: 50%; background: rgba(255, 255, 255, .45); }
.gift__dots i.on { width: 12px; border-radius: 3px; background: #fff; }

/* 金刚区 */
.cats { display: grid; grid-template-columns: repeat(5, 1fr); row-gap: 14px; }
.cats__item { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.cats__icon { width: 42px; height: 42px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 22px; }
.cats__label { font-size: 12px; color: var(--text-1); }

/* 限时秒杀 */
.seckill__head { display: flex; align-items: center; gap: 8px; }
.seckill__title { font-size: 16px; font-weight: 700; display: inline-flex; align-items: center; gap: 4px; }
.seckill__fire { font-size: 15px; }
.seckill__count { font-size: 11px; color: var(--text-2); background: var(--panel-2); border: 1px solid var(--line); border-radius: var(--r-tag); padding: 2px 7px; }
.seckill__count b { color: var(--error); font-weight: 600; }
.seckill__more { margin-left: auto; font-size: 12px; color: var(--text-3); }
.seckill__list { display: flex; gap: 10px; margin-top: 12px; }
.seckill__item { flex: 1; display: flex; flex-direction: column; align-items: flex-start; gap: 4px; }
.seckill__img { width: 100%; height: 74px; border-radius: var(--r-img); display: flex; align-items: center; justify-content: center; font-size: 30px; }
.seckill__name { font-size: 12px; color: var(--text-1); }
.seckill__price { font-size: 11px; }
.seckill__price b { color: var(--price-red); font-size: 15px; margin-right: 4px; }
.seckill__price s { color: var(--text-3); }

/* 优惠券入口 */
.coupons { display: flex; gap: 10px; margin: 0 12px 12px; }
.coupon { flex: 1; display: flex; align-items: center; justify-content: space-between; background: var(--panel); border-radius: var(--r-card); padding: 10px 12px; box-shadow: var(--shadow-light); text-align: left; }
.coupon__left { display: flex; flex-direction: column; gap: 2px; }
.coupon__title { font-size: 13px; font-weight: 600; }
.coupon__amount { font-size: 11px; color: var(--text-2); }
.coupon__amount b { color: var(--price-red); font-size: 15px; }
.coupon__note { font-size: 10px; color: var(--text-3); }
.coupon__cta { font-size: 11px; color: #fff; background: var(--brand-grad); border-radius: var(--r-tag); padding: 4px 8px; flex-shrink: 0; }

/* 排序栏 */
.sortbar { display: flex; align-items: center; gap: 16px; padding: 2px 12px 10px; }
.sortbar__item { font-size: 13px; color: var(--text-2); position: relative; }
.sortbar__item.active { color: var(--text-1); font-weight: 600; }
.sortbar__item.active::after { content: ''; position: absolute; left: 50%; bottom: -5px; transform: translateX(-50%); width: 14px; height: 2px; border-radius: 2px; background: var(--brand-grad); }
.sortbar__filter { margin-left: auto; display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: var(--text-2); }

/* 店铺卡片 */
.shop { width: calc(100% - 24px); margin: 0 12px 12px; display: flex; gap: 10px; background: var(--panel); border-radius: var(--r-card); padding: 12px; box-shadow: var(--shadow-light); text-align: left; }
.shop__img { width: 84px; height: 84px; border-radius: var(--r-img); flex-shrink: 0; display: flex; align-items: center; justify-content: center; font-size: 34px; }
.shop__main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 5px; }
.shop__name { font-size: 15px; font-weight: 700; }
.shop__meta { display: flex; align-items: center; gap: 8px; font-size: 11px; }
.shop__meta b { color: #f2a600; }
.shop__tags { display: flex; flex-wrap: wrap; gap: 4px; }
.shop__bottom { display: flex; align-items: center; gap: 8px; font-size: 11px; }
.shop__fee { color: var(--success); font-weight: 600; }
.shop__eta { color: var(--info); }
.shops__empty { text-align: center; color: var(--text-3); font-size: 13px; padding: 30px 0; }
.shops__end { text-align: center; color: var(--text-3); font-size: 11px; padding: 4px 0 18px; }

/* 筛选弹层 */
.mask { position: absolute; inset: 0; background: rgba(23, 24, 26, .4); z-index: 50; display: flex; align-items: flex-end; }
.sheet { width: 100%; background: var(--panel); border-radius: var(--r-sheet) var(--r-sheet) 0 0; padding: 18px 18px 26px; }
.sheet__title { font-size: 15px; font-weight: 700; margin-bottom: 12px; }
.sheet__opt { display: flex; align-items: center; gap: 8px; font-size: 13px; padding: 8px 0; }
.sheet__opt input { width: 16px; height: 16px; accent-color: var(--brand-orange); }
.sheet__btns { display: flex; gap: 10px; margin-top: 12px; }
.sheet__btns .btn { flex: 1; }
</style>
