<template>
  <div class="page page--notab">
    <NavBar title="发表评价" />

    <div v-if="loading" style="padding: 12px">
      <div class="skeleton mt-12" style="height: 150px; border-radius: 10px" />
    </div>

    <div v-else class="page-body">
      <!-- 已评价：只读 -->
      <div v-if="existing" class="card">
        <div class="card__title">
          <span>我的评价</span>
          <small>{{ existing.shopName }}</small>
        </div>
        <div class="stars">
          <Icon
            v-for="i in 5"
            :key="i"
            name="star"
            :size="22"
            :color="i <= existing.rating ? '#FF9500' : '#DADDE2'"
          />
        </div>
        <div class="text-desc mt-8">{{ existing.content || '（无文字评价）' }}</div>
        <div v-if="existing.images && existing.images.length" class="imgs mt-8">
          <img v-for="(u, i) in existing.images" :key="i" :src="u" class="thumb" alt="评价图" />
        </div>
        <div v-if="existing.reply" class="reply mt-8">商家回复：{{ existing.reply }}</div>
      </div>

      <template v-else>
        <div class="card">
          <div class="card__title">
            <span>餐品评分</span>
            <small>{{ rating }} 星 · {{ ratingText }}</small>
          </div>
          <div class="stars">
            <button v-for="i in 5" :key="i" type="button" class="star-btn" @click="rating = i">
              <Icon name="star" :size="32" :color="i <= rating ? '#FF9500' : '#DADDE2'" />
            </button>
          </div>
          <div class="divider" />
          <textarea
            v-model="content"
            class="rv-textarea"
            rows="4"
            maxlength="500"
            placeholder="说说这次的用餐体验：口味、包装、配送速度…"
          />
          <div class="text-aux" style="text-align: right">{{ content.length }}/500</div>
        </div>

        <div class="card mt-12">
          <div class="card__title">
            <span>晒图</span>
            <small>{{ images.length }}/9</small>
          </div>
          <div class="imgs">
            <div v-for="(it, i) in images" :key="it.key" class="thumb-wrap">
              <img :src="it.url" class="thumb" alt="晒图" />
              <button type="button" class="thumb-del" @click="removeImage(i)">
                <Icon name="close" :size="10" color="#fff" />
              </button>
            </div>
            <button
              v-if="images.length < 9"
              type="button"
              class="thumb-add"
              :disabled="uploading"
              @click="pick"
            >
              <span v-if="uploading" class="text-aux">上传中…</span>
              <Icon v-else name="plus" :size="22" color="#969799" />
            </button>
          </div>
          <input ref="fileInput" type="file" accept="image/*" class="hidden-file" @change="onPick" />
        </div>

        <button class="btn btn--primary btn--block mt-16" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中…' : '提交评价' }}
        </button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import Icon from '../components/Icon.vue'
import { createReview, getOrderReview, uploadReviewImage } from '../api'
import { toast } from '../api/http'
import { clearDraft, loadDraft, saveDraft } from '../utils/draft'

const route = useRoute()
const router = useRouter()
const orderNo = route.params.orderNo
/** 草稿键：按订单隔离，避免不同订单的评价草稿串味 */
const DRAFT_KEY = `review-${orderNo}`

const loading = ref(true)
const existing = ref(null)
const rating = ref(5)
const content = ref('')
/** [{ key, url }]：key 提交给后端，url 用于展示 */
const images = ref([])
const uploading = ref(false)
const submitting = ref(false)
const fileInput = ref(null)

const RATING_TEXT = { 1: '很不满意', 2: '不太满意', 3: '一般', 4: '满意', 5: '非常满意' }
const ratingText = computed(() => RATING_TEXT[rating.value] || '')

onMounted(async () => {
  try {
    const r = await getOrderReview(orderNo)
    existing.value = r.data || null
  } catch (e) {
    existing.value = null
  } finally {
    loading.value = false
    restoreDraft()
  }
})

/**
 * 评价内容变化即暂存。
 *
 * 场景：写评价写到一半登录态失效被跳到登录页；登录失败或暂时放弃登录后回到本页，
 * 评分/文字/已上传图片都还在，不用重写（见 restoreDraft）。
 */
watch([rating, content, images], () => {
  if (loading.value || existing.value) return
  saveDraft(DRAFT_KEY, {
    rating: rating.value,
    content: content.value,
    images: images.value.map(x => ({ key: x.key, url: x.url }))
  }, route.path)
}, { deep: true })

function restoreDraft() {
  if (existing.value) return
  const d = loadDraft(DRAFT_KEY)
  if (!d) return
  if (d.rating) rating.value = d.rating
  if (typeof d.content === 'string') content.value = d.content
  if (Array.isArray(d.images)) images.value = d.images.filter(x => x && x.key && x.url)
  if (content.value || images.value.length || d.rating) {
    toast('已恢复上次未提交的评价内容', 'info')
  }
}

function pick() {
  if (uploading.value) return
  fileInput.value?.click()
}

async function onPick(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  uploading.value = true
  try {
    const r = await uploadReviewImage(file)
    const d = r.data || {}
    if (d.objectKey && d.url) images.value.push({ key: d.objectKey, url: d.url })
  } catch (err) {
    if (err && err.code === 11001) return
    toast(err?.message || '图片上传失败，请重试', 'error')
  } finally {
    uploading.value = false
  }
}

function removeImage(i) {
  images.value.splice(i, 1)
}

async function submit() {
  if (submitting.value) return
  submitting.value = true
  try {
    await createReview({
      orderNo,
      rating: rating.value,
      content: content.value,
      images: images.value.map(x => x.key)
    })
    clearDraft(DRAFT_KEY)
    toast('评价提交成功', 'success')
    router.replace(`/order/${orderNo}`)
  } catch (e) {
    /* 拦截器已提示（如已评价 / 未完成）；失败时草稿保留，登录后回来可继续 */
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.stars {
  display: flex;
  gap: 6px;
  margin: 8px 0;
}
.star-btn {
  background: none;
  border: 0;
  padding: 0;
  line-height: 0;
}
.rv-textarea {
  width: 100%;
  border: 0;
  background: var(--bg-sunken, #F4F5F7);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  color: var(--t-1, #17181A);
  resize: none;
  margin-top: 8px;
}
.imgs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.thumb-wrap {
  position: relative;
}
.thumb {
  width: 76px;
  height: 76px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #ebedf0;
  background: #f7f8fa;
}
.thumb-del {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 0;
  background: rgba(0, 0, 0, .6);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}
.thumb-add {
  width: 76px;
  height: 76px;
  border-radius: 8px;
  border: 1px dashed #dcdee0;
  background: #f7f8fa;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}
.reply {
  background: #fff7ed;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 13px;
  color: #a15c00;
}
.hidden-file {
  display: none;
}
</style>
