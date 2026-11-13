import { ref } from 'vue'

const message = ref('')
let timer = null

export function useToast() {
  return {
    message,
    show(text, duration = 1800) {
      message.value = text
      clearTimeout(timer)
      timer = setTimeout(() => (message.value = ''), duration)
    }
  }
}
