<template>
  <AuthLayout slogan="三分钟开一餐，新用户立减 8 元">
    <div class="auth__mark" aria-hidden="true">星</div>
    <h1 class="auth__title">创建账号</h1>
    <p class="auth__sub">注册后自动登录，无需再次输入密码</p>

    <form class="auth-form" novalidate @submit.prevent="onSubmit">
      <div class="afld" :class="{ 'afld--err': !!errors.username }">
        <label class="afld__label" for="reg-username">用户名</label>
        <input
          id="reg-username"
          ref="usernameEl"
          v-model="form.username"
          class="afld__input"
          type="text"
          name="username"
          placeholder="3-20 位字母、数字或下划线"
          autocomplete="username"
          autocapitalize="none"
          autocorrect="off"
          spellcheck="false"
          enterkeyhint="next"
          :aria-invalid="!!errors.username"
          :aria-describedby="errors.username ? 'reg-username-err' : 'reg-username-hint'"
          @blur="onBlur('username')"
          @input="onInput('username')"
        />
        <p v-if="errors.username" id="reg-username-err" class="afld__err" role="alert">
          {{ errors.username }}
        </p>
        <p v-else id="reg-username-hint" class="pw-meter__label">登录时可使用用户名或邮箱</p>
      </div>

      <div class="afld" :class="{ 'afld--err': !!errors.email }">
        <label class="afld__label" for="reg-email">邮箱</label>
        <input
          id="reg-email"
          ref="emailEl"
          v-model="form.email"
          class="afld__input"
          type="email"
          name="email"
          inputmode="email"
          placeholder="name@example.com"
          autocomplete="email"
          autocapitalize="none"
          autocorrect="off"
          spellcheck="false"
          enterkeyhint="next"
          :aria-invalid="!!errors.email"
          :aria-describedby="errors.email ? 'reg-email-err' : null"
          @blur="onBlur('email')"
          @input="onInput('email')"
        />
        <p v-if="errors.email" id="reg-email-err" class="afld__err" role="alert">
          {{ errors.email }}
        </p>
      </div>

      <div class="afld" :class="{ 'afld--err': !!errors.phone }">
        <label class="afld__label" for="reg-phone">手机号</label>
        <input
          id="reg-phone"
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
          :aria-describedby="errors.phone ? 'reg-phone-err' : null"
          @blur="onBlur('phone')"
          @input="onInput('phone')"
        />
        <p v-if="errors.phone" id="reg-phone-err" class="afld__err" role="alert">
          {{ errors.phone }}
        </p>
      </div>

      <div class="afld" :class="{ 'afld--err': !!errors.code }">
        <label class="afld__label" for="reg-code">验证码</label>
        <div class="afld__wrap">
          <input
            id="reg-code"
            ref="codeEl"
            v-model="form.code"
            class="afld__input afld__input--with-code"
            type="text"
            name="one-time-code"
            inputmode="numeric"
            maxlength="6"
            placeholder="请输入验证码"
            autocomplete="one-time-code"
            enterkeyhint="next"
            :aria-invalid="!!errors.code"
            :aria-describedby="errors.code ? 'reg-code-err' : null"
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
        <p v-if="errors.code" id="reg-code-err" class="afld__err" role="alert">
          {{ errors.code }}
        </p>
        <p v-else-if="devCode" class="afld__hint">
          演示环境验证码：<b>{{ devCode }}</b>（已模拟下发，不会真实发短信）
        </p>
      </div>

      <div class="afld" :class="{ 'afld--err': !!errors.password }">
        <label class="afld__label" for="reg-password">密码</label>
        <div class="afld__wrap">
          <input
            id="reg-password"
            ref="passwordEl"
            v-model="form.password"
            class="afld__input afld__input--with-toggle"
            :type="showPassword ? 'text' : 'password'"
            name="new-password"
            placeholder="至少 8 位，含字母和数字"
            autocomplete="new-password"
            enterkeyhint="next"
            :aria-invalid="!!errors.password"
            :aria-describedby="errors.password ? 'reg-password-err' : 'reg-password-hint'"
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

        <!-- 强度提示：纯装饰性反馈，强度结论通过 aria-live 文本播报 -->
        <div v-if="strength > 0" class="pw-meter" aria-hidden="true">
          <i
            v-for="i in 3"
            :key="i"
            class="pw-meter__bar"
            :class="{ ['pw-meter__bar--' + strength]: i <= strength }"
          />
        </div>
        <p
          v-if="form.password && !errors.password"
          id="reg-password-hint"
          class="pw-meter__label"
          aria-live="polite"
        >
          密码强度：{{ strengthLabel }}
        </p>
        <p v-if="errors.password" id="reg-password-err" class="afld__err" role="alert">
          {{ errors.password }}
        </p>
      </div>

      <div class="afld" :class="{ 'afld--err': !!errors.confirm }">
        <label class="afld__label" for="reg-confirm">确认密码</label>
        <input
          id="reg-confirm"
          ref="confirmEl"
          v-model="form.confirm"
          class="afld__input"
          :type="showPassword ? 'text' : 'password'"
          name="confirm-password"
          placeholder="请再次输入密码"
          autocomplete="new-password"
          enterkeyhint="done"
          :aria-invalid="!!errors.confirm"
          :aria-describedby="errors.confirm ? 'reg-confirm-err' : null"
          @blur="onBlur('confirm')"
          @input="onInput('confirm')"
        />
        <p v-if="errors.confirm" id="reg-confirm-err" class="afld__err" role="alert">
          {{ errors.confirm }}
        </p>
      </div>

      <label class="agree" :class="{ 'agree--err': !!errors.agree }">
        <input
          ref="agreeEl"
          v-model="form.agree"
          type="checkbox"
          name="agree"
          :aria-invalid="!!errors.agree"
          :aria-describedby="errors.agree ? 'reg-agree-err' : null"
          @change="onAgreeChange"
        />
        <span>
          我已阅读并同意
          <a href="#/register" @click.prevent="onTerms">《用户协议》</a>
          与
          <a href="#/register" @click.prevent="onPrivacy">《隐私政策》</a>
        </span>
      </label>
      <p v-if="errors.agree" id="reg-agree-err" class="afld__err" role="alert">
        {{ errors.agree }}
      </p>

      <p v-if="formError" class="auth__formerr" role="alert">{{ formError }}</p>

      <button
        class="btn btn--primary btn--block auth__submit"
        type="submit"
        :disabled="auth.pending"
        :aria-busy="auth.pending"
      >
        {{ auth.pending ? '注册中…' : '注册并登录' }}
      </button>
    </form>

    <p class="auth__switch">
      已有账号？
      <router-link :to="loginTarget">去登录</router-link>
    </p>

    <p class="auth__hint">注册即成为普通会员，消费累积成长值自动升级</p>
  </AuthLayout>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthLayout from '../components/AuthLayout.vue'
import { useAuthStore } from '../store/auth'
import { toast } from '../api/http'
import { useSmsCode } from '../composables/useSmsCode'
import { safeRedirect } from '../utils/nav'
import {
  passwordStrength,
  validateConfirm,
  validateEmail,
  validatePassword,
  validatePhone,
  validateSmsCode,
  validateUsername
} from '../utils/validate'
import '../styles/auth.css'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  username: '',
  email: '',
  phone: '',
  code: '',
  password: '',
  confirm: '',
  agree: false
})
const errors = reactive({ username: '', email: '', phone: '', code: '', password: '', confirm: '', agree: '' })
const touched = reactive({ username: false, email: false, phone: false, code: false, password: false, confirm: false, agree: false })
const formError = ref('')
const showPassword = ref(false)

const usernameEl = ref(null)
const emailEl = ref(null)
const phoneEl = ref(null)
const codeEl = ref(null)
const passwordEl = ref(null)
const confirmEl = ref(null)
const agreeEl = ref(null)

/* 发码与重发倒计时（与登录页共用同一套逻辑） */
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

const loginTarget = computed(() => ({
  path: '/login',
  query: route.query.redirect ? { redirect: route.query.redirect } : {}
}))

const strength = computed(() => passwordStrength(form.password))
const strengthLabel = computed(() => ['', '弱', '中', '强'][strength.value] || '')

function validateField(key) {
  switch (key) {
    case 'username':
      return validateUsername(form.username)
    case 'email':
      return validateEmail(form.email)
    case 'phone':
      return validatePhone(form.phone)
    case 'code':
      return validateSmsCode(form.code)
    case 'password':
      return validatePassword(form.password)
    case 'confirm':
      return validateConfirm(form.confirm, form.password)
    default:
      return ''
  }
}

function onBlur(key) {
  touched[key] = true
  errors[key] = validateField(key)
  // 密码改了要顺带复检确认密码，否则「两次不一致」会一直挂着旧结论
  if (key === 'password' && touched.confirm) errors.confirm = validateField('confirm')
}

function onInput(key) {
  formError.value = ''
  // 改手机号后，上一条验证码与新号码无关，提示要撤掉
  if (key === 'phone') clearDevCode()
  if (touched[key]) errors[key] = validateField(key)
  if (key === 'password' && touched.confirm) errors.confirm = validateField('confirm')
}

function focusFirstInvalid() {
  const order = ['username', 'email', 'phone', 'code', 'password', 'confirm', 'agree']
  const refs = {
    username: usernameEl,
    email: emailEl,
    phone: phoneEl,
    code: codeEl,
    password: passwordEl,
    confirm: confirmEl,
    agree: agreeEl
  }
  for (const k of order) {
    if (!errors[k]) continue
    refs[k].value?.focus()
    return
  }
}

/**
 * 勾选/取消协议。必须显式处理：复选框没有 blur 语义，
 * 若只靠提交时校验，用户勾上之后红色提示会一直挂着，看起来像没生效。
 */
function onAgreeChange() {
  formError.value = ''
  if (form.agree) {
    errors.agree = ''
  } else if (touched.agree) {
    errors.agree = '请先阅读并勾选同意用户协议与隐私政策'
  }
}

/** 发送验证码：成功后清掉「请输入验证码」的旧结论并把焦点交给验证码框 */
async function onSendCode() {
  formError.value = ''
  touched.phone = true
  errors.phone = ''

  const ok = await send()
  if (!ok) return
  errors.code = ''
  codeEl.value?.focus()
}

function onTerms() {
  toast('用户协议为演示占位，正式版本需接入法务文本', 'info')
}

function onPrivacy() {
  toast('隐私政策为演示占位，正式版本需接入法务文本', 'info')
}

async function onSubmit() {
  formError.value = ''
  Object.keys(touched).forEach((k) => { touched[k] = true })
  errors.username = validateUsername(form.username)
  errors.email = validateEmail(form.email)
  errors.phone = validatePhone(form.phone)
  errors.code = validateSmsCode(form.code)
  errors.password = validatePassword(form.password)
  errors.confirm = validateConfirm(form.confirm, form.password)
  errors.agree = form.agree ? '' : '请先阅读并勾选同意用户协议与隐私政策'

  if (errors.username || errors.email || errors.phone || errors.code
    || errors.password || errors.confirm || errors.agree) {
    focusFirstInvalid()
    return
  }

  try {
    await auth.register({
      username: form.username.trim(),
      email: form.email.trim(),
      phone: form.phone.trim(),
      code: form.code.trim(),
      password: form.password
    })
    toast('注册成功，已为你自动登录', 'success')
    router.replace(safeRedirect(route.query.redirect))
  } catch (e) {
    // 把服务端的字段级冲突落回对应输入框，其余作为表单级错误
    if (e?.code === 11002) {
      errors.username = e.message
      usernameEl.value?.focus()
    } else if (e?.code === 11003) {
      errors.email = e.message
      emailEl.value?.focus()
    } else if (e?.code === 11010) {
      errors.phone = e.message
      phoneEl.value?.focus()
    } else if (e?.code === 11009 || e?.code === 11007 || e?.code === 11008) {
      errors.code = e.message
      codeEl.value?.focus()
    } else {
      formError.value = e?.message || '注册失败，请稍后重试'
    }
  }
}
</script>
