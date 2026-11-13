/**
 * 表单校验规则。
 *
 * 与服务端 AuthService 保持同一套规则——客户端校验只为「即时反馈」，
 * 服务端必须独立校验，两边不一致时以服务端为准。
 *
 * 约定：校验函数返回错误文案（字符串），通过则返回空字符串 ''，
 * 这样模板里可以直接 `v-if="errors.x"`，不需要额外的布尔转换。
 */

const USERNAME_RE = /^[A-Za-z][A-Za-z0-9_]{2,19}$/
const EMAIL_RE = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const PHONE_RE = /^1[3-9]\d{9}$/
const SMS_CODE_RE = /^\d{4,6}$/

export const RULES = {
  username: '3-20 位字母、数字或下划线，需以字母开头',
  email: '例如 name@example.com',
  password: '至少 8 位，需同时包含字母和数字'
}

/** 用户名 */
export function validateUsername(value) {
  const v = (value || '').trim()
  if (!v) return '请输入用户名'
  if (v.length < 3) return '用户名至少 3 个字符'
  if (v.length > 20) return '用户名最多 20 个字符'
  if (!USERNAME_RE.test(v)) return '仅支持字母、数字、下划线，且需以字母开头'
  return ''
}

/** 邮箱 */
export function validateEmail(value) {
  const v = (value || '').trim()
  if (!v) return '请输入邮箱'
  if (v.length > 128) return '邮箱地址过长'
  if (!EMAIL_RE.test(v)) return '邮箱格式不正确，请检查后重试'
  return ''
}

/** 密码 */
export function validatePassword(value) {
  const v = value || ''
  if (!v) return '请输入密码'
  if (v.length < 8) return '密码至少 8 位'
  if (v.length > 64) return '密码最多 64 位'
  if (!/[A-Za-z]/.test(v) || !/\d/.test(v)) return '密码需同时包含字母和数字'
  return ''
}

/** 确认密码 */
export function validateConfirm(value, password) {
  const v = value || ''
  if (!v) return '请再次输入密码'
  if (v !== password) return '两次输入的密码不一致'
  return ''
}

/** 登录账号：用户名或邮箱，只做「非空 + 形态合理」判断 */
export function validateAccount(value) {
  const v = (value || '').trim()
  if (!v) return '请输入用户名或邮箱'
  if (v.includes('@')) return validateEmail(v)
  return validateUsername(v)
}

/** 手机号（中国大陆） */
export function validatePhone(value) {
  const v = (value || '').trim()
  if (!v) return '请输入手机号'
  if (v.length !== 11) return '手机号应为 11 位数字'
  if (!PHONE_RE.test(v)) return '请输入有效的手机号'
  return ''
}

/** 短信验证码：4-6 位数字（mock 环境为 4 位） */
export function validateSmsCode(value) {
  const v = (value || '').trim()
  if (!v) return '请输入验证码'
  if (!SMS_CODE_RE.test(v)) return '验证码为 4-6 位数字'
  return ''
}

/**
 * 密码强度（0-3），仅用于输入框下方的进度提示。
 * 0 空 / 1 弱 / 2 中 / 3 强
 *
 * 只要非空就至少是 1，否则「abc」这种输入会渲染出三条全灰的空进度条，
 * 看起来像组件坏了。
 */
export function passwordStrength(value) {
  const v = value || ''
  if (!v) return 0
  const hasLetterAndDigit = /[A-Za-z]/.test(v) && /\d/.test(v)
  const hasSymbol = /[^A-Za-z0-9]/.test(v)
  if (v.length >= 12 || (v.length >= 8 && hasLetterAndDigit && hasSymbol)) return 3
  if (v.length >= 8 && hasLetterAndDigit) return 2
  return 1
}
