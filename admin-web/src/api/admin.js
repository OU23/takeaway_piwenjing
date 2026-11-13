import axios from 'axios'
import { auth, logout } from '../store/auth'

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000
})

// 每次请求附带后台登录 token
http.interceptors.request.use((config) => {
  if (auth.token) {
    config.headers['X-Admin-Token'] = auth.token
  }
  return config
})

/* --------------------- 登录态失效跳转（保留回跳地址） --------------------- */

const REDIRECT_KEY = 'xg_admin_redirect'
/** 与 router 的 createWebHistory('/admin') 保持一致 */
const BASE = '/admin'

function handleUnauthorized() {
  logout()
  if (window.location.pathname.startsWith(BASE + '/login')) return
  try {
    // 记住失效时所在页面，登录成功后回跳，避免运营重新找菜单
    sessionStorage.setItem(REDIRECT_KEY, window.location.pathname + window.location.search)
  } catch (e) {
    /* 写不了就回默认首页 */
  }
  window.location.replace(BASE + '/login')
}

/** 登录页只读：是否由会话失效跳来（决定展示回跳说明） */
export function hasAdminRedirect() {
  try {
    const v = sessionStorage.getItem(REDIRECT_KEY) || ''
    return v.startsWith(BASE + '/') && !v.startsWith(BASE + '/login')
  } catch (e) {
    return false
  }
}

/** 登录页读取：取走回跳地址并转换为 router 路径（读后即清） */
export function takeAdminRedirect() {
  let v = ''
  try {
    v = sessionStorage.getItem(REDIRECT_KEY) || ''
    sessionStorage.removeItem(REDIRECT_KEY)
  } catch (e) {
    v = ''
  }
  if (!v.startsWith(BASE + '/') || v.startsWith(BASE + '/login')) return '/overview'
  return v.slice(BASE.length) || '/overview'
}

http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      if (body.code === 401) {
        handleUnauthorized()
        return Promise.reject(new Error('登录已失效，请重新登录'))
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (err) => {
    if (err.response?.status === 401) {
      handleUnauthorized()
      return Promise.reject(new Error('登录已失效，请重新登录'))
    }
    const msg = err.response?.data?.message
      || (err.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '')
      || (!err.response ? '无法连接服务，请确认后端已启动' : err.message)
      || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

export function post(url, data, params) {
  return http.post(url, data, { params })
}
export function get(url, params) {
  return http.get(url, { params })
}

// 文件上传走 multipart，单独封装以便复用
export function upload(file, biz = 'common') {
  const fd = new FormData()
  fd.append('file', file)
  return http.post('/admin/files', fd, {
    params: { biz },
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

export const adminApi = {
  login: (username, password) => post('/admin/login', { username, password }),
  overview: () => get('/admin/overview'),
  trend: (days) => get('/admin/dashboard/trend', { days }),
  orders: (params) => get('/admin/orders', params),
  orderDetail: (no) => get(`/admin/orders/${no}`),
  advance: (no) => post(`/admin/orders/${no}/advance`),
  markException: (no, note) => post(`/admin/orders/${no}/exception`, null, { note }),
  cancelOrder: (no, reason) => post(`/admin/orders/${no}/cancel`, null, { reason }),
  shops: () => get('/admin/shops'),
  dishes: (shopId) => get('/admin/dishes', { shopId }),
  toggleDish: (id) => post(`/admin/dishes/${id}/toggle-status`),
  ratingMonitor: () => get('/admin/rating-monitor'),
  applications: (status) => get('/admin/merchant-applications', { status }),
  approveApp: (id) => post(`/admin/merchant-applications/${id}/approve`),
  rejectApp: (id, reason) => post(`/admin/merchant-applications/${id}/reject`, null, { reason }),
  dispatchOverview: () => get('/admin/dispatch/overview'),
  users: (params) => get('/admin/users', params),
  complaints: (params) => get('/admin/complaints', params),
  payments: (params) => get('/admin/payments', params),
  // 文件与对象存储（MinIO）
  fileStatus: () => get('/admin/files/status'),
  files: (prefix, limit) => get('/admin/files/list', { prefix, limit }),
  uploadFile: (file, biz) => upload(file, biz),
  deleteFile: (key) => http.delete('/admin/files', { params: { key } }),
  presign: (key, expiry) => get('/admin/files/presign', { key, expiry }),
  bindDishImage: (id, objectKey) => post(`/admin/files/bind/dish/${id}`, null, { objectKey }),
  bindShopLogo: (id, objectKey) => post(`/admin/files/bind/shop/${id}`, null, { objectKey }),
  bindUserAvatar: (id, objectKey) => post(`/admin/files/bind/user/${id}`, null, { objectKey })
}

export default http
