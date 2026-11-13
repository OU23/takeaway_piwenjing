<template>
  <div>
    <div class="page-head">
      <div>
        <h2>文件管理</h2>
        <div class="sub">MinIO 对象存储 · 商家资质 / 商品图 / 门店 Logo / 用户头像</div>
      </div>
    </div>

    <div class="card">
      <div class="toolbar">
        <select class="select" v-model="prefix" @change="load">
          <option value="">全部业务</option>
          <option value="dish">商品图 dish</option>
          <option value="shop">门店 Logo shop</option>
          <option value="avatar">头像 avatar</option>
          <option value="license">资质 license</option>
        </select>
        <FileUpload
          v-if="canManage"
          biz="common"
          label="上传文件"
          :max-mb="status.maxSizeMb || 5"
          @uploaded="onUploaded"
          @error="onError"
        />
        <button class="btn btn--sm" @click="load">刷新</button>
        <span class="muted" style="margin-left:auto">
          {{ status.ready ? `桶 ${status.bucket}` : '对象存储未配置' }}
        </span>
      </div>

      <div v-if="!status.ready" class="notice">
        对象存储尚未配置：请设置环境变量 <code>XG_OSS_ACCESS_KEY</code> / <code>XG_OSS_SECRET_KEY</code>，
        并确认桶 <code>{{ status.bucket || 'xingguang-oss' }}</code> 已创建。当前无法上传或读取对象。
      </div>
      <div v-if="error" class="notice">{{ error }}</div>

      <div class="meta-row">
        <span class="muted">S3 API：<code>{{ status.endpoint || '—' }}</code></span>
        <span class="muted">控制台：<code>http://192.168.35.120:9001/</code></span>
        <span v-if="!canManage" class="tag tag--warn">当前角色只读，上传需运营 / 超管</span>
      </div>

      <div v-if="loading" class="spin">加载中…</div>

      <div v-else class="grid">
        <div v-for="f in files" :key="f.objectKey" class="item">
          <div class="item__thumb">
            <img v-if="isImage(f.objectKey)" :src="f.url" :alt="f.objectKey" loading="lazy" />
            <span v-else class="item__ext">{{ extOf(f.objectKey) }}</span>
          </div>
          <div class="item__name" :title="f.objectKey">{{ shortName(f.objectKey) }}</div>
          <div class="item__meta">{{ humanSize(f.size) }} · {{ shortKey(f.objectKey) }}</div>
          <div class="item__ops">
            <button class="btn btn--sm" @click="copy(f.url)">复制链接</button>
            <button v-if="canManage" class="btn btn--sm btn--danger" @click="remove(f)">删除</button>
          </div>
        </div>
        <div v-if="!files.length" class="empty" style="grid-column:1/-1">
          暂无对象。上传一张图片试试，或检查桶名与前缀。
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import FileUpload from '../components/FileUpload.vue'
import { adminApi } from '../api/admin'
import { canManage } from '../store/auth'

const status = ref({ ready: false, bucket: '', endpoint: '', maxSizeMb: 5 })
const files = ref([])
const prefix = ref('')
const loading = ref(false)
const error = ref('')

function humanSize(n) {
  if (!n && n !== 0) return '—'
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(1)} MB`
}
function isImage(key) {
  return /\.(png|jpe?g|webp|gif)$/i.test(key || '')
}
function extOf(key) {
  const m = /\.([a-z0-9]+)$/i.exec(key || '')
  return m ? m[1].toUpperCase() : 'FILE'
}
function shortName(key) {
  const parts = String(key || '').split('/')
  return parts[parts.length - 1]
}
function shortKey(key) {
  const parts = String(key || '').split('/')
  return parts.length > 1 ? parts.slice(0, -1).join('/') : '—'
}

async function loadStatus() {
  try {
    const r = await adminApi.fileStatus()
    status.value = { ...status.value, ...(r.data || {}) }
  } catch (e) {
    error.value = `存储状态获取失败：${e.message}`
  }
}

async function load() {
  error.value = ''
  if (!status.value.ready) {
    files.value = []
    return
  }
  loading.value = true
  try {
    const r = await adminApi.files(prefix.value || undefined, 60)
    files.value = r.data || []
  } catch (e) {
    error.value = `文件列表加载失败：${e.message}`
    files.value = []
  } finally {
    loading.value = false
  }
}

function onUploaded(file) {
  if (file) files.value = [file, ...files.value]
}

function onError(msg) {
  error.value = msg
}

async function remove(f) {
  if (!window.confirm(`确认删除 ${shortName(f.objectKey)} ？该操作不可恢复。`)) return
  error.value = ''
  try {
    await adminApi.deleteFile(f.objectKey)
    files.value = files.value.filter((x) => x.objectKey !== f.objectKey)
  } catch (e) {
    error.value = e.message
  }
}

async function copy(url) {
  try {
    await navigator.clipboard.writeText(url)
    error.value = ''
  } catch {
    window.prompt('复制以下链接：', url)
  }
}

onMounted(async () => {
  await loadStatus()
  await load()
})
</script>

<style scoped>
.meta-row { display: flex; flex-wrap: wrap; gap: 16px; align-items: center; margin-bottom: 12px; font-size: 12px; }
.meta-row code { background: var(--panel-2); padding: 1px 5px; border-radius: 3px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(168px, 1fr)); gap: 12px; }
.item { border: 1px solid var(--line); border-radius: var(--radius-sm); padding: 8px; background: var(--panel-2); }
.item__thumb { height: 96px; border-radius: 6px; overflow: hidden; display: flex; align-items: center; justify-content: center; background: #fff; border: 1px solid var(--line); }
.item__thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.item__ext { font-size: 12px; color: var(--text-3); }
.item__name { margin-top: 7px; font-size: 12px; color: var(--text-1); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.item__meta { font-size: 11px; color: var(--text-3); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 2px; }
.item__ops { display: flex; gap: 6px; margin-top: 8px; }
.item__ops .btn { flex: 1; }
</style>
