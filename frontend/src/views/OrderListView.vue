<template>
  <div class="page">
    <PullRefresh :on-refresh="onPullRefresh">
    <NavBar title="订单" :back="false" :brand="true" />

    <!-- 状态分组 -->
    <div class="tabs">
      <button
        v-for="g in groups"
        :key="g"
        class="tabs__item"
        :class="{ 'tabs__item--active': group === g }"
        @click="switchGroup(g)"
      >
        {{ g }}
        <i v-if="counts[g]" class="tabs__badge num">{{ counts[g] }}</i>
      </button>
    </div>

    <div class="page-body">
      <!-- 进行中订单悬浮卡片 -->
      <div v-if="processing" class="processing" @click="goDetail(processing.orderNo)">
        <div class="row row--between">
          <span class="text-strong row" style="gap: 5px">
            <Icon :name="processing.fulfillType === 'PICKUP' ? 'store' : 'truck'" :size="14" color="#fff" />
            {{ processing.statusLabel }}
          </span>
          <span class="text-aux" style="color: rgba(255,255,255,.85)">
            {{ processing.promiseText }}
            <Icon name="chevron" :size="12" color="rgba(255,255,255,.85)" />
          </span>
        </div>
        <div class="processing__body">
          <PhImage style="width: 40px; height: 40px" :image="processing.shopHeaderImg" :label="processing.shopName.slice(0, 2)" />
          <div class="flex-1" style="min-width: 0">
            <div class="ellipsis" style="color: #fff">{{ processing.shopName }}</div>
            <div class="text-aux" style="color: rgba(255,255,255,.7)">
              {{ processing.items?.map(i => i.dishName).join('、') }}
            </div>
          </div>
          <span v-if="processing.pickupCode" class="pickup-code num">{{ processing.pickupCode }}</span>
        </div>
      </div>

      <div v-if="!ready">
        <div v-for="i in 3" :key="i" class="skeleton mt-12" style="height: 120px; border-radius: 10px" />
      </div>

      <template v-else>
        <div :class="{ 'is-switching': switching }">
        <div v-for="o in list" :key="o.orderNo" class="order-card">
          <div class="row row--between" @click="goDetail(o.orderNo)">
            <span class="text-strong row ellipsis" style="gap: 6px; max-width: 68%">
              <PhImage style="width: 20px; height: 20px" :image="o.shopHeaderImg" :label="''" />
              {{ o.shopName }}
            </span>
            <span class="tag" :class="statusTagClass(o.status)">{{ o.statusLabel }}</span>
          </div>

          <div class="order-card__body" @click="goDetail(o.orderNo)">
            <div class="order-card__thumbs">
              <PhImage
                v-for="(it, i) in (o.items || []).slice(0, 3)"
                :key="i"
                style="width: 52px; height: 52px"
                :image="it.dishImage"
                :label="it.dishName.slice(0, 2)"
              />
            </div>
            <div class="order-card__sum">
              <div class="text-aux">{{ (o.items || []).map(i => i.dishName).join('、') }}</div>
              <div class="text-aux mt-4">
                共 {{ o.totalQuantity }} 件
                <span class="tag tag--plain" style="margin-left: 4px">
                  {{ o.fulfillType === 'PICKUP' ? '到店自取' : '平台配送' }}
                </span>
              </div>
              <div class="mt-4">
                <span class="text-aux" style="margin-right: 4px">实付</span>
                <span class="price price--sm"><span class="price__sym">¥</span>{{ yuan(o.payAmount) }}</span>
              </div>
            </div>
          </div>

          <div v-if="o.pickupCode" class="order-card__code">
            <Icon name="qrcode" :size="14" color="#0F8A78" />
            取餐码 <b class="num">{{ o.pickupCode }}</b>
          </div>

          <div class="order-card__foot">
            <span class="text-aux">{{ fromNow(o.createdAt) }}</span>
            <div class="row" style="gap: 8px">
              <button
                v-for="a in o.actions"
                :key="a"
                class="btn btn--xs"
                :class="actionClass(a)"
                @click.stop="onAction(a, o)"
              >
                {{ ACTION_LABEL[a] || a }}
              </button>
            </div>
          </div>
        </div>
        </div>

        <EmptyState
          v-if="!list.length"
          icon="receipt"
          title="暂无相关订单"
          desc="下单后可在这里查看订单状态与配送进度"
          action="去逛逛"
          @action="$router.push('/')"
        />
      </template>
    </div>
    </PullRefresh>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import Icon from '../components/Icon.vue'
import PhImage from '../components/PhImage.vue'
import EmptyState from '../components/EmptyState.vue'
import PullRefresh from '../components/PullRefresh.vue'
import { cancelOrder, confirmOrder, getOrderGroupCount, listOrders, urgeOrder } from '../api'
import { toast } from '../api/http'
import { confirmDialog } from '../utils/confirm'
import { fromNow, statusTagClass, yuan } from '../utils/format'
import { useAppStore } from '../store/app'

const router = useRouter()
const app = useAppStore()

const groups = ['全部', '待付款', '待接单', '进行中', '待评价', '退款售后']
const group = ref('全部')
const list = ref([])
const counts = ref({})
const ready = ref(false)
const switching = ref(false)

const ACTION_LABEL = {
  PAY: '去支付',
  CANCEL: '取消订单',
  URGE: '催单',
  CONFIRM: '确认收货',
  REBUY: '再来一单',
  MODIFY_ADDRESS: '修改地址'
}

/** 进行中订单悬浮卡片（取最近一单） */
const processing = computed(() => list.value.find(o => o.statusGroup === '进行中' && group.value !== '进行中'))

onMounted(load)

async function load() {
  try {
    const [l, c] = await Promise.all([
      listOrders({ group: group.value, pageNum: 1, pageSize: 50 }),
      getOrderGroupCount()
    ])
    list.value = l.data?.list || []
    counts.value = c.data || {}
  } finally {
    ready.value = true
  }
}

/** 下拉刷新：保留当前列表静默更新，不闪骨架 */
async function onPullRefresh() {
  try {
    const [l, c] = await Promise.all([
      listOrders({ group: group.value, pageNum: 1, pageSize: 50 }),
      getOrderGroupCount()
    ])
    list.value = l.data?.list || []
    counts.value = c.data || {}
  } catch (e) {
    /* 拦截器已提示 */
  }
}

async function switchGroup(g) {
  if (group.value === g) return
  group.value = g
  switching.value = true
  try {
    await load()
  } finally {
    switching.value = false
  }
}

function goDetail(orderNo) {
  router.push(`/order/${orderNo}`)
}

function actionClass(a) {
  if (a === 'PAY' || a === 'CONFIRM') return 'btn--primary'
  return 'btn--ghost'
}

async function onAction(action, o) {
  try {
    if (action === 'PAY') {
      router.push(`/pay/${o.orderNo}`)
      return
    }
    if (action === 'REBUY') {
      router.push(`/shop/${o.shopId}`)
      return
    }
    if (action === 'CANCEL') {
      const ok = await confirmDialog({
        title: '取消订单',
        message: `确认取消订单 ${o.orderNo}？取消后无法恢复。`,
        confirmText: '确认取消',
        cancelText: '再想想',
        danger: true
      })
      if (!ok) return
      await cancelOrder(o.orderNo, '用户主动取消')
      toast('订单已取消', 'success')
      await load()
      return
    }
    if (action === 'URGE') {
      await urgeOrder(o.orderNo)
      toast('已催单，商家与骑手将收到提醒', 'success')
      return
    }
    if (action === 'CONFIRM') {
      await confirmOrder(o.orderNo)
      toast('已确认收货，感谢使用', 'success')
      await load()
      return
    }
    if (action === 'MODIFY_ADDRESS') {
      toast('修改地址为 P1 项，本期原型聚焦 P0 下单链路', 'info')
    }
  } catch (e) {
    /* 拦截器已提示 */
  }
}
</script>

<style scoped>
.tabs {
  display: flex;
  gap: 2px;
  overflow-x: auto;
  background: var(--bg-card);
  padding: 8px var(--sp-page);
  border-bottom: 1px solid var(--line);
  position: sticky;
  /* 导航条高度已含状态栏安全区，这里同步偏移，避免吸顶时与导航条重叠 */
  top: calc(var(--navbar-h) + env(safe-area-inset-top, 0px));
  z-index: 15;
}

.tabs__item {
  flex-shrink: 0;
  height: 28px;
  /* 6 个分组在 390px 下需刚好排满一屏，左右留白压到 7px */
  padding: 0 7px;
  border-radius: var(--r-btn);
  font-size: var(--fs-body);
  color: var(--t-2);
  /* 角标改为流内元素，避免绝对定位压住分组文字 */
  display: inline-flex;
  align-items: center;
  gap: 3px;
  transition: background-color .15s ease;
}

.tabs__item--active {
  background: #FFF1E6;
  color: var(--brand-3);
  font-weight: var(--fw-title);
}

.tabs__badge {
  min-width: 14px;
  height: 14px;
  padding: 0 4px;
  border-radius: 7px;
  background: var(--c-error);
  color: #fff;
  font-size: 9px;
  line-height: 14px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.page-body {
  padding: 12px;
}

/* ---------------- 进行中悬浮卡 ---------------- */
.processing {
  background: linear-gradient(135deg, #FF9A3D, #F5481F);
  border-radius: var(--r-card);
  padding: 12px;
  color: #fff;
  box-shadow: var(--sh-brand);
  margin-bottom: 12px;
}

.processing__body {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, .22);
}

.pickup-code {
  background: rgba(255, 255, 255, .2);
  border-radius: var(--r-tag);
  padding: 3px 7px;
  font-size: var(--fs-body);
  font-weight: var(--fw-title);
}

/* ---------------- 订单卡 ---------------- */
.order-card {
  background: var(--bg-card);
  border-radius: var(--r-card);
  padding: 12px;
  box-shadow: var(--sh-light);
  transition: background-color .12s ease;
}

/* 卡片整体可点，给一个极轻的按压反馈 */
.order-card:active {
  background: #FAFBFC;
}

.is-switching {
  opacity: .5;
  transition: opacity .15s ease;
}

.order-card + .order-card {
  margin-top: 10px;
}

.order-card__body {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.order-card__thumbs {
  display: flex;
  gap: 5px;
  flex-shrink: 0;
}

.order-card__sum {
  flex: 1;
  min-width: 0;
  text-align: right;
}

.order-card__code {
  display: flex;
  align-items: center;
  gap: 5px;
  background: #E6FAF6;
  color: #0F8A78;
  border-radius: var(--r-img);
  padding: 7px 9px;
  font-size: var(--fs-aux);
  margin-top: 10px;
}

.order-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--line);
}
</style>
