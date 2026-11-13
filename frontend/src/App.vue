<template>
  <div class="app-shell" :class="{ 'app-shell--wide': route.meta.wide }">
    <router-view v-slot="{ Component }">
      <transition name="page">
        <keep-alive :include="cachedViews">
          <component :is="Component" />
        </keep-alive>
      </transition>
    </router-view>

    <TabBar v-if="showTab" />

    <!-- 全局提示 -->
    <transition-group name="toast" tag="div" class="toast-host">
      <div v-for="t in toastState.list" :key="t.id" class="toast" :class="'toast--' + t.type">
        {{ t.msg }}
      </div>
    </transition-group>

    <!-- 应用内确认弹层（替代 window.confirm） -->
    <ConfirmDialog />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import TabBar from './components/TabBar.vue'
import ConfirmDialog from './components/ConfirmDialog.vue'
import { toastState } from './api/http'
import { useAppStore } from './store/app'

const route = useRoute()
const app = useAppStore()

const showTab = computed(() => !!route.meta.tab)

/**
 * 只缓存「浏览型」tab 页：
 * 首页/附近返回时保留滚动位置与已加载数据，避免每次切 tab 都重新请求 + 重新渲染；
 * 订单/消息/我的属于「每次都要最新」的页面，不缓存。
 */
const cachedViews = ['HomeView', 'NearbyView']

onMounted(() => {
  app.bootstrap()
})
</script>

<style scoped>
.toast-host {
  position: fixed;
  top: calc(18% + env(safe-area-inset-top, 0px));
  left: 50%;
  transform: translateX(-50%);
  z-index: 200;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  pointer-events: none;
  width: max-content;
  max-width: 78vw;
}

.toast {
  padding: 9px 16px;
  border-radius: var(--r-btn);
  background: rgba(23, 24, 26, .88);
  color: #fff;
  font-size: var(--fs-body);
  box-shadow: var(--sh-float);
  text-align: center;
  line-height: 1.45;
}

.toast--error {
  background: rgba(229, 72, 77, .94);
}

.toast--success {
  background: rgba(18, 191, 160, .94);
}

.toast-enter-active,
.toast-leave-active {
  /* 只过渡合成属性，避免 all 触发额外样式计算 */
  transition: opacity .2s ease, transform .2s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
