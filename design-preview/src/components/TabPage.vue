<template>
  <!-- 带底部 Tab 的页面骨架：固定滚动区 + TabBar + 弹层插槽 -->
  <div class="tab-page">
    <div class="tab-page__scroll screen__scroll">
      <slot name="header" />
      <slot />
    </div>
    <TabBar />
    <slot name="sheet" />
  </div>
</template>

<script setup>
import TabBar from './TabBar.vue'
</script>

<style scoped>
.tab-page { height: 100%; display: flex; flex-direction: column; position: relative; }
</style>
