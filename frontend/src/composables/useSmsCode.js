import { onUnmounted, ref } from 'vue'
import { sendSmsCode } from '../api'
import { toast } from '../api/http'
import { validatePhone } from '../utils/validate'

/**
 * 发送短信验证码 + 重发倒计时。
 *
 * 登录页和注册页都要这套逻辑（校验手机号 → 发码 → 倒计时 → 展示演示验证码），
 * 抽出来避免两处各自实现后行为漂移。
 *
 * @param {() => string} getPhone 取当前手机号
 * @param {(field: 'phone'|'form', message: string) => void} onError 错误回调
 */
export function useSmsCode(getPhone, onError) {
  const counting = ref(0)
  const sending = ref(false)
  /** mock 环境下服务端回传的明文验证码，用于提示 */
  const devCode = ref('')
  let timer = null

  function startCountdown(seconds) {
    counting.value = seconds
    clearInterval(timer)
    timer = setInterval(() => {
      counting.value -= 1
      if (counting.value <= 0) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  }

  function stopCountdown() {
    clearInterval(timer)
    timer = null
    counting.value = 0
  }

  /** @returns {Promise<boolean>} 是否发送成功 */
  async function send() {
    const message = validatePhone(getPhone())
    if (message) {
      onError?.('phone', message)
      return false
    }

    sending.value = true
    try {
      const r = await sendSmsCode(String(getPhone()).trim())
      devCode.value = r.data?.devCode || ''
      startCountdown(r.data?.resendSeconds || 60)
      toast('验证码已发送', 'success')
      return true
    } catch (e) {
      onError?.('form', e?.message || '发送失败，请稍后重试')
      return false
    } finally {
      sending.value = false
    }
  }

  /** 手机号变了，上一条验证码与它无关，提示要撤掉 */
  function clearDevCode() {
    devCode.value = ''
  }

  onUnmounted(stopCountdown)

  return { counting, sending, devCode, send, stopCountdown, clearDevCode }
}
