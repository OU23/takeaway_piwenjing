<template>
  <div>
    <div class="page-head">
      <div>
        <h2>{{ tab === 'complaints' ? '客诉管理' : '用户管理' }}</h2>
        <div class="sub">{{ tab === 'complaints' ? '投诉工单查看与跟进（本期为只读视图）' : '账号信息 · 投诉记录（本期为只读视图）' }}</div>
      </div>
    </div>

    <div class="card">
      <div class="tabs">
        <div class="tab" :class="{ active: tab === 'users' }" @click="tab = 'users'">用户列表</div>
        <div class="tab" :class="{ active: tab === 'complaints' }" @click="tab = 'complaints'">投诉记录</div>
      </div>

      <div v-if="error" class="notice">{{ error }}</div>

      <!-- 用户列表 -->
      <div v-if="tab === 'users'">
        <div class="toolbar">
          <input class="input search" v-model="kw" placeholder="手机号 / 昵称" @keyup.enter="searchUsers" />
          <button class="btn btn--primary btn--sm" @click="searchUsers">查询</button>
          <span class="muted" style="margin-left:auto">共 {{ uTotal }} 人</span>
        </div>
        <table class="tbl">
          <thead><tr><th>用户ID</th><th>昵称</th><th>头像</th><th>手机号</th><th>会员等级</th><th>余额(元)</th><th>成长值</th><th>注册时间</th></tr></thead>
          <tbody>
            <tr v-for="u in users" :key="u.id">
              <td class="num">{{ u.id }}</td>
              <td class="text-strong">{{ u.nickname || '—' }}</td>
              <td>
                <div class="media-cell">
                  <img v-if="mediaUrl(u.avatar)" :src="mediaUrl(u.avatar)" class="mini" alt="avatar" loading="lazy" decoding="async" />
                  <FileUpload
                    v-if="canManage"
                    biz="avatar"
                    label="上传"
                    @uploaded="(f) => onAvatar(u.id, f)"
                    @error="onError"
                  />
                  <span v-else-if="!mediaUrl(u.avatar)" class="muted">—</span>
                </div>
              </td>
              <td class="num">{{ maskPhone(u.phone) }}</td>
              <td><span class="tag tag--primary">{{ levelLabel(u.memberLevel) }}</span></td>
              <td class="num">{{ (u.balance / 100).toFixed(2) }}</td>
              <td class="num">{{ u.growth }}</td>
              <td class="num muted">{{ fmt(u.createdAt) }}</td>
            </tr>
            <tr v-if="!users.length"><td colspan="8" class="empty">暂无用户</td></tr>
          </tbody>
        </table>
        <div class="row row--between mt-12" v-if="uTotal > pageSize">
          <span class="muted">第 {{ uPage }} / {{ Math.ceil(uTotal / pageSize) }} 页</span>
          <div class="row" style="gap:6px">
            <button class="btn btn--sm" :disabled="uPage <= 1" @click="uPage--; searchUsers()">上一页</button>
            <button class="btn btn--sm" :disabled="uPage >= Math.ceil(uTotal / pageSize)" @click="uPage++; searchUsers()">下一页</button>
          </div>
        </div>
      </div>

      <!-- 投诉记录 -->
      <div v-else>
        <div class="toolbar">
          <select class="select" v-model="cType">
            <option value="ALL">全部类型</option>
            <option value="REFUND">退款</option>
            <option value="DELAY">超时</option>
            <option value="QUALITY">质量</option>
            <option value="SERVICE">服务</option>
            <option value="OTHER">其他</option>
          </select>
          <select class="select" v-model="cStatus">
            <option value="ALL">全部状态</option>
            <option value="OPEN">待处理</option>
            <option value="HANDLING">处理中</option>
            <option value="RESOLVED">已解决</option>
            <option value="CLOSED">已关闭</option>
          </select>
          <button class="btn btn--primary btn--sm" @click="searchComplaints">查询</button>
          <span class="muted" style="margin-left:auto">共 {{ cTotal }} 条</span>
        </div>
        <table class="tbl">
          <thead><tr><th>投诉ID</th><th>关联订单</th><th>类型</th><th>级别</th><th>内容</th><th>状态</th><th>处理人</th></tr></thead>
          <tbody>
            <tr v-for="c in complaints" :key="c.complaint.id">
              <td class="num">{{ c.complaint.id }}</td>
              <td class="num">{{ c.complaint.orderNo || '—' }}</td>
              <td>{{ typeLabel(c.complaint.type) }}</td>
              <td><span class="tag" :class="c.complaint.level === 'URGENT' ? 'tag--danger' : 'tag--info'">{{ c.complaint.level === 'URGENT' ? '紧急' : '普通' }}</span></td>
              <td class="ellipsis" style="max-width:260px">{{ c.complaint.content }}</td>
              <td><span class="tag" :class="cStatusTag(c.complaint.status)">{{ cStatusLabel(c.complaint.status) }}</span></td>
              <td class="muted">{{ c.handlerName || '—' }}</td>
            </tr>
            <tr v-if="!complaints.length"><td colspan="7" class="empty">暂无投诉</td></tr>
          </tbody>
        </table>
        <div class="row row--between mt-12" v-if="cTotal > pageSize">
          <span class="muted">第 {{ cPage }} / {{ Math.ceil(cTotal / pageSize) }} 页</span>
          <div class="row" style="gap:6px">
            <button class="btn btn--sm" :disabled="cPage <= 1" @click="cPage--; searchComplaints()">上一页</button>
            <button class="btn btn--sm" :disabled="cPage >= Math.ceil(cTotal / pageSize)" @click="cPage++; searchComplaints()">下一页</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { adminApi } from '../api/admin'
import { canManage } from '../store/auth'
import FileUpload from '../components/FileUpload.vue'

const route = useRoute()
const tab = ref(route.name === 'complaints' ? 'complaints' : 'users')
const users = ref([])
const uTotal = ref(0)
const uPage = ref(1)
const kw = ref('')

const complaints = ref([])
const cTotal = ref(0)
const cType = ref('ALL')
const cStatus = ref('ALL')
const cPage = ref(1)
const pageSize = 10
const error = ref('')
const oss = ref({ publicBaseUrl: '', bucket: '' })

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
    /* 仅影响头像预览 */
  }
}
async function onAvatar(userId, f) {
  try {
    await adminApi.bindUserAvatar(userId, f.objectKey)
    await searchUsers()
  } catch (e) {
    error.value = e.message
  }
}

function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '-' }
function maskPhone(p) {
  if (!p) return '—'
  return String(p).replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
}
function levelLabel(v) {
  return ({ NORMAL: '普通', SILVER: '白银', GOLD: '黄金', PLATINUM: '铂金', BLACKGOLD: '黑金', BLACKDIAMOND: '黑钻' })[v] || v
}
function typeLabel(v) { return ({ REFUND: '退款', DELAY: '超时', QUALITY: '质量', SERVICE: '服务', OTHER: '其他' })[v] || v }
function cStatusLabel(v) { return ({ OPEN: '待处理', HANDLING: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' })[v] || v }
function cStatusTag(v) { return ({ OPEN: 'tag--danger', HANDLING: 'tag--warn', RESOLVED: 'tag--success', CLOSED: 'tag--info' })[v] || 'tag' }

async function searchUsers() {
  error.value = ''
  try {
    const r = await adminApi.users({ keyword: kw.value, pageNum: uPage.value, pageSize })
    users.value = r.data?.list || []
    uTotal.value = r.data?.total || 0
  } catch (e) {
    error.value = `用户加载失败：${e.message}`
    users.value = []
    uTotal.value = 0
  }
}
async function searchComplaints() {
  error.value = ''
  try {
    const r = await adminApi.complaints({ type: cType.value, status: cStatus.value, pageNum: cPage.value, pageSize })
    complaints.value = r.data?.list || []
    cTotal.value = r.data?.total || 0
  } catch (e) {
    error.value = `投诉加载失败：${e.message}`
    complaints.value = []
    cTotal.value = 0
  }
}

watch(tab, (t) => {
  if (t === 'complaints') searchComplaints()
  else searchUsers()
})
// 从菜单「客诉管理」进入时，路由复用本组件，按路由名切换 Tab
watch(() => route.name, (n) => {
  tab.value = n === 'complaints' ? 'complaints' : 'users'
})

onMounted(() => {
  loadOss()
  if (tab.value === 'complaints') searchComplaints()
  else searchUsers()
})
</script>

<style scoped>
.media-cell { display: flex; align-items: center; gap: 8px; }
.mini {
  width: 34px; height: 34px; border-radius: 50%; object-fit: cover;
  border: 1px solid var(--line); flex-shrink: 0; background: #fff;
}
</style>
