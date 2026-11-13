<template>
  <van-tabs v-model:active="activeIndex" sticky swipeable @change="onChange">
    <van-tab v-for="t in tabs" :key="t.key" :title="t.label" />
  </van-tabs>

  <div class="list-wrap">
    <van-card
      v-for="o in orders"
      :key="o.orderNo"
      :title="o.orderNo"
      :desc="o.fulfillTypeLabel + ' ｜ 实付 ¥' + yuan(o.payAmount)"
      :tag="o.statusLabel"
      :tag-type="statusTagType(o.status)"
    >
      <template #tags>
        <div v-for="it in o.items" :key="it.dishId" style="font-size:13px;color:#646566;padding:2px 0;">
          {{ it.dishName }}<span v-if="it.specText" class="muted">（{{ it.specText }}）</span> ×{{ it.quantity }}
        </div>
      </template>
      <template #bottom>
        <div v-if="o.receiverName" class="muted">收货：{{ o.receiverName }} {{ o.receiverPhone }}</div>
      </template>
      <template #footer v-if="actionOf(o)">
        <van-button size="small" type="primary" round @click="doAction(o)">{{ actionOf(o).label }}</van-button>
      </template>
    </van-card>
    <van-empty v-if="orders.length === 0" description="该分类暂无订单" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showToast } from 'vant'
import { merchantApi, yuan } from '../api/merchant'

const tabs = [
  { key: 'WAIT_ACCEPT', label: '待接单', group: '待接单' },
  { key: 'PROCESSING', label: '进行中', group: '进行中' },
  { key: '待评价', label: '待评价', group: '待评价' },
  { key: '退款售后', label: '退款售后', group: '退款售后' },
  { key: 'ALL', label: '全部', group: 'ALL' }
]
const activeIndex = ref(0)
const orders = ref([])

function statusTagType(s) {
  if (s === 'WAIT_ACCEPT') return 'warning'
  if (['PREPARING', 'WAIT_PICKUP', 'WAIT_MEAL', 'DELIVERING', 'DELIVERED'].includes(s)) return 'primary'
  if (['COMPLETED', 'PICKED'].includes(s)) return 'success'
  if (['CANCELED', 'REFUNDED', 'EXCEPTION', 'PART_REFUNDED'].includes(s)) return 'danger'
  return 'default'
}

function actionOf(o) {
  if (o.status === 'WAIT_ACCEPT') return { label: '接单', fn: merchantApi.acceptOrder }
  if (o.status === 'PREPARING') return { label: '出餐', fn: merchantApi.prepareOrder }
  if (o.status === 'WAIT_PICKUP' || o.status === 'WAIT_MEAL') return { label: '完成', fn: merchantApi.completeOrder }
  return null
}

async function load() {
  const t = tabs[activeIndex.value]
  const data = await merchantApi.orders({ group: t.group, pageSize: 50 })
  orders.value = data.list || []
}

function onChange() { load() }

async function doAction(o) {
  const a = actionOf(o)
  if (!a) return
  try {
    await a.fn(o.orderNo)
    showToast(a.label + '成功')
    load()
  } catch (e) {
    showToast(e.message)
  }
}

onMounted(load)
</script>
