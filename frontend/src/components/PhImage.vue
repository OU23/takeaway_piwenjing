<template>
  <div class="ph" :class="cls" :data-label="showLabel ? label : ''">
    <!--
      真实图片走原生 img 标签：浏览器可对其做懒加载与异步解码，
      CSS background-image 做不到这两点（SRS 5.4.3 要求支持懒加载）。
    -->
    <img
      v-if="isUrl"
      class="ph__img"
      :src="image"
      :alt="label || ''"
      loading="lazy"
      decoding="async"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

/**
 * 商品 / 门店图容器。
 * - 后端返回 grad-* → 渐变占位（演示数据）
 * - 后端返回真实 URL → <img> 懒加载
 */
const props = defineProps({
  image: { type: String, default: '' },
  label: { type: String, default: '' }
})

const isUrl = computed(() => /^(https?:)?\/\//.test(props.image || ''))
const cls = computed(() => (isUrl.value ? '' : 'ph--' + (props.image || 'grad-1')))
/** 有真实图片时不再叠加占位文案，避免文字压在图片上 */
const showLabel = computed(() => !isUrl.value && !!props.label)
</script>

<style scoped>
.ph__img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: inherit;
}
</style>
