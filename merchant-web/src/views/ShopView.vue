<template>
  <div v-if="shop">
    <van-cell-group inset title="门店信息">
      <van-cell title="门店名称" :value="shop.name" />
      <van-cell title="评分" :value="String(shop.rating)" />
      <van-cell title="月售" :value="shop.monthSales + ' 单'" />
      <van-cell title="地址" :label="shop.address" />
      <van-cell title="营业时间">
        <template #value>{{ shop.openTime }} - {{ shop.closeTime }}</template>
      </van-cell>
    </van-cell-group>

    <van-cell-group inset title="门店 Logo">
      <div class="logo-box">
        <img v-if="logoUrl" :src="logoUrl" class="logo-preview" alt="门店Logo" />
        <van-uploader
          v-model="logoFiles"
          :max-count="1"
          :after-read="onLogoRead"
          accept="image/*"
        />
      </div>
      <div class="logo-tip">支持 JPG / PNG / WEBP / GIF，单张 ≤ 5MB；上传后自动绑定到本门店</div>
    </van-cell-group>

    <van-cell-group inset title="门店头图">
      <div class="logo-box">
        <img v-if="headerUrl" :src="headerUrl" class="logo-preview" alt="门店头图" />
        <div v-else class="header-ph">未设置</div>
        <van-uploader
          v-model="headerFiles"
          :max-count="1"
          :after-read="onHeaderRead"
          accept="image/*"
        />
      </div>
      <div class="logo-tip">展示在用户端门店卡片顶部；支持 JPG / PNG / WEBP / GIF，单张 ≤ 5MB</div>
    </van-cell-group>

    <van-cell-group inset title="门店公告与营业时间">
      <van-field v-model="notice" label="公告" type="textarea" rows="2" autosize placeholder="写点营业提示…" />
      <van-field v-model="openTime" label="开门" placeholder="09:00" />
      <van-field v-model="closeTime" label="关门" placeholder="22:00" />
    </van-cell-group>

    <div style="padding:8px 16px 0;">
      <van-button type="primary" block round :loading="saving" @click="save">保存门店信息</van-button>
    </div>

    <van-cell-group inset title="营业开关">
      <van-cell title="当前状态">
        <template #value><van-tag :type="statusType">{{ statusLabel }}</van-tag></template>
      </van-cell>
      <van-cell title="切换营业状态">
        <template #value>
          <van-button size="small" type="primary" @click="setStatus('OPEN')">营业中</van-button>
          <van-button size="small" plain @click="setStatus('REST')">休息</van-button>
          <van-button size="small" plain type="danger" @click="setStatus('CLOSED')">打烊</van-button>
        </template>
      </van-cell>
      <van-cell label="营业开关会实时影响用户端是否可下单" />
    </van-cell-group>
  </div>
  <van-loading v-else style="margin-top:45%;" vertical>加载中…</van-loading>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { showToast } from 'vant'
import { merchantApi } from '../api/merchant'

const shop = ref(null)
const notice = ref('')
const openTime = ref('')
const closeTime = ref('')
const saving = ref(false)
const logoUrl = ref('')
const logoFiles = ref([])
const headerUrl = ref('')
const headerFiles = ref([])

const statusLabel = computed(() => ({ OPEN: '营业中', CLOSED: '打烊', REST: '休息中' }[shop.value?.status] || shop.value?.status))
const statusType = computed(() => ({ OPEN: 'success', CLOSED: 'danger', REST: 'warning' }[shop.value?.status] || 'default'))

async function load() {
  shop.value = await merchantApi.myShop()
  notice.value = shop.value.notice || ''
  openTime.value = shop.value.openTime || ''
  closeTime.value = shop.value.closeTime || ''
  // 后端已把真实 objectKey 解析成可访问 URL 直接返回；渐变占位 key 原样（空）
  const raw = shop.value.logo
  if (raw && raw.startsWith('http')) {
    logoUrl.value = raw
  } else if (raw && raw.includes('/')) {
    try {
      const u = await merchantApi.ossUrl(raw)
      logoUrl.value = u.url
    } catch (e) {
      logoUrl.value = ''
    }
  } else {
    logoUrl.value = ''
  }
  await loadHeaderUrl()
}

/** 头图与 Logo 同样三段式解析：http 直链直接用，裸 objectKey 走 ossUrl，占位/空则置空 */
async function loadHeaderUrl() {
  const raw = shop.value.headerImg
  if (raw && raw.startsWith('http')) {
    headerUrl.value = raw
  } else if (raw && raw.includes('/')) {
    try {
      headerUrl.value = (await merchantApi.ossUrl(raw)).url
    } catch (e) {
      headerUrl.value = ''
    }
  } else {
    headerUrl.value = ''
  }
}

async function onHeaderRead(item) {
  const f = Array.isArray(item) ? item[0] : item
  f.status = 'uploading'
  try {
    const r = await merchantApi.upload(f.file, 'header')
    const bind = await merchantApi.bindShopHeader(r.objectKey)
    headerUrl.value = bind.url
    f.status = 'done'
    f.url = bind.url
    showToast('门店头图已更新')
  } catch (e) {
    f.status = 'failed'
    showToast(e.message || '上传失败')
  }
}

async function onLogoRead(item) {
  const f = Array.isArray(item) ? item[0] : item
  try {
    const r = await merchantApi.upload(f.file, 'shop')
    const bind = await merchantApi.bindShopLogo(r.objectKey)
    logoUrl.value = bind.url
    f.status = 'done'
    f.url = bind.url
    showToast('门店 Logo 已更新')
  } catch (e) {
    f.status = 'failed'
    showToast(e.message || '上传失败')
  }
}

async function save() {
  saving.value = true
  try {
    shop.value = await merchantApi.updateShop({ notice: notice.value, openTime: openTime.value, closeTime: closeTime.value })
    showToast('已保存')
  } catch (e) {
    showToast(e.message)
  } finally {
    saving.value = false
  }
}

async function setStatus(s) {
  try {
    shop.value = await merchantApi.updateShop({ status: s })
    showToast('营业状态：' + statusLabel.value)
  } catch (e) {
    showToast(e.message)
  }
}

onMounted(load)
</script>

<style scoped>
.logo-box {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 4px 4px;
}
.logo-preview {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid #ebedf0;
  background: #f7f8fa;
}
.logo-tip {
  padding: 0 16px 12px;
  font-size: 12px;
  color: #969799;
}
.header-ph {
  width: 120px;
  height: 64px;
  border-radius: 10px;
  background: #f7f8fa;
  border: 1px dashed #dcdee0;
  color: #969799;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
