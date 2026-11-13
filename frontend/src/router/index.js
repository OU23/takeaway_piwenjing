import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { safeRedirect } from '../utils/nav'
import { getRiderToken } from '../utils/rider'

/**
 * meta 约定：
 * - tab          底部 TabBar 高亮的 tab 页
 * - requiresAuth 需要登录，未登录跳登录页并带上 redirect
 * - guestOnly    仅未登录可见（登录/注册页），已登录直接送回站内
 * - wide         桌面端需要突破 480px 应用壳的页面（登录/注册分栏布局）
 */
const routes = [
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue'), meta: { tab: true } },
  { path: '/nearby', name: 'nearby', component: () => import('../views/NearbyView.vue'), meta: { tab: true } },
  { path: '/orders', name: 'orders', component: () => import('../views/OrderListView.vue'), meta: { tab: true, requiresAuth: true } },
  { path: '/messages', name: 'messages', component: () => import('../views/MessageView.vue'), meta: { tab: true } },
  { path: '/mine', name: 'mine', component: () => import('../views/MineView.vue'), meta: { tab: true, requiresAuth: true } },

  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { guestOnly: true, wide: true } },
  { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue'), meta: { guestOnly: true, wide: true } },

  { path: '/shop/:shopId', name: 'shop', component: () => import('../views/ShopView.vue') },
  { path: '/address', name: 'address', component: () => import('../views/AddressView.vue'), meta: { requiresAuth: true } },
  { path: '/checkout/:shopId', name: 'checkout', component: () => import('../views/CheckoutView.vue'), meta: { requiresAuth: true } },
  { path: '/pay/:orderNo', name: 'pay', component: () => import('../views/PayView.vue'), meta: { requiresAuth: true } },
  { path: '/order/:orderNo', name: 'orderDetail', component: () => import('../views/OrderDetailView.vue'), meta: { requiresAuth: true } },
  { path: '/review/:orderNo', name: 'review', component: () => import('../views/ReviewView.vue'), meta: { requiresAuth: true } },
  { path: '/reviews', name: 'myReviews', component: () => import('../views/MyReviewsView.vue'), meta: { requiresAuth: true } },
  { path: '/settings', name: 'settings', component: () => import('../views/SettingsView.vue'), meta: { requiresAuth: true } },
  { path: '/rider/login', name: 'riderLogin', component: () => import('../views/RiderLoginView.vue') },
  { path: '/rider', name: 'riderHome', component: () => import('../views/RiderHomeView.vue') },
  { path: '/rider/task/:orderNo', name: 'riderTask', component: () => import('../views/RiderTaskView.vue') },
  { path: '/rider/income', name: 'riderIncome', component: () => import('../views/RiderIncomeView.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

/**
 * 只接受站内相对路径作为回跳目标（见 utils/nav.js）。
 */

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  /**
   * 返回（popstate）时恢复离开时的滚动位置：
   * 从店铺详情返回首页/订单列表，用户不会被拽回顶部。
   */
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return { ...savedPosition, behavior: 'instant' }
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    return { top: 0, behavior: 'instant' }
  }
})

/**
 * 路由守卫。
 *
 * 首次导航前先等 restore() 完成，否则刷新页面时 store 还没校验完令牌，
 * 会把已登录用户误判成未登录、闪一下登录页。
 */
router.beforeEach(async (to) => {
  // 骑手端独立登录态：未登录跳骑手登录页
  if (to.path.startsWith('/rider') && to.path !== '/rider/login' && !getRiderToken()) {
    return { path: '/rider/login', query: { redirect: to.fullPath } }
  }

  const auth = useAuthStore()
  if (!auth.ready) {
    await auth.restore()
  }

  if (to.meta.guestOnly && auth.isLoggedIn) {
    return safeRedirect(to.query.redirect)
  }
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
