<template>
  <div>
    <div class="page-head">
      <div><h2>财务结算</h2><div class="sub">支付流水与结算概览 · 数据源：支付单（只读）</div></div>
    </div>

    <div v-if="error" class="notice">{{ error }}</div>
    <div v-if="!ready && !error" class="spin">加载中…</div>
    <template v-else>
      <div class="stat-grid">
        <div class="stat">
          <div class="stat__label">成功交易额（元）</div>
          <div class="stat__value">{{ yuan(summary.successAmount) }}</div>
          <div class="stat__hint">累计 {{ summary.successCount }} 笔</div>
        </div>
        <div class="stat">
          <div class="stat__label">退款金额（元）</div>
          <div class="stat__value">{{ yuan(summary.refundAmount) }}</div>
          <div class="stat__hint">{{ summary.refundCount }} 笔已退款</div>
        </div>
        <div class="stat">
          <div class="stat__label">待支付 / 失败</div>
          <div class="stat__value">{{ summary.waitCount }} <small>/ {{ summary.failCount }}</small></div>
          <div class="stat__hint">笔待支付 / 失败</div>
        </div>
        <div class="stat">
          <div class="stat__label">总流水笔数</div>
          <div class="stat__value">{{ list.length }}</div>
          <div class="stat__hint">含全部状态</div>
        </div>
      </div>

      <div class="card mt-16">
        <div class="card__title"><span>支付渠道占比</span><small>按成功交易额</small></div>
        <EChart :option="channelOption" :height="260" />
      </div>

      <div class="card mt-16">
        <div class="card__title">
          <span>支付流水</span>
          <div class="toolbar" style="margin:0">
            <select class="select" v-model="channelFilter">
              <option value="">全部渠道</option>
              <option value="ALIPAY_SANDBOX">支付宝沙箱</option>
              <option value="WECHAT_MOCK">微信(模拟)</option>
              <option value="BALANCE">余额支付</option>
            </select>
            <select class="select" v-model="statusFilter">
              <option value="">全部状态</option>
              <option value="SUCCESS">已支付</option>
              <option value="REFUNDED">已退款</option>
              <option value="WAIT">待支付</option>
              <option value="FAIL">失败</option>
              <option value="CLOSED">已关闭</option>
            </select>
          </div>
        </div>
        <table class="tbl">
          <thead><tr><th>支付单号</th><th>订单号</th><th>渠道</th><th>金额(元)</th><th>状态</th><th>创建时间</th><th>支付时间</th></tr></thead>
          <tbody>
            <tr v-for="p in filtered" :key="p.id">
              <td class="num">{{ p.payNo }}</td>
              <td class="num">{{ p.orderNo }}</td>
              <td>{{ channelLabel(p.channel) }}</td>
              <td class="num text-price">¥{{ yuan(p.amount) }}</td>
              <td><span class="tag" :class="statusTag(p.status)">{{ statusLabel(p.status) }}</span></td>
              <td class="num muted">{{ fmt(p.createdAt) }}</td>
              <td class="num muted">{{ fmt(p.paidAt) }}</td>
            </tr>
            <tr v-if="!filtered.length"><td colspan="7" class="empty">暂无流水</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import EChart from '../components/EChart.vue'
import { adminApi } from '../api/admin'

const list = ref([])
const ready = ref(false)
const error = ref('')
const channelFilter = ref('')
const statusFilter = ref('')

const summary = computed(() => {
  let successAmount = 0, successCount = 0, refundAmount = 0, refundCount = 0, waitCount = 0, failCount = 0
  for (const p of list.value) {
    if (p.status === 'SUCCESS') { successAmount += p.amount || 0; successCount++ }
    else if (p.status === 'REFUNDED') { refundAmount += p.amount || 0; refundCount++ }
    else if (p.status === 'WAIT') waitCount++
    else if (p.status === 'FAIL') failCount++
  }
  return { successAmount, successCount, refundAmount, refundCount, waitCount, failCount }
})

const filtered = computed(() => list.value.filter(p =>
  (!channelFilter.value || p.channel === channelFilter.value) &&
  (!statusFilter.value || p.status === statusFilter.value)
))

const channelOption = computed(() => {
  const map = {}
  for (const p of list.value) if (p.status === 'SUCCESS') map[p.channel] = (map[p.channel] || 0) + (p.amount || 0)
  const colors = { ALIPAY_SANDBOX: '#ff7a1a', WECHAT_MOCK: '#12bfa0', BALANCE: '#3a6bf0' }
  return {
    color: Object.keys(map).map(k => colors[k] || '#9ca0a8'),
    tooltip: { trigger: 'item', formatter: pp => `${channelLabel(pp.name)}<br/>¥${((pp.value || 0) / 100).toFixed(2)} (${pp.percent}%)` },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['50%', '44%'],
      label: { formatter: '{b}\n{c}' },
      data: Object.entries(map).map(([k, v]) => ({ name: k, value: v }))
    }]
  }
})

function yuan(v) { return ((v || 0) / 100).toFixed(2) }
function channelLabel(c) { return ({ ALIPAY_SANDBOX: '支付宝沙箱', WECHAT_MOCK: '微信(模拟)', BALANCE: '余额支付' })[c] || c }
function statusLabel(s) { return ({ WAIT: '待支付', SUCCESS: '已支付', FAIL: '失败', CLOSED: '已关闭', REFUNDED: '已退款' })[s] || s }
function statusTag(s) { return ({ WAIT: 'tag--warn', SUCCESS: 'tag--success', FAIL: 'tag--danger', CLOSED: 'tag--info', REFUNDED: 'tag--warn' })[s] || 'tag' }
function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '—' }

async function load() {
  error.value = ''
  try {
    const r = await adminApi.payments()
    list.value = r.data || []
    ready.value = true
  } catch (e) {
    error.value = `支付流水加载失败：${e.message}`
  }
}
onMounted(load)
</script>
