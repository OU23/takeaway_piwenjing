<template>
  <div>
    <div class="page-head"><div><h2>权限角色</h2><div class="sub">后台角色与权限矩阵（演示数据）</div></div></div>

    <div class="card">
      <div class="card__title"><span>角色列表</span></div>
      <table class="tbl">
        <thead><tr><th>角色</th><th>描述</th><th>可访问模块</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="r in roles" :key="r.key">
            <td class="text-strong">{{ r.name }}</td>
            <td class="muted">{{ r.desc }}</td>
            <td>{{ r.modules.join('、') }}</td>
            <td><button class="btn btn--sm" @click="view(r)">查看权限</button></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="active" class="card mt-16">
      <div class="card__title"><span>{{ active.name }} · 权限矩阵</span><small>✓ 表示拥有该权限</small></div>
      <table class="tbl">
        <thead><tr><th>功能模块</th><th>查看</th><th>操作（写）</th></tr></thead>
        <tbody>
          <tr v-for="m in modules" :key="m.key">
            <td class="text-strong">{{ m.label }}</td>
            <td><span class="dot" :class="active.access[m.key] && active.access[m.key].view ? 'dot--on' : 'dot--off'" />{{ active.access[m.key] && active.access[m.key].view ? '可查看' : '无' }}</td>
            <td><span class="dot" :class="active.access[m.key] && active.access[m.key].write ? 'dot--on' : 'dot--off'" />{{ active.access[m.key] && active.access[m.key].write ? '可操作' : '只读' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const modules = [
  { key: 'overview', label: '总览 / 看板' },
  { key: 'merchant', label: '商家管理 / 审核' },
  { key: 'order', label: '订单管理' },
  { key: 'dispatch', label: '骑手调度' },
  { key: 'finance', label: '财务结算' },
  { key: 'user', label: '用户 / 客诉' },
  { key: 'campaign', label: '运营活动' },
  { key: 'category', label: '品类管理' },
  { key: 'role', label: '权限角色' }
]

function buildAccess(superAll, denyWrite, readOnly) {
  const a = {}
  for (const m of modules) {
    const denied = denyWrite.includes(m.key) || readOnly.includes(m.key)
    a[m.key] = { view: true, write: superAll || !denied }
    if (readOnly.includes(m.key)) a[m.key].write = false
  }
  return a
}

const roles = ref([
  { key: 'SUPER_ADMIN', name: '超级管理员', desc: '平台最高权限', modules: ['全部'], access: buildAccess(true, [], []) },
  { key: 'OPERATOR', name: '运营', desc: '日常运营与审核', modules: ['总览', '商家', '订单', '调度', '财务', '运营', '品类'], access: buildAccess(false, ['role'], []) },
  { key: 'CS', name: '客服', desc: '用户与客诉处理', modules: ['总览', '订单', '用户'], access: buildAccess(false, ['merchant', 'finance', 'dispatch', 'campaign', 'category', 'role'], ['order']) }
])
const active = ref(null)
function view(r) { active.value = r }
</script>
