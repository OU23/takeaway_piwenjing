<template>
  <div class="page theme-rider page--notab" v-if="task">
    <NavBar title="配送任务" brand>
      <template #extra>
        <span class="task-no num">{{ task.orderNo }}</span>
      </template>
    </NavBar>

    <!-- 状态横幅 -->
    <div class="banner" :class="'banner--' + task.status">
      <div class="banner__status">{{ task.statusLabel }}</div>
      <div class="banner__sub">{{ bannerSub }}</div>
    </div>

    <div class="page-body">
      <!-- 配送地图（高德：门店 + 骑手当前位置） -->
      <AmapView class="map" height="150px" :markers="mapMarkers" :center="mapCenter" />

      <!-- 顾客信息 -->
      <div class="card">
        <div class="card__title">顾客信息</div>
        <div class="row row--between">
          <span class="text-strong">{{ task.receiverName }}</span>
          <span class="text-desc num">{{ task.receiverPhone }}</span>
        </div>
        <div class="text-desc mt-8">{{ task.receiverAddress }}</div>
        <div class="row row--between mt-12">
          <span class="text-aux">预计送达</span>
          <span class="text-strong num">{{ task.promiseText }}</span>
        </div>
        <button class="btn btn--weak btn--sm btn--block mt-12" @click="toast('拨打顾客电话为原型占位（SRS 9.11 需二次授权）', 'info')">
          联系顾客
        </button>
      </div>

      <!-- 商品明细 -->
      <div class="card mt-12">
        <div class="card__title">商品明细</div>
        <div v-for="it in task.items" :key="it.dishId" class="goods">
          <span class="text-desc flex-1 ellipsis">{{ it.dishName }}</span>
          <span class="text-aux num" style="margin: 0 12px">×{{ it.quantity }}</span>
          <span class="num text-strong">¥{{ yuan(it.amount) }}</span>
        </div>
        <div class="divider" />
        <div class="fee-row"><span>商品小计</span><span class="fee-row__value">¥{{ yuan(amount.itemAmount) }}</span></div>
        <div class="fee-row"><span>打包费</span><span class="fee-row__value">¥{{ yuan(amount.packFee) }}</span></div>
        <div class="fee-row"><span>配送费（含平台补贴）</span><span class="fee-row__value">¥{{ yuan(amount.deliveryFee) }}</span></div>
        <div class="fee-row fee-row--discount" v-if="amount.discountAmount">
          <span>优惠</span><span class="fee-row__value">-¥{{ yuan(amount.discountAmount) }}</span>
        </div>
        <div class="fee-row fee-row--total"><span>实付</span><span class="fee-row__value">¥{{ yuan(amount.payAmount) }}</span></div>
      </div>

      <!-- 状态时间轴 -->
      <div class="card mt-12">
        <div class="card__title">配送进度</div>
        <div class="timeline">
          <div
            v-for="(log, i) in timeline"
            :key="i"
            class="timeline__item"
            :class="i === 0 ? 'timeline__item--current' : 'timeline__item--done'"
          >
            <div class="timeline__title">{{ log.toLabel }} · {{ log.operatorLabel }}</div>
            <div class="timeline__meta">{{ log.remark }} · {{ fmtTime(log.createdAt) }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部操作 -->
    <div class="action-bar" v-if="canPickup || canDeliver">
      <button v-if="canPickup" class="btn btn--primary btn--block" @click="doPickup">确认取货</button>
      <button v-if="canDeliver" class="btn btn--primary btn--block" @click="doDeliver">确认送达</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import Icon from '../components/Icon.vue'
import AmapView from '../components/AmapView.vue'
import { getRiderTask, pickupOrder, deliverOrder } from '../api/rider'
import { toast } from '../api/http'
import { yuan, fmtTime } from '../utils/format'

const route = useRoute()
const task = ref(null)

const amount = computed(() => task.value?.amount || {})
const timeline = computed(() => task.value?.timeline || [])
const canPickup = computed(() => task.value?.status === 'WAIT_PICKUP')
const canDeliver = computed(() => task.value?.status === 'DELIVERING')

/* 配送地图打点：门店 + 顾客地址；骑手当前位置由 AmapView 用浏览器定位 */
const mapMarkers = computed(() => {
  const t = task.value
  const list = []
  if (t && t.shopLat != null && t.shopLng != null) {
    list.push({ id: 'shop', lng: t.shopLng, lat: t.shopLat, title: t.shopName })
  }
  if (t && t.receiverLat != null && t.receiverLng != null) {
    list.push({ id: 'receiver', lng: t.receiverLng, lat: t.receiverLat, title: '顾客地址' })
  }
  return list
})
const mapCenter = computed(() => {
  const t = task.value
  if (t && t.shopLat != null && t.shopLng != null) return [Number(t.shopLng), Number(t.shopLat)]
  if (t && t.receiverLat != null && t.receiverLng != null) return [Number(t.receiverLng), Number(t.receiverLat)]
  return null
})

const bannerSub = computed(() => {
  const s = task.value?.status
  if (s === 'WAIT_PICKUP') return '商家已出餐，请尽快到店取货'
  if (s === 'DELIVERING') return '正在配送中，请安全送达'
  if (s === 'DELIVERED') return '已送达，等待顾客确认'
  return '任务进行中'
})

onMounted(async () => {
  const r = await getRiderTask(route.params.orderNo)
  task.value = r.data
})

async function doPickup() {
  const r = await pickupOrder(route.params.orderNo)
  task.value = r.data
  toast('已取货，开始配送', 'success')
}

async function doDeliver() {
  const r = await deliverOrder(route.params.orderNo)
  task.value = r.data
  toast('已送达，收入已结算', 'success')
}
</script>

<style scoped>
.task-no { font-size: var(--fs-aux); color: rgba(255, 255, 255, .85); }

.banner {
  padding: 14px var(--sp-page);
  background: linear-gradient(135deg, #2A3040, #171B26);
  color: #fff;
}
.banner__status { font-size: var(--fs-title-lg); font-weight: var(--fw-title); }
.banner__sub { font-size: var(--fs-aux); opacity: .85; margin-top: 4px; }

.map {
  position: relative; height: 150px; margin: var(--sp-page) var(--sp-page) 0;
  border-radius: var(--r-card); overflow: hidden;
  background: linear-gradient(135deg, #E9F2FF, #D6E4F5);
}
.map__route {
  position: absolute; left: 24px; top: 30px; right: 24px; bottom: 30px;
  border: 2px dashed #9DBBE6; border-radius: 40%;
}
.map__pin {
  position: absolute; width: 26px; height: 26px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
}
.map__pin--shop { left: 18px; top: 24px; background: var(--c-info); }
.map__pin--user { right: 18px; bottom: 24px; background: var(--c-price); }
.map__tip {
  position: absolute; left: 0; right: 0; bottom: 0;
  font-size: 10px; color: #fff; background: rgba(23, 24, 26, .42);
  padding: 4px 8px; text-align: center;
}

.goods {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 0; font-size: var(--fs-body);
}
</style>
