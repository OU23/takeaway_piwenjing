<template>
  <TabPage>
    <template #header>
      <header class="mine-head">
        <StatusBar />
        <div class="mine-head__user">
          <span class="mine-head__avatar">星</span>
          <span class="mine-head__info">
            <span class="mine-head__name">星光用户</span>
            <span class="mine-head__phone">138****8888</span>
          </span>
          <span class="mine-head__level">黄金会员</span>
        </div>
      </header>
    </template>

    <div class="assets">
      <button class="asset" @click="toast('演示版：钱包余额后续开放')">
        <b class="num">128.50</b><span>钱包余额</span>
      </button>
      <button class="asset" @click="toast('演示版：优惠券后续开放')">
        <b class="num">6</b><span>优惠券</span>
      </button>
      <button class="asset" @click="toast('演示版：积分后续开放')">
        <b class="num">320</b><span>积分</span>
      </button>
      <button class="asset" @click="toast('演示版：签到后续开放')">
        <b class="num">已签 3 天</b><span>每日签到</span>
      </button>
    </div>

    <div class="card list">
      <button v-for="f in features" :key="f" class="list__item" @click="toast(`演示版：${f}后续开放`)">
        <span>{{ f }}</span>
        <span class="list__arrow">›</span>
      </button>
    </div>
  </TabPage>
</template>

<script setup>
import TabPage from '../components/TabPage.vue'
import StatusBar from '../components/StatusBar.vue'
import { useToast } from '../store/toast'

const { show } = useToast()
const toast = show

const features = ['收货地址', '我的评价', '我的收藏', '邀请有礼', '会员中心', '客服与帮助', '设置']
</script>

<style scoped>
.mine-head { background: var(--brand-grad); padding-bottom: 26px; border-radius: 0 0 18px 18px; box-shadow: 0 8px 18px rgba(245, 72, 31, .18); }
.mine-head__user { display: flex; align-items: center; gap: 12px; padding: 6px 16px 0; }
.mine-head__avatar { width: 52px; height: 52px; border-radius: 50%; background: rgba(255, 255, 255, .22); border: 1px solid rgba(255, 255, 255, .5); color: #fff; font-size: 22px; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.mine-head__info { flex: 1; display: flex; flex-direction: column; gap: 3px; color: #fff; }
.mine-head__name { font-size: 17px; font-weight: 700; }
.mine-head__phone { font-size: 12px; opacity: .85; }
.mine-head__level { font-size: 11px; color: #7a4b00; background: var(--member-grad); border-radius: var(--r-tag); padding: 4px 9px; font-weight: 600; }

.assets { display: flex; gap: 10px; margin: -18px 12px 12px; }
.asset { flex: 1; background: var(--panel); border-radius: var(--r-card); box-shadow: var(--shadow-light); padding: 12px 8px; display: flex; flex-direction: column; align-items: center; gap: 3px; }
.asset b { font-size: 16px; color: var(--text-1); }
.asset span { font-size: 11px; color: var(--text-3); }

.list { padding: 0 14px; }
.list__item { width: 100%; display: flex; align-items: center; justify-content: space-between; padding: 13px 0; font-size: 14px; }
.list__item + .list__item { border-top: 1px solid var(--line); }
.list__arrow { color: var(--text-3); font-size: 16px; }
</style>
