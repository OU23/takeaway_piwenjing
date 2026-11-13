<template>
  <div v-if="message" class="toast">{{ message }}</div>
</template>

<script setup>
import { useToast } from '../store/toast'

const { message } = useToast()
</script>
