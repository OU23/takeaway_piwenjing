<template>
  <header class="navbar" :class="{ 'navbar--brand': brand }">
    <button v-if="back" class="navbar__back" aria-label="返回" @click="onBack">
      <Icon name="back" :size="20" color="currentColor" />
    </button>
    <div class="navbar__title">{{ title }}</div>
    <div class="navbar__extra">
      <slot name="extra" />
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import Icon from './Icon.vue'

const props = defineProps({
  title: { type: String, default: '' },
  back: { type: Boolean, default: true },
  brand: { type: Boolean, default: false }
})

const router = useRouter()

function onBack() {
  if (window.history.length > 1) router.back()
  else router.push('/')
}
</script>
