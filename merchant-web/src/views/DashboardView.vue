<template>
  <div v-if="d">
    <div class="brand-hero">
      <div class="muted" style="color:#ffe;">{{ d.shopName || '我的门店' }}</div>
      <div style="font-size:30px;font-weight:700;margin:6px 0;">¥{{ yuan(d.todayGmv) }}</div>
      <div class="muted" style="color:#ffe;">今日营业额（已支付）</div>
    </div>

    <div class="stat-grid" style="margin-top:12px;">
      <div class="stat-card"><div class="stat-num">{{ d.todayOrderCount }}</div><div class="muted">今日订单</div></div>
      <div class="stat-card"><div class="stat-num">{{ d.processingOrderCount }}</div><div class="muted">进行中</div></div>
      <div class="stat-card"><div class="stat-num">{{ d.totalOrderCount }}</div><div class="muted">累计订单</div></div>
    </div>

    <van-cell-group inset title="订单状态分布">
      <van-cell v-for="item in distList" :key="item.k" :title="item.k" :value="String(item.c)" />
      <van-empty v-if="distList.length === 0" description="暂无数据" />
    </van-cell-group>
  </div>
  <van-loading v-else style="margin-top:45%;" vertical>加载中…</van-loading>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { merchantApi, yuan } from '../api/merchant'

const d = ref(null)
const distList = computed(() =>
  Object.entries(d.value?.statusDistribution || {}).map(([k, c]) => ({ k, c }))
)

onMounted(async () => { d.value = await merchantApi.dashboard() })
</script>
