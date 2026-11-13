<template>
  <span class="fu">
    <button type="button" class="btn btn--sm" :disabled="loading || disabled" @click="pick">
      <Icon name="upload" :size="14" />
      <span>{{ loading ? '上传中…' : label }}</span>
    </button>
    <input
      ref="inp"
      class="fu__file"
      type="file"
      accept="image/jpeg,image/png,image/webp,image/gif"
      @change="onChange"
    />
  </span>
</template>

<script setup>
import { ref } from 'vue'
import Icon from './Icon.vue'
import { adminApi } from '../api/admin'

const props = defineProps({
  /** 业务前缀：dish / shop / avatar / license */
  biz: { type: String, default: 'common' },
  label: { type: String, default: '上传图片' },
  maxMb: { type: Number, default: 5 },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['uploaded', 'error'])

const inp = ref(null)
const loading = ref(false)
const ALLOWED = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

function pick() {
  if (props.disabled || loading.value) return
  inp.value?.click()
}

async function onChange(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  if (!ALLOWED.includes(file.type)) {
    emit('error', '仅支持 JPG / PNG / WEBP / GIF 图片')
    return
  }
  if (file.size > props.maxMb * 1024 * 1024) {
    emit('error', `文件超过 ${props.maxMb}MB 限制`)
    return
  }
  loading.value = true
  try {
    const r = await adminApi.uploadFile(file, props.biz)
    emit('uploaded', r.data)
  } catch (err) {
    emit('error', err.message || '上传失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.fu { display: inline-flex; }
.fu .btn { display: inline-flex; align-items: center; gap: 5px; }
.fu__file { display: none; }
</style>
