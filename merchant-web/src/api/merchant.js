const TOKEN_KEY = 'xg_merchant_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}
export function setToken(t) {
  localStorage.setItem(TOKEN_KEY, t)
}
export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/* --------------------- 登录态失效跳转（保留回跳地址） --------------------- */

const REDIRECT_KEY = 'xg_merchant_redirect'

/**
 * 令牌失效时统一跳登录页，并把当前页面记下来。
 * 登录成功后按记录回跳，用户不至于"改到一半被踢出、登录完回到首页重找页面"。
 */
function gotoLogin() {
  if (location.hash.startsWith('#/login')) return
  try {
    sessionStorage.setItem(REDIRECT_KEY, location.hash || '#/dashboard')
  } catch (e) {
    /* 隐私模式下写不了，退化为登录后回首页 */
  }
  location.hash = '#/login'
}

/** 登录页读取：取走回跳地址（读后即清，避免下次登录误跳） */
export function takeRedirect() {
  let v = ''
  try {
    v = sessionStorage.getItem(REDIRECT_KEY) || ''
    sessionStorage.removeItem(REDIRECT_KEY)
  } catch (e) {
    v = ''
  }
  if (!v.startsWith('#/') || v.startsWith('#/login')) return '/dashboard'
  return v.slice(1)
}

/** 登录页只读：判断是否由会话失效跳来（决定是否展示引导文案） */
export function hasRedirect() {
  try {
    const v = sessionStorage.getItem(REDIRECT_KEY) || ''
    return v.startsWith('#/')
  } catch (e) {
    return false
  }
}

async function request(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (auth) headers['X-Merchant-Token'] = getToken()
  const res = await fetch('/api' + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  })
  let json
  try {
    json = await res.json()
  } catch (e) {
    throw new Error('服务异常（无法解析响应）')
  }
  if (json.code !== 0) {
    if (json.code === 401) {
      clearToken()
      gotoLogin()
    }
    throw new Error(json.message || '请求失败')
  }
  return json.data
}

export const merchantApi = {
  login: (username, password) =>
    request('/v1/merchant/login', { method: 'POST', body: { username, password }, auth: false }),
  myShop: () => request('/v1/merchant/shop'),
  updateShop: (patch) => request('/v1/merchant/shop', { method: 'PUT', body: patch }),
  myDishes: () => request('/v1/merchant/dishes'),
  toggleDish: (id) => request(`/v1/merchant/dishes/${id}/toggle-status`, { method: 'POST' }),
  updateDish: (id, price, originPrice) =>
    request(`/v1/merchant/dishes/${id}?price=${price}` + (originPrice != null ? `&originPrice=${originPrice}` : ''), { method: 'PUT' }),
  orders: (params = {}) => {
    const qs = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => {
      if (v != null && v !== '') qs.append(k, v)
    })
    return request('/v1/merchant/orders?' + qs.toString())
  },
  orderDetail: (orderNo) => request(`/v1/merchant/orders/${orderNo}`),
  acceptOrder: (orderNo) => request(`/v1/merchant/orders/${orderNo}/accept`, { method: 'POST' }),
  prepareOrder: (orderNo) => request(`/v1/merchant/orders/${orderNo}/prepare`, { method: 'POST' }),
  completeOrder: (orderNo) => request(`/v1/merchant/orders/${orderNo}/complete`, { method: 'POST' }),
  dashboard: () => request('/v1/merchant/dashboard'),
  // ---- 真实图片上传（MinIO）----
  upload: (file, biz = 'shop') => uploadFile(file, biz),
  bindShopLogo: (objectKey) => request(`/v1/merchant/shop/logo?objectKey=${encodeURIComponent(objectKey)}`, { method: 'POST' }),
  bindDishImage: (id, objectKey) => request(`/v1/merchant/dishes/${id}/image?objectKey=${encodeURIComponent(objectKey)}`, { method: 'POST' }),
  bindShopHeader: (objectKey) => request(`/v1/merchant/shop/header?objectKey=${encodeURIComponent(objectKey)}`, { method: 'POST' }),
  shopReviews: () => request('/v1/merchant/reviews'),
  ossUrl: (key) => request(`/v1/merchant/oss/url?key=${encodeURIComponent(key)}`)
}

/**
 * 真实图片上传：multipart/form-data，不走 JSON 序列化。
 * 成功返回 { objectKey, url }；失败抛错（与 request 同一套 code/message 约定）。
 */
async function uploadFile(file, biz) {
  const token = getToken()
  const fd = new FormData()
  fd.append('file', file)
  fd.append('biz', biz)
  const res = await fetch('/api/v1/merchant/upload', {
    method: 'POST',
    headers: token ? { 'X-Merchant-Token': token } : {},
    body: fd
  })
  let json
  try {
    json = await res.json()
  } catch (e) {
    throw new Error('上传服务异常（无法解析响应）')
  }
  if (json.code !== 0) {
    if (json.code === 401) {
      clearToken()
      gotoLogin()
    }
    throw new Error(json.message || '上传失败')
  }
  return json.data
}

export function yuan(fen) {
  if (fen == null) return '0.00'
  return (fen / 100).toFixed(2)
}
