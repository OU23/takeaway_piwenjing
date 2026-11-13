<template>
  <div class="page page--notab">
    <NavBar title="我的评价" />
    <div class="page-body">
      <div v-if="loading">
        <div v-for="i in 3" :key="i" class="skeleton mt-12" style="height: 110px; border-radius: 10px" />
      </div>

      <template v-else>
        <div v-if="!list.length" class="empty">
          <Icon name="star" :size="30" color="#C4C7CE" />
          <div class="text-aux mt-8">还没有评价，去订单里评价一下吧</div>
        </div>

        <div v-for="r in list" :key="r.id" class="card mt-12">
          <div class="row row--between">
            <span class="text-strong">{{ r.shopName }}</span>
            <span class="text-aux">{{ fmtTime(r.createdAt) }}</span>
          </div>
          <div class="stars mt-4">
            <Icon
              v-for="i in 5"
              :key="i"
              name="star"
              :size="16"
              :color="i <= r.rating ? '#FF9500' : '#DADDE2'"
            />
          </div>
          <div v-if="r.content" class="text-desc mt-8">{{ r.content }}</div>
          <div v-if="r.images && r.images.length" class="imgs mt-8">
            <img v-for="(u, i) in r.images" :key="i" :src="u" class="thumb" alt="评价图" />
          </div>
          <div v-if="r.reply" class="reply mt-8">商家回复：{{ r.reply }}</div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import NavBar from '../components/NavBar.vue'
import Icon from '../components/Icon.vue'
import { listMyReviews } from '../api'
import { fmtTime } from '../utils/format'

const loading = ref(true)
const list = ref([])

onMounted(async () => {
  try {
    const r = await listMyReviews()
    list.value = r.data || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stars {
  display: flex;
  gap: 3px;
}
.imgs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.thumb {
  width: 76px;
  height: 76px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #ebedf0;
  background: #f7f8fa;
}
.reply {
  background: #fff7ed;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 13px;
  color: #a15c00;
}
.empty {
  text-align: center;
  padding: 60px 0;
}
</style>
