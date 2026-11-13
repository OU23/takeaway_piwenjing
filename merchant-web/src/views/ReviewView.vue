<template>
  <div>
    <van-cell-group v-if="stats" inset title="评价概览">
      <van-cell title="平均评分" :value="stats.avgRating + ' 分'" />
      <van-cell title="评价总数" :value="stats.total + ' 条'" />
    </van-cell-group>

    <van-loading v-if="loading" style="margin-top: 45%;" vertical>加载中…</van-loading>

    <van-empty v-else-if="!list.length" description="本店还没有评价" />

    <div v-else class="rv-list">
      <div v-for="r in list" :key="r.id" class="rv-card">
        <div class="rv-head">
          <span class="rv-name">{{ r.nickname || '匿名用户' }}</span>
          <span class="rv-time">{{ fmt(r.createdAt) }}</span>
        </div>
        <div class="rv-stars">
          {{ '★'.repeat(r.rating) }}<span class="rv-stars-off">{{ '★'.repeat(5 - r.rating) }}</span>
        </div>
        <div v-if="r.content" class="rv-content">{{ r.content }}</div>
        <div v-if="r.images && r.images.length" class="rv-imgs">
          <img v-for="(u, i) in r.images" :key="i" :src="u" class="rv-img" alt="晒图" />
        </div>
        <div v-if="r.reply" class="rv-reply">商家回复：{{ r.reply }}</div>
        <div class="rv-order">订单号 {{ r.orderNo }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { merchantApi } from '../api/merchant'

const loading = ref(true)
const list = ref([])
const stats = ref(null)

function fmt(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

onMounted(async () => {
  try {
    const d = await merchantApi.shopReviews()
    stats.value = { avgRating: d.avgRating, total: d.total }
    list.value = d.list || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.rv-list {
  padding: 8px 12px 24px;
}
.rv-card {
  background: #fff;
  border-radius: 10px;
  padding: 12px;
  margin-top: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, .04);
}
.rv-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.rv-name {
  font-weight: 600;
  color: #17181A;
}
.rv-time {
  font-size: 12px;
  color: #969799;
}
.rv-stars {
  color: #FF9500;
  font-size: 14px;
  margin-top: 4px;
  letter-spacing: 1px;
}
.rv-stars-off {
  color: #DADDE2;
}
.rv-content {
  margin-top: 6px;
  color: #323233;
  font-size: 14px;
  line-height: 1.5;
}
.rv-imgs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.rv-img {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
}
.rv-reply {
  margin-top: 8px;
  background: #fff7ed;
  color: #a15c00;
  border-radius: 8px;
  padding: 6px 8px;
  font-size: 13px;
}
.rv-order {
  margin-top: 8px;
  font-size: 12px;
  color: #969799;
}
</style>
