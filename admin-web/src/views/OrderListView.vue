<template>
  <div>
    <div class="page-head">
      <div><h2>订单管理</h2><div class="sub">实时订单列表 · 状态流转 · 异常与取消处理</div></div>
    </div>

    <div class="card">
      <div class="toolbar">
        <select class="select" v-model="status">
          <option v-for="s in statusOptions" :key="s.code" :value="s.code">{{ s.label }}</option>
        </select>
        <input class="input search" v-model="keyword" placeholder="订单号 / 门店名" @keyup.enter="search" />
        <button class="btn btn--primary btn--sm" @click="search">查询</button>
        <button class="btn btn--sm" @click="reset">重置</button>
        <span class="muted" style="margin-left:auto">共 {{ total }} 单</span>
      </div>

      <div v-if="error" class="notice">{{ error }}</div>

      <table class="tbl">
        <thead>
          <tr>
            <th>订单号</th><th>门店</th><th>履约</th><th>状态</th>
            <th>实付(元)</th><th>件数</th><th>下单时间</th><th style="width:160px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="o in list" :key="o.orderNo">
            <td class="num">{{ o.orderNo }}</td>
            <td class="ellipsis" style="max-width:160px">{{ o.shopName }}</td>
            <td>{{ o.fulfillType === 'PICKUP' ? '到店自取' : '平台配送' }}</td>
            <td><span class="tag" :class="statusClass(o.status)">{{ o.statusLabel }}</span></td>
            <td class="num text-price">{{ yuan(o.payAmount) }}</td>
            <td class="num">{{ o.totalQuantity }}</td>
            <td class="num muted">{{ fmtTime(o.createdAt) }}</td>
            <td>
              <button class="btn btn--sm" @click="openDetail(o.orderNo)">详情</button>
            </td>
          </tr>
          <tr v-if="!list.length"><td colspan="8" class="empty">暂无订单</td></tr>
        </tbody>
      </table>

      <div class="row row--between mt-12" v-if="total > pageSize">
        <span class="muted">第 {{ pageNum }} / {{ Math.ceil(total / pageSize) }} 页</span>
        <div class="row" style="gap:6px">
          <button class="btn btn--sm" :disabled="pageNum <= 1" @click="pageNum--; search()">上一页</button>
          <button class="btn btn--sm" :disabled="pageNum >= Math.ceil(total / pageSize)" @click="pageNum++; search()">下一页</button>
        </div>
      </div>
    </div>

    <!-- 订单详情抽屉 -->
    <div class="mask" v-if="detail" @click.self="closeDetail">
      <div class="drawer">
        <div class="drawer__head">
          <div>
            <div class="text-strong">订单详情</div>
            <div class="muted num" style="font-size:12px">{{ detail.orderNo }}</div>
          </div>
          <button class="btn btn--ghost btn--sm" @click="closeDetail"><Icon name="close" :size="16" /></button>
        </div>
        <div class="drawer__body">
          <div class="row row--between mb-12">
            <span class="tag" :class="statusClass(detail.status)">{{ detail.statusLabel }}</span>
            <span class="muted">{{ detail.fulfillType === 'PICKUP' ? '到店自取' : '平台配送' }}</span>
          </div>

          <dl class="kv">
            <dt>收货人</dt><dd>{{ detail.receiverName }} · {{ detail.receiverPhone }}</dd>
            <dt>收货地址</dt><dd>{{ detail.receiverAddress || '—' }}</dd>
            <dt>备注</dt><dd>{{ detail.remark || '—' }}</dd>
            <dt>取餐码</dt><dd>{{ detail.pickupCode || '—' }}</dd>
          </dl>

          <div class="divider" />
          <div class="text-strong mb-12">商品明细</div>
          <div v-for="it in detail.items" :key="it.dishId" class="row row--between" style="padding:6px 0">
            <span>{{ it.dishName }} <span class="muted" v-if="it.specText">（{{ it.specText }}）</span></span>
            <span class="num">¥{{ yuan(it.unitPrice) }} × {{ it.quantity }}</span>
          </div>
          <div class="divider" />
          <dl class="kv">
            <dt>商品小计</dt><dd class="num">¥{{ yuan(detail.itemAmount) }}</dd>
            <dt>打包费</dt><dd class="num">¥{{ yuan(detail.packFee) }}</dd>
            <dt>配送费</dt><dd class="num">¥{{ yuan(detail.deliveryFee) }}</dd>
            <dt>优惠</dt><dd class="num">-¥{{ yuan(detail.discountAmount) }}</dd>
            <dt>实付</dt><dd class="num text-price">¥{{ yuan(detail.payAmount) }}</dd>
          </dl>

          <div class="divider" />
          <div class="text-strong mb-12">状态时间轴</div>
          <div class="timeline">
            <div v-for="(t, i) in detail.timeline" :key="i" class="tl-item">
              <div class="text-strong">{{ t.toLabel }} <span class="tag tag--info" style="margin-left:6px">{{ operatorLabel(t.operator) }}</span></div>
              <div class="muted">{{ t.remark }}</div>
              <div class="tl-time num">{{ fmtTime(t.createdAt) }}</div>
            </div>
            <div v-if="!detail.timeline || !detail.timeline.length" class="muted">暂无流水</div>
          </div>

          <div class="divider" />
          <div class="row" style="gap:8px; flex-wrap:wrap">
            <button class="btn btn--primary btn--sm" :disabled="isFinal(detail.status) || !canManage" @click="onAdvance">推进至下一状态</button>
            <button v-if="canManage" class="btn btn--danger btn--sm" :disabled="isFinal(detail.status)" @click="onException">标记异常</button>
            <button v-if="canManage" class="btn btn--sm" :disabled="isFinal(detail.status)" @click="onCancel">取消订单</button>
          </div>
          <div v-if="!canManage" class="notice">当前角色（客服）仅可查看，无操作权限</div>
          <div v-else-if="isFinal(detail.status)" class="muted mt-8">当前订单已是终态，不可再操作</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import Icon from '../components/Icon.vue'
import { adminApi } from '../api/admin'
import { canManage } from '../store/auth'

const statusOptions = [
  { code: 'ALL', label: '全部状态' },
  { code: 'UNPAID', label: '待支付' },
  { code: 'WAIT_ACCEPT', label: '待接单' },
  { code: 'PREPARING', label: '备餐中' },
  { code: 'WAIT_PICKUP', label: '待取货' },
  { code: 'DELIVERING', label: '配送中' },
  { code: 'DELIVERED', label: '已送达' },
  { code: 'WAIT_MEAL', label: '待取餐' },
  { code: 'PICKED', label: '已取餐' },
  { code: 'COMPLETED', label: '已完成' },
  { code: 'CANCELED', label: '已取消' },
  { code: 'REFUNDED', label: '已退款' },
  { code: 'PART_REFUNDED', label: '部分退款' },
  { code: 'EXCEPTION', label: '异常' }
]

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref('ALL')
const keyword = ref('')

const detail = ref(null)
const detailNo = ref('')
const error = ref('')

function yuan(v) { return ((v || 0) / 100).toFixed(2) }
function fmtTime(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '-' }
// 终态口径与后端 OrderStateMachine.isFinal 保持一致
function isFinal(s) { return ['COMPLETED', 'CANCELED', 'REFUNDED'].includes(s) }

function statusClass(s) {
  return ({
    UNPAID: 'tag--info', WAIT_ACCEPT: 'tag--info', PREPARING: 'tag--warn',
    WAIT_PICKUP: 'tag--warn', DELIVERING: 'tag--primary', DELIVERED: 'tag--primary',
    WAIT_MEAL: 'tag--warn', PICKED: 'tag--primary', COMPLETED: 'tag--success',
    CANCELED: 'tag--info', REFUNDED: 'tag--info', PART_REFUNDED: 'tag--warn',
    EXCEPTION: 'tag--danger'
  })[s] || 'tag'
}
function operatorLabel(o) {
  return ({ USER: '用户', MERCHANT: '商家', RIDER: '骑手', ADMIN: '平台', SYSTEM: '系统' })[o] || o
}

async function search() {
  error.value = ''
  try {
    const r = await adminApi.orders({ status: status.value, keyword: keyword.value, pageNum: pageNum.value, pageSize: pageSize.value })
    list.value = r.data?.list || []
    total.value = r.data?.total || 0
  } catch (e) {
    error.value = `订单加载失败：${e.message}`
    list.value = []
    total.value = 0
  }
}
function reset() { status.value = 'ALL'; keyword.value = ''; pageNum.value = 1; search() }

async function openDetail(no) {
  error.value = ''
  detailNo.value = no
  try {
    const r = await adminApi.orderDetail(no)
    detail.value = r.data
  } catch (e) {
    error.value = `订单详情加载失败：${e.message}`
    detail.value = null
  }
}
function closeDetail() { detail.value = null }

async function refreshOrder() {
  await openDetail(detailNo.value)
  await search()
}
async function onAdvance() {
  try {
    await adminApi.advance(detailNo.value)
    await refreshOrder()
  } catch (e) {
    error.value = e.message
  }
}
async function onException() {
  const note = window.prompt('异常说明（可为空）')
  if (note === null) return
  try {
    await adminApi.markException(detailNo.value, note)
    await refreshOrder()
  } catch (e) {
    error.value = e.message
  }
}
async function onCancel() {
  const reason = window.prompt('取消原因')
  if (reason === null) return
  try {
    await adminApi.cancelOrder(detailNo.value, reason)
    await refreshOrder()
  } catch (e) {
    error.value = e.message
  }
}

function onKeydown(e) {
  if (e.key === 'Escape' && detail.value) closeDetail()
}

onMounted(() => {
  search()
  window.addEventListener('keydown', onKeydown)
})
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>
