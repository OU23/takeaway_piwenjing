/**
 * 回跳地址清洗。
 *
 * 登录成功后要把用户送回他原本想去的页面，这个值来自 query，
 * 必须只接受站内相对路径：`//evil.com` 这类协议相对地址会被浏览器当成外站，
 * 直接跳转就成了开放重定向漏洞。
 */
export function safeRedirect(value, fallback = '/mine') {
  if (typeof value !== 'string') return fallback
  if (!value.startsWith('/') || value.startsWith('//')) return fallback
  // 指回登录/注册页会形成循环
  if (value.startsWith('/login') || value.startsWith('/register')) return fallback
  return value
}
