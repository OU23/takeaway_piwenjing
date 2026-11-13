<template>
  <div class="stage">
    <div class="phone">
      <div class="phone__notch" />
      <div class="screen">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
        <ToastHost />
      </div>
    </div>
    <div class="stage__hint">
      数创星光外卖 · 用户端 <b>设计演示稿（静态 Mock）</b><br />
      仅用于视觉与交互演示，数据写死在 src/mock/*，不连接后端、无下单/支付链路。<br />
      真实可运行原型（接后端 + 118 项 e2e）见同级目录 <b>frontend/</b>，勿混淆。
    </div>
  </div>
</template>

<script setup>
import ToastHost from './components/ToastHost.vue'
</script>
