<template>
  <div class="page page--notab rider-login">
    <div class="rl-head">
      <div class="rl-head__badge"><Icon name="truck" :size="20" color="#fff" /></div>
      <div class="rl-head__title">骑手登录</div>
      <div class="rl-head__sub">数创星光外卖 · 配送端</div>
    </div>

    <div class="page-body" style="margin-top: -20px">
      <div class="card">
        <div class="rl-label">手机号</div>
        <input v-model="phone" class="rl-field" type="tel" maxlength="11" placeholder="请输入骑手手机号" />

        <div class="rl-label mt-12">验证码</div>
        <div class="row" style="gap: 8px">
          <input
            v-model="code"
            class="rl-field flex-1"
            maxlength="6"
            inputmode="numeric"
            placeholder="请输入验证码"
          />
          <button class="btn btn--ghost btn--sm" :disabled="counting" @click="sendCode">
            {{ counting ? countdown + 's' : '获取验证码' }}
          </button>
        </div>

        <p v-if="errorMsg" class="rl-err" role="alert">{{ errorMsg }}</p>

        <button class="btn btn--primary btn--block mt-16" :disabled="loading" @click="doLogin">
          {{ loading ? '登录中…' : '登录' }}
        </button>

        <div class="text-aux mt-12" style="text-align: center">
          演示账号：13700008888 · 验证码 8888
        </div>
      </div>

      <div class="text-aux mt-16" style="text-align: center; line-height: 1.6">
        登录即表示同意《骑手服务协议》与《隐私政策》
      </div>
    </div>
  </div>
</template>

<script setup>
import { onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import { loginRider } from '../api/rider'
import { setRiderToken } from '../utils/rider'
import { toast } from '../api/http'

const route = useRoute()
const router = useRouter()

const phone = ref('13700008888')
const code = ref('')
const loading = ref(false)
/** 登录失败的内联提示：比 toast 停留更久，且输入内容保持不动可直接重试 */
const errorMsg = ref('')
const counting = ref(false)
const countdown = ref(60)
let timer = null

function sendCode() {
  if (!/^1\d{10}$/.test(phone.value.trim())) {
    toast('请输入有效的 11 位手机号', 'error')
    return
  }
  // 演示环境验证码固定 8888，直接填入，免真实短信
  code.value = '8888'
  counting.value = true
  countdown.value = 60
  toast('演示验证码已填入：8888', 'success')
  clearInterval(timer)
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(timer)
      counting.value = false
    }
  }, 1000)
}

async function doLogin() {
  if (loading.value) return
  errorMsg.value = ''
  if (!/^1\d{10}$/.test(phone.value.trim())) {
    errorMsg.value = '请输入有效的 11 位手机号'
    return
  }
  if (!code.value.trim()) {
    errorMsg.value = '请输入验证码'
    return
  }
  loading.value = true
  try {
    const r = await loginRider({ phone: phone.value.trim(), code: code.value.trim() })
    const token = r.data?.token
    if (!token) throw new Error('登录结果缺少 token')
    setRiderToken(token)
    toast('登录成功', 'success')
    const redirect =
      typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/rider')
        ? route.query.redirect
        : '/rider'
    router.replace(redirect)
  } catch (e) {
    // 登录未成功：手机号与验证码都保留在输入框里，用户修正后可直接重试
    errorMsg.value = e?.message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.rider-login {
  min-height: 100%;
  background: var(--bg-sunken, #f4f5f7);
}
.rl-head {
  padding: calc(28px + env(safe-area-inset-top, 0px)) var(--sp-page) 46px;
  background: linear-gradient(135deg, #2a3040, #171b26);
  color: #fff;
}
.rl-head__badge {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: rgba(255, 255, 255, .16);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}
.rl-head__title {
  font-size: 22px;
  font-weight: 700;
}
.rl-head__sub {
  font-size: 13px;
  opacity: .8;
  margin-top: 4px;
}
.rl-label {
  font-size: 13px;
  color: var(--t-2, #555);
}
.rl-field {
  width: 100%;
  height: 42px;
  margin-top: 6px;
  padding: 0 12px;
  border-radius: var(--r-btn, 10px);
  border: 1px solid var(--line, #ebedf0);
  background: #fff;
  font-size: 15px;
  color: var(--t-1, #17181a);
  outline: none;
}
.rl-field::placeholder {
  color: var(--t-3, #999);
}
.rl-err {
  margin-top: 12px;
  font-size: 13px;
  line-height: 1.5;
  color: #d93026;
}
</style>
