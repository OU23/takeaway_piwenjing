<template>
  <div>
    <div class="page-head">
      <div>
        <h2>数据看板</h2>
        <div class="sub">实时经营概览 · 数据每 30 秒自动刷新</div>
      </div>
    </div>

    <div v-if="error" class="notice">{{ error }}　<button class="btn btn--sm" @click="load">重新加载</button></div>
    <div v-if="!ready && !error" class="spin">加载中…</div>
    <template v-else>
      <div class="stat-grid">
        <div class="stat">
          <div class="stat__label">今日 GMV（元）</div>
          <div class="stat__value">{{ yuan(ov.todayGmv) }}</div>
          <div class="stat__hint">累计 {{ yuan(ov.totalGmv) }} 元</div>
        </div>
        <div class="stat">
          <div class="stat__label">今日订单</div>
          <div class="stat__value">{{ ov.todayOrderCount }}</div>
          <div class="stat__hint">共 {{ ov.totalOrderCount }} 单</div>
        </div>
        <div class="stat">
          <div class="stat__label">客单价（元）</div>
          <div class="stat__value">{{ yuan(ov.avgOrderAmount) }}</div>
          <div class="stat__hint">今日均值</div>
        </div>
        <div class="stat">
          <div class="stat__label">进行中订单</div>
          <div class="stat__value">{{ ov.processingOrderCount }}</div>
          <div class="stat__hint">待接单 / 备餐 / 配送</div>
        </div>
        <div class="stat">
          <div class="stat__label">门店 / 商家</div>
          <div class="stat__value">{{ ov.shopCount }} <small>/ {{ ov.merchantCount }}</small></div>
          <div class="stat__hint">在架商品 {{ ov.dishCount }}</div>
        </div>
        <div class="stat">
          <div class="stat__label">骑手 / 用户</div>
          <div class="stat__value">{{ ov.riderCount }} <small>/ {{ ov.userCount }}</small></div>
          <div class="stat__hint">自取单占比参考</div>
        </div>
      </div>

      <div class="card mt-16">
        <div class="card__title">
          <span>近 7 日趋势</span>
          <small>营业额（元） · 订单量（单）</small>
        </div>
        <EChart :option="trendOption" :height="300" />
        <div class="row" style="gap: 24px; margin-top: 8px">
          <span class="muted">平均配送时长：<b class="text-strong">{{ trend.avgDeliveryMinutes || 0 }}</b> 分钟</span>
        </div>
      </div>

      <div class="card mt-16">
        <div class="card__title"><span>订单状态分布</span></div>
        <EChart :option="statusOption" :height="240" />
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'
import EChart from '../components/EChart.vue'
import { adminApi } from '../api/admin'

const ov = ref({})
const trend = ref({ gmvTrend: [], orderTrend: [], avgDeliveryMinutes: 0 })
const ready = ref(false)
const error = ref('')
let timer = null

function yuan(v) {
  return ((v || 0) / 100).toFixed(2)
}

const trendOption = ref({})
const statusOption = ref({})

function buildTrendOption() {
  const g = (trend.value.gmvTrend || []).map(i => [i.date, (i.value || 0) / 100])
  const c = (trend.value.orderTrend || []).map(i => [i.date, i.value || 0])
  trendOption.value = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['营业额', '订单量'], right: 10, top: 0 },
    grid: { left: 50, right: 50, top: 36, bottom: 30 },
    xAxis: { type: 'category', data: g.map(i => i[0]), axisLine: { lineStyle: { color: '#d8dde9' } } },
    yAxis: [
      { type: 'value', name: '元', axisLabel: { color: '#9aa3b2' } },
      { type: 'value', name: '单', axisLabel: { color: '#9aa3b2' } }
    ],
    series: [
      { name: '营业额', type: 'line', smooth: true, data: g.map(i => i[1]), itemStyle: { color: '#ff7a1a' }, lineStyle: { width: 2 }, areaStyle: { color: 'rgba(255,122,26,.10)' } },
      { name: '订单量', type: 'bar', yAxisIndex: 1, data: c.map(i => i[1]), itemStyle: { color: '#3a6bf0', borderRadius: [3, 3, 0, 0] }, barWidth: 18 }
    ]
  }
}

function buildStatusOption() {
  const dist = ov.value.statusDistribution || {}
  statusOption.value = {
    color: ['#ff7a1a', '#12bfa0', '#ff9500', '#3a6bf0', '#e5484d', '#9ca0a8', '#ffb03a', '#5c6169'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['50%', '44%'],
      label: { formatter: '{b}\n{c}' },
      data: Object.entries(dist).map(([k, v]) => ({ name: k, value: v }))
    }]
  }
}

async function load() {
  error.value = ''
  try {
    const [o, t] = await Promise.all([adminApi.overview(), adminApi.trend(7)])
    ov.value = o.data || {}
    trend.value = t.data || { gmvTrend: [], orderTrend: [], avgDeliveryMinutes: 0 }
    buildTrendOption()
    buildStatusOption()
    ready.value = true
  } catch (e) {
    error.value = `数据加载失败：${e.message}`
  }
}

onMounted(() => {
  load()
  timer = setInterval(load, 30000)
})
onBeforeUnmount(() => timer && clearInterval(timer))
</script>
