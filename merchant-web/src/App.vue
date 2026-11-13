<template>
  <router-view v-if="$route.meta.public" />
  <div v-else class="app-shell">
    <van-nav-bar :title="title" />
    <div class="page">
      <router-view />
    </div>
    <van-tabbar route>
      <van-tabbar-item to="/shop" icon="shop-o">门店</van-tabbar-item>
      <van-tabbar-item to="/dishes" icon="goods-o">菜品</van-tabbar-item>
      <van-tabbar-item to="/orders" icon="orders-o">接单</van-tabbar-item>
      <van-tabbar-item to="/reviews" icon="comment-o">评价</van-tabbar-item>
      <van-tabbar-item to="/dashboard" icon="chart-trending-o">看板</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()
const title = computed(() => route.meta?.title || '商家端')
</script>
