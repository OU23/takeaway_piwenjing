<template>
  <div>
    <div class="page-head">
      <div><h2>商家管理</h2><div class="sub">门店与商品 · 入驻审核 · 菜品上下架 · 评分监控</div></div>
    </div>

    <div class="card">
      <div class="tabs">
        <div class="tab" :class="{ active: tab === 'shops' }" @click="tab = 'shops'">门店列表</div>
        <div class="tab" :class="{ active: tab === 'apps' }" @click="tab = 'apps'">入驻审核</div>
        <div class="tab" :class="{ active: tab === 'dishes' }" @click="tab = 'dishes'">菜品上下架</div>
        <div class="tab" :class="{ active: tab === 'rating' }" @click="tab = 'rating'">评分监控</div>
      </div>

      <div v-if="error" class="notice">{{ error }}</div>

      <!-- 门店列表 -->
      <div v-if="tab === 'shops'">
        <table class="tbl">
          <thead><tr><th>门店</th><th>Logo</th><th>所属商家</th><th>抽佣</th><th>评分</th><th>商品</th><th>订单</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="s in shops" :key="s.shop.id">
              <td class="text-strong">{{ s.shop.name }}</td>
              <td>
                <div class="media-cell">
                  <img v-if="mediaUrl(s.shop.logo)" :src="mediaUrl(s.shop.logo)" class="mini" alt="logo" loading="lazy" decoding="async" />
                  <FileUpload
                    v-if="canManage"
                    biz="shop"
                    label="上传"
                    @uploaded="(f) => onShopLogo(s.shop.id, f)"
                    @error="onError"
                  />
                  <span v-else-if="!mediaUrl(s.shop.logo)" class="muted">—</span>
                </div>
              </td>
              <td class="muted">{{ s.merchantName }}</td>
              <td class="num">{{ (s.commissionRate / 100).toFixed(1) }}%</td>
              <td class="num">{{ s.shop.rating }}</td>
              <td class="num">{{ s.dishCount }}</td>
              <td class="num">{{ s.orderCount }}</td>
              <td><span class="tag" :class="s.shop.status === 'OPEN' ? 'tag--success' : 'tag--warn'">{{ s.shop.status === 'OPEN' ? '营业中' : '非营业' }}</span></td>
            </tr>
            <tr v-if="!shops.length"><td colspan="8" class="empty">暂无门店</td></tr>
          </tbody>
        </table>
      </div>

      <!-- 入驻审核 -->
      <div v-else-if="tab === 'apps'">
        <div class="toolbar">
          <select class="select" v-model="appStatus">
            <option value="ALL">全部</option>
            <option value="PENDING">待审核</option>
            <option value="APPROVED">已通过</option>
            <option value="REJECTED">已驳回</option>
          </select>
          <button class="btn btn--primary btn--sm" @click="loadApps">查询</button>
          <span v-if="!canManage" class="notice" style="margin:0">当前角色（客服）仅可查看，审核操作需运营 / 超级管理员</span>
        </div>
        <table class="tbl">
          <thead><tr><th>商家</th><th>联系人</th><th>品类</th><th>门店</th><th>地址</th><th>状态</th><th style="width:200px">操作</th></tr></thead>
          <tbody>
            <tr v-for="a in apps" :key="a.id">
              <td class="text-strong">{{ a.merchantName }}</td>
              <td class="muted">{{ a.contact }} · {{ a.phone }}</td>
              <td>{{ a.category }}</td>
              <td class="ellipsis" style="max-width:140px">{{ a.shopName }}</td>
              <td class="ellipsis" style="max-width:160px">{{ a.address }}</td>
              <td><span class="tag" :class="appTag(a.status)">{{ appLabel(a.status) }}</span></td>
              <td>
                <template v-if="a.status === 'PENDING'">
                  <button v-if="canManage" class="btn btn--success btn--sm" @click="approve(a.id)">通过</button>
                  <button v-if="canManage" class="btn btn--danger btn--sm" @click="reject(a.id)">驳回</button>
                  <span v-else class="muted">待审核</span>
                </template>
                <span v-else class="muted">{{ a.rejectReason || '—' }}</span>
              </td>
            </tr>
            <tr v-if="!apps.length"><td colspan="7" class="empty">暂无申请</td></tr>
          </tbody>
        </table>
      </div>

      <!-- 菜品上下架 -->
      <div v-else-if="tab === 'dishes'">
        <div class="toolbar">
          <select class="select" v-model="dishShop" @change="loadDishes">
            <option v-for="s in shops" :key="s.shop.id" :value="s.shop.id">{{ s.shop.name }}</option>
          </select>
          <span v-if="!canManage" class="notice" style="margin:0">当前角色（客服）仅可查看，上下架需运营 / 超级管理员</span>
        </div>
        <table class="tbl">
          <thead><tr><th>商品</th><th>图片</th><th>分类</th><th>单价(元)</th><th>月售</th><th>状态</th><th style="width:120px">操作</th></tr></thead>
          <tbody>
            <tr v-for="d in dishes" :key="d.id">
              <td class="text-strong">{{ d.name }}</td>
              <td>
                <div class="media-cell">
                  <img v-if="mediaUrl(d.image)" :src="mediaUrl(d.image)" class="mini" alt="dish" loading="lazy" decoding="async" />
                  <FileUpload
                    v-if="canManage"
                    biz="dish"
                    label="换图"
                    @uploaded="(f) => onDishImage(d.id, f)"
                    @error="onError"
                  />
                  <span v-else-if="!mediaUrl(d.image)" class="muted">—</span>
                </div>
              </td>
              <td class="muted">{{ d.categoryId }}</td>
              <td class="num">¥{{ (d.price / 100).toFixed(2) }}</td>
              <td class="num">{{ d.monthSales }}</td>
              <td><span class="tag" :class="d.status === 'ON_SALE' ? 'tag--success' : 'tag--warn'">{{ d.status === 'ON_SALE' ? '在售' : '已下架' }}</span></td>
              <td>
                <button v-if="canManage" class="btn btn--sm" @click="toggleDish(d)">{{ d.status === 'ON_SALE' ? '下架' : '上架' }}</button>
                <span v-else class="muted">—</span>
              </td>
            </tr>
            <tr v-if="!dishes.length"><td colspan="7" class="empty">该门店暂无商品</td></tr>
          </tbody>
        </table>
      </div>

      <!-- 评分监控 -->
      <div v-else>
        <table class="tbl">
          <thead><tr><th>门店</th><th>评分</th><th>月售</th><th>累计订单</th><th>关联投诉</th><th>预警</th></tr></thead>
          <tbody>
            <tr v-for="r in ratings" :key="r.shopId">
              <td class="text-strong">{{ r.shopName }}</td>
              <td class="num" :style="{ color: r.lowRating ? 'var(--danger)' : '' }">{{ r.rating }}</td>
              <td class="num">{{ r.monthSales }}</td>
              <td class="num">{{ r.orderCount }}</td>
              <td class="num">{{ r.complaintCount }}</td>
              <td>
                <span v-if="r.lowRating" class="tag tag--danger">低分预警</span>
                <span v-else class="tag tag--success">正常</span>
              </td>
            </tr>
            <tr v-if="!ratings.length"><td colspan="6" class="empty">暂无数据</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { adminApi } from '../api/admin'
import { canManage } from '../store/auth'
import FileUpload from '../components/FileUpload.vue'

const tab = ref('shops')
const shops = ref([])
const apps = ref([])
const dishes = ref([])
const ratings = ref([])
const appStatus = ref('PENDING')
const dishShop = ref(null)
const error = ref('')
const oss = ref({ publicBaseUrl: '', bucket: '' })

/** 库中存 objectKey；公开直链前缀可用时拼出可直接渲染的地址 */
function mediaUrl(key) {
  if (!key) return ''
  if (/^https?:\/\//i.test(key)) return key
  if (!oss.value.publicBaseUrl || !oss.value.bucket) return ''
  if (!/\.(png|jpe?g|webp|gif)$/i.test(key)) return ''
  return `${String(oss.value.publicBaseUrl).replace(/\/$/, '')}/${oss.value.bucket}/${key}`
}
function onError(msg) {
  error.value = msg
}
async function loadOss() {
  try {
    const r = await adminApi.fileStatus()
    oss.value = r.data || {}
  } catch {
    /* 仅影响缩略图预览，不阻塞页面 */
  }
}
async function onShopLogo(shopId, f) {
  try {
    await adminApi.bindShopLogo(shopId, f.objectKey)
    await loadShops()
  } catch (e) {
    error.value = e.message
  }
}
async function onDishImage(dishId, f) {
  try {
    await adminApi.bindDishImage(dishId, f.objectKey)
    await loadDishes()
  } catch (e) {
    error.value = e.message
  }
}

async function loadShops() {
  error.value = ''
  try {
    const r = await adminApi.shops()
    shops.value = r.data || []
    if (!dishShop.value && shops.value.length) dishShop.value = shops.value[0].shop.id
  } catch (e) {
    error.value = `门店加载失败：${e.message}`
  }
}
async function loadApps() {
  error.value = ''
  try {
    const r = await adminApi.applications(appStatus.value)
    apps.value = r.data || []
  } catch (e) {
    error.value = `入驻申请加载失败：${e.message}`
  }
}
async function loadDishes() {
  if (!dishShop.value) return
  error.value = ''
  try {
    const r = await adminApi.dishes(dishShop.value)
    dishes.value = r.data || []
  } catch (e) {
    error.value = `商品加载失败：${e.message}`
  }
}
async function loadRatings() {
  error.value = ''
  try {
    const r = await adminApi.ratingMonitor()
    ratings.value = r.data || []
  } catch (e) {
    error.value = `评分数据加载失败：${e.message}`
  }
}

watch(tab, (t) => {
  if (t === 'apps') loadApps()
  if (t === 'dishes') loadDishes()
  if (t === 'rating') loadRatings()
})

function appTag(s) { return ({ PENDING: 'tag--warn', APPROVED: 'tag--success', REJECTED: 'tag--danger' })[s] || 'tag' }
function appLabel(s) { return ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' })[s] || s }

async function approve(id) {
  try {
    await adminApi.approveApp(id)
    loadApps()
  } catch (e) {
    error.value = e.message
  }
}
async function reject(id) {
  const reason = window.prompt('驳回原因')
  if (reason === null) return
  try {
    await adminApi.rejectApp(id, reason)
    loadApps()
  } catch (e) {
    error.value = e.message
  }
}
async function toggleDish(d) {
  try {
    await adminApi.toggleDish(d.id)
    loadDishes()
  } catch (e) {
    error.value = e.message
  }
}

onMounted(() => {
  loadShops()
  loadOss()
})
</script>

<style scoped>
.media-cell { display: flex; align-items: center; gap: 8px; }
.mini {
  width: 34px; height: 34px; border-radius: 5px; object-fit: cover;
  border: 1px solid var(--line); flex-shrink: 0; background: #fff;
}
</style>
