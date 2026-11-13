<template>
  <TabPage>
    <template #header>
      <header class="page-head">
        <StatusBar />
        <div class="page-head__bar">
          <div class="page-head__search">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="butt" stroke-linejoin="miter"><circle cx="11" cy="11" r="6.5" /><path d="M15.8 15.8 20.5 20.5" /></svg>
            <input placeholder="搜索附近店铺 / 商品" />
          </div>
        </div>
      </header>
    </template>

    <div class="placeholder">
      <div class="placeholder__icon">🗺️</div>
      <div class="placeholder__title">附近 · 地图找店</div>
      <div class="placeholder__desc">地图图层与店铺气泡将接入高德 / 百度开放平台个人 Key（免费额度）；<br />本期首页已提供按距离排序的店铺流，可先在「首页 → 距离最近」体验。</div>
    </div>
  </TabPage>
</template>

<script setup>
import TabPage from '../components/TabPage.vue'
import StatusBar from '../components/StatusBar.vue'
</script>

<style scoped>
.page-head { background: var(--brand-grad); padding-bottom: 12px; border-radius: 0 0 18px 18px; }
.page-head__bar { display: flex; gap: 8px; padding: 4px 14px 2px; }
.page-head__search { flex: 1; height: 34px; background: #fff; border-radius: var(--r-btn); display: flex; align-items: center; gap: 6px; padding: 0 10px; color: var(--text-3); }
.page-head__search input { flex: 1; border: none; outline: none; font-size: 13px; min-width: 0; }
</style>
