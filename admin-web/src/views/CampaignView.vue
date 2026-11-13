<template>
  <div>
    <div class="page-head">
      <div><h2>运营活动</h2><div class="sub">营销与会员 · 满减 / 折扣 / 新人礼 / 免配送费（演示数据）</div></div>
      <button class="btn btn--primary" @click="openCreate">+ 新建活动</button>
    </div>

    <div class="card">
      <table class="tbl">
        <thead><tr><th>活动名称</th><th>类型</th><th>门槛 / 优惠</th><th>活动时间</th><th>参与门店</th><th>状态</th><th style="width:160px">操作</th></tr></thead>
        <tbody>
          <tr v-for="c in campaigns" :key="c.id">
            <td class="text-strong">{{ c.name }}</td>
            <td>{{ typeLabel(c.type) }}</td>
            <td>{{ c.threshold ? '满 ¥' + c.threshold : '无门槛' }} → {{ c.benefit }}</td>
            <td class="muted">{{ c.start }} ~ {{ c.end }}</td>
            <td class="num">{{ c.shops }} 家</td>
            <td><span class="tag" :class="c.online ? 'tag--success' : 'tag--warn'">{{ c.online ? '进行中' : '已下线' }}</span></td>
            <td>
              <button class="btn btn--sm" @click="openEdit(c)">编辑</button>
              <button class="btn btn--sm" :class="c.online ? 'btn--danger' : 'btn--success'" @click="toggle(c)">{{ c.online ? '下线' : '上线' }}</button>
            </td>
          </tr>
          <tr v-if="!campaigns.length"><td colspan="7" class="empty">暂无活动</td></tr>
        </tbody>
      </table>
    </div>

    <div v-if="showDialog" class="mask" @click.self="showDialog = false">
      <div class="drawer">
        <div class="drawer__head"><b>{{ editing ? '编辑活动' : '新建活动' }}</b><button class="btn btn--ghost btn--sm" @click="showDialog = false">关闭</button></div>
        <div class="drawer__body">
          <div class="kv">
            <dt>活动名称</dt><dd><input class="input" v-model="form.name" placeholder="如：新客立减 5 元" /></dd>
            <dt>类型</dt><dd>
              <select class="select" v-model="form.type">
                <option value="FULL_REDUCE">满减</option>
                <option value="DISCOUNT">折扣</option>
                <option value="NEW_USER">新人礼</option>
                <option value="FREE_DELIVERY">免配送费</option>
              </select>
            </dd>
            <dt>门槛(元)</dt><dd><input class="input" type="number" v-model="form.threshold" placeholder="0 表示无门槛" /></dd>
            <dt>优惠</dt><dd><input class="input" v-model="form.benefit" placeholder="如：减 5 / 8.5 折" /></dd>
            <dt>开始</dt><dd><input class="input" type="date" v-model="form.start" /></dd>
            <dt>结束</dt><dd><input class="input" type="date" v-model="form.end" /></dd>
            <dt>参与门店</dt><dd><input class="input" type="number" v-model="form.shops" /></dd>
          </div>
          <button class="btn btn--primary btn-block mt-16" @click="save">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const campaigns = ref([
  { id: 1, name: '新客立减 5 元', type: 'NEW_USER', threshold: 0, benefit: '减 5', start: '2026-10-20', end: '2026-11-20', shops: 120, online: true },
  { id: 2, name: '满 30 减 8', type: 'FULL_REDUCE', threshold: 30, benefit: '减 8', start: '2026-10-01', end: '2026-12-31', shops: 86, online: true },
  { id: 3, name: '下午茶 8.5 折', type: 'DISCOUNT', threshold: 0, benefit: '8.5 折', start: '2026-10-15', end: '2026-11-15', shops: 54, online: false }
])
const showDialog = ref(false)
const editing = ref(false)
const form = ref({ name: '', type: 'FULL_REDUCE', threshold: 0, benefit: '', start: '', end: '', shops: 0 })
let editingId = null

function typeLabel(t) { return ({ FULL_REDUCE: '满减', DISCOUNT: '折扣', NEW_USER: '新人礼', FREE_DELIVERY: '免配送费' })[t] || t }
function openCreate() { editing.value = false; editingId = null; form.value = { name: '', type: 'FULL_REDUCE', threshold: 0, benefit: '', start: '', end: '', shops: 0 }; showDialog.value = true }
function openEdit(c) { editing.value = true; editingId = c.id; form.value = { ...c }; showDialog.value = true }
function save() {
  if (editingId) {
    const i = campaigns.value.findIndex(x => x.id === editingId)
    if (i >= 0) campaigns.value[i] = { ...campaigns.value[i], ...form.value }
  } else {
    campaigns.value.unshift({ id: Date.now(), online: true, ...form.value })
  }
  showDialog.value = false
}
function toggle(c) { c.online = !c.online }
</script>
