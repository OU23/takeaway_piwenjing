<!--
  应用内确认弹层（替代 window.confirm）
  - 移动端从底部升起，拇指区可达
  - 打开时锁定背景滚动，避免滚动穿透
-->
<template>
  <Teleport to="body">
    <Transition name="cf">
      <div v-if="confirmState.open" class="cf-mask" @click.self="onCancel">
        <div class="cf-sheet" role="alertdialog" aria-modal="true" :aria-label="confirmState.title">
          <div class="cf-sheet__title">{{ confirmState.title }}</div>
          <div v-if="confirmState.message" class="cf-sheet__msg">{{ confirmState.message }}</div>
          <div class="cf-sheet__actions">
            <button class="cf-btn cf-btn--cancel" @click="onCancel">
              {{ confirmState.cancelText }}
            </button>
            <button
              class="cf-btn cf-btn--ok"
              :class="{ 'cf-btn--danger': confirmState.danger }"
              @click="onConfirm"
            >
              {{ confirmState.confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { onUnmounted, watch } from 'vue'
import { confirmState, settleConfirm } from '../utils/confirm'
import { lockScroll, unlockScroll } from '../utils/ui'

let locked = false

watch(
  () => confirmState.open,
  (open) => {
    if (open && !locked) {
      lockScroll()
      locked = true
    } else if (!open && locked) {
      unlockScroll()
      locked = false
    }
  }
)

function onConfirm() {
  settleConfirm(true)
}

function onCancel() {
  settleConfirm(false)
}

onUnmounted(() => {
  if (locked) {
    unlockScroll()
    locked = false
  }
})
</script>

<style scoped>
.cf-mask {
  position: fixed;
  inset: 0;
  z-index: 300;
  background: rgba(23, 24, 26, .45);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: env(safe-area-inset-bottom, 0px);
}

.cf-sheet {
  width: 100%;
  max-width: var(--app-max, 480px);
  background: var(--bg-card, #fff);
  border-radius: 14px 14px 0 0;
  padding: 18px 16px calc(14px + env(safe-area-inset-bottom, 0px));
  animation: cfUp .2s cubic-bezier(.32, .72, 0, 1);
  will-change: transform;
}

.cf-sheet__title {
  font-size: 16px;
  font-weight: 600;
  text-align: center;
  color: var(--t-1, #17181a);
}

.cf-sheet__msg {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--t-2, #5c6169);
  text-align: center;
}

.cf-sheet__actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}

.cf-btn {
  flex: 1;
  height: 44px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  transition: transform .1s ease, opacity .1s ease;
}

.cf-btn:active {
  transform: scale(.97);
  opacity: .88;
}

.cf-btn--cancel {
  background: var(--bg-sunken, #f0f1f4);
  color: var(--t-2, #5c6169);
}

.cf-btn--ok {
  background: var(--brand-grad, linear-gradient(135deg, #ffb03a, #f5481f));
  color: #fff;
}

.cf-btn--danger {
  background: var(--c-error, #e5484d);
}

@keyframes cfUp {
  from { transform: translate3d(0, 100%, 0); }
  to { transform: translate3d(0, 0, 0); }
}

.cf-enter-active,
.cf-leave-active {
  transition: opacity .18s ease;
}

.cf-enter-from,
.cf-leave-to {
  opacity: 0;
}
</style>
