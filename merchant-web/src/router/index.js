import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '../api/merchant'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
  { path: '/', redirect: '/dashboard' },
  { path: '/shop', name: 'shop', component: () => import('../views/ShopView.vue'), meta: { tab: 'shop', title: '我的门店' } },
  { path: '/dishes', name: 'dishes', component: () => import('../views/DishView.vue'), meta: { tab: 'dishes', title: '菜品管理' } },
  { path: '/orders', name: 'orders', component: () => import('../views/OrderView.vue'), meta: { tab: 'orders', title: '接单' } },
  { path: '/dashboard', name: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { tab: 'dashboard', title: '数据看板' } },
  { path: '/reviews', name: 'reviews', component: () => import('../views/ReviewView.vue'), meta: { tab: 'reviews', title: '评价' } }
]

const router = createRouter({
  history: createWebHashHistory('/merchant'),
  routes
})

router.beforeEach((to) => {
  const loggedIn = !!getToken()
  if (!to.meta.public && !loggedIn) return '/login'
  if (to.path === '/login' && loggedIn) return '/dashboard'
  return true
})

router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} · 商家端` : '商家端 · 数创星光外卖'
})

export default router
