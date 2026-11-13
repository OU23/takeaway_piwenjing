<template>
  <div class="page">
    <!-- 品牌渐变头部 -->
    <div class="mine-head">
      <div class="row" style="gap: 12px">
        <button type="button" class="mine-head__avatar" :disabled="uploadingAvatar" @click="pickAvatar">
          <img v-if="avatarUrl" class="mine-head__avatar-img" :src="avatarUrl" alt="头像" />
          <Icon v-else name="user" :size="24" color="#fff" />
          <span v-if="uploadingAvatar" class="mine-head__avatar-mask">
            <Icon name="refresh" :size="16" color="#fff" />
          </span>
          <span v-else class="mine-head__avatar-cam">换</span>
        </button>
        <input ref="fileInput" type="file" accept="image/*" class="hidden-file" @change="onPickAvatar" />
        <div class="flex-1" style="min-width: 0">
          <div class="row" style="gap: 6px">
            <span class="mine-head__name">{{ user?.nickname || '星光用户' }}</span>
            <span class="tag tag--member">{{ user?.memberLevelLabel || '普通' }}</span>
          </div>
          <div class="mine-head__phone num">{{ identity }}</div>
        </div>
        <Icon name="settings" :size="18" color="rgba(255,255,255,.9)" />
      </div>

      <div class="growth">
        <div class="growth__bar">
          <i :style="{ width: growthPercent + '%' }" />
        </div>
        <div class="growth__text">
          <span>成长值 {{ user?.growth || 0 }}</span>
          <span v-if="user?.growthToNext">
            再消费 ¥{{ (user.growthToNext / 100).toFixed(2) }} 升级{{ user.nextLevelLabel }}
          </span>
          <span v-else>{{ user?.nextLevelLabel || '' }}</span>
        </div>
      </div>
    </div>

    <div class="page-body" style="margin-top: -22px">
      <!-- 资产卡 -->
      <div class="card">
        <div class="assets">
          <div class="assets__item">
            <div class="price"><span class="price__sym">¥</span>{{ yuan(user?.balance || 0) }}</div>
            <span>钱包余额</span>
          </div>
          <div class="assets__item" @click="toast('优惠券 / 红包为 P1 营销能力（SRS 4.6）', 'info')">
            <div class="num assets__num">2</div>
            <span>优惠券</span>
          </div>
          <div class="assets__item" @click="toast('积分体系为 P1 能力（SRS 4.6）', 'info')">
            <div class="num assets__num">320</div>
            <span>我的积分</span>
          </div>
        </div>
        <div class="divider" />
        <div class="row row--between" @click="toast('签到有礼为 P1 能力（SRS 4.6）', 'info')">
          <span class="text-desc row" style="gap: 6px">
            <Icon name="gift" :size="15" color="#FF7A1A" /> 每日签到领积分
          </span>
          <span class="tag tag--promo">去签到</span>
        </div>
      </div>

      <!-- 会员权益 -->
      <div class="member-card">
        <div class="row row--between">
          <div>
            <div class="member-card__title">
              <Icon name="star" :size="13" color="#7A4A00" />
              会员权益 · {{ user?.memberLevelLabel || '普通' }}
            </div>
            <div class="member-card__sub">美团式六档等级会员（成长值 + 神券权益）</div>
          </div>
          <span class="member-card__btn" @click="toast('会员中心为 P1 能力（SRS 5.2.11）', 'info')">
            查看权益
          </span>
        </div>
        <div class="member-card__perks">
          <div><b class="num">2</b><span>无门槛神券</span></div>
          <div><b class="num">1</b><span>免配送费</span></div>
          <div><b>专属</b><span>客服通道</span></div>
          <div><b>生日</b><span>专属礼遇</span></div>
        </div>
      </div>

      <!-- 我的订单 -->
      <div class="card mt-12">
        <div class="card__title" @click="$router.push('/orders')">
          <span>我的订单</span>
          <small>全部订单 <Icon name="chevron" :size="11" /></small>
        </div>
        <div class="quick">
          <div
            v-for="q in quickOrders"
            :key="q.label"
            class="quick__item"
            @click="$router.push('/orders')"
          >
            <div class="quick__icon">
              <Icon :name="q.icon" :size="19" color="#5C6169" />
              <i v-if="counts[q.group]" class="quick__badge num">{{ counts[q.group] }}</i>
            </div>
            <span>{{ q.label }}</span>
          </div>
        </div>
      </div>

      <!-- 功能列表 -->
      <div class="card mt-12">
        <div v-for="(m, i) in menus" :key="m.label" class="list-row" :style="i === 0 ? {} : {}" @click="onMenu(m)">
          <span class="list-row__label row" style="gap: 8px">
            <Icon :name="m.icon" :size="16" color="#FF7A1A" />
            {{ m.label }}
          </span>
          <span class="list-row__value">
            <template v-if="m.value">{{ m.value }}</template>
            <Icon name="chevron" :size="13" color="#C4C7CE" />
          </span>
        </div>
      </div>

      <!-- 服务器地址（App 联调用） -->
      <div class="card mt-12" @click="openServer">
        <div class="row row--between">
          <span class="text-desc row" style="gap: 8px">
            <Icon name="nearby" :size="16" color="#12BFA0" />
            服务器地址
            <span v-if="apiBaseCustom" class="tag tag--promo">自定义</span>
          </span>
          <span class="list-row__value ellipsis" style="max-width: 56%">
            {{ apiBase }}
            <Icon name="chevron" :size="14" color="#C4C7CE" />
          </span>
        </div>
      </div>

      <!-- 平台运营后台（原型调试用） -->
      <div class="card mt-12" @click="$router.push('/admin')">
        <div class="row row--between">
          <span class="text-desc row" style="gap: 8px">
            <Icon name="chart" :size="16" color="#3A6BF0" />
            平台运营后台（只读看板 + 订单状态模拟推进）
          </span>
          <Icon name="chevron" :size="14" color="#C4C7CE" />
        </div>
      </div>

      <!-- 骑手端入口（原型演示用） -->
      <div class="card mt-12" @click="$router.push('/rider')">
        <div class="row row--between">
          <span class="text-desc row" style="gap: 8px">
            <Icon name="truck" :size="16" color="#171B26" />
            骑手端工作台（接单大厅 / 取货送达 / 收入结算）
          </span>
          <Icon name="chevron" :size="14" color="#C4C7CE" />
        </div>
      </div>

      <button type="button" class="mine-logout" @click="onLogout">退出登录</button>

      <div class="text-aux mt-16" style="text-align: center; line-height: 1.6">
        数创星光外卖 · 用户端 H5 原型<br />
        技术栈 Vue 3 + Vite + Spring Boot 3 + MySQL 8
      </div>
    </div>

    <!-- 服务器地址设置 -->
    <div v-if="serverOpen" class="mask" @click.self="serverOpen = false">
      <div class="sheet">
        <div class="sheet__head">
          <div style="min-width: 0">
            <div class="text-strong">后端服务器地址</div>
            <div class="text-aux mt-4">
              手机 App 内页面由本机 WebView 承载，不使用 dev 代理，需直连后端
            </div>
          </div>
          <button class="sheet__close" @click="serverOpen = false">
            <Icon name="close" :size="13" />
          </button>
        </div>
        <div class="sheet__body">
          <div class="text-aux mb-8">接口根地址（需包含 /api/v1）</div>
          <input
            v-model="serverInput"
            class="field"
            placeholder="http://192.168.1.100:8090/api/v1"
            autocapitalize="off"
            autocorrect="off"
            spellcheck="false"
          />
          <div class="text-aux mt-12" style="line-height: 1.65">
            · Android 模拟器访问宿主机：<b>{{ emulatorBase }}</b><br />
            · 真机需与本机同一局域网：用 <b>ipconfig</b> 查电脑 IPv4 地址后填入<br />
            · 电脑需放行入站 8090 端口，且后端已启动
          </div>
          <button class="btn btn--ghost btn--sm btn--block mt-12" @click="serverInput = emulatorBase">
            填入模拟器地址
          </button>
        </div>
        <div class="sheet__foot" style="display: flex; gap: 8px">
          <button class="btn btn--weak btn--sm flex-1" @click="resetServer">恢复默认</button>
          <button class="btn btn--ghost btn--sm flex-1" :disabled="testing" @click="testServer">
            {{ testing ? '测试中…' : '测试连接' }}
          </button>
          <button class="btn btn--primary btn--sm flex-1" @click="saveServer">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import { getOrderGroupCount, listAddresses, uploadAvatar, bindAvatar } from '../api'
import { toast } from '../api/http'
import {
  EMULATOR_BASE,
  defaultApiBase,
  getApiBase,
  healthUrl,
  isCustomApiBase,
  isNative,
  setApiBase
} from '../api/base'
import { yuan } from '../utils/format'
import { useAppStore } from '../store/app'
import { useAuthStore } from '../store/auth'
import { confirmDialog } from '../utils/confirm'

const app = useAppStore()
const auth = useAuthStore()
const router = useRouter()

/** 头部身份行：有手机号显示脱敏手机号，否则回落到邮箱 / 用户名 */
const identity = computed(() => app.user?.phoneMasked || auth.accountLabel || '未绑定手机号')

async function onLogout() {
  const ok = await confirmDialog({
    title: '退出登录',
    message: '退出后需要重新登录才能查看订单与地址。',
    confirmText: '退出',
    danger: true
  })
  if (!ok) return
  await auth.logout()
  toast('已退出登录', 'success')
  router.replace('/')
}
const counts = ref({})
const addressCount = ref(0)
const serverOpen = ref(false)
const serverInput = ref('')
const testing = ref(false)
const apiBaseVersion = ref(0)

const apiBase = computed(() => {
  apiBaseVersion.value // 依赖：设置变更后刷新显示
  return getApiBase()
})
const apiBaseCustom = computed(() => isCustomApiBase())
const emulatorBase = EMULATOR_BASE

function openServer() {
  serverInput.value = apiBaseCustom.value ? getApiBase() : ''
  serverOpen.value = true
}

function saveServer() {
  const v = serverInput.value.trim()
  if (v && !/^https?:\/\//i.test(v)) {
    toast('地址需以 http:// 或 https:// 开头', 'error')
    return
  }
  setApiBase(v)
  apiBaseVersion.value++
  serverOpen.value = false
  toast(`已切换到 ${getApiBase()}`, 'success')
  app.bootstrap()
}

function resetServer() {
  setApiBase('')
  serverInput.value = ''
  apiBaseVersion.value++
  toast(`已恢复默认：${defaultApiBase()}`, 'success')
}

async function testServer() {
  testing.value = true
  const url = healthUrl(serverInput.value.trim() || getApiBase())
  try {
    const resp = await fetch(url, { headers: { 'X-User-Id': '1' } })
    const body = await resp.json()
    if (body && body.code === 0) toast('连接成功，后端可用', 'success')
    else toast(`已连通但返回异常：${body && body.message}`, 'error')
  } catch (e) {
    toast('连接失败：地址不可达或后端未启动', 'error')
  } finally {
    testing.value = false
  }
}

const user = computed(() => app.user)

/* ---------------- 头像上传 ---------------- */
const fileInput = ref(null)
const uploadingAvatar = ref(false)
const avatarUrl = computed(() => {
  const a = user.value?.avatar
  return a && /^https?:\/\//.test(a) ? a : ''
})

function pickAvatar() {
  if (uploadingAvatar.value) return
  fileInput.value?.click()
}

async function onPickAvatar(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = '' // 清空，允许再次选择同一文件
  if (!file) return
  await doUploadAvatar(file)
}

async function doUploadAvatar(file) {
  uploadingAvatar.value = true
  try {
    const up = await uploadAvatar(file)
    const objectKey = up.data?.objectKey
    if (!objectKey) throw new Error('上传结果缺少 objectKey')
    await bindAvatar(objectKey)
    await app.loadUser() // 刷新全局用户，头像即时更新
    toast('头像已更新', 'success')
  } catch (err) {
    // 令牌失效由拦截器统一跳登录，无需重复提示
    if (err && err.code === 11001) return
    toast(err?.message || '头像上传失败，请再次点击头像重试', 'error')
  } finally {
    uploadingAvatar.value = false
  }
}

const LEVEL_MAX = 2500 // 当前等级（黄金）的成长值门槛，仅用于进度条展示

const growthPercent = computed(() => {
  const g = user.value?.growth || 0
  return Math.min(100, Math.round((g / LEVEL_MAX) * 100))
})

const quickOrders = [
  { label: '待付款', group: '待付款', icon: 'clock' },
  { label: '待接单', group: '待接单', icon: 'receipt' },
  { label: '配送中', group: '进行中', icon: 'truck' },
  { label: '待评价', group: '待评价', icon: 'star' },
  { label: '退款售后', group: '退款售后', icon: 'refresh' }
]

const menus = computed(() => [
  { label: '收货地址', icon: 'nearby', route: 'address', value: addressCount.value ? `${addressCount.value} 个` : '' },
  { label: '我的评价', icon: 'star', route: 'reviews' },
  { label: '我的收藏', icon: 'heart', hint: '收藏店铺 / 商品（SRS 5.2.9）' },
  { label: '邀请有礼', icon: 'gift', hint: '邀请有礼为 P2 选做项（SRS 4.6）' },
  { label: '客服与帮助', icon: 'service', hint: '在线客服基于自建 IM（SRS 4.10）' },
  { label: '设置', icon: 'settings', route: 'settings' }
])

onMounted(async () => {
  const [c, a] = await Promise.all([getOrderGroupCount(), listAddresses()])
  counts.value = c.data || {}
  addressCount.value = (a.data || []).length
})

function onMenu(m) {
  if (m.route) {
    router.push('/' + m.route)
    return
  }
  toast(m.hint || `${m.label}：页面在 SRS 14.1 清单中`, 'info')
}
</script>

<style scoped>
.mine-head {
  background: var(--brand-head);
  /* 顶部预留状态栏安全区，让渐变铺满刘海屏状态栏区域 */
  padding: calc(16px + env(safe-area-inset-top, 0px)) var(--sp-page) 34px;
  color: #fff;
}

.mine-head__avatar {
  position: relative;
  width: 50px;
  height: 50px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, .22);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  cursor: pointer;
}

.mine-head__avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mine-head__avatar-cam {
  position: absolute;
  right: 2px;
  bottom: 2px;
  width: 18px;
  height: 18px;
  line-height: 18px;
  text-align: center;
  border-radius: 50%;
  background: rgba(0, 0, 0, .5);
  color: #fff;
  font-size: 10px;
}

.mine-head__avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, .45);
  border-radius: 50%;
}

.hidden-file {
  display: none;
}

.mine-head__name {
  font-size: var(--fs-title-lg);
  font-weight: var(--fw-title);
}

.mine-head__phone {
  font-size: var(--fs-aux);
  opacity: .9;
  margin-top: 4px;
}

.growth {
  margin-top: 14px;
}

.growth__bar {
  height: 5px;
  border-radius: 3px;
  background: rgba(255, 255, 255, .28);
  overflow: hidden;
}

.growth__bar i {
  display: block;
  height: 100%;
  background: var(--c-member-grad);
  border-radius: 3px;
}

.growth__text {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  opacity: .92;
  margin-top: 5px;
}

/* ---------------- 资产 ---------------- */
.assets {
  display: flex;
}

.assets__item {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.assets__item span {
  font-size: var(--fs-aux);
  color: var(--t-3);
}

.assets__num {
  font-size: var(--fs-price);
  font-weight: var(--fw-title);
  color: var(--t-1);
}

/* ---------------- 会员卡 ---------------- */
.member-card {
  background: var(--c-member-grad);
  border-radius: var(--r-card);
  padding: 12px;
  margin-top: 12px;
  color: #7A4A00;
  box-shadow: var(--sh-light);
}

.member-card__title {
  font-size: var(--fs-title);
  font-weight: var(--fw-title);
  display: flex;
  align-items: center;
  gap: 4px;
}

.member-card__sub {
  font-size: var(--fs-aux);
  opacity: .82;
  margin-top: 3px;
}

.member-card__btn {
  font-size: var(--fs-aux);
  background: rgba(122, 74, 0, .14);
  border-radius: var(--r-tag);
  padding: 3px 8px;
}

.member-card__perks {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid rgba(122, 74, 0, .16);
}

.member-card__perks div {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 10px;
}

.member-card__perks b {
  font-size: var(--fs-body);
}

/* ---------------- 订单快捷 ---------------- */
.quick {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 4px;
}

.quick__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  font-size: var(--fs-aux);
  color: var(--t-2);
}

.quick__icon {
  position: relative;
}

.quick__badge {
  position: absolute;
  top: -5px;
  right: -9px;
  min-width: 15px;
  height: 15px;
  padding: 0 4px;
  border-radius: 8px;
  background: var(--c-error);
  color: #fff;
  font-size: 9px;
  line-height: 15px;
  text-align: center;
}

.field {
  width: 100%;
  height: 42px;
  padding: 0 12px;
  border-radius: var(--r-btn);
  background: var(--bg-sunken);
  font-size: var(--fs-body);
  color: var(--t-1);
}

.field::placeholder {
  color: var(--t-3);
}

/* 退出登录：整宽描边按钮，弱化到列表末尾，避免误触 */
.mine-logout {
  width: 100%;
  height: 44px;
  margin-top: 12px;
  border-radius: var(--r-btn);
  background: var(--bg-card);
  border: 1px solid var(--line-strong);
  font-size: var(--fs-body);
  font-weight: var(--fw-title);
  color: var(--c-error);
}

.mine-logout:active {
  background: rgba(229, 72, 77, .06);
}
</style>
