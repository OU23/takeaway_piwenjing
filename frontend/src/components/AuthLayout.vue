<template>
  <div class="auth">
    <!-- 桌面端品牌栏；移动端 display:none，不占位也不进无障碍树 -->
    <aside class="auth__aside">
      <div class="auth__aside-mark">
        <span class="auth__aside-dot" aria-hidden="true">星</span>
        数创星光外卖
      </div>

      <div>
        <h2 class="auth__slogan">{{ slogan }}</h2>
        <ul class="auth__points">
          <li v-for="p in points" :key="p">{{ p }}</li>
        </ul>
      </div>

      <div class="auth__aside-foot">© 2026 数创星光 · 本地生活服务平台</div>
    </aside>

    <main class="auth__main">
      <div class="auth__card">
        <slot />
      </div>
    </main>
  </div>
</template>

<script setup>
defineProps({
  slogan: { type: String, default: '本地好味，半小时送到' },
  points: {
    type: Array,
    default: () => ['附近好店，出餐进度实时可见', '到店自取，免配送费再立减', '会员成长值，越吃越划算']
  }
})
</script>
