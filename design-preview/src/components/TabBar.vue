<template>
  <nav class="tabbar">
    <button
      v-for="t in tabs"
      :key="t.path"
      class="tabbar__item"
      :class="{ active: route.path === t.path }"
      @click="go(t.path)"
    >
      <span class="tabbar__icon" v-html="t.icon" />
      <span class="tabbar__label">{{ t.label }}</span>
    </button>
  </nav>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 细描边线性图标（1.5px 描边、直角端点）
const tabs = [
  { path: '/home', label: '首页', icon: '<svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="butt" stroke-linejoin="miter"><path d="M4 11 12 4l8 7"/><path d="M6 10v9h12v-9"/><path d="M10 19v-5h4v5"/></svg>' },
  { path: '/nearby', label: '附近', icon: '<svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="butt" stroke-linejoin="miter"><circle cx="12" cy="12" r="8.5"/><path d="M12 3.5c3 3 3 14 0 17"/><path d="M3.5 12h17"/></svg>' },
  { path: '/orders', label: '订单', icon: '<svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="butt" stroke-linejoin="miter"><path d="M6 3h12v18l-3-2-3 2-3-2-3 2z"/><path d="M9 8h6M9 12h6M9 16h3"/></svg>' },
  { path: '/message', label: '消息', icon: '<svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="butt" stroke-linejoin="miter"><path d="M4 5h16v11H9l-5 4z"/><path d="M8 9h8M8 12h5"/></svg>' },
  { path: '/mine', label: '我的', icon: '<svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="butt" stroke-linejoin="miter"><circle cx="12" cy="8" r="3.6"/><path d="M4.8 20c0-3.8 3.2-5.8 7.2-5.8s7.2 2 7.2 5.8"/></svg>' }
]

function go(path) {
  if (route.path !== path) router.push(path)
}
</script>
