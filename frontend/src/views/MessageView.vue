<template>
  <div class="page">
    <NavBar title="消息" :back="false" />

    <div class="page-body">
      <!-- 聚合入口 -->
      <div class="agg">
        <div class="agg__item" @click="toast('系统通知：平台公告与活动通知（SRS 5.2.7）', 'info')">
          <div class="agg__icon agg__icon--sys"><Icon name="bell" :size="17" color="#fff" /></div>
          <span>系统通知</span>
          <i class="agg__dot" />
        </div>
        <div class="agg__item" @click="scrollToMessages">
          <div class="agg__icon agg__icon--order"><Icon name="receipt" :size="17" color="#fff" /></div>
          <span>订单消息</span>
          <i class="agg__dot">{{ orderMessages.length }}</i>
        </div>
        <div class="agg__item" @click="toast('优惠活动：神券 / 满减 / 秒杀推送（SRS 5.2.10）', 'info')">
          <div class="agg__icon agg__icon--promo"><Icon name="ticket" :size="17" color="#fff" /></div>
          <span>优惠活动</span>
        </div>
      </div>

      <!-- 订单消息 -->
      <div ref="msgEl" class="section-title">
        订单消息
        <small>实时来自订单状态流水</small>
      </div>

      <div v-if="!ready">
        <div v-for="i in 3" :key="i" class="skeleton mt-12" style="height: 62px; border-radius: 10px" />
      </div>

      <template v-else>
        <div
          v-for="m in orderMessages"
          :key="m.key"
          class="msg"
          @click="$router.push(`/order/${m.orderNo}`)"
        >
          <PhImage style="width: 40px; height: 40px" :image="m.image" :label="m.shopName.slice(0, 2)" />
          <div class="flex-1" style="min-width: 0">
            <div class="row row--between">
              <span class="text-strong ellipsis" style="max-width: 62%">{{ m.shopName }}</span>
              <span class="text-aux">{{ fromNow(m.time) }}</span>
            </div>
            <div class="text-desc ellipsis mt-4">{{ m.text }}</div>
          </div>
        </div>

        <EmptyState v-if="!orderMessages.length" icon="chat" title="暂无消息" desc="下单后订单节点消息会实时推送" />
      </template>

      <!-- 会话列表（IM） -->
      <div class="section-title">
        会话
        <small>商家 / 骑手</small>
      </div>

      <div v-for="c in conversations" :key="c.orderNo" class="msg" @click="$router.push(`/order/${c.orderNo}`)">
        <div class="conv-avatar">
          <Icon :name="c.fulfillType === 'PICKUP' ? 'store' : 'truck'" :size="17" color="#fff" />
        </div>
        <div class="flex-1" style="min-width: 0">
          <div class="row row--between">
            <span class="text-strong ellipsis" style="max-width: 60%">
              {{ c.shopName }} · {{ c.fulfillType === 'PICKUP' ? '商家' : '骑手' }}
            </span>
            <span class="text-aux">{{ fromNow(c.time) }}</span>
          </div>
          <div class="text-desc ellipsis mt-4">{{ c.text }}</div>
        </div>
      </div>

      <div class="im-tip">
        <Icon name="info" :size="13" color="#5C6169" />
        <span>
          即时通讯基于自建 WebSocket（SRS 1.5【保留】），App 前台实时推送、后台轮询兜底 +
          端上本地通知。会话与订单强关联（N:1），订单完成后保留 N 天转只读留档。
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import NavBar from '../components/NavBar.vue'
import Icon from '../components/Icon.vue'
import PhImage from '../components/PhImage.vue'
import EmptyState from '../components/EmptyState.vue'
import { listOrders } from '../api'
import { toast } from '../api/http'
import { fromNow } from '../utils/format'

const orders = ref([])
const ready = ref(false)
const msgEl = ref(null)

/** 订单消息：取每单最新一条状态流水 */
const orderMessages = computed(() =>
  orders.value
    .filter(o => (o.timeline || []).length || o.statusLabel)
    .slice(0, 12)
    .map(o => {
      const last = (o.timeline || [])[0]
      return {
        key: o.orderNo,
        orderNo: o.orderNo,
        shopName: o.shopName,
        image: o.shopHeaderImg,
        time: o.createdAt,
        text: `${o.statusLabel} · ${last?.remark || '订单状态更新'}`
      }
    })
)

/** 会话列表：进行中的订单生成与商家 / 骑手的会话 */
const conversations = computed(() =>
  orders.value
    .filter(o => ['进行中', '待接单'].includes(o.statusGroup))
    .slice(0, 6)
    .map(o => ({
      orderNo: o.orderNo,
      shopName: o.shopName,
      fulfillType: o.fulfillType,
      time: o.createdAt,
      text:
        o.fulfillType === 'PICKUP'
          ? '您好，请问什么时候可以取餐？'
          : '骑手已取货，正在为您配送中'
    }))
)

onMounted(async () => {
  const r = await listOrders({ pageNum: 1, pageSize: 20 })
  orders.value = r.data?.list || []
  ready.value = true
})

function scrollToMessages() {
  msgEl.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<style scoped>
.agg {
  display: flex;
  gap: 10px;
}

.agg__item {
  flex: 1;
  background: var(--bg-card);
  border-radius: var(--r-card);
  padding: 12px 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  font-size: var(--fs-aux);
  color: var(--t-2);
  position: relative;
  box-shadow: var(--sh-light);
}

.agg__icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.agg__icon--sys { background: linear-gradient(135deg, #B7D9F7, #2E6FBF); }
.agg__icon--order { background: linear-gradient(135deg, #FFB03A, #F5481F); }
.agg__icon--promo { background: linear-gradient(135deg, #FFE39A, #F2B23C); }

.agg__dot {
  position: absolute;
  top: 8px;
  right: 14px;
  min-width: 15px;
  height: 15px;
  padding: 0 4px;
  border-radius: 8px;
  background: var(--c-error);
  color: #fff;
  font-size: 9px;
  line-height: 15px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.msg {
  display: flex;
  gap: 10px;
  align-items: center;
  background: var(--bg-card);
  border-radius: var(--r-card);
  padding: 11px;
  box-shadow: var(--sh-light);
}

.msg + .msg {
  margin-top: 8px;
}

.conv-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2A3040, #171B26);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.im-tip {
  display: flex;
  gap: 6px;
  align-items: flex-start;
  background: var(--bg-card);
  border-radius: var(--r-card);
  padding: 10px;
  margin-top: 16px;
  font-size: var(--fs-aux);
  color: var(--t-2);
  line-height: 1.55;
  box-shadow: var(--sh-light);
}
</style>
