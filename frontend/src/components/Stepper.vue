<template>
  <div class="stepper">
    <button
      v-if="quantity > 0"
      class="stepper__btn stepper__btn--minus"
      aria-label="减少一件"
      @click.stop="$emit('minus')"
    >
      <Icon name="minus" :size="12" />
    </button>
    <span v-if="quantity > 0" class="stepper__num num">{{ quantity }}</span>
    <button
      class="stepper__btn stepper__btn--plus"
      aria-label="增加一件"
      :disabled="disabled"
      @click.stop="$emit('plus', $event)"
    >
      <Icon name="plus" :size="12" color="#fff" />
    </button>
  </div>
</template>

<script setup>
import Icon from './Icon.vue'

defineProps({
  quantity: { type: Number, default: 0 },
  disabled: { type: Boolean, default: false }
})

defineEmits(['plus', 'minus'])
</script>
