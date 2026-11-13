import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn, canManage } from '../store/auth'

// 菜单与权限元数据：roles 控制菜单可见，canManage 控制写操作按钮
export const menuRoutes = [
  {
    path: '/overview',
    name: 'overview',
    component: () => import('../views/OverviewView.vue'),
    meta: { title: '项目总览', icon: 'overview', roles: ['SUPER_ADMIN', 'OPERATOR', 'CS'] }
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('../views/DashboardView.vue'),
    meta: { title: '数据看板', icon: 'dashboard', roles: ['SUPER_ADMIN', 'OPERATOR', 'CS'] }
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('../views/OrderListView.vue'),
    meta: { title: '订单管理', icon: 'order', roles: ['SUPER_ADMIN', 'OPERATOR', 'CS'] }
  },
  {
    path: '/merchants',
    name: 'merchants',
    component: () => import('../views/MerchantView.vue'),
    meta: { title: '商家管理', icon: 'shop', roles: ['SUPER_ADMIN', 'OPERATOR', 'CS'] }
  },
  {
    path: '/dispatch',
    name: 'dispatch',
    component: () => import('../views/DispatchView.vue'),
    meta: { title: '骑手调度', icon: 'rider', roles: ['SUPER_ADMIN', 'OPERATOR', 'CS'] }
  },
  {
    path: '/users',
    name: 'users',
    component: () => import('../views/UserView.vue'),
    meta: { title: '用户管理', icon: 'user', roles: ['SUPER_ADMIN', 'OPERATOR', 'CS'] }
  },
  {
    path: '/complaints',
    name: 'complaints',
    component: () => import('../views/UserView.vue'),
    meta: { title: '客诉管理', icon: 'alert', roles: ['SUPER_ADMIN', 'OPERATOR', 'CS'] }
  },
  {
    path: '/files',
    name: 'files',
    component: () => import('../views/MediaView.vue'),
    meta: { title: '文件管理', icon: 'folder', roles: ['SUPER_ADMIN', 'OPERATOR', 'CS'] }
  },
  {
    path: '/settlement',
    name: 'settlement',
    component: () => import('../views/SettlementView.vue'),
    meta: { title: '财务结算', icon: 'chart', roles: ['SUPER_ADMIN', 'OPERATOR', 'CS'] }
  },
  {
    path: '/reports',
    name: 'reports',
    component: () => import('../views/ReportView.vue'),
    meta: { title: '数据报表', icon: 'dashboard', roles: ['SUPER_ADMIN', 'OPERATOR', 'CS'] }
  },
  {
    path: '/campaigns',
    name: 'campaigns',
    component: () => import('../views/CampaignView.vue'),
    meta: { title: '运营活动', icon: 'image', roles: ['SUPER_ADMIN', 'OPERATOR'] }
  },
  {
    path: '/categories',
    name: 'categories',
    component: () => import('../views/CategoryView.vue'),
    meta: { title: '品类管理', icon: 'folder', roles: ['SUPER_ADMIN', 'OPERATOR'] }
  },
  {
    path: '/roles',
    name: 'roles',
    component: () => import('../views/RoleView.vue'),
    meta: { title: '权限角色', icon: 'shield', roles: ['SUPER_ADMIN'] }
  }
]

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
  {
    path: '/',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/overview',
    children: menuRoutes
  },
  { path: '/:pathMatch(.*)*', redirect: '/overview' }
]

const router = createRouter({
  history: createWebHistory('/admin'),
  routes
})

router.beforeEach((to) => {
  if (to.path === '/login') {
    if (isLoggedIn.value) return '/overview'
    return true
  }
  if (!isLoggedIn.value) return '/login'
  // 菜单可见性（按角色）
  const meta = to.meta || {}
  if (meta.roles && !meta.roles.includes(authRole())) {
    return '/dashboard'
  }
  return true
})

router.afterEach((to) => {
  const base = '数创星光外卖 · 运营后台'
  document.title = to.meta?.title ? `${to.meta.title} · ${base}` : base
})

function authRole() {
  // 延迟引入避免循环依赖
  try {
    return JSON.parse(localStorage.getItem('xg_admin_auth') || '{}').role || ''
  } catch (e) {
    return ''
  }
}

// 供组件判断写权限
export { canManage }
export default router
