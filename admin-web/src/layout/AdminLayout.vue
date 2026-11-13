<template>
  <div class="layout">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="sidebar__brand">
        <div class="sidebar__logo" />
        <span>数创星光 · 运营后台</span>
      </div>
      <nav class="sidebar__nav">
        <div
          v-for="m in menus"
          :key="m.name"
          class="nav-item"
          :class="{ active: route.name === m.name }"
          @click="go(m)"
          :title="m.meta.title"
        >
          <Icon :name="m.meta.icon" :size="18" />
          <span class="ni-label">{{ m.meta.title }}</span>
        </div>
      </nav>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="row" style="gap: 12px">
          <button class="btn btn--ghost btn--sm" @click="collapsed = !collapsed">
            <Icon :name="collapsed ? 'chevron' : 'back'" :size="16" :style="collapsed ? 'transform:rotate(180deg)' : ''" />
          </button>
          <div class="topbar__crumb">{{ currentTitle }}</div>
        </div>
        <div class="topbar__right">
          <span class="tag" :class="roleTagClass">{{ auth.roleLabel }}</span>
          <div class="topbar__user">
            <div class="avatar">{{ initial }}</div>
            <span class="text-strong">{{ auth.name }}</span>
          </div>
          <button class="btn btn--ghost btn--sm" @click="onLogout">退出</button>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import { menuRoutes } from '../router'
import { auth, logout } from '../store/auth'

const route = useRoute()
const router = useRouter()
const collapsed = ref(false)

const menus = menuRoutes
const currentTitle = computed(() => route.meta?.title || '运营后台')
const initial = computed(() => (auth.name || '?').slice(0, 1))

const roleTagClass = computed(() => ({
  SUPER_ADMIN: 'tag--primary',
  OPERATOR: 'tag--success',
  CS: 'tag--info'
}[auth.role] || 'tag--info'))

function go(m) {
  router.push(m.path)
}

function onLogout() {
  logout()
  router.push('/login')
}
</script>
