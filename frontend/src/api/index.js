import http from './http'

/* -------------------------------- 账号认证 --------------------------------
 * 四个接口都带 silent：登录/注册失败要在表单上就地提示，
 * 会话恢复失败属于预期分支（未登录），都不该弹全局 toast。
 */
export const registerAccount = (data) => http.post('/auth/register', data, { silent: true })
export const loginAccount = (data) => http.post('/auth/login', data, { silent: true })
export const getSession = () => http.get('/auth/me', { silent: true })
export const logoutAccount = () => http.post('/auth/logout', null, { silent: true })

/* 手机验证码登录：发码与校验同样走 silent，错误就地提示 */
export const sendSmsCode = (phone) => http.post('/auth/sms/code', { phone }, { silent: true })
export const loginBySms = (data) => http.post('/auth/sms/login', data, { silent: true })

/* ------------------------------ 首页 / 门店 ------------------------------ */
export const getHome = () => http.get('/home')
export const getCategories = () => http.get('/categories')
export const listShops = (params) => http.get('/shops', { params })
export const getShopDetail = (shopId) => http.get(`/shops/${shopId}`)
export const getDish = (dishId) => http.get(`/dishes/${dishId}`)
export const getDict = () => http.get('/meta/dict')

/* -------------------------------- 购物车 -------------------------------- */
export const getCart = (shopId) => http.get('/cart', { params: { shopId } })
export const getCartCount = () => http.get('/cart/count')
export const addToCart = (data) => http.post('/cart/items', data)
export const updateCartItem = (itemId, quantity) => http.put(`/cart/items/${itemId}`, { quantity })
export const removeCartItem = (itemId) => http.delete(`/cart/items/${itemId}`)
export const clearCart = (shopId) => http.delete('/cart', { params: { shopId } })

/* --------------------------------- 订单 --------------------------------- */
export const previewOrder = (data) => http.post('/orders/preview', data)
export const submitOrder = (data, idemKey) =>
  http.post('/orders', data, idemKey ? { headers: { 'Idempotency-Key': idemKey } } : undefined)
export const listOrders = (params) => http.get('/orders', { params })
export const getOrderGroupCount = () => http.get('/orders/group-count')
export const getOrderDetail = (orderNo) => http.get(`/orders/${orderNo}`)
export const cancelOrder = (orderNo, reason) =>
  http.post(`/orders/${orderNo}/cancel`, null, { params: { reason } })
export const urgeOrder = (orderNo) => http.post(`/orders/${orderNo}/urge`)
export const confirmOrder = (orderNo) => http.post(`/orders/${orderNo}/confirm`)
export const pickupOrder = (orderNo) => http.post(`/orders/${orderNo}/pickup`)

/* --------------------------------- 支付 --------------------------------- */
// 注意：/pay/notify 回调接口仅渠道服务器可调（需验签），前端禁止调用（S1 修复）
export const getPayChannels = () => http.get('/pay/channels')
export const prepay = (data) => http.post('/pay/prepay', data)
export const getPaymentByOrder = (orderNo) => http.get(`/pay/order/${orderNo}`)

/* -------------------------------- 用户中心 -------------------------------- */
export const getUserProfile = () => http.get('/user/profile')
export const listAddresses = () => http.get('/user/addresses')
export const addAddress = (data) => http.post('/user/addresses', data)
export const updateAddress = (id, data) => http.put(`/user/addresses/${id}`, data)
export const deleteAddress = (id) => http.delete(`/user/addresses/${id}`)
export const setDefaultAddress = (id) => http.put(`/user/addresses/${id}/default`)

/* 头像上传 / 绑定（C 端用户）：silent 交给页面自己提示，便于做"失败点击头像重试" */
export const uploadAvatar = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return http.post('/user/upload', fd, {
    params: { biz: 'avatar' },
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
    silent: true
  })
}
export const bindAvatar = (objectKey) =>
  http.post('/user/avatar', null, { params: { objectKey }, silent: true })

/* ------------------------------ 偏好设置 ------------------------------
 * 全部 silent：设置同步失败不打断页面流程，store 会静默降级为本地模式，
 * 设置页在需要时（如重置失败）再自行提示。
 */
export const getUserSettings = () => http.get('/user/settings', { silent: true })
// 补丁为「分组.键」扁平键值，如 { 'appearance.theme': 'dark' }
export const updateUserSettings = (patch) => http.put('/user/settings', patch, { silent: true })
export const resetUserSettings = () => http.delete('/user/settings', { silent: true })

/* -------------------------------- 评价 -------------------------------- */
export const createReview = (data) => http.post('/reviews', data)
export const listMyReviews = () => http.get('/reviews/mine')
export const getOrderReview = (orderNo) => http.get(`/reviews/order/${orderNo}`, { silent: true })
export const listShopReviews = (shopId) => http.get(`/reviews/shop/${shopId}`)
export const uploadReviewImage = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return http.post('/user/upload', fd, {
    params: { biz: 'review' },
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
    silent: true
  })
}

/* ------------------------------ 运营后台 ------------------------------ */
// M14 修复：用户端 H5 移除后台管理能力（/admin 路由与接口），运营后台统一走 admin-web
