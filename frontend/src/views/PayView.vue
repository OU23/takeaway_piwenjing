<template>
  <div class="page page--notab">
    <NavBar title="收银台" />

    <div v-if="!ready" style="padding: 12px">
      <div class="skeleton" style="height: 130px; border-radius: 10px" />
    </div>

    <template v-else-if="paid">
      <div class="paid-wrap">
        <div class="paid-icon">
          <Icon name="check" :size="38" color="#fff" />
        </div>
        <div class="paid-title">支付成功</div>
        <div class="price mt-8" style="font-size: 26px">
          <span class="price__sym">¥</span>{{ yuan(order.payAmount) }}
        </div>
        <div class="text-aux mt-8">
          {{ order.fulfillType === 'PICKUP' ? '商家备餐完成后将下发取餐码' : '商家接单后将尽快为您配送' }}
        </div>

        <div class="card mt-16" style="width: 100%">
          <div class="fee-row">
            <span>订单号</span>
            <span class="fee-row__value num">{{ order.orderNo }}</span>
          </div>
          <div class="fee-row">
            <span>支付渠道</span>
            <span class="fee-row__value">{{ channelLabel }}</span>
          </div>
          <div class="fee-row">
            <span>门店</span>
            <span class="fee-row__value">{{ order.shopName }}</span>
          </div>
        </div>

        <div class="row mt-16" style="gap: 10px; width: 100%">
          <button class="btn btn--ghost flex-1" @click="goHome">返回首页</button>
          <button class="btn btn--primary flex-1" @click="goOrder">查看订单</button>
        </div>

        <div class="text-aux mt-16" style="text-align: center; line-height: 1.6">
          {{ paidNotice }}
        </div>
      </div>
    </template>

    <template v-else>
      <div class="page-body">
        <!-- 待支付倒计时 -->
        <div class="countdown-bar">
          <Icon name="clock" :size="14" color="#fff" />
          <span>请在 <b class="num">{{ leftText }}</b> 内完成支付，超时订单将自动取消</span>
        </div>

        <!-- 订单摘要 -->
        <div class="card">
          <div class="row row--between">
            <span class="text-strong">{{ order.shopName }}</span>
            <span class="tag tag--promo">
              {{ order.fulfillType === 'PICKUP' ? '到店自取' : '平台配送' }}
            </span>
          </div>
          <div class="text-aux mt-4 num">订单号 {{ order.orderNo }}</div>

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
            <span>配送费{{ order.fulfillType === 'PICKUP' ? '（自取免配送费）' : '' }}</span>
            <span class="fee-row__value num">¥{{ yuan(order.deliveryFee) }}</span>
          </div>
          <div v-if="order.discountAmount" class="fee-row fee-row--discount">
            <span>优惠合计</span>
            <span class="fee-row__value num">-¥{{ yuan(order.discountAmount) }}</span>
          </div>
          <div class="fee-row fee-row--total">
            <span class="text-strong">应付</span>
            <span class="fee-row__value num">¥{{ yuan(order.payAmount) }}</span>
          </div>
        </div>

        <!-- 支付渠道 -->
        <div class="card mt-12">
          <div class="card__title">
            <span>选择支付方式</span>
            <small>SRS 1.5 三分法</small>
          </div>
          <div
            v-for="c in channels"
            :key="c.code"
            class="pay-opt"
            :class="{ 'pay-opt--active': channel === c.code, 'pay-opt--off': !c.available }"
            @click="selectChannel(c)"
          >
            <div class="pay-opt__logo" :class="'pay-opt__logo--' + c.code.toLowerCase()">
              {{ c.label.slice(0, 1) }}
            </div>
            <div class="flex-1" style="min-width: 0">
              <div class="row" style="gap: 6px">
                <span class="text-strong">{{ c.label }}</span>
                <span v-if="c.mock" class="tag tag--warn">模拟通道</span>
              </div>
              <div class="text-aux ellipsis">{{ c.remark }}</div>
            </div>
            <Icon v-if="channel === c.code" name="check" :size="16" color="#12BFA0" />
          </div>

          <div v-if="selectedChannel && selectedChannel.mock" class="mock-tip mt-8">
            <Icon name="shield" :size="13" color="#A5690A" />
            <span>
              该渠道为《需求规格说明书》1.5 的【替换】实现：接口形态与真实通道一致，
              点击支付即完成"一键支付成功"。生产环境必须关闭（SRS 9.3）。
            </span>
          </div>
        </div>

        <div class="text-aux mt-16" style="text-align: center; line-height: 1.6">
          支付回调具备幂等性，重复通知不会重复扣款（SRS 9.2）
        </div>
      </div>

      <div class="action-bar">
        <div class="flex-1">
          <span class="text-aux">应付</span>
          <span class="price" style="margin-left: 4px">
            <span class="price__sym">¥</span>{{ yuan(order.payAmount) }}
          </span>
        </div>
        <button class="btn btn--weak" @click="goOrder">稍后支付</button>
        <button class="btn btn--primary" :disabled="paying || !channel" @click="doPay">
          {{ paying ? '支付中…' : '确认支付' }}
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
import { getOrderDetail, getPayChannels, getUserProfile, prepay } from '../api'
import { toast } from '../api/http'
import { fmtDuration, yuan } from '../utils/format'
import { useAppStore } from '../store/app'

const route = useRoute()
const router = useRouter()
const app = useAppStore()

const orderNo = route.params.orderNo
const order = ref({})
const channels = ref([])
const channel = ref('')
const ready = ref(false)
const paying = ref(false)
const paid = ref(false)
const paidNotice = ref('')
const left = ref(900)

const selectedChannel = computed(() => channels.value.find(c => c.code === channel.value))
const channelLabel = computed(() => selectedChannel.value?.label || channel.value)
const leftText = computed(() => fmtDuration(left.value))

let timer = null

onMounted(async () => {
  try {
    const [o, c] = await Promise.all([getOrderDetail(orderNo), getPayChannels()])
    order.value = o.data
    if (order.value.status !== 'UNPAID') {
      paid.value = true
      paidNotice.value = `订单当前状态：${order.value.statusLabel}`
      return
    }
    channels.value = c.data || []
    const first = channels.value.find(x => x.available)
    channel.value = first ? first.code : ''
  } finally {
    ready.value = true
  }
  timer = setInterval(() => {
    // 页面不可见时不做无意义的状态更新
    if (document.hidden) return
    left.value = left.value > 0 ? left.value - 1 : 0
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

function selectChannel(c) {
  if (!c.available) {
    toast(c.remark, 'error')
    return
  }
  channel.value = c.code
}

async function doPay() {
  if (paying.value) return
  paying.value = true
  try {
    const r = await prepay({ orderNo, channel: channel.value })
    const pv = r.data
    if (pv.status === 'SUCCESS') {
      // 同步通道（余额 / 模拟通道由服务端结算）：发起支付即完成
      await afterPaid(
        pv.mock
          ? '本次走「模拟支付通道」，未产生真实资金流转（SRS 1.5【替换】）'
          : '余额支付为同步通道，已实时扣款'
      )
    } else {
      // 沙箱等异步通道：等待渠道服务器验签回调，前端不得也不需要自行确认
      toast('已提交支付，等待渠道确认后订单自动更新', 'info')
      goOrder()
    }
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    paying.value = false
  }
}

async function afterPaid(notice) {
  const o = await getOrderDetail(orderNo)
  order.value = o.data
  paid.value = true
  paidNotice.value = notice
  if (timer) clearInterval(timer)
  await Promise.allSettled([app.refreshCartCount(), app.loadUser()])
  toast('支付成功', 'success')
}

function goOrder() {
  router.replace(`/order/${orderNo}`)
}

function goHome() {
  router.replace('/')
}
</script>

<style scoped>
.paid-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 36px 20px;
}

.paid-icon {
  width: 66px;
  height: 66px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2FD0AE, #0F8A78);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 26px rgba(18, 191, 160, .3);
}

.paid-title {
  font-size: 19px;
  font-weight: var(--fw-title);
  margin-top: 14px;
}

.countdown-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #FF9500, #F5481F);
  color: #fff;
  border-radius: var(--r-card);
  padding: 9px 12px;
  font-size: var(--fs-aux);
  margin-bottom: 12px;
}

.countdown-bar b {
  font-variant-numeric: tabular-nums;
}

.pay-opt {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 8px;
  border-radius: var(--r-img);
}

.pay-opt + .pay-opt {
  border-top: 1px solid var(--line);
}

.pay-opt--active {
  background: #E6FAF6;
}

.pay-opt--off {
  opacity: .5;
}

.pay-opt__logo {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fs-title);
  font-weight: var(--fw-title);
  flex-shrink: 0;
  background: var(--brand-grad);
}

.pay-opt__logo--balance {
  background: linear-gradient(135deg, #2FD0AE, #0F8A78);
}

.mock-tip {
  display: flex;
  gap: 6px;
  align-items: flex-start;
  background: #FFF6E5;
  color: #A5690A;
  border-radius: var(--r-img);
  padding: 9px;
  font-size: var(--fs-aux);
  line-height: 1.5;
}
</style>
