<template>
  <nav class="tabbar">
    <router-link
      v-for="t in tabs"
      :key="t.path"
      class="tabbar__item"
      :class="{ 'tabbar__item--active': isActive(t.path) }"
      :to="t.path"
      :aria-label="t.name"
      :aria-current="isActive(t.path) ? 'page' : undefined"
    >
      <Icon :name="t.icon" :size="22" :stroke="isActive(t.path) ? 1.9 : 1.5" />
      <span>{{ t.name }}</span>
      <i v-if="t.badge && badgeCount > 0" class="tabbar__badge num">{{ badgeCount > 99 ? '99+' : badgeCount }}</i>
    </router-link>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Icon from './Icon.vue'
import { useAppStore } from '../store/app'

const route = useRoute()
const app = useAppStore()

const tabs = [
  { path: '/', name: '首页', icon: 'home' },
  { path: '/nearby', name: '附近', icon: 'nearby' },
  { path: '/orders', name: '订单', icon: 'receipt', badge: true },
  { path: '/messages', name: '消息', icon: 'chat' },
  { path: '/mine', name: '我的', icon: 'user' }
]

const badgeCount = computed(() => app.cartCount)

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
</script>
