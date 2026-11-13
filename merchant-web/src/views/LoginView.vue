<template>
  <div class="login-page">
    <div class="brand">
      <img class="logo" :src="logo" alt="数创星光外卖" />
      <div class="brand-name">数创星光外卖</div>
      <div class="brand-sub">商家中心 · 登录</div>
    </div>

    <div class="login-card">
      <van-tabs v-model:active="loginType" line-width="22" title-active-color="#ff6010">
        <van-tab title="账号登录" />
        <van-tab title="短信登录" />
        <van-tab title="扫码登录" />
      </van-tabs>

      <div v-if="loginType === 0" class="form">
        <van-field
          v-model="username"
          label="账号"
          placeholder="商家账号，如 m101"
          left-icon="user-o"
          clearable
        />
        <van-field
          v-model="password"
          :type="showPwd ? 'text' : 'password'"
          label="密码"
          placeholder="默认 123456"
          left-icon="lock"
          :right-icon="showPwd ? 'eye-o' : 'closed-eye'"
          @click-right-icon="showPwd = !showPwd"
        />
        <div class="demo-tip" @click="fillDemo">
          演示账号：m101~m107 / 123456（点击填入 m101）
        </div>
        <div v-if="errorMsg" class="login-error" role="alert">{{ errorMsg }}</div>
        <van-button type="primary" block round :loading="loading" class="login-btn" @click="doLogin">
          登 录
        </van-button>
      </div>

      <div v-else-if="loginType === 1" class="form">
        <van-field
          v-model="phone"
          label="手机号"
          placeholder="注册时填写的手机号"
          left-icon="phone-o"
          clearable
        />
        <van-field v-model="code" label="验证码" placeholder="请输入验证码" left-icon="comment-o">
          <template #button>
            <van-button size="small" type="primary" plain :disabled="countdown > 0" @click="sendCode">
              {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
            </van-button>
          </template>
        </van-field>
        <van-button type="primary" block round class="login-btn" @click="doSmsLogin">登 录</van-button>
      </div>

      <div v-else class="form scan">
        <div class="qr-box">📱<span>二维码占位</span></div>
        <div class="scan-tip">请使用「数创星光商家 App」扫码登录</div>
        <van-button type="primary" block round plain class="login-btn" @click="doScanTip">我已扫码</van-button>
      </div>

      <!-- 由登录态失效跳转而来：说明回跳与失败影响，避免用户重复找页面 -->
      <div v-if="guideVisible" class="login-guide">
        登录成功后会自动返回刚才的页面。若登录失败，已填写的账号密码会保留，可直接重试。
      </div>
    </div>

    <div class="agreement">登录即代表同意《商家服务协议》《隐私政策》</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import logo from '../assets/logo.svg'
import { showToast } from 'vant'
import { hasRedirect, merchantApi, setToken, takeRedirect } from '../api/merchant'

const router = useRouter()
const loginType = ref(0)
const username = ref('m101')
const password = ref('123456')
const showPwd = ref(false)
const loading = ref(false)
/** 登录失败的内联提示（账号密码不清空，修正后可直接重试） */
const errorMsg = ref('')
/** 是否由「操作中途登录态失效」跳来，决定要不要展示回跳说明 */
const guideVisible = ref(hasRedirect())
const phone = ref('')
const code = ref('')
const countdown = ref(0)

function fillDemo() {
  username.value = 'm101'
  password.value = '123456'
}

function startCountdown() {
  countdown.value = 60
  const t = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) clearInterval(t)
  }, 1000)
}

function sendCode() {
  if (!/^1\d{10}$/.test(phone.value)) {
    showToast('请输入正确的手机号')
    return
  }
  startCountdown()
  showToast('演示环境暂未开通短信，请使用账号登录')
}

function doSmsLogin() {
  showToast('演示环境暂不支持短信登录，请切换到「账号登录」')
}

function doScanTip() {
  showToast('演示环境：请使用「账号登录」')
}

async function doLogin() {
  errorMsg.value = ''
  if (!username.value.trim() || !password.value) {
    errorMsg.value = '请输入账号和密码'
    return
  }
  loading.value = true
  try {
    const data = await merchantApi.login(username.value.trim(), password.value)
    setToken(data.token)
    // 回到被踢出前的页面；没有记录时回数据看板
    router.replace(takeRedirect())
  } catch (e) {
    // 登录未成功：账号密码保留在输入框里，提示留在页面上而不是一闪而过的 toast
    errorMsg.value = e?.message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100%;
  background: #f5f6f8;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 0 24px;
}
.brand {
  width: 100%;
  text-align: center;
  padding: 48px 0 28px;
}
.logo {
  width: 60px; height: 60px; margin: 0 auto 12px; border-radius: 16px;
  display: block; object-fit: contain;
  box-shadow: 0 6px 16px rgba(255, 96, 16, .3);
}
.brand-name { font-size: 20px; font-weight: 700; color: #323233; }
.brand-sub { font-size: 13px; color: #969799; margin-top: 4px; }
.login-card {
  width: calc(100% - 32px);
  max-width: 400px;
  background: #fff;
  border-radius: 14px;
  padding: 4px 12px 18px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, .06);
}
.form { padding: 8px 4px 0; }
.login-btn { margin-top: 18px; }
.login-error {
  margin-top: 12px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #fef2f1;
  color: #d93026;
  font-size: 13px;
  line-height: 1.5;
}
.login-guide {
  margin: 10px 12px 4px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff7f2;
  border: 1px solid #ffd9c2;
  color: #a5620f;
  font-size: 12px;
  line-height: 1.6;
}
.demo-tip {
  font-size: 12px; color: #ff6010; margin: 6px 0 4px;
  background: #fff7f2; border-radius: 6px; padding: 8px 10px; cursor: pointer;
}
.scan { text-align: center; padding-top: 18px; }
.qr-box {
  width: 160px; height: 160px; margin: 0 auto 14px; border-radius: 12px;
  border: 1px dashed #ff6010; color: #ff6010; font-size: 40px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  background: #fff7f2;
}
.qr-box span { font-size: 13px; margin-top: 8px; color: #969799; }
.scan-tip { font-size: 13px; color: #646566; margin-bottom: 16px; }
.agreement {
  font-size: 12px; color: #c8c9cc; margin-top: 22px; text-align: center;
}
</style>
