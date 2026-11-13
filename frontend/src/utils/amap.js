/**
 * 高德地图 JS API 2.0 动态加载。
 *
 * - Key 走 VITE_AMAP_KEY（见 frontend/.env），避免写死在源码里；
 * - 可选 VITE_AMAP_SECURITY_CODE：2.0 新版 Key 需成对的安全密钥，没有可留空；
 * - 加载失败（离线 / Key 类型不对 / 未配置）会 reject，页面据此回退到占位地图，不影响其它功能。
 */
let loading = null

export function loadAMap() {
  if (typeof window !== 'undefined' && window.AMap) return Promise.resolve(window.AMap)
  if (loading) return loading

  const key = import.meta.env.VITE_AMAP_KEY
  if (!key) return Promise.reject(new Error('未配置 VITE_AMAP_KEY'))

  const security = import.meta.env.VITE_AMAP_SECURITY_CODE
  if (security) {
    window._AMapSecurityConfig = { securityJsCode: security }
  }

  const plugins = 'AMap.Geolocation,AMap.Scale,AMap.ToolBar'
  const src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(key)}&plugin=${plugins}`

  loading = new Promise((resolve, reject) => {
    let done = false
    const timer = setTimeout(() => {
      if (done) return
      done = true
      reject(new Error('高德地图加载超时'))
    }, 8000)

    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.onload = () => {
      if (done) return
      done = true
      clearTimeout(timer)
      if (window.AMap) resolve(window.AMap)
      else reject(new Error('高德地图未就绪'))
    }
    script.onerror = () => {
      if (done) return
      done = true
      clearTimeout(timer)
      reject(new Error('高德地图脚本加载失败'))
    }
    document.head.appendChild(script)
  })

  return loading
}
