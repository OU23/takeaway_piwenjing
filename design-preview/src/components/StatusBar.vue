<template>
  <div class="statusbar" :class="{ 'statusbar--dark': dark }">
    <span class="num">12:36</span>
    <span class="statusbar__icons">
      <svg width="17" height="11" viewBox="0 0 17 11" fill="currentColor" aria-hidden="true">
        <rect x="0" y="7" width="3" height="4" rx="1" />
        <rect x="4.5" y="5" width="3" height="6" rx="1" />
        <rect x="9" y="2.5" width="3" height="8.5" rx="1" />
        <rect x="13.5" y="0" width="3" height="11" rx="1" />
      </svg>
      <svg width="15" height="11" viewBox="0 0 15 11" fill="none" stroke="currentColor" stroke-width="1.4" aria-hidden="true">
        <path d="M1 4.2A9 9 0 0 1 14 4.2" />
        <path d="M3.4 6.8A5.6 5.6 0 0 1 11.6 6.8" />
        <path d="M6 9.2a2 2 0 0 1 3 0" />
      </svg>
      <svg width="24" height="12" viewBox="0 0 24 12" aria-hidden="true">
        <rect x="0.6" y="0.6" width="19" height="10.8" rx="3" fill="none" stroke="currentColor" stroke-opacity=".45" />
        <rect x="2.4" y="2.4" width="13" height="7.2" rx="1.8" fill="currentColor" />
        <path d="M21.4 4.2v3.6a2.4 2.4 0 0 0 0-3.6z" fill="currentColor" fill-opacity=".5" />
      </svg>
    </span>
  </div>
</template>

<script setup>
defineProps({ dark: { type: Boolean, default: false } })
</script>
