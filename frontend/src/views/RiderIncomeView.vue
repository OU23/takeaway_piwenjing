<template>
  <div class="page theme-rider page--notab" v-if="ready">
    <NavBar title="收入与结算" brand />

    <div class="page-body">
      <!-- 收入概览 -->
      <div class="income-hero">
        <div class="income-hero__label">今日收入（元）</div>
        <div class="income-hero__amount">¥<span class="num">{{ yuan(income.todayIncome) }}</span></div>
        <div class="income-hero__row num">
          <div><small>累计收入</small>¥{{ yuan(income.totalIncome) }}</div>
          <div><small>完成单量</small>{{ income.totalCount }}</div>
        </div>
      </div>

      <div class="settle-row">
        <div class="settle-row__item">
          <div class="price"><span class="price__sym">¥</span>{{ yuan(income.settledIncome) }}</div>
          <span>已结算</span>
        </div>
        <div class="settle-row__item">
          <div class="price"><span class="price__sym">¥</span>{{ yuan(income.unsettledIncome) }}</div>
          <span>待结算</span>
        </div>
        <button class="btn btn--primary btn--sm" @click="toast('结算打款为 SRS 1.4 结算单能力（P2），原型仅展示', 'info')">
          申请结算
        </button>
      </div>

      <!-- 收入明细 -->
      <div class="section-title">收入明细 <small>每单配送费 + 平台补贴</small></div>
      <div v-if="income.list.length" class="income-list">
        <div v-for="(it, i) in income.list" :key="i" class="income-item">
          <div class="flex-1" style="min-width: 0">
            <div class="row row--between">
              <span class="text-strong ellipsis">{{ it.shopName }}</span>
              <span class="num text-price">+¥{{ yuan(it.incomeAmount) }}</span>
            </div>
            <div class="text-aux mt-4 num">
              {{ it.orderNo }} · {{ fmtTime(it.createdAt) }}
            </div>
            <div class="text-aux mt-4 num">
              配送费 ¥{{ yuan(it.baseFee) }} + 补贴 ¥{{ yuan(it.subsidy) }}
              <template v-if="it.penalty"> - 罚款 ¥{{ yuan(it.penalty) }}</template>
            </div>
          </div>
          <span class="tag" :class="it.settled ? 'tag--success' : 'tag--warn'">
            {{ it.settled ? '已结算' : '待结算' }}
          </span>
        </div>
      </div>
      <EmptyState v-else icon="wallet" title="暂无收入记录" desc="完成配送后这里会显示每单收入" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import NavBar from '../components/NavBar.vue'
import EmptyState from '../components/EmptyState.vue'
import { getRiderIncome } from '../api/rider'
import { toast } from '../api/http'
import { yuan, fmtTime } from '../utils/format'

const ready = ref(false)
const income = ref({ list: [] })

onMounted(async () => {
  const r = await getRiderIncome()
  income.value = r.data || { list: [] }
  ready.value = true
})
</script>

<style scoped>
.income-hero {
  background: linear-gradient(135deg, #2A3040, #171B26);
  border-radius: var(--r-card); padding: 18px; color: #fff;
}
.income-hero__label { font-size: var(--fs-aux); opacity: .82; }
.income-hero__amount { font-size: 32px; font-weight: var(--fw-title); margin: 4px 0 12px; }
.income-hero__row {
  display: flex; gap: 24px; font-size: var(--fs-aux); opacity: .9;
}
.income-hero__row small { margin-right: 6px; opacity: .7; }

.settle-row {
  display: flex; align-items: center; gap: 12px; margin-top: 10px;
  background: var(--bg-card); border-radius: var(--r-card); padding: 14px; box-shadow: var(--sh-light);
}
.settle-row__item { flex: 1; display: flex; flex-direction: column; gap: 3px; font-size: var(--fs-aux); color: var(--t-3); }
.settle-row .price { font-size: var(--fs-price); font-weight: var(--fw-title); color: var(--t-1); }

.income-list { display: flex; flex-direction: column; gap: 8px; }
.income-item {
  display: flex; gap: 10px; align-items: flex-start;
  background: var(--bg-card); border-radius: var(--r-card); padding: 11px; box-shadow: var(--sh-light);
}
</style>
