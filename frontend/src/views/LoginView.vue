<template>
  <AuthLayout slogan="本地好味，半小时送到">
    <div class="auth__mark" aria-hidden="true">星</div>
    <h1 class="auth__title">欢迎回来</h1>
    <p class="auth__sub">登录后可查看订单、同步购物车</p>

    <!-- 登录方式切换：tablist 语义 + 左右方向键切换 -->
    <div class="auth-tabs" role="tablist" aria-label="登录方式">
      <button
        v-for="(t, i) in tabs"
        :key="t.key"
        ref="tabEls"
        type="button"
        role="tab"
        class="auth-tabs__item"
        :class="{ 'auth-tabs__item--on': mode === t.key }"
        :id="'login-tab-' + t.key"
        :aria-selected="mode === t.key"
        :aria-controls="mode === t.key ? 'login-panel-' + t.key : null"
        :tabindex="mode === t.key ? 0 : -1"
        @click="switchMode(t.key)"
        @keydown="onTabKey($event, i)"
      >
        {{ t.label }}
      </button>
    </div>

    <!-- ---------------- 密码登录 ---------------- -->
    <form
      v-if="mode === 'password'"
      id="login-panel-password"
      class="auth-form"
      role="tabpanel"
      aria-labelledby="login-tab-password"
      novalidate
      @submit.prevent="onSubmit"
    >
      <div class="afld" :class="{ 'afld--err': !!errors.account }">
        <label class="afld__label" for="login-account">用户名或邮箱</label>
        <input
          id="login-account"
          ref="accountEl"
          v-model="form.account"
          class="afld__input"
          type="text"
          name="username"
          placeholder="请输入用户名或邮箱"
          autocomplete="username"
          autocapitalize="none"
          autocorrect="off"
          spellcheck="false"
          enterkeyhint="next"
          :aria-invalid="!!errors.account"
          :aria-describedby="errors.account ? 'login-account-err' : null"
          @blur="onBlur('account')"
          @input="onInput('account')"
        />
        <p v-if="errors.account" id="login-account-err" class="afld__err" role="alert">
          {{ errors.account }}
        </p>
      </div>

      <div class="afld" :class="{ 'afld--err': !!errors.password }">
        <label class="afld__label" for="login-password">密码</label>
        <div class="afld__wrap">
          <input
            id="login-password"
            ref="passwordEl"
            v-model="form.password"
            class="afld__input afld__input--with-toggle"
            :type="showPassword ? 'text' : 'password'"
            name="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            enterkeyhint="go"
            :aria-invalid="!!errors.password"
            :aria-describedby="errors.password ? 'login-password-err' : null"
            @blur="onBlur('password')"
            @input="onInput('password')"
          />
          <button
            type="button"
            class="afld__toggle"
            :aria-label="showPassword ? '隐藏密码' : '显示密码'"
            :aria-pressed="showPassword"
            @click="showPassword = !showPassword"
          >
            {{ showPassword ? '隐藏' : '显示' }}
          </button>
        </div>
        <p v-if="errors.password" id="login-password-err" class="afld__err" role="alert">
          {{ errors.password }}
        </p>
      </div>

      <p v-if="formError" class="auth__formerr" role="alert">{{ formError }}</p>

      <button
        class="btn btn--primary btn--block auth__submit"
        type="submit"
        :disabled="auth.pending"
        :aria-busy="auth.pending"
      >
        {{ auth.pending ? '登录中…' : '登录' }}
      </button>
    </form>

    <!-- ---------------- 验证码登录 ---------------- -->
    <form
      v-else
      id="login-panel-sms"
      class="auth-form"
      role="tabpanel"
      aria-labelledby="login-tab-sms"
      novalidate
      @submit.prevent="onSubmit"
    >
      <div class="afld" :class="{ 'afld--err': !!errors.phone }">
        <label class="afld__label" for="login-phone">手机号</label>
        <input
          id="login-phone"
          ref="phoneEl"
          v-model="form.phone"
          class="afld__input"
          type="tel"
          name="tel"
          inputmode="numeric"
          maxlength="11"
          placeholder="请输入 11 位手机号"
          autocomplete="tel"
          enterkeyhint="next"
          :aria-invalid="!!errors.phone"
          :aria-describedby="errors.phone ? 'login-phone-err' : null"
          @blur="onBlur('phone')"
          @input="onInput('phone')"
        />
        <p v-if="errors.phone" id="login-phone-err" class="afld__err" role="alert">
          {{ errors.phone }}
        </p>
      </div>

      <div class="afld" :class="{ 'afld--err': !!errors.code }">
        <label class="afld__label" for="login-code">验证码</label>
        <div class="afld__wrap">
          <input
            id="login-code"
            ref="codeEl"
            v-model="form.code"
            class="afld__input afld__input--with-code"
            type="text"
            name="one-time-code"
            inputmode="numeric"
            maxlength="6"
            placeholder="请输入验证码"
            autocomplete="one-time-code"
            enterkeyhint="go"
            :aria-invalid="!!errors.code"
            :aria-describedby="errors.code ? 'login-code-err' : null"
            @blur="onBlur('code')"
            @input="onInput('code')"
          />
          <button
            type="button"
            class="afld__code-btn"
            :disabled="counting > 0 || sending"
            @click="onSendCode"
          >
            {{ counting > 0 ? counting + 's 后重发' : sending ? '发送中…' : '获取验证码' }}
          </button>
        </div>
        <p v-if="errors.code" id="login-code-err" class="afld__err" role="alert">
          {{ errors.code }}
        </p>
        <p v-else-if="devCode" class="afld__hint">
          演示环境验证码：<b>{{ devCode }}</b>（已模拟下发，不会真实发短信）
        </p>
      </div>

      <p v-if="formError" class="auth__formerr" role="alert">{{ formError }}</p>

      <button
        class="btn btn--primary btn--block auth__submit"
        type="submit"
        :disabled="auth.pending"
        :aria-busy="auth.pending"
      >
        {{ auth.pending ? '登录中…' : '登录' }}
      </button>

      <p class="afld__hint" style="margin-top: 12px">
        未注册的手机号将自动创建账号，无需单独注册
      </p>
    </form>

    <p class="auth__switch">
      还没有账号？
      <router-link :to="registerTarget">立即注册</router-link>
    </p>

    <!-- 被「操作中途登录态失效」跳转而来：说明去向与失败影响，避免用户以为修改丢了 -->
    <div v-if="redirectTarget" class="auth__guide">
      <p class="auth__guide-title">登录成功后将返回继续刚才的操作</p>
      <p class="auth__guide-sub">
        登录失败不会清空您已填写的修改，可重试；也可以先返回，稍后再登录
      </p>
      <button type="button" class="btn btn--ghost btn--sm btn--block" @click="backToOrigin">
        返回上一页继续
      </button>
    </div>

    <!-- 登录失败对话框：清晰传达「未成功 / 已暂存 / 可重试 / 可返回」四点 -->
    <div v-if="failDialog" class="dlg-mask" @click.self="closeFailDialog">
      <div class="dlg" role="alertdialog" aria-modal="true" aria-labelledby="dlg-title" aria-describedby="dlg-desc">
        <h2 id="dlg-title" class="dlg__title">本次登录未成功</h2>
        <p id="dlg-desc" class="dlg__desc">您的修改已自动暂存，不会丢失。可直接重试登录，或返回上一页查看暂存内容。</p>
        <div class="dlg__actions">
          <button type="button" class="btn btn--primary btn--block" :disabled="auth.pending" @click="retryLogin">
            {{ auth.pending ? '登录中…' : '重试登录' }}
          </button>
          <button type="button" class="btn btn--ghost btn--block" @click="backToOrigin">返回上一页继续</button>
        </div>
      </div>
    </div>

    <button type="button" class="auth__skip" @click="onSkip">
      {{ redirectTarget ? '暂不登录，返回上一页' : '暂不登录，先去逛逛' }}
    </button>

    <p class="auth__hint">
      演示账号 <b>demo</b> · 密码 <b>demo1234</b> · 验证码 <b>8888</b>
    </p>
  </AuthLayout>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthLayout from '../components/AuthLayout.vue'
import { useAuthStore } from '../store/auth'
import { toast } from '../api/http'
import { useSmsCode } from '../composables/useSmsCode'
import { safeRedirect } from '../utils/nav'
import { clearDraft, loadDraft, saveDraft } from '../utils/draft'
import {
  validateAccount,
  validatePassword,
  validatePhone,
  validateSmsCode
} from '../utils/validate'
import '../styles/auth.css'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const tabs = [
  { key: 'password', label: '密码登录' },
  { key: 'sms', label: '验证码登录' }
]
const mode = ref('password')

const form = reactive({ account: '', password: '', phone: '', code: '' })
const errors = reactive({ account: '', password: '', phone: '', code: '' })
const touched = reactive({ account: false, password: false, phone: false, code: false })
const formError = ref('')
/** 登录失败对话框可见性：告知未成功 + 暂存不丢，并给出两条出路 */
const failDialog = ref(false)
/** 登录表单自身的暂存键：失败时保存，刷新后回填 */
const LOGIN_DRAFT_KEY = 'login-form'
const showPassword = ref(false)

const accountEl = ref(null)
const passwordEl = ref(null)
const phoneEl = ref(null)
const codeEl = ref(null)
const tabEls = ref([])

/* 发码与重发倒计时（与注册页共用同一套逻辑） */
const { counting, sending, devCode, send, clearDevCode } = useSmsCode(
  () => form.phone,
  (field, message) => {
    if (field === 'phone') {
      touched.phone = true
      errors.phone = message
      phoneEl.value?.focus()
    } else {
      formError.value = message
    }
  }
)

const registerTarget = computed(() => ({
  path: '/register',
  query: route.query.redirect ? { redirect: route.query.redirect } : {}
}))

/** 跳转来源（登录成功后要回去的站内路径）；为空表示用户主动来的登录页 */
const redirectTarget = computed(() => {
  const r = route.query.redirect
  return typeof r === 'string' && r.startsWith('/') && !r.startsWith('//') ? r : ''
})

/* ----------------------- 登录失败：暂存与恢复 ----------------------- */

onMounted(() => {
  // 上次登录失败暂存的表单回填（sessionStorage，刷新页面后仍可恢复），恢复成功即清除
  // 出于安全考虑，密码/验证码不入草稿，回填后需重新输入
  const saved = loadDraft(LOGIN_DRAFT_KEY)
  if (!saved) return
  if (saved.mode === 'password' || saved.mode === 'sms') mode.value = saved.mode
  if (typeof saved.account === 'string' && saved.account) form.account = saved.account
  if (typeof saved.phone === 'string' && saved.phone) form.phone = saved.phone
  clearDraft(LOGIN_DRAFT_KEY)
})

/** 登录失败：暂存当前登录表单（连同登录方式），并弹出可操作的提示对话框 */
function markLoginFailed() {
  // 安全约定：密码与短信验证码不落盘（sessionStorage 也算存储），
  // 只暂存登录方式与账号/手机号；留在内存里的表单在重试时原样保留。
  saveDraft(LOGIN_DRAFT_KEY, {
    mode: mode.value,
    account: form.account,
    phone: form.phone
  })
  failDialog.value = true
}

/** 对话框内「重试登录」：关闭对话框并原样重新发起登录（表单内容不变） */
function retryLogin() {
  failDialog.value = false
  onSubmit()
}

/** 点遮罩关闭对话框：暂存仍在，输入框内容也不变，可继续手动编辑 */
function closeFailDialog() {
  failDialog.value = false
}

/**
 * 返回来源页。
 *
 * 中途被踢来登录的用户，返回后原页面靠草稿恢复已填内容（见 utils/draft.js），
 * 所以这里优先用浏览器返回（保留原来的浏览栈），没有栈才按 redirect 硬跳。
 */
function backToOrigin() {
  if (window.history.length > 1) router.back()
  else router.replace(safeRedirect(redirectTarget.value))
}

/** 「暂不登录」：被跳转来的回原页（草稿还在），自己点进来的回首页逛逛 */
function onSkip() {
  if (redirectTarget.value) backToOrigin()
  else router.replace('/')
}

function validateField(key) {
  switch (key) {
    case 'account':
      return validateAccount(form.account)
    case 'password':
      return validatePassword(form.password)
    case 'phone':
      return validatePhone(form.phone)
    case 'code':
      return validateSmsCode(form.code)
    default:
      return ''
  }
}

function onBlur(key) {
  touched[key] = true
  errors[key] = validateField(key)
}

function onInput(key) {
  formError.value = ''
  // 改手机号后，上一条验证码与新号码无关，提示要撤掉
  if (key === 'phone') clearDevCode()
  if (touched[key]) errors[key] = validateField(key)
}

function switchMode(next) {
  if (mode.value === next) return
  mode.value = next
  formError.value = ''
  Object.keys(errors).forEach((k) => { errors[k] = '' })
  nextTick(() => {
    if (next === 'sms') phoneEl.value?.focus()
    else accountEl.value?.focus()
  })
}

/** 左右方向键在 tab 之间移动焦点并切换，符合 tablist 键盘惯例 */
function onTabKey(e, index) {
  if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return
  e.preventDefault()
  const dir = e.key === 'ArrowRight' ? 1 : -1
  const next = (index + dir + tabs.length) % tabs.length
  switchMode(tabs[next].key)
  nextTick(() => tabEls.value?.[next]?.focus())
}

function focusFirstInvalid(keys) {
  for (const k of keys) {
    if (!errors[k]) continue
    const el = { account: accountEl, password: passwordEl, phone: phoneEl, code: codeEl }[k]
    el.value?.focus()
    return
  }
}

/* ------------------------------ 发送验证码 ------------------------------ */
async function onSendCode() {
  formError.value = ''
  touched.phone = true
  errors.phone = ''

  const ok = await send()
  if (!ok) return

  // 刚下发新码，之前「请输入验证码」的旧结论已经过期，清掉避免误导
  errors.code = ''
  codeEl.value?.focus()
}

/* -------------------------------- 提交 -------------------------------- */
async function onSubmit() {
  formError.value = ''

  if (mode.value === 'password') {
    touched.account = true
    touched.password = true
    errors.account = validateAccount(form.account)
    errors.password = validatePassword(form.password)
    if (errors.account || errors.password) {
      focusFirstInvalid(['account', 'password'])
      return
    }
    try {
      await auth.login(form.account, form.password)
      // 登录成功即视为恢复完成，清掉失败时暂存的表单
      clearDraft(LOGIN_DRAFT_KEY)
      toast(`欢迎回来，${auth.displayName}`, 'success')
      router.replace(safeRedirect(route.query.redirect))
    } catch (e) {
      formError.value = e?.message || '登录失败，请稍后重试'
      markLoginFailed()
    }
    return
  }

  touched.phone = true
  touched.code = true
  errors.phone = validatePhone(form.phone)
  errors.code = validateSmsCode(form.code)
  if (errors.phone || errors.code) {
    focusFirstInvalid(['phone', 'code'])
    return
  }
  try {
    await auth.loginSms(form.phone.trim(), form.code.trim())
    // 登录成功即视为恢复完成，清掉失败时暂存的表单
    clearDraft(LOGIN_DRAFT_KEY)
    toast(`欢迎回来，${auth.displayName}`, 'success')
    router.replace(safeRedirect(route.query.redirect))
  } catch (e) {
    // 11007 过期 / 11008 次数过多 / 11009 验证码错误：落到验证码字段上
    if (e?.code === 11009 || e?.code === 11007 || e?.code === 11008) {
      errors.code = e.message
      codeEl.value?.focus()
    } else {
      formError.value = e?.message || '登录失败，请稍后重试'
    }
    markLoginFailed()
  }
}

</script>

<style scoped>
/* 登录失败对话框：居中模态，遮罩点击可关闭 */
.dlg-mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.dlg {
  width: 100%;
  max-width: 320px;
  background: #fff;
  border-radius: 14px;
  padding: 20px 18px 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.22);
}

.dlg__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #3a2a18;
}

.dlg__desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: #8a6a3a;
}

.dlg__actions {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 被跳转来登录时的说明区：告诉用户去哪、失败会怎样，降低"改的东西丢了"的焦虑 */
.auth__guide {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(255, 150, 0, 0.08);
  border: 1px solid rgba(255, 150, 0, 0.24);
}

.auth__guide-title {
  font-size: 14px;
  font-weight: 600;
  color: #8a4b00;
}

.auth__guide-sub {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: #a5620f;
}

.auth__guide-fail {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: #c0392b;
}

.auth__guide .btn {
  margin-top: 10px;
}
</style>
