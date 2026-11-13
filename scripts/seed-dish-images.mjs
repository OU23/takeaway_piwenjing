#!/usr/bin/env node
/**
 * 批量给「商品 / 门店」配图（仅用于演示数据，图片来源于公开搜索引擎，禁止商用）。
 *
 * 依赖：Node 18+（用到内置 fetch / FormData / Blob），无需 npm 安装任何包。
 * 流程：读取商品列表 → 拼搜索词 → 搜索引擎抓候选图 → 下载字节 →
 *       走后端 /admin/files 上传进 MinIO → 绑定 objectKey 到商品。
 *
 * 用法示例：
 *   node scripts/seed-dish-images.mjs                    # 给所有缺图商品配图
 *   node scripts/seed-dish-images.mjs --dry              # 只预览，不下载不上传
 *   node scripts/seed-dish-images.mjs --only=103,106     # 只处理指定门店
 *   node scripts/seed-dish-images.mjs --force            # 已配图的也重换一张
 *   node scripts/seed-dish-images.mjs --shops            # 同时给门店 Logo / 头图配图
 *   node scripts/seed-dish-images.mjs --keyword=307=白米饭 --keyword=394=冰淇淋
 *   node scripts/seed-dish-images.mjs --local=D:/外卖/img # 搜索失败时用本地图片兜底
 *
 * 可选环境变量：
 *   XG_API           后端地址，默认 http://127.0.0.1:8090/api/v1
 *   XG_ADMIN_TOKEN   管理员兜底 token（原型模式即 admin_user.id），默认 1
 */
import { writeFileSync } from 'node:fs'
import { mkdir, readdir, readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT_DIR = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

// ------------------------------ 参数 ------------------------------
const args = process.argv.slice(2)
const opt = (name, def) => {
  const hit = args.find((a) => a === `--${name}` || a.startsWith(`--${name}=`))
  return hit ? (hit.includes('=') ? hit.split('=').slice(1).join('=') : true) : def
}
const listOpt = (name, def = []) => {
  const v = opt(name, '')
  return v && v !== true ? String(v).split(',').map((s) => s.trim()).filter(Boolean) : def
}
const DRY = !!opt('dry')
const FORCE = !!opt('force')
const WITH_SHOPS = !!opt('shops')
const VERIFY = !!opt('verify')
const ONLY = listOpt('only')
const LOCAL_DIR = opt('local', '') === true ? '' : String(opt('local', ''))
const DELAY = Number(opt('delay', 400))
const REPORT = path.resolve(ROOT_DIR, String(opt('report', 'scripts/dish-images-report.json')))
const keyArg = listOpt('keyword')
const KEYWORD_ARG = Object.fromEntries(keyArg.map((kv) => kv.split('=')).map(([k, v]) => [k, v]))

const API_BASE = String(process.env.XG_API || 'http://127.0.0.1:8090/api/v1').replace(/\/+$/, '')
const ADMIN_TOKEN = String(process.env.XG_ADMIN_TOKEN || '1')

const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36'
const ALLOWED_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/gif'])
const EXT_OF_TYPE = { 'image/jpeg': 'jpg', 'image/png': 'png', 'image/webp': 'webp', 'image/gif': 'gif' }
const MIN_BYTES = 8 * 1024
const MAX_BYTES = 4 * 1024 * 1024

// ------------------------------ 搜索词策略 ------------------------------
/** 营销前缀：进“菜品主体”搜索时去掉，避免搜到无关结果。 */
const MARKETING_PREFIX = ['招牌', '秘制', '农家', '手工', '经典', '特制', '自制', '现做', '招牌级']
/** 个别菜品手工指定关键词（自动规则搜不准时用）。 */
const KEYWORD_OVERRIDES = {
  '米饭': '白米饭',
  '卤蛋': '卤鸡蛋',
  '摇摇奶昔': '奶昔 饮品',
  '摩天脆脆冰淇淋': '冰淇淋蛋筒',
  '葡式蛋挞': '蛋挞',
  '鸡胸肉藜麦沙拉': '鸡胸肉沙拉',
  '招牌红烧牛肉面': '红烧牛肉面',
  '招牌珍珠奶茶': '珍珠奶茶',
  '经典珍珠奶茶': '珍珠奶茶',
  '招牌辣子鸡': '辣子鸡',
  '凉拌海带丝': '凉拌海带丝'
}

/** 依门店业态给出后缀，让“意面/柠檬水”这类通用词更贴合。 */
function categorySuffix(shopName = '') {
  if (/奶茶|茶饮|都可|冰城|柠檬|果茶/.test(shopName)) return ' 饮品'
  if (/沙拉|轻食/.test(shopName)) return ' 轻食'
  if (/面|粉/.test(shopName)) return ' 面食'
  if (/花/.test(shopName)) return ' 鲜花'
  return ' 菜品'
}

function buildKeyword(dishName, shopName) {
  if (KEYWORD_ARG[String(dishName)]) return KEYWORD_ARG[String(dishName)]
  if (KEYWORD_OVERRIDES[dishName]) return KEYWORD_OVERRIDES[dishName]
  let base = String(dishName)
  for (const p of MARKETING_PREFIX) if (base.startsWith(p) && base.length > p.length) base = base.slice(p.length)
  return base + categorySuffix(shopName)
}

// ------------------------------ 通用工具 ------------------------------
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function api(pathname, { method = 'GET', body, tokenType = 'admin', shopId } = {}) {
  const headers = {}
  if (tokenType === 'admin') headers['X-Admin-Token'] = ADMIN_TOKEN
  // 原型模式：merchant_user.id = shopId + 900（见 db/data.sql：101→1001）
  if (tokenType === 'merchant') headers['X-Merchant-Token'] = String(Number(shopId) + 900)
  if (body) headers['Content-Type'] = 'application/json'
  const res = await fetch(API_BASE + pathname, { method, headers, body, signal: AbortSignal.timeout(30000) })
  if (!res.ok) throw new Error(`HTTP ${res.status} ${pathname}`)
  const j = await res.json()
  if (j.code !== 0) throw new Error(`${j.message || '接口失败'}（${pathname}）`)
  return j.data
}

/** 拉取稍带重试的网络请求。 */
async function net(url, { headers = {}, timeout = 15000, tries = 2 } = {}) {
  let lastErr
  for (let i = 0; i < tries; i++) {
    try {
      return await fetch(url, { headers: { 'User-Agent': UA, 'Accept-Language': 'zh-CN,zh;q=0.9', ...headers }, signal: AbortSignal.timeout(timeout), redirect: 'follow' })
    } catch (e) {
      lastErr = e
      await sleep(500 * (i + 1))
    }
  }
  throw lastErr
}

// ------------------------------ 搜索引擎 ------------------------------
/** 百度图片（结构化 JSON，稳定最好用）。 */
async function searchBaidu(keyword, want = 8) {
  const url = `https://image.baidu.com/search/acjson?tn=resultjson_com&logid=${Date.now()}`
    + `&ipn=rj&ct=201326592&is=&fp=result&word=${encodeURIComponent(keyword)}&pn=0&rn=${want}`
  const res = await net(url)
  const text = await res.text()
  const j = JSON.parse(text.replace(/\\'/g, "'"))
  return (j.data || [])
    .filter((x) => x && (x.middleURL || x.thumbURL || x.hoverURL))
    .slice(0, want)
    .map((x) => ({ url: x.middleURL || x.hoverURL || x.thumbURL, referer: 'https://image.baidu.com/' }))
}

/** 360 图片，作为百度失败时的兜底源。 */
async function search360(keyword, want = 8) {
  const res = await net(`https://image.so.com/j?q=${encodeURIComponent(keyword)}&pn=0&sn=${want}`)
  const j = JSON.parse(await res.text())
  return (j.list || [])
    .filter((x) => x && (x.img || x.thumb))
    .slice(0, want)
    .map((x) => ({ url: x.img || x.thumb, referer: 'https://image.so.com/' }))
}

const SEARCHERS = [['百度图片', searchBaidu], ['360图片', search360]]

// ------------------------------ 下载 / 上传 / 绑定 ------------------------------
/** 逐个候选尝试下载，返回第一张真正可用的图片。 */
async function downloadFirst(candidates) {
  for (const c of candidates) {
    try {
      const res = await net(c.url, { headers: { Referer: c.referer, Accept: 'image/*,*/*;q=0.8' }, tries: 1 })
      if (!res.ok) continue
      const type = (res.headers.get('content-type') || '').split(';')[0].trim().toLowerCase()
      if (!ALLOWED_TYPES.has(type)) continue
      const buf = Buffer.from(await res.arrayBuffer())
      if (buf.length < MIN_BYTES || buf.length > MAX_BYTES) continue
      return { buf, type, from: c.url }
    } catch {
      // 换下一个候选
    }
    await sleep(150)
  }
  return null
}

/** 本地兜底轮转分配（用于离线或搜索引擎受限时）。 */
async function loadLocalImages(dir) {
  const exts = new Set(['.jpg', '.jpeg', '.png', '.webp', '.gif'])
  const entries = (await readdir(dir, { withFileTypes: true })).filter((e) => e.isFile() && exts.has(path.extname(e.name).toLowerCase()))
  const files = []
  for (const e of entries) {
    const full = path.join(dir, e.name)
    const type = { '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.png': 'image/png', '.webp': 'image/webp', '.gif': 'image/gif' }[path.extname(e.name).toLowerCase()]
    files.push({ buf: await readFile(full), type, name: e.name, from: full })
  }
  return files
}

async function uploadImage(pic, biz) {
  const fd = new FormData()
  const ext = EXT_OF_TYPE[pic.type] || 'jpg'
  const blob = new Blob([new Uint8Array(pic.buf)], { type: pic.type })
  fd.append('file', blob, `seed-${Date.now()}.${ext}`)
  const res = await fetch(`${API_BASE}/admin/files?biz=${biz}`, {
    method: 'POST',
    headers: { 'X-Admin-Token': ADMIN_TOKEN },
    body: fd,
    signal: AbortSignal.timeout(30000)
  })
  const j = await res.json()
  if (!res.ok || j.code !== 0) throw new Error(j.message || `上传失败 HTTP ${res.status}`)
  return j.data
}

const bindDish = (id, objectKey) => api(`/admin/files/bind/dish/${id}?objectKey=${encodeURIComponent(objectKey)}`, { method: 'POST' })

/** 公开直链前缀：<public-base-url>/<bucket>/<objectKey>（用于把 objectKey 拼成可访问 URL）。 */
async function publicBase() {
  const st = await api('/admin/files/status')
  return `${String(st.publicBaseUrl || st.endpoint).replace(/\/+$/, '')}/${st.bucket}`
}

/**
 * 判断已有图片是否还有效。
 * @returns {boolean} true=已在对象存储里且能公开读取；false=占位值、链接失效或被删除
 */
async function imageAlive(raw, base) {
  let url = String(raw || '')
  if (!url.includes('/')) return false // grad-* 之类的占位值
  if (!url.startsWith('http')) url = `${base}/${url.replace(/^\/+/, '')}`
  try {
    const res = await net(url, { tries: 1, timeout: 15000, headers: { Accept: 'image/*,*/*;q=0.8' } })
    return res.ok && ALLOWED_TYPES.has((res.headers.get('content-type') || '').split(';')[0])
  } catch {
    return false
  }
}

/** 巡检：确认已绑定的图片地址都能公开读取（--verify）。 */
async function verify() {
  const base = await publicBase()
  const shops = await api('/admin/shops')
  const targets = shops.filter((s) => !ONLY.length || ONLY.includes(String(s.shop.id)))
  let total = 0
  let placeholder = 0
  const bad = []
  const check = async (label, raw) => {
    if (!String(raw || '').includes('/')) {
      placeholder++
      console.log(`  - ${label} 仍是占位 ${raw}（未配图）`)
      return
    }
    total++
    if (!(await imageAlive(raw, base))) bad.push(`${label} 不可访问 ${raw}`)
  }
  for (const row of targets) {
    const shop = row.shop
    await check(`[${shop.id}] ${shop.name} logo`, shop.logo)
    await check(`[${shop.id}] ${shop.name} 头图`, shop.headerImg)
    const dishes = await api(`/admin/dishes?shopId=${shop.id}`)
    for (const d of dishes) await check(`[${shop.id}] ${d.name}`, d.image)
  }
  console.log(`巡检图片 ${total} 张，异常 ${bad.length} 张，未配图（占位）${placeholder} 项`)
  bad.forEach((b) => console.log('  ×', b))
}

// ------------------------------ 主流程 ------------------------------
async function main() {
  const report = { at: new Date().toISOString(), api: API_BASE, items: [] }
  const cache = new Map() // keyword -> objectKey，同名菜品复用同一张图
  const localImages = LOCAL_DIR ? await loadLocalImages(LOCAL_DIR) : []
  let localIdx = 0
  const base = await publicBase()

  const shops = await api('/admin/shops')
  const targets = shops.filter((s) => !ONLY.length || ONLY.includes(String(s.shop.id)))
  console.log(`门店 ${targets.length} 家；dry=${DRY ? '是' : '否'} force=${FORCE ? '是' : '否'} local=${localImages.length || 0} 张\n`)

  for (const row of targets) {
    const shop = row.shop
    const shopName = shop.name
    let dishes
    try {
      dishes = await api(`/admin/dishes?shopId=${shop.id}`)
    } catch (e) {
      console.log(`  × [${shop.id}] ${shopName} 商品拉取失败：${e.message}`)
      continue
    }
    // 占位值（grad-*）+ 已有 objectKey 但对象已失效（取不到或不是图片）的才需要重配
    const pending = dishes.filter((d) => !String(d.image || '').includes('/'))
    const withKey = dishes.filter((d) => String(d.image || '').includes('/'))
    const dead = FORCE ? withKey : []
    if (!FORCE) {
      const flags = await Promise.all(withKey.map((d) => imageAlive(d.image, base)))
      dead.push(...withKey.filter((_, i) => !flags[i]))
    }
    const todo = pending.concat(dead)
    console.log(`[${shop.id}] ${shopName}：商品 ${dishes.length} 个，待配图 ${todo.length} 个`)

    for (const d of todo) {
      const keyword = buildKeyword(d.name, shopName)
      const line = `  · ${d.id} ${d.name} ← “${keyword}”`
      if (DRY) {
        console.log(`${line}（dry）`)
        report.items.push({ id: d.id, name: d.name, shopId: shop.id, keyword, status: 'dry' })
        continue
      }
      try {
        let objectKey = cache.get(keyword)
        if (!objectKey) {
          const pic = (await fetchFromSearchers(keyword)) || pickLocal()
          if (!pic) {
            console.log(`${line} 未找到可用图片`)
            report.items.push({ id: d.id, name: d.name, shopId: shop.id, keyword, status: 'skipped' })
            continue
          }
          const up = await uploadImage(pic, 'dish')
          objectKey = up.objectKey
          cache.set(keyword, objectKey)
          await bindDish(d.id, objectKey)
          console.log(`${line} ✓ ${up.url}`)
          report.items.push({ id: d.id, name: d.name, shopId: shop.id, keyword, objectKey, url: up.url, source: pic.from, status: 'ok' })
        } else {
          await bindDish(d.id, objectKey)
          console.log(`${line} ✓ 复用同名图 ${objectKey}`)
          report.items.push({ id: d.id, name: d.name, shopId: shop.id, keyword, objectKey, status: 'ok(reused)' })
        }
        await sleep(DELAY)
      } catch (e) {
        console.log(`${line} × ${e.message}`)
        report.items.push({ id: d.id, name: d.name, shopId: shop.id, keyword, status: 'failed', error: String(e.message) })
      }
    }

    // 门店 Logo / 头图
    if (WITH_SHOPS) {
      const needsHeader = FORCE || !(await imageAlive(shop.headerImg, base))
      const needsLogo = FORCE || !(await imageAlive(shop.logo, base))
      if (!needsHeader && !needsLogo) continue
      const keyword = String(shopName).replace(/·.*$/, '') + (/花/.test(shopName) ? ' 花店' : ' 餐饮')
      if (DRY) {
        console.log(`  · 门店配图 ← “${keyword}”（dry）`)
        report.items.push({ shopId: shop.id, name: shopName, keyword, status: 'dry' })
        continue
      }
      try {
        const pic = (await fetchFromSearchers(keyword, '门店')) || pickLocal()
        if (!pic) {
          console.log(`  · 门店配图 未找到可用图片`)
          continue
        }
        const bound = []
        if (needsLogo) {
          const logo = await uploadImage(pic, 'shop')
          await bindShop(shop.id, logo.objectKey, 'logo')
          bound.push(`logo=${logo.objectKey}`)
        }
        if (needsHeader) {
          const header = await uploadImage(pic, 'header')
          await bindShop(shop.id, header.objectKey, 'header')
          bound.push(`header=${header.objectKey}`)
        }
        console.log(`  · 门店配图 ✓ ${bound.join(' ')}`)
        report.items.push({ shopId: shop.id, name: shopName, keyword, objectKey: bound.join(','), status: 'ok' })
        await sleep(DELAY)
      } catch (e) {
        console.log(`  · 门店配图 × ${e.message}`)
      }
    }
  }

  if (!DRY) {
    await mkdir(path.dirname(REPORT), { recursive: true })
    writeFileSync(REPORT, JSON.stringify(report, null, 2), 'utf8')
    const ok = report.items.filter((i) => String(i.status).startsWith('ok')).length
    console.log(`\n完成：成功 ${ok} 项，失败/跳过 ${report.items.length - ok} 项；报告 → ${REPORT}`)
  } else {
    console.log('\ndry 模式结束，未产生任何写操作。')
  }

  async function fetchFromSearchers(keyword, tag = '商品') {
    for (const [label, fn] of SEARCHERS) {
      try {
        const candidates = await fn(keyword)
        if (!candidates.length) continue
        const pic = await downloadFirst(candidates)
        if (pic) {
          pic.from = `${label}｜${pic.from}`
          return pic
        }
      } catch (e) {
        console.log(`    ${label} 源异常（${tag}）：${e.message}`)
      }
      await sleep(300)
    }
    return null
  }

  function pickLocal() {
    if (!localImages.length) return null
    const pic = localImages[localIdx % localImages.length]
    localIdx++
    return { ...pic, from: `本地兜底｜${pic.from}` }
  }
}

async function bindShop(shopId, objectKey, kind) {
  // Logo 走管理端接口；头图只有商家端接口，用商家令牌（原型：merchant_user.id = shopId + 900）。
  if (kind === 'logo') {
    return api(`/admin/files/bind/shop/${shopId}?objectKey=${encodeURIComponent(objectKey)}`, { method: 'POST' })
  }
  return api(`/merchant/shop/header?objectKey=${encodeURIComponent(objectKey)}`, { method: 'POST', tokenType: 'merchant', shopId })
}

(VERIFY ? verify() : main()).catch((e) => {
  console.error('运行失败：', e)
  process.exit(1)
})
