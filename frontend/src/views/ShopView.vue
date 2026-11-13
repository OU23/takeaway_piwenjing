<template>
  <div class="page page--notab">
    <NavBar :brand="true" :title="shop.name || ''">
      <template #extra>
        <Icon name="heart" :size="18" color="#fff" />
      </template>
    </NavBar>

    <div v-if="!ready" style="padding: 12px">
      <div class="skeleton" style="height: 120px; border-radius: 10px" />
      <div class="skeleton mt-12" style="height: 260px; border-radius: 10px" />
    </div>

    <template v-else>
      <!-- 门店头图 -->
      <PhImage class="shop-hero" :image="shop.headerImg" :label="shop.name" style="border-radius: 0" />

      <div class="page-body" style="margin-top: -14px">
        <!-- 门店信息卡 -->
        <div class="card">
          <div class="row row--between">
            <div class="shop-name ellipsis">{{ shop.name }}</div>
            <span class="tag" :class="shop.status === 'OPEN' ? 'tag--success' : 'tag--plain'">
              {{ shop.statusLabel }}
            </span>
          </div>

          <div class="shop-stats">
            <span class="text-warn">
              <Icon name="star" :size="11" color="#FF9500" /> {{ shop.rating }}
            </span>
            <span>月售 {{ shop.monthSales }}</span>
            <span>起送 ¥{{ yuan(shop.minPrice) }}</span>
            <span>配送 ¥{{ yuan(shop.deliveryFee) }}</span>
            <span>{{ shop.distanceKm }}km</span>
          </div>

          <div class="shop-fees">
            <span><Icon name="clock" :size="11" /> {{ shop.openTime }}–{{ shop.closeTime }}</span>
            <span class="row flex-1 ellipsis" style="gap: 3px">
              <Icon name="nearby" :size="11" />
              {{ shop.address }}
            </span>
          </div>

          <div v-if="shop.notice" class="shop-notice">
            <span class="tag tag--promo">公告</span>
            <span class="ellipsis">{{ shop.notice }}</span>
          </div>

          <div ref="tagsEl" class="tag-list mt-8">
            <span v-for="(p, i) in promos" :key="'p' + i" class="tag tag--promo">{{ p }}</span>
            <span v-if="shop.pickupEnabled" class="tag tag--success">
              支持到店自取<template v-if="shop.pickupDiscount > 0"> · 减 ¥{{ yuan(shop.pickupDiscount) }}</template>
            </span>
            <span v-for="(t, i) in plainTags" :key="'t' + i" class="tag tag--plain">{{ t }}</span>
          </div>
        </div>

        <!-- 左分类 + 右商品 -->
        <div class="card mt-12">
          <div class="menu">
            <ul class="menu__left">
              <li
                v-for="c in categories"
                :key="c.id"
                class="menu__cat tap-card"
                :class="{ 'menu__cat--active': activeCat === c.id }"
                @click="scrollToCat(c.id)"
              >
                {{ c.name }}
                <i v-if="catCountMap[c.id]" class="menu__badge num">{{ catCountMap[c.id] }}</i>
              </li>
            </ul>

            <div ref="rightEl" class="menu__right" @scroll="onRightScroll">
              <div v-for="c in categories" :key="c.id" :ref="(el) => setSectionRef(c.id, el)" class="menu__section">
                <div class="menu__section-title">{{ c.name }}</div>
                <DishRow
                  v-for="d in dishesByCat[c.id] || []"
                  :key="d.id"
                  :dish="d"
                  :quantity="qtyByDish[d.id] || 0"
                  @plus="onPlus(d, $event)"
                  @minus="onMinus(d, $event)"
                  @spec="onOpenSpec(d)"
                />
              </div>
              <div class="text-aux" style="text-align: center; padding: 18px 0">
                — 已经到底了 —
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 底部悬浮购物车栏 -->
    <div class="cart-bar">
      <div class="cart-bar__icon" :class="{ 'cart-bar__icon--empty': !totalQty }" ref="cartIconEl" @click="toggleCart">
        <Icon name="receipt" :size="21" color="#fff" />
        <i v-if="totalQty" class="cart-bar__badge num">{{ totalQty }}</i>
      </div>
      <div class="cart-bar__info" @click="toggleCart">
        <template v-if="totalQty">
          <div class="price"><span class="price__sym">¥</span>{{ yuan(itemAmount) }}</div>
          <div class="text-aux">另需配送费 ¥{{ yuan(shop.deliveryFee) }}</div>
        </template>
        <template v-else>
          <div class="text-aux" style="font-size: 13px">购物车是空的</div>
          <div class="text-aux">起送 ¥{{ yuan(shop.minPrice) }}</div>
        </template>
      </div>
      <button
        class="btn btn--primary cart-bar__btn"
        :disabled="!canCheckout"
        @click="goCheckout"
      >
        {{ checkoutLabel }}
      </button>
    </div>

    <!-- 购物车展开清单 -->
    <div v-if="cartOpen" class="mask" @click.self="cartOpen = false">
      <div class="sheet">
        <div class="sheet__head">
          <div>
            <div class="text-strong">已选商品</div>
            <div class="text-aux mt-4">{{ cart.shopName }}</div>
          </div>
          <button class="sheet__close" @click="cartOpen = false">
            <Icon name="close" :size="13" />
          </button>
        </div>
        <div class="sheet__body">
          <div v-for="l in cart.lines" :key="l.id" class="cart-line">
            <PhImage style="width: 44px; height: 44px" :image="l.dishImage" :label="l.dishName.slice(0, 2)" />
            <div class="flex-1">
              <div class="text-strong ellipsis">{{ l.dishName }}</div>
              <div v-if="l.specText" class="text-aux ellipsis">{{ l.specText }}</div>
            </div>
            <div style="text-align: right">
              <div class="price price--sm"><span class="price__sym">¥</span>{{ yuan(l.amount) }}</div>
              <Stepper
                class="mt-4"
                :quantity="l.quantity"
                @plus="changeLine(l, l.quantity + 1)"
                @minus="changeLine(l, l.quantity - 1)"
              />
            </div>
          </div>
        </div>
        <div class="sheet__foot row row--between">
          <button class="btn btn--weak btn--sm" @click="clearAll">清空购物车</button>
          <button class="btn btn--primary btn--sm" :disabled="!canCheckout" @click="goCheckout">
            {{ checkoutLabel }}
          </button>
        </div>
      </div>
    </div>

    <!-- 规格弹窗 -->
    <SpecSheet
      v-if="specDish"
      :dish="specDish"
      :initial-spec-ids="specInitial"
      @close="specDish = null"
      @confirm="onSpecConfirm"
    />
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import Icon from '../components/Icon.vue'
import PhImage from '../components/PhImage.vue'
import DishRow from '../components/DishRow.vue'
import Stepper from '../components/Stepper.vue'
import SpecSheet from '../components/SpecSheet.vue'
import { addToCart, clearCart, getCart, getDish, getShopDetail, removeCartItem, updateCartItem } from '../api'
import { toast } from '../api/http'
import { yuan } from '../utils/format'
import { lockScroll, rafThrottle, unlockScroll } from '../utils/ui'
import { useAppStore } from '../store/app'

const route = useRoute()
const router = useRouter()
const app = useAppStore()

const shopId = route.params.shopId
const shop = ref({})
const categories = ref([])
const dishes = ref([])
const cart = ref({ lines: [], itemAmount: 0, totalQuantity: 0 })
const ready = ref(false)
const cartOpen = ref(false)
const activeCat = ref(null)
const specDish = ref(null)
const specInitial = ref([])

const rightEl = ref(null)
const cartIconEl = ref(null)
const sectionRefs = {}
let suppressSpy = false
let spyTimer = null

/* ------------------------------------------------------------------ */
/* 派生数据：一次计算，模板只做查表                                    */
/* ------------------------------------------------------------------ */
/*
 * 原实现把 dishesOf / qtyOf / catCount 直接写在模板里，每次渲染都要
 * 对全量菜品 + 全量购物车行做 filter + reduce，而 catCount 内部又嵌套
 * 调用 dishesOf 与 qtyOf —— 复杂度是 O(菜品数 × 购物车行数)。
 * 菜品上百条时，每点一次加购都会卡一下。这里统一预计算成字典。
 */
const dishesByCat = computed(() => {
  const map = Object.create(null)
  for (const d of dishes.value) {
    const key = d.categoryId
    if (!map[key]) map[key] = []
    map[key].push(d)
  }
  return map
})

const qtyByDish = computed(() => {
  const map = Object.create(null)
  for (const l of cart.value.lines || []) {
    map[l.dishId] = (map[l.dishId] || 0) + (l.quantity || 0)
  }
  return map
})

const catCountMap = computed(() => {
  const map = Object.create(null)
  for (const catId in dishesByCat.value) {
    let n = 0
    for (const d of dishesByCat.value[catId]) n += qtyByDish.value[d.id] || 0
    if (n) map[catId] = n
  }
  return map
})

/** 购物车总件数：以本地行为准，保证乐观更新后角标立刻变化 */
const totalQty = computed(() =>
  (cart.value.lines || []).reduce((sum, l) => sum + (l.quantity || 0), 0)
)

const itemAmount = computed(() => {
  const lines = cart.value.lines || []
  if (!lines.length) return 0
  return lines.reduce((sum, l) => sum + (l.amount || 0), 0)
})

const canCheckout = computed(() =>
  totalQty.value > 0 && itemAmount.value >= (shop.value.minPrice || 0)
)

const checkoutLabel = computed(() => {
  if (!totalQty.value) return `¥${yuan(shop.value.minPrice || 0)} 起送`
  const gap = (shop.value.minPrice || 0) - itemAmount.value
  return gap > 0 ? `差 ¥${yuan(gap)} 起送` : '去结算'
})

const promos = computed(() => shop.value.promotions || [])
const plainTags = computed(() =>
  (shop.value.tags || []).filter(t => !/满|减|支持到店自取/.test(t))
)

function setSectionRef(id, el) {
  if (el) sectionRefs[id] = el
}

/* ------------------------------------------------------------------ */
/* 左侧分类 ↔ 右侧滚动联动                                             */
/* ------------------------------------------------------------------ */
/*
 * 滚动时逐帧读取 el.offsetTop 会强制同步布局（layout thrashing），
 * 在低端安卓上直接表现为「滑动时左侧分类卡顿」。
 * 改为：数据就绪后测量一次各分组位置并缓存，滚动回调只查表 + rAF 节流。
 */
let sectionTops = []

function measureSections() {
  const container = rightEl.value
  if (!container) return
  const origin = container.getBoundingClientRect().top - container.scrollTop
  sectionTops = categories.value
    .map((c) => {
      const el = sectionRefs[c.id]
      if (!el) return null
      return { id: c.id, top: el.getBoundingClientRect().top - origin }
    })
    .filter(Boolean)
}

const onRightScroll = rafThrottle(() => {
  if (suppressSpy || !rightEl.value) return
  const y = rightEl.value.scrollTop + 12
  let current = sectionTops[0]?.id
  for (const s of sectionTops) {
    if (s.top <= y) current = s.id
    else break
  }
  if (current !== undefined && current !== activeCat.value) activeCat.value = current
})

function scrollToCat(catId) {
  activeCat.value = catId
  const hit = sectionTops.find((s) => s.id === catId)
  if (!hit || !rightEl.value) return
  suppressSpy = true
  rightEl.value.scrollTo({ top: Math.max(0, hit.top - 2), behavior: 'smooth' })
  if (spyTimer) clearTimeout(spyTimer)
  spyTimer = setTimeout(() => { suppressSpy = false }, 420)
}

/* ------------------------------------------------------------------ */
/* 购物车：本地乐观更新 + 串行同步                                     */
/* ------------------------------------------------------------------ */
/*
 * 原实现每次加减都要「发请求 → 再拉一次购物车」，用户点一下要等两个
 * 往返 UI 才变，弱网下像卡死；连点时请求还会乱序。
 * 现在：先改本地（立即响应），请求串行排队，最后静默校准一次。
 */
let syncChain = Promise.resolve()
let pendingSync = 0
let reconcileTimer = null
let tempLineId = -1
/**
 * 本地购物车版本号：每次乐观变更 +1。
 * 校准请求是异步的，回来时若版本号已变，说明用户在这期间又点了，
 * 这份结果是旧快照，必须丢弃，否则会把刚加进去的商品冲掉。
 */
let cartVersion = 0

function recalcTotals() {
  const lines = cart.value.lines || []
  cart.value.totalQuantity = lines.reduce((s, l) => s + (l.quantity || 0), 0)
  cart.value.itemAmount = lines.reduce((s, l) => s + (l.amount || 0), 0)
}

function enqueue(task) {
  pendingSync += 1
  syncChain = syncChain.then(async () => {
    try {
      await task()
    } catch (e) {
      /* 错误由请求拦截器统一提示 */
    } finally {
      pendingSync -= 1
      // 只有队列彻底清空才校准：请求还在飞时拉购物车，拿到的是中间态
      if (pendingSync === 0) scheduleReconcile()
    }
  })
  return syncChain
}

function scheduleReconcile() {
  if (reconcileTimer) clearTimeout(reconcileTimer)
  reconcileTimer = setTimeout(reconcile, 600)
}

/**
 * 用服务端真值校准本地购物车。
 * 注意：队列非空时直接返回，而不是「放弃校准」——
 * 剩余请求落地时 enqueue 的 finally 会重新排期，所以不会漏掉。
 * （早先的写法在这里 return 且不重试，弱网下临时行会永久残留。）
 */
async function reconcile() {
  reconcileTimer = null
  if (pendingSync > 0) return

  const version = cartVersion
  try {
    const c = await getCart(shopId)
    if (version !== cartVersion || pendingSync > 0) {
      // 校准期间用户又操作了，本次结果已过期
      scheduleReconcile()
      return
    }
    if (c?.data) {
      cart.value = c.data
      app.refreshCartCount()
    }
  } catch (e) {
    /* 校准失败不影响本地状态，下次操作会再校准 */
  }
}

/** 乐观加购：同商品同规格自动合并（与服务端规则一致） */
function optimisticAdd(dish, specText, unitPrice, quantity) {
  const lines = cart.value.lines || (cart.value.lines = [])
  const hit = lines.find((l) => l.dishId === dish.id && (l.specText || '') === (specText || ''))
  if (hit) {
    hit.quantity += quantity
    hit.amount = (hit.unitPrice || unitPrice) * hit.quantity
  } else {
    lines.push({
      id: tempLineId--,
      dishId: dish.id,
      dishName: dish.name,
      dishImage: dish.image,
      specText: specText || '',
      unitPrice,
      quantity,
      amount: unitPrice * quantity
    })
  }
  cartVersion += 1
  recalcTotals()
}

/** 乐观改量 / 删除 */
function optimisticChange(line, quantity) {
  const lines = cart.value.lines || []
  const idx = lines.findIndex((l) => l.id === line.id)
  if (idx < 0) return
  if (quantity <= 0) {
    lines.splice(idx, 1)
  } else {
    const l = lines[idx]
    l.quantity = quantity
    l.amount = (l.unitPrice || 0) * quantity
  }
  cartVersion += 1
  recalcTotals()
  if (!lines.length) cartOpen.value = false
}

/** 从规格 id 推导展示文案与单价（用于乐观占位行） */
function specInfoOf(dish, specIds) {
  const names = []
  let delta = 0
  for (const g of dish.specGroups || []) {
    for (const o of g.options || []) {
      if (specIds.includes(o.id)) {
        names.push(o.name)
        delta += o.priceDelta || 0
      }
    }
  }
  return { text: names.join(' / '), unitPrice: (dish.price || 0) + delta }
}

/* ------------------------------------------------------------------ */
/* 加购动效：商品飞入购物车（SRS 5.4.3 反馈规范）                       */
/* ------------------------------------------------------------------ */
function flyToCart(evt, label) {
  const from = evt?.currentTarget?.getBoundingClientRect?.()
  const to = cartIconEl.value?.getBoundingClientRect?.()
  if (!from || !to) return
  // 尊重系统「减弱动态效果」
  if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return

  const size = 18
  const x0 = from.left + from.width / 2
  const y0 = from.top + from.height / 2
  const x1 = to.left + to.width / 2
  const y1 = to.top + to.height / 2
  const dx = x1 - x0
  const dy = y1 - y0

  const dot = document.createElement('div')
  dot.className = 'fly-dot'
  dot.textContent = label || ''
  dot.style.left = `${x0 - size / 2}px`
  dot.style.top = `${y0 - size / 2}px`
  document.body.appendChild(dot)

  const anim = dot.animate(
    [
      { transform: 'translate3d(0,0,0) scale(1)', opacity: 1, offset: 0 },
      // 中途抬高，模拟抛物线顶点
      { transform: `translate3d(${dx * 0.55}px, ${Math.min(dy, 0) - 90}px, 0) scale(.85)`, opacity: 1, offset: 0.5 },
      { transform: `translate3d(${dx}px, ${dy}px, 0) scale(.35)`, opacity: 0.15, offset: 1 }
    ],
    { duration: 560, easing: 'cubic-bezier(.32,.72,.28,1)' }
  )
  anim.onfinish = () => dot.remove()
  anim.oncancel = () => dot.remove()

  // 购物车图标被「砸」一下
  const icon = cartIconEl.value
  if (icon) {
    icon.classList.remove('cart-bar__icon--bump')
    // 强制重排，保证连续加购时动画能重复播放
    void icon.offsetWidth
    icon.classList.add('cart-bar__icon--bump')
    setTimeout(() => icon.classList.remove('cart-bar__icon--bump'), 320)
  }
}

/* ------------------------------------------------------------------ */
/* 数据加载                                                            */
/* ------------------------------------------------------------------ */
let locked = false

onMounted(async () => {
  try {
    const [d, c] = await Promise.all([getShopDetail(shopId), getCart(shopId)])
    shop.value = d.data.shop
    categories.value = d.data.categories || []
    dishes.value = d.data.dishes || []
    cart.value = c.data || { lines: [] }
    activeCat.value = categories.value[0]?.id || null
  } finally {
    ready.value = true
  }
  await nextTick()
  measureSections()
  window.addEventListener('resize', measureSections)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', measureSections)
  if (spyTimer) clearTimeout(spyTimer)
  if (reconcileTimer) clearTimeout(reconcileTimer)
  if (locked) unlockScroll()
})

/* 弹层打开时锁定背景滚动，避免滚动穿透 */
watch(
  () => cartOpen.value || !!specDish.value,
  (open) => {
    if (open && !locked) {
      lockScroll()
      locked = true
    } else if (!open && locked) {
      unlockScroll()
      locked = false
    }
  }
)

/* ------------------------------------------------------------------ */
/* 加购 / 减购                                                         */
/* ------------------------------------------------------------------ */
/** 打开规格弹窗：`+` 号与「选规格」标签共用同一入口，避免两处行为分叉 */
function onOpenSpec(dish) {
  if (dish.soldOut) return
  specDish.value = dish
  specInitial.value = []
}

async function onPlus(dish, evt) {
  if (dish.soldOut) return
  if (dish.hasSpec) {
    onOpenSpec(dish)
    return
  }
  // 先给动效反馈，再改状态：用户点下去就有回应
  flyToCart(evt, dish.name.slice(0, 1))
  // 立即响应，不等网络
  optimisticAdd(dish, '', dish.price, 1)
  enqueue(async () => {
    const r = await addToCart({ dishId: dish.id, specIds: [], quantity: 1 })
    // 没有未落地的本地行时，直接用返回的权威购物车替换
    if (r?.data && !(cart.value.lines || []).some((l) => l.id < 0)) {
      cart.value = r.data
      app.refreshCartCount()
    }
  })
}

/** 减购：优先减掉该商品最后一条购物车记录 */
async function onMinus(dish) {
  const lines = (cart.value.lines || []).filter((l) => l.dishId === dish.id)
  if (!lines.length) return
  changeLine(lines[lines.length - 1], lines[lines.length - 1].quantity - 1)
}

async function onSpecConfirm({ specIds, quantity }) {
  const dish = specDish.value
  if (!dish) return
  specDish.value = null

  const info = specInfoOf(dish, specIds)
  optimisticAdd(dish, info.text, info.unitPrice, quantity)

  enqueue(async () => {
    const r = await addToCart({ dishId: dish.id, specIds, quantity })
    if (r?.data && !(cart.value.lines || []).some((l) => l.id < 0)) {
      cart.value = r.data
      app.refreshCartCount()
    }
  })

  toast(`已加入「${dish.name}」×${quantity}`, 'success')

  // 后台刷新该菜品（拿最新规格默认值），不阻塞加购反馈
  enqueue(async () => {
    const d = await getDish(dish.id)
    const idx = dishes.value.findIndex((x) => x.id === dish.id)
    if (idx > -1 && d?.data) dishes.value.splice(idx, 1, d.data)
  })
}

function changeLine(line, quantity) {
  const isTemp = line.id < 0
  // 先在本地落定，UI 立即响应
  optimisticChange(line, quantity)

  if (isTemp) {
    /*
     * 这一行还没落地（服务端 id 未知）。
     * 不能只改本地：用户「点 + 后立刻点 -」时，加购请求仍在队列里，
     * 等它落地后服务端会多出一件，只靠校准会让商品「减了又回来」。
     * 正确做法是排到队尾，等加购完成后再定位真实行执行减 / 删。
     */
    enqueue(async () => {
      const c = await getCart(shopId)
      const real = (c?.data?.lines || []).find(
        (l) => l.dishId === line.dishId && (l.specText || '') === (line.specText || '')
      )
      if (!real) return
      if (quantity <= 0) await removeCartItem(real.id)
      else await updateCartItem(real.id, quantity)
    })
    return
  }

  enqueue(async () => {
    if (quantity <= 0) await removeCartItem(line.id)
    else await updateCartItem(line.id, quantity)
  })
}

async function clearAll() {
  cart.value = { ...cart.value, lines: [] }
  cartVersion += 1
  recalcTotals()
  cartOpen.value = false
  toast('购物车已清空')
  enqueue(async () => {
    await clearCart(shopId)
  })
}

function toggleCart() {
  if (!totalQty.value) {
    toast('购物车还是空的，先选点好吃的', 'info')
    return
  }
  cartOpen.value = !cartOpen.value
}

function goCheckout() {
  if (!canCheckout.value) {
    toast(`还差 ¥${yuan((shop.value.minPrice || 0) - itemAmount.value)} 起送`, 'error')
    return
  }
  router.push(`/checkout/${shopId}`)
}
</script>

<style scoped>
.shop-hero {
  width: 100%;
  height: 132px;
  border-radius: 0;
}

.shop-name {
  font-size: var(--fs-title-lg);
  font-weight: var(--fw-title);
  max-width: 70%;
}

.shop-stats,
.shop-fees {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: var(--fs-aux);
  color: var(--t-3);
  margin-top: 7px;
}

.shop-stats span {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.shop-notice {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 7px 8px;
  background: var(--bg-sunken);
  border-radius: var(--r-img);
  font-size: var(--fs-aux);
  color: var(--t-2);
}

/* ---------------- 左分类右商品 ---------------- */
.menu {
  display: flex;
  gap: 8px;
  height: 58vh;
  min-height: 380px;
}

.menu__left {
  width: 82px;
  flex-shrink: 0;
  overflow-y: auto;
  background: var(--bg-sunken);
  border-radius: var(--r-img);
}

.menu__cat {
  padding: 11px 6px;
  font-size: var(--fs-body);
  color: var(--t-2);
  text-align: center;
  position: relative;
}

.menu__cat--active {
  background: var(--bg-card);
  color: var(--brand-3);
  font-weight: var(--fw-title);
}

.menu__badge {
  position: absolute;
  top: 6px;
  right: 6px;
  min-width: 14px;
  height: 14px;
  border-radius: 7px;
  background: var(--c-price);
  color: #fff;
  font-size: 9px;
  line-height: 14px;
  text-align: center;
}

.menu__right {
  flex: 1;
  overflow-y: auto;
  padding-left: 4px;
}

/*
 * 右侧分组标题做成「通栏色带」而不是普通文字：
 * 左菜单项已经是「热销/主食/小食」，若右栏标题也是白底纯文字，
 * 两者吸顶后并排同高会被误读成重复渲染。用浅灰底 + 小字号 + 品牌竖条，
 * 让右侧读起来是「分组分隔条」，左侧读起来是「导航」。
 */
.menu__section-title {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: var(--fs-aux);
  font-weight: var(--fw-title);
  color: var(--t-2);
  letter-spacing: .04em;
  padding: 4px 8px;
  margin-bottom: 2px;
  border-radius: var(--r-tag);
  background: var(--bg-sunken);
  position: sticky;
  top: 0;
  z-index: 2;
}

.menu__section-title::before {
  content: '';
  width: 2px;
  height: 9px;
  border-radius: 1px;
  background: var(--brand-3);
  flex-shrink: 0;
}

/* ---------------- 购物车栏 ---------------- */
.cart-bar {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  bottom: 0;
  width: 100%;
  max-width: var(--app-max);
  height: calc(58px + env(safe-area-inset-bottom, 0px));
  padding-bottom: env(safe-area-inset-bottom, 0px);
  background: #23262C;
  display: flex;
  align-items: center;
  gap: 10px;
  padding-left: 14px;
  padding-right: 10px;
  z-index: 35;
}

.cart-bar__icon {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: var(--brand-grad);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  flex-shrink: 0;
  box-shadow: var(--sh-brand);
}

.cart-bar__icon--empty {
  background: #3A3F47;
  box-shadow: none;
}

.cart-bar__badge {
  position: absolute;
  top: -3px;
  right: -4px;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  border-radius: 9px;
  background: var(--c-error);
  color: #fff;
  font-size: 10px;
  line-height: 17px;
  text-align: center;
}

.cart-bar__info {
  flex: 1;
  min-width: 0;
  color: #fff;
}

.cart-bar__info .text-aux {
  color: rgba(255, 255, 255, .6);
}

.cart-bar__btn {
  height: 40px;
  min-width: 100px;
  flex-shrink: 0;
}

/* ---------------- 购物车行 ---------------- */
.cart-line {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
}

.cart-line + .cart-line {
  border-top: 1px solid var(--line);
}
</style>
