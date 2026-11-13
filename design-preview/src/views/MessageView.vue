<template>
  <TabPage>
    <template #header>
      <header class="page-head">
        <StatusBar dark />
        <div class="page-head__title">消息</div>
      </header>
    </template>

    <div class="entries">
      <button v-for="e in entries" :key="e.title" class="entry" @click="toast(`演示版：${e.title}后续开放`)">
        <span class="entry__icon" :style="{ background: e.bg }">{{ e.icon }}</span>
        <span class="entry__main">
          <span class="entry__title">{{ e.title }}</span>
          <span class="entry__desc">{{ e.desc }}</span>
        </span>
        <span v-if="e.badge" class="entry__badge">{{ e.badge }}</span>
      </button>
    </div>

    <div class="placeholder">
      <div class="placeholder__desc">IM 会话基于自建 WebSocket（见 SRS 1.5 / 4.9），<br />前台实时推送、后台轮询兜底 + 端上本地通知。</div>
    </div>
  </TabPage>
</template>

<script setup>
import TabPage from '../components/TabPage.vue'
import StatusBar from '../components/StatusBar.vue'
import { useToast } from '../store/toast'

const { show } = useToast()
const toast = show

const entries = [
  { title: '系统通知', desc: '平台公告与活动通知', icon: '📢', badge: 2, bg: 'linear-gradient(135deg,#FFE7C6,#FFC48A)' },
  { title: '订单消息', desc: '订单状态、支付结果、配送提醒', icon: '🧾', badge: 0, bg: 'linear-gradient(135deg,#DDEFFF,#B9DBFF)' },
  { title: '优惠活动', desc: '领券、秒杀与会员权益通知', icon: '🎁', badge: 0, bg: 'linear-gradient(135deg,#FFE1EC,#FFB7CE)' },
  { title: '会话', desc: '与商家 / 骑手实时沟通', icon: '💬', badge: 1, bg: 'linear-gradient(135deg,#E6F7EF,#B7E7D2)' }
]
</script>

<style scoped>
.page-head { background: var(--panel); border-bottom: 1px solid var(--line); }
.page-head__title { font-size: 18px; font-weight: 700; padding: 4px 14px 12px; }
.entries { background: var(--panel); margin: 12px; border-radius: var(--r-card); box-shadow: var(--shadow-light); overflow: hidden; }
.entry { width: 100%; display: flex; align-items: center; gap: 10px; padding: 12px 14px; text-align: left; }
.entry + .entry { border-top: 1px solid var(--line); }
.entry__icon { width: 38px; height: 38px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 19px; flex-shrink: 0; }
.entry__main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.entry__title { font-size: 14px; font-weight: 600; }
.entry__desc { font-size: 11px; color: var(--text-3); }
.entry__badge { min-width: 17px; height: 17px; padding: 0 5px; border-radius: 10px; background: var(--error); color: #fff; font-size: 11px; display: inline-flex; align-items: center; justify-content: center; }
</style>
