import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/home', name: 'home', component: () => import('../views/HomeView.vue'), meta: { tab: 'home', title: '首页' } },
  { path: '/nearby', name: 'nearby', component: () => import('../views/NearbyView.vue'), meta: { tab: 'nearby', title: '附近' } },
  { path: '/orders', name: 'orders', component: () => import('../views/OrdersView.vue'), meta: { tab: 'orders', title: '订单' } },
  { path: '/message', name: 'message', component: () => import('../views/MessageView.vue'), meta: { tab: 'message', title: '消息' } },
  { path: '/mine', name: 'mine', component: () => import('../views/MineView.vue'), meta: { tab: 'mine', title: '我的' } }
]

const router = createRouter({
  history: createWebHistory('/user'),
  routes
})

router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} · 数创星光外卖` : '数创星光外卖'
})

export default router
