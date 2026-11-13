<template>
  <svg class="ico" :width="size" :height="size" viewBox="0 0 24 24" fill="none"
       :stroke="color" :stroke-width="stroke" stroke-linecap="butt" stroke-linejoin="miter"
       stroke-miterlimit="6" aria-hidden="true">
    <path v-for="(p, i) in paths" :key="i" :d="p" />
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 18 },
  color: { type: String, default: 'currentColor' },
  stroke: { type: [Number, String], default: 1.6 }
})

const ICONS = {
  dashboard: ['M3 13h8V3H3 Z', 'M13 21h8V3h-8 Z', 'M3 21h8v-6H3 Z'],
  overview: ['M4 4 H10 V10 H4 Z', 'M14 4 H20 V10 H14 Z', 'M4 14 H10 V20 H4 Z', 'M14 14 H20 V20 H14 Z'],
  order: ['M6 2 H18 V22 L15 20 L12 22 L9 20 L6 22 Z', 'M9 7 H15', 'M9 11 H15', 'M9 15 H13'],
  shop: ['M4 9 L6 3 H18 L20 9 V21 H4 Z', 'M9.5 21 V14 H14.5 V21', 'M4 9 H20'],
  rider: ['M2.5 6.5 H14.5 V16 H2.5 Z', 'M14.5 9.5 H18 L21.5 12.5 V16 H14.5', 'M6 16 a1.7 1.7 0 1 0 0 3.4 a1.7 1.7 0 0 0 0 -3.4 Z', 'M17.6 16 a1.7 1.7 0 1 0 0 3.4 a1.7 1.7 0 0 0 0 -3.4 Z'],
  user: ['M12 4.5 a3.6 3.6 0 1 0 0 7.2 a3.6 3.6 0 0 0 0 -7.2 Z', 'M4.5 20.5 C4.5 16.8 7.8 14.6 12 14.6 C16.2 14.6 19.5 16.8 19.5 20.5'],
  alert: ['M12 3.5 a8.5 8.5 0 1 0 0 17 a8.5 8.5 0 0 0 0 -17 Z', 'M12 7.5 V13', 'M12 15.8 V16.6'],
  search: ['M11 4.5 a6.5 6.5 0 1 0 0 13 a6.5 6.5 0 0 0 0 -13 Z', 'M15.8 15.8 L20.5 20.5'],
  back: ['M14.5 5 L7.5 12 L14.5 19'],
  chevron: ['M9.5 5 L16.5 12 L9.5 19'],
  close: ['M6.5 6.5 L17.5 17.5', 'M17.5 6.5 L6.5 17.5'],
  chart: ['M4 20 H20', 'M7 20 V12', 'M12 20 V6', 'M17 20 V9'],
  shield: ['M12 3.5 L20 6.5 V12 C20 16.4 16.6 19.5 12 20.5 C7.4 19.5 4 16.4 4 12 V6.5 Z'],
  folder: ['M3 6.5 H9.5 L11.5 9 H21 V19.5 H3 Z'],
  upload: ['M12 16.5 V5.5', 'M7.5 10 L12 5.5 L16.5 10', 'M4.5 16 V19.5 H19.5 V16'],
  image: ['M3.5 5 H20.5 V19 H3.5 Z', 'M3.5 15.5 L8.5 10.5 L12.5 14.5 L15.5 11.5 L20.5 16.5', 'M15.2 8.6 a1.3 1.3 0 1 0 0 .1 Z']
}

const paths = computed(() => ICONS[props.name] || ICONS.dashboard)
</script>

<style scoped>
.ico { display: block; flex-shrink: 0; }
</style>
