<template>
  <div>
    <div class="page-head">
      <div><h2>骑手调度</h2><div class="sub">配送分配 · 轨迹追踪 · 时效统计（本期为只读概览，分配动作后续迭代）</div></div>
    </div>

    <div v-if="error" class="notice">{{ error }}　<button class="btn btn--sm" @click="load">重新加载</button></div>
    <div v-if="!ready && !error" class="spin">加载中…</div>
    <template v-if="ready">
      <div class="notice">
        本期为「高价值优先」只读版本：展示待分配配送单与骑手运力池，便于运营掌握调度大盘；实际一键派单 / 改派将在后续迭代开放。
      </div>

      <div class="card">
        <div class="card__title">
          <span>待分配配送单</span>
          <small>共 {{ pendingCount }} 单（商家已出餐 / 待取货，尚未指派骑手）</small>
        </div>
        <table class="tbl">
          <thead><tr><th>订单号</th><th>门店</th><th>收货地址</th><th>状态</th><th>下单时间</th></tr></thead>
          <tbody>
            <tr v-for="o in pendingOrders" :key="o.orderNo">
              <td class="num">{{ o.orderNo }}</td>
              <td class="ellipsis" style="max-width:140px">{{ o.shopName }}</td>
              <td class="ellipsis" style="max-width:220px">{{ o.receiverAddress }}</td>
              <td><span class="tag tag--warn">{{ o.statusLabel }}</span></td>
              <td class="num muted">{{ fmt(o.createdAt) }}</td>
            </tr>
            <tr v-if="!pendingOrders.length"><td colspan="5" class="empty">暂无待分配订单 🎉</td></tr>
          </tbody>
        </table>
      </div>

      <div class="card mt-16">
        <div class="card__title"><span>骑手运力池</span><small>在线状态 · 今日完成 · 历史单量</small></div>
        <div class="rider-grid">
          <div v-for="r in riders" :key="r.rider.id" class="rider">
            <div class="row" style="gap:8px">
              <span class="dot" :class="r.rider.status === 'ONLINE' ? 'dot--on' : 'dot--off'" />
              <span class="text-strong">{{ r.rider.name }}</span>
              <span class="tag" :class="r.rider.status === 'ONLINE' ? 'tag--success' : 'tag--info'">{{ r.rider.status === 'ONLINE' ? '接单中' : '下班' }}</span>
            </div>
            <div class="rider__meta">
              <div><span class="muted">评分</span> {{ r.rider.rating }}</div>
              <div><span class="muted">今日完成</span> <b>{{ r.todayDeliveries }}</b></div>
              <div><span class="muted">累计单量</span> {{ r.rider.completedCount }}</div>
            </div>
          </div>
          <div v-if="!riders.length" class="empty">暂无骑手</div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../api/admin'

const pendingOrders = ref([])
const riders = ref([])
const pendingCount = ref(0)
const ready = ref(false)
const error = ref('')

function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '-' }

async function load() {
  error.value = ''
  try {
    const r = await adminApi.dispatchOverview()
    const d = r.data || {}
    pendingOrders.value = d.pendingOrders || []
    riders.value = d.riders || []
    pendingCount.value = d.pendingCount || 0
    ready.value = true
  } catch (e) {
    error.value = `调度数据加载失败：${e.message}`
  }
}
onMounted(load)
</script>

<style scoped>
.rider-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
.rider { border: 1px solid var(--line); border-radius: var(--radius-sm); padding: 12px 14px; }
.rider__meta { display: flex; gap: 16px; margin-top: 10px; font-size: 13px; }
.rider__meta .muted { font-size: 12px; }
</style>
