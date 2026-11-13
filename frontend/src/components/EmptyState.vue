<template>
  <div class="empty">
    <div class="empty__art">
      <Icon :name="icon" :size="34" color="#C4C7CE" />
    </div>
    <div class="empty__title">{{ title }}</div>
    <div v-if="desc" class="empty__desc">{{ desc }}</div>
    <button v-if="action" class="btn btn--ghost btn--sm" @click="$emit('action')">{{ action }}</button>
  </div>
</template>

<script setup>
import Icon from './Icon.vue'

defineProps({
  icon: { type: String, default: 'receipt' },
  title: { type: String, default: '暂无内容' },
  desc: { type: String, default: '' },
  action: { type: String, default: '' }
})

defineEmits(['action'])
</script>
