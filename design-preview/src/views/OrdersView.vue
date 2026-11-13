<template>
  <TabPage>
    <template #header>
      <header class="page-head">
        <StatusBar dark />
        <div class="page-head__tabs">
          <span v-for="t in tabs" :key="t" class="page-head__tab" :class="{ active: t === '全部' }">{{ t }}</span>
        </div>
      </header>
    </template>

    <div class="placeholder">
      <div class="placeholder__icon">🧾</div>
      <div class="placeholder__title">订单</div>
      <div class="placeholder__desc">订单列表将按「全部 / 待付款 / 待接单 / 进行中 / 待评价 / 退款售后」分组，<br />配送单与到店自取单共用同一套状态机（见 SRS 8.1 / 8.9）。</div>
    </div>
  </TabPage>
</template>

<script setup>
import TabPage from '../components/TabPage.vue'
import StatusBar from '../components/StatusBar.vue'

const tabs = ['全部', '待付款', '待接单', '进行中', '待评价', '退款售后']
</script>

<style scoped>
.page-head { background: var(--panel); border-bottom: 1px solid var(--line); }
.page-head__tabs { display: flex; gap: 18px; padding: 4px 14px 10px; overflow-x: auto; }
.page-head__tabs::-webkit-scrollbar { width: 0; height: 0; }
.page-head__tab { font-size: 13px; color: var(--text-2); white-space: nowrap; position: relative; }
.page-head__tab.active { color: var(--text-1); font-weight: 600; }
.page-head__tab.active::after { content: ''; position: absolute; left: 50%; bottom: -5px; transform: translateX(-50%); width: 16px; height: 2px; border-radius: 2px; background: var(--brand-grad); }
</style>
