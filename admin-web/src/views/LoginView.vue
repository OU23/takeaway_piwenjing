<template>
  <div class="login">
    <div class="login__card">
      <div class="login__brand">
        <div class="sidebar__logo" />
        <div>
          <div class="login__title">数创星光 · 运营后台</div>
          <div class="login__sub">平台运营 · 商家管理 · 骑手调度 · 数据看板</div>
        </div>
      </div>

      <div class="login__form">
        <label class="login__label">账号</label>
        <input class="input" style="width: 100%" v-model="form.username" placeholder="请输入运营账号" />

        <label class="login__label mt-12">密码</label>
        <input class="input" style="width: 100%" type="password" v-model="form.password"
               placeholder="请输入密码" @keyup.enter="onLogin" />

        <div v-if="guideVisible" class="login-guide">
          登录成功后会自动返回刚才的页面；若登录失败，已填写的账号密码会保留，可直接重试。
        </div>

        <div v-if="err" class="notice" style="margin-top: 12px">{{ err }}</div>

        <button class="btn btn--primary btn-block mt-16" :disabled="loading" @click="onLogin">
          {{ loading ? '登录中…' : '登 录' }}
        </button>

        <!-- S6 修复：演示账号快捷填充仅在本地开发构建中存在，不进入生产包 -->
        <div v-if="isDev" class="login__demo">
          <div class="muted" style="margin-bottom: 6px">演示账号（仅本地开发，密码均为 123456）：</div>
          <div class="row" style="gap: 8px; flex-wrap: wrap">
            <button v-for="a in demo" :key="a.username" class="btn btn--sm"
                    @click="fill(a.username)">{{ a.label }}（{{ a.username }}）</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi, hasAdminRedirect, takeAdminRedirect } from '../api/admin'
import { setAuth } from '../store/auth'

// S6 修复：演示账号清单不再写死在全局状态源码里，且仅在 dev 构建可见
const isDev = import.meta.env.DEV
const demo = isDev
  ? [
      { label: '超级管理员', username: 'admin' },
      { label: '运营', username: 'operator' },
      { label: '客服', username: 'cs' }
    ]
  : []

const router = useRouter()
const form = reactive({ username: '', password: '' })
const err = ref('')
const loading = ref(false)
/** 由会话失效跳来时的说明（登录后回跳、失败不清空输入） */
const guideVisible = hasAdminRedirect()

function fill(u) {
  form.username = u
  form.password = '123456'
}

async function onLogin() {
  err.value = ''
  if (!form.username.trim() || !form.password) {
    err.value = '请输入账号和密码'
    return
  }
  loading.value = true
  try {
    const r = await adminApi.login(form.username.trim(), form.password)
    const d = r.data || {}
    setAuth({ token: d.token, name: d.name, role: d.role, roleLabel: d.roleLabel })
    // 回到被踢出前的页面；没有记录或越权时由路由守卫兜底
    router.replace(takeAdminRedirect())
  } catch (e) {
    // 登录未成功：账号密码保留在表单里，错误留在页面上便于重试
    err.value = e.message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login { height: 100%; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #2a1a10, #3a2413 55%, #4a2410); }
.login__card { width: 380px; background: #fff; border-radius: 12px; padding: 28px 30px; box-shadow: 0 18px 50px rgba(0,0,0,.35); }
.login__brand { display: flex; align-items: center; gap: 12px; margin-bottom: 22px; }
.login__title { font-size: 18px; font-weight: 700; }
.login__sub { color: var(--text-3); font-size: 12px; margin-top: 3px; }
.login__label { font-size: 13px; color: var(--text-2); }
.login__demo { margin-top: 18px; padding-top: 16px; border-top: 1px solid var(--line); }
.login-guide {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff7f2;
  border: 1px solid #ffd9c2;
  color: #a5620f;
  font-size: 12px;
  line-height: 1.6;
}
.sidebar__logo { width: 34px; height: 34px; border-radius: 9px; background: var(--grad-brand); }
</style>
