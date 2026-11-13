<template>
  <div>
    <div class="page-head">
      <div><h2>品类管理</h2><div class="sub">平台商品类目 · 列表来自真实接口，新建/编辑/删除为演示（后端待补写接口）</div></div>
      <button class="btn btn--primary" @click="openCreate">+ 新建类目</button>
    </div>

    <div v-if="error" class="notice">{{ error }}</div>
    <div v-if="!ready && !error" class="spin">加载中…</div>
    <div v-else class="card">
      <table class="tbl">
        <thead><tr><th>ID</th><th>类目名称</th><th>图标</th><th>排序</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="c in categories" :key="c.id">
            <td class="num">{{ c.id }}</td>
            <td class="text-strong">{{ c.name }}</td>
            <td>{{ c.icon || '—' }}</td>
            <td class="num">{{ c.sort }}</td>
            <td>
              <button class="btn btn--sm" @click="openEdit(c)">编辑</button>
              <button class="btn btn--sm btn--danger" @click="remove(c)">删除</button>
            </td>
          </tr>
          <tr v-if="!categories.length"><td colspan="5" class="empty">暂无类目</td></tr>
        </tbody>
      </table>
    </div>

    <div v-if="showDialog" class="mask" @click.self="showDialog = false">
      <div class="drawer">
        <div class="drawer__head"><b>{{ editing ? '编辑类目' : '新建类目' }}</b><button class="btn btn--ghost btn--sm" @click="showDialog = false">关闭</button></div>
        <div class="drawer__body">
          <div class="kv">
            <dt>类目名称</dt><dd><input class="input" v-model="form.name" /></dd>
            <dt>图标</dt><dd><input class="input" v-model="form.icon" placeholder="emoji 或图标名" /></dd>
            <dt>排序</dt><dd><input class="input" type="number" v-model="form.sort" /></dd>
          </div>
          <button class="btn btn--primary btn-block mt-16" @click="save">保存</button>
          <p class="muted mt-12">注：当前为演示，保存仅在本地生效，未落库。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { get } from '../api/admin'

const categories = ref([])
const ready = ref(false)
const error = ref('')
const showDialog = ref(false)
const editing = ref(false)
const form = ref({ name: '', icon: '', sort: 0 })
let editingId = null

async function load() {
  error.value = ''
  try {
    const r = await get('/categories')
    categories.value = r.data || []
    ready.value = true
  } catch (e) {
    error.value = `类目加载失败：${e.message}`
  }
}
function openCreate() { editing.value = false; editingId = null; form.value = { name: '', icon: '', sort: 0 }; showDialog.value = true }
function openEdit(c) { editing.value = true; editingId = c.id; form.value = { ...c }; showDialog.value = true }
function save() {
  if (editingId) {
    const i = categories.value.findIndex(x => x.id === editingId)
    if (i >= 0) categories.value[i] = { ...categories.value[i], ...form.value }
  } else {
    categories.value.push({ id: 'new-' + Date.now(), ...form.value })
  }
  showDialog.value = false
}
function remove(c) { categories.value = categories.value.filter(x => x.id !== c.id) }
onMounted(load)
</script>
