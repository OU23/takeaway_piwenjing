<template>
  <div class="page page--notab">
    <NavBar title="订单详情" />

    <div v-if="!ready" style="padding: 12px">
      <div v-for="i in 3" :key="i" class="skeleton mt-12" style="height: 100px; border-radius: 10px" />
    </div>

    <template v-else>
      <div class="page-body">
        <!-- 状态卡 -->
        <div class="status-card" :class="'status-card--' + statusKind">
          <div class="row row--between">
            <div>
              <div class="status-card__title">{{ order.statusLabel }}</div>
              <div class="status-card__sub">{{ statusSub }}</div>
            </div>
            <Icon :name="statusIcon" :size="34" color="rgba(255,255,255,.85)" />
          </div>
          <div class="status-card__promise">
            <Icon name="clock" :size="13" color="rgba(255,255,255,.9)" />
            <span>{{ order.promiseText || '—' }}</span>
            <span class="status-card__type">
              {{ order.fulfillType === 'PICKUP' ? '到店自取' : '平台配送' }}
            </span>
          </div>
        </div>

        <!-- 评价入口 / 我的评价 -->
        <div v-if="order.status === 'COMPLETED'" class="card mt-12">
          <div class="card__title">
            <span>评价</span>
            <small v-if="orderReview">已评价</small>
          </div>
          <template v-if="orderReview">
            <div class="review-stars">
              <Icon
                v-for="i in 5"
                :key="i"
                name="star"
                :size="16"
                :color="i <= orderReview.rating ? '#FF9500' : '#DADDE2'"
              />
            </div>
            <div v-if="orderReview.content" class="text-desc mt-4">{{ orderReview.content }}</div>
            <div v-if="orderReview.images && orderReview.images.length" class="review-imgs mt-8">
              <img v-for="(u, i) in orderReview.images" :key="i" :src="u" class="review-thumb" alt="评价图" />
            </div>
            <div v-if="orderReview.reply" class="text-aux mt-8">商家回复：{{ orderReview.reply }}</div>
          </template>
          <button
            v-else
            class="btn btn--primary btn--sm btn--block"
            @click="$router.push(`/review/${orderNo}`)"
          >
            去评价
          </button>
        </div>

        <!-- 自取单：取餐码 -->
        <div v-if="isPickup && order.pickupCode" class="card mt-12 pickup-card">
          <div class="text-aux" style="text-align: center">取餐码 · 到店出示核销</div>
          <div class="pickup-card__code num">{{ order.pickupCode }}</div>
          <div class="qrcode" v-html="qrSvg" />
          <div class="text-aux" style="text-align: center; margin-top: 8px">
            支持「数字码输码核销」与「扫码核销」两种方式
          </div>
          <div class="divider" />
          <div class="row row--between">
            <span class="text-aux">门店</span>
            <span class="text-desc">{{ order.shopName }}</span>
          </div>
          <div class="row row--between mt-8">
            <span class="text-aux">门店地址</span>
            <span class="text-desc ellipsis" style="max-width: 70%">{{ order.receiverAddress }}</span>
          </div>
          <div class="row mt-12" style="gap: 10px">
            <button class="btn btn--ghost btn--sm flex-1" @click="toast('唤起高德/百度地图为 URL Scheme，本期原型不接入', 'info')">
              <Icon name="nearby" :size="14" /> 导航到店
            </button>
            <button
              v-if="order.status === 'WAIT_MEAL'"
              class="btn btn--pickup btn--sm flex-1"
              @click="onPickup"
            >
              <Icon name="check" :size="14" color="#fff" /> 我已取餐
            </button>
          </div>
        </div>

        <div v-else-if="isPickup && order.status === 'PREPARING'" class="card mt-12">
          <div class="row" style="gap: 8px">
            <Icon name="clock" :size="16" color="#FF9500" />
            <div class="flex-1">
              <div class="text-strong">商家备餐中</div>
              <div class="text-aux mt-4">备餐完成后将下发取餐码并推送通知（SRS 8.6）</div>
            </div>
          </div>
        </div>

        <!-- 配送单：骑手卡 + 地图 -->
        <template v-if="!isPickup && showRider">
          <div class="card mt-12">
            <div class="row" style="gap: 10px">
              <div class="rider-avatar">
                <Icon name="user" :size="18" color="#fff" />
              </div>
              <div class="flex-1">
                <div class="row" style="gap: 6px">
                  <span class="text-strong">星光骑手 · 小王</span>
                  <span class="tag tag--info">准时率 98%</span>
                </div>
                <div class="text-aux mt-4">
                  <Icon name="star" :size="11" color="#FF9500" /> 4.9 · 已完成 1286 单
                </div>
              </div>
              <button class="btn btn--weak btn--xs" @click="toast('电话联系骑手需虚拟号通道（SRS 6.2.3【替换】），原型仅演示 IM 入口', 'info')">
                <Icon name="phone" :size="13" /> 联系骑手
              </button>
            </div>
          </div>

          <div class="card mt-12">
            <div class="card__title">
              <span>配送地图</span>
              <small>地图为个人 Key 能力（SRS 1.5【保留】）</small>
            </div>
            <AmapView height="150px" :markers="mapMarkers" :center="mapCenter" />
            <div class="row mt-12" style="gap: 8px">
              <button class="btn btn--ghost btn--sm flex-1" @click="onUrge">催单</button>
              <button class="btn btn--weak btn--sm flex-1" @click="toast('实时轨迹需接入高德/百度个人 Key（SRS 1.5【保留】）', 'info')">
                查看轨迹
              </button>
            </div>
          </div>
        </template>

        <!-- 状态时间轴 -->
        <div class="card mt-12">
          <div class="card__title">
            <span>订单进度</span>
            <small>{{ timelineDone }} / {{ order.timeline?.length || 0 }}</small>
          </div>
          <div class="timeline">
            <div
              v-for="(t, i) in order.timeline"
              :key="i"
              class="timeline__item"
              :class="{
                'timeline__item--done': i < (order.timeline.length - 1),
                'timeline__item--current': i === order.timeline.length - 1
              }"
            >
              <div class="timeline__title" :class="{ 'timeline__title--muted': i < order.timeline.length - 1 }">
                {{ t.toLabel }}
                <span class="text-aux" style="font-weight: 400">· {{ t.operatorLabel }}</span>
              </div>
              <div class="timeline__meta">
                {{ fmtTime(t.createdAt) }}
                <template v-if="t.remark"> · {{ t.remark }}</template>
              </div>
            </div>
          </div>
        </div>

        <!-- 商品明细 -->
        <div class="card mt-12">
          <div class="card__title">
            <span class="row" style="gap: 6px">
              <PhImage style="width: 22px; height: 22px" :image="order.shopHeaderImg" :label="''" />
              {{ order.shopName }}
            </span>
            <small>{{ order.fulfillType === 'PICKUP' ? '到店自取' : '平台配送' }}</small>
          </div>
          <div v-for="(it, i) in order.items" :key="i" class="row row--between" style="padding: 6px 0">
            <div class="flex-1" style="min-width: 0">
              <div class="ellipsis">{{ it.dishName }}</div>
              <div v-if="it.specText" class="text-aux ellipsis">{{ it.specText }}</div>
            </div>
            <div class="text-aux num" style="width: 44px; text-align: right">×{{ it.quantity }}</div>
            <div class="num" style="width: 62px; text-align: right">¥{{ yuan(it.amount) }}</div>
          </div>

          <div class="divider" />
          <div class="fee-row">
            <span>商品小计</span>
            <span class="fee-row__value num">¥{{ yuan(order.itemAmount) }}</span>
          </div>
          <div v-if="order.packFee" class="fee-row">
            <span>打包费</span>
            <span class="fee-row__value num">¥{{ yuan(order.packFee) }}</span>
          </div>
          <div class="fee-row">
            <span>配送费{{ isPickup ? '（自取免配送费）' : '' }}</span>
            <span class="fee-row__value num">¥{{ yuan(order.deliveryFee) }}</span>
          </div>
          <div v-if="order.discountAmount" class="fee-row fee-row--discount">
            <span>优惠合计</span>
            <span class="fee-row__value num">-¥{{ yuan(order.discountAmount) }}</span>
          </div>
          <div class="fee-row fee-row--total">
            <span class="text-strong">实付</span>
            <span class="fee-row__value num">¥{{ yuan(order.payAmount) }}</span>
          </div>
        </div>

        <!-- 订单信息 -->
        <div class="card mt-12">
          <div class="card__title"><span>订单信息</span></div>
          <div class="list-row">
            <span class="list-row__label text-aux">订单号</span>
            <span class="list-row__value num">{{ order.orderNo }}</span>
          </div>
          <div class="list-row">
            <span class="list-row__label text-aux">下单时间</span>
            <span class="list-row__value num">{{ fmtTime(order.createdAt) }}</span>
          </div>
          <div class="list-row">
            <span class="list-row__label text-aux">支付时间</span>
            <span class="list-row__value num">{{ order.paidAt ? fmtTime(order.paidAt) : '未支付' }}</span>
          </div>
          <div class="list-row">
            <span class="list-row__label text-aux">
              {{ isPickup ? '自取门店' : '收货地址' }}
            </span>
            <span class="list-row__value" style="max-width: 66%; text-align: right">
              {{ order.receiverAddress }}
            </span>
          </div>
          <div v-if="!isPickup" class="list-row">
            <span class="list-row__label text-aux">收货人</span>
            <span class="list-row__value num">{{ order.receiverName }} {{ order.receiverPhone }}</span>
          </div>
          <div class="list-row">
            <span class="list-row__label text-aux">备注</span>
            <span class="list-row__value" style="max-width: 62%; text-align: right">
              {{ order.remark || '无' }}
            </span>
          </div>
          <div v-if="order.cancelReason" class="list-row">
            <span class="list-row__label text-aux">取消原因</span>
            <span class="list-row__value">{{ order.cancelReason }}</span>
          </div>
        </div>

        <!-- M14 修复：移除用户端「状态模拟推进」调试入口（演示推进接口已要求管理权限） -->

        <div class="text-aux mt-16" style="text-align: center; line-height: 1.6">
          订单状态机与状态流水与后端强一致（SRS 8.9）
        </div>
      </div>

      <!-- 底部操作 -->
      <div v-if="order.actions?.length" class="action-bar">
        <button
          v-for="a in order.actions"
          :key="a"
          class="btn flex-1"
          :class="a === 'PAY' || a === 'CONFIRM' ? 'btn--primary' : 'btn--ghost'"
          @click="onAction(a)"
        >
          {{ ACTION_LABEL[a] || a }}
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import Icon from '../components/Icon.vue'
import PhImage from '../components/PhImage.vue'
import AmapView from '../components/AmapView.vue'
import { cancelOrder, confirmOrder, getOrderDetail, getOrderReview, pickupOrder, urgeOrder } from '../api'
import { toast } from '../api/http'
import { confirmDialog } from '../utils/confirm'
import { createVisiblePoller } from '../utils/ui'
import { fmtTime, yuan } from '../utils/format'
import { useAppStore } from '../store/app'

const route = useRoute()
const router = useRouter()
const app = useAppStore()

const orderNo = route.params.orderNo
const order = ref({})
const ready = ref(false)
const orderReview = ref(null)

/* 配送地图打点：门店 + 收货地址；「我」由 AmapView 内部用浏览器定位 */
const mapMarkers = computed(() => {
  const o = order.value
  const list = []
  if (o.shopLat != null && o.shopLng != null) {
    list.push({ id: 'shop', lng: o.shopLng, lat: o.shopLat, title: o.shopName })
  }
  if (o.receiverLat != null && o.receiverLng != null) {
    list.push({ id: 'receiver', lng: o.receiverLng, lat: o.receiverLat, title: '收货地址' })
  }
  return list
})
const mapCenter = computed(() => {
  const o = order.value
  if (o.shopLat != null && o.shopLng != null) return [Number(o.shopLng), Number(o.shopLat)]
  if (o.receiverLat != null && o.receiverLng != null) return [Number(o.receiverLng), Number(o.receiverLat)]
  return null
})
/** 上一次渲染的数据指纹，用于跳过无变化的轮询结果，避免整页重渲染 */
let lastSig = ''

const ACTION_LABEL = {
  PAY: '去支付',
  CANCEL: '取消订单',
  URGE: '催单',
  CONFIRM: '确认收货',
  REBUY: '再来一单',
  MODIFY_ADDRESS: '修改地址'
}

const isPickup = computed(() => order.value.fulfillType === 'PICKUP')
const timelineDone = computed(() => order.value.timeline?.length || 0)

const showRider = computed(() =>
  ['WAIT_PICKUP', 'DELIVERING', 'DELIVERED'].includes(order.value.status)
)

const statusKind = computed(() => {
  const s = order.value.status
  if (['COMPLETED', 'PICKED'].includes(s)) return 'done'
  if (['CANCELED', 'REFUNDED', 'PART_REFUNDED', 'EXCEPTION'].includes(s)) return 'off'
  if (s === 'UNPAID') return 'warn'
  return 'active'
})

const statusIcon = computed(() => {
  const s = order.value.status
  if (['COMPLETED', 'PICKED'].includes(s)) return 'check'
  if (['CANCELED', 'REFUNDED', 'PART_REFUNDED'].includes(s)) return 'close'
  if (s === 'UNPAID') return 'clock'
  return isPickup.value ? 'store' : 'truck'
})

const statusSub = computed(() => {
  const s = order.value.status
  const map = {
    UNPAID: '请在 15 分钟内完成支付',
    WAIT_ACCEPT: '已通知商家，等待接单',
    PREPARING: isPickup.value ? '商家正在备餐，请稍候' : '商家正在出餐，请稍候',
    WAIT_PICKUP: '出餐完成，等待骑手取货',
    DELIVERING: '骑手正在配送，请保持电话畅通',
    DELIVERED: '订单已送达，请确认收货',
    WAIT_MEAL: '备餐完成，凭取餐码到店取餐',
    PICKED: '已取餐，欢迎评价',
    COMPLETED: '订单已完成，感谢使用',
    CANCELED: order.value.cancelReason || '订单已取消',
    REFUNDED: '退款已完成',
    PART_REFUNDED: '部分退款已完成',
    EXCEPTION: '订单异常，客服正在处理'
  }
  return map[s] || ''
})

/** 用取餐码生成一个稳定的假二维码网格（占位，非真实二维码） */
const qrSvg = computed(() => {
  const code = order.value.pickupCode || order.value.orderNo || 'SG'
  const N = 21
  let seed = 0
  for (const ch of code) seed = (seed * 31 + ch.charCodeAt(0)) % 100000
  const rand = (i) => {
    seed = (seed * 1103515245 + 12345 + i) % 2147483648
    return seed / 2147483648
  }
  const cells = []
  const finder = (x, y) =>
    (x < 7 && y < 7) || (x >= N - 7 && y < 7) || (x < 7 && y >= N - 7)
  for (let y = 0; y < N; y++) {
    for (let x = 0; x < N; x++) {
      let on
      if (finder(x, y)) {
        const lx = x < 7 ? x : x - (N - 7)
        const ly = y < 7 ? y : 0
        on = lx === 0 || lx === 6 || ly === 0 || ly === 6 || (lx >= 2 && lx <= 4 && ly >= 2 && ly <= 4)
      } else {
        on = rand(x * 31 + y) > 0.52
      }
      if (on) cells.push(`<rect x="${x}" y="${y}" width="1" height="1" fill="#17181A"/>`)
    }
  }
  return `<svg viewBox="0 0 ${N} ${N}" width="132" height="132" shape-rendering="crispEdges">${cells.join('')}</svg>`
})

/**
 * 配送追踪轮询：SRS 5.4.4 要求「进入订单详情自动开启 10s 轮询，
 * 页面不可见时暂停」。createVisiblePoller 同时提供失败退避，
 * 弱网下不会每 10 秒硬打一次请求。
 */
const poller = createVisiblePoller(load, { interval: 10000, maxInterval: 60000 })

onMounted(async () => {
  try {
    await load()
  } catch (e) {
    /* 首次加载失败由拦截器提示，轮询仍继续重试 */
  }
  try {
    const rr = await getOrderReview(orderNo)
    orderReview.value = rr.data || null
  } catch (e) {
    /* 未评价 / 未登录等都按无评价处理 */
  }
  poller.start()
})

onUnmounted(() => {
  poller.stop()
})

async function load() {
  try {
    const r = await getOrderDetail(orderNo)
    const d = r.data || {}
    // 状态、时间轴、支付信息都没变就不触发渲染（10s 一次的全量 diff 省下来）
    const sig = [
      d.orderNo, d.status, d.statusLabel, d.timeline?.length, d.paidAt, d.pickupCode, d.promiseText
    ].join('|')
    if (sig !== lastSig) {
      lastSig = sig
      order.value = d
    }
  } finally {
    ready.value = true
  }
}

async function onAction(a) {
  try {
    if (a === 'PAY') {
      router.push(`/pay/${orderNo}`)
      return
    }
    if (a === 'REBUY') {
      router.push(`/shop/${order.value.shopId}`)
      return
    }
    if (a === 'CANCEL') {
      const ok = await confirmDialog({
        title: '取消订单',
        message: `确认取消订单 ${orderNo}？取消后无法恢复。`,
        confirmText: '确认取消',
        cancelText: '再想想',
        danger: true
      })
      if (!ok) return
      await cancelOrder(orderNo, '用户主动取消')
      toast('订单已取消', 'success')
    } else if (a === 'URGE') {
      await urgeOrder(orderNo)
      toast('已催单', 'success')
    } else if (a === 'CONFIRM') {
      await confirmOrder(orderNo)
      toast('已确认收货', 'success')
    } else if (a === 'MODIFY_ADDRESS') {
      toast('修改地址为 P1 项，本期原型聚焦 P0 下单链路', 'info')
      return
    }
    await load()
    await app.refreshCartCount()
  } catch (e) {
    /* 拦截器已提示 */
  }
}

async function onPickup() {
  try {
    await pickupOrder(orderNo)
    toast('取餐码核销成功', 'success')
    await load()
  } catch (e) {
    /* ignore */
  }
}

async function onUrge() {
  try {
    await urgeOrder(orderNo)
    toast('已催单，商家与骑手将收到提醒', 'success')
  } catch (e) {
    /* ignore */
  }
}
</script>

<style scoped>
.status-card {
  border-radius: var(--r-card);
  padding: 14px;
  color: #fff;
  background: linear-gradient(135deg, #FFB03A, #F5481F);
  box-shadow: var(--sh-brand);
}

.status-card--active {
  background: linear-gradient(135deg, #FF9A3D, #F5481F);
}

.status-card--done {
  background: linear-gradient(135deg, #2FD0AE, #0F8A78);
  box-shadow: 0 10px 26px rgba(18, 191, 160, .28);
}

.status-card--warn {
  background: linear-gradient(135deg, #FFC152, #FF8A00);
}

.status-card--off {
  background: linear-gradient(135deg, #9CA0A8, #5C6169);
  box-shadow: none;
}

.status-card__title {
  font-size: 19px;
  font-weight: var(--fw-title);
}

.status-card__sub {
  font-size: var(--fs-aux);
  opacity: .92;
  margin-top: 4px;
}

.status-card__promise {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, .22);
  font-size: var(--fs-aux);
}

.status-card__type {
  margin-left: auto;
  background: rgba(255, 255, 255, .22);
  border-radius: var(--r-tag);
  padding: 2px 6px;
}

/* ---------------- 取餐码 ---------------- */
.pickup-card {
  text-align: center;
}

.pickup-card__code {
  font-size: 38px;
  font-weight: var(--fw-title);
  letter-spacing: 2px;
  color: #0F8A78;
  margin: 6px 0 10px;
}

.qrcode {
  display: flex;
  justify-content: center;
  padding: 8px;
  background: #fff;
  border-radius: var(--r-img);
}

/* ---------------- 骑手 / 地图 ---------------- */
.rider-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2A3040, #171B26);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.map-ph {
  position: relative;
  height: 140px;
  border-radius: var(--r-img);
  background:
    repeating-linear-gradient(0deg, rgba(58, 107, 240, .07) 0 1px, transparent 1px 22px),
    repeating-linear-gradient(90deg, rgba(58, 107, 240, .07) 0 1px, transparent 1px 22px),
    linear-gradient(135deg, #EAF0FE, #F4F5F7);
  overflow: hidden;
}

.map-ph__route {
  position: absolute;
  left: 14%;
  top: 68%;
  width: 72%;
  height: 2px;
  background: linear-gradient(90deg, #F5481F, #12BFA0);
  transform: rotate(-14deg);
  transform-origin: left center;
}

.map-ph__dot {
  position: absolute;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  color: #fff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.map-ph__dot--shop {
  left: 12%;
  top: 66%;
  background: var(--brand-grad);
}

.map-ph__dot--rider {
  left: 47%;
  top: 46%;
  background: #23262C;
}

.map-ph__dot--me {
  left: 82%;
  top: 26%;
  background: #3A6BF0;
}

/* ---------------- 调试卡 ---------------- */
.dev-card {
  border: 1px dashed #B7C8FA;
  background: #F7FAFF;
}

/* ---------------- 评价 ---------------- */
.review-stars {
  display: flex;
  gap: 3px;
  margin: 6px 0;
}

.review-imgs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.review-thumb {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #ebedf0;
  background: #f7f8fa;
}
</style>
