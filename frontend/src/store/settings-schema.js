/**
 * 设置项清单（单一事实来源）。
 *
 * 设置模块的键、默认值、取值范围都在这里登记；设置页 UI、本地校验、
 * 后端 UserSettingService.ALLOWED 白名单都以这份清单为准。
 * 新增设置项只需三步：
 *   1. 在 DEFAULT_SETTINGS 加默认值
 *   2. 在 SETTING_ITEMS 登记键路径 / 类型 / 取值范围 / 界面文案
 *   3. 后端 ALLOWED 白名单同步登记（防脏数据入库）
 * 不需要改任何表结构与通用代码。
 */

/** 默认值（与后端 UserSettingService.defaults() 保持一致） */
export const DEFAULT_SETTINGS = {
  appearance: {
    /** 界面主题：light 亮色 / dark 暗色 / system 跟随系统 */
    theme: 'system',
    /** 字体大小：small 小 / standard 标准 / large 大 */
    fontSize: 'standard'
  },
  notification: {
    /** 订单通知：接单、配送、送达等交易类通知 */
    order: true,
    /** 营销推送：优惠券、活动等推广消息 */
    promo: false,
    /** 免打扰：免打扰时段内不推送营销与评价提醒 */
    quietMode: false,
    /** 免打扰开始时刻 HH:mm */
    quietStart: '23:00',
    /** 免打扰结束时刻 HH:mm（跨零点合法，如 23:00 → 07:00） */
    quietEnd: '07:00'
  },
  privacy: {
    /** 个性化推荐：关闭后首页/猜你喜欢不再按浏览与下单行为排序 */
    personalization: true
  }
}

/**
 * 设置项定义（界面渲染 + 本地校验共用）。
 * - path    键路径（点分），后端白名单同名
 * - type    switch | choice | time
 * - options choice 的候选项
 * - showIf  条件显示（依赖其他设置项）
 */
export const SETTING_ITEMS = [
  // —— 通用 ——
  {
    path: 'appearance.theme',
    group: '通用',
    label: '外观模式',
    desc: '暗色模式降低夜间眩光，跟随系统自动切换',
    type: 'choice',
    options: [
      { value: 'light', label: '亮色' },
      { value: 'dark', label: '暗色' },
      { value: 'system', label: '跟随系统' }
    ]
  },
  {
    path: 'appearance.fontSize',
    group: '通用',
    label: '字体大小',
    desc: '影响全部页面的文字大小',
    type: 'choice',
    options: [
      { value: 'small', label: '小' },
      { value: 'standard', label: '标准' },
      { value: 'large', label: '大' }
    ]
  },
  // —— 消息通知 ——
  {
    path: 'notification.order',
    group: '消息通知',
    label: '订单通知',
    desc: '商家接单、骑手取货/送达、退款结果',
    type: 'switch'
  },
  {
    path: 'notification.promo',
    group: '消息通知',
    label: '营销推送',
    desc: '优惠券到账、限时活动、神券提醒',
    type: 'switch'
  },
  {
    path: 'notification.quietMode',
    group: '消息通知',
    label: '夜间免打扰',
    desc: '免打扰时段内不推送营销与评价提醒（订单通知不受影响）',
    type: 'switch'
  },
  {
    path: 'notification.quietStart',
    group: '消息通知',
    label: '免打扰开始',
    type: 'time',
    showIf: (s) => s.notification.quietMode
  },
  {
    path: 'notification.quietEnd',
    group: '消息通知',
    label: '免打扰结束',
    type: 'time',
    showIf: (s) => s.notification.quietMode
  },
  // —— 隐私 ——
  {
    path: 'privacy.personalization',
    group: '隐私',
    label: '个性化推荐',
    desc: '按浏览与下单行为优化首页排序；关闭后仅按距离与销量排序',
    type: 'switch'
  }
]

const HHMM = /^([01]\d|2[0-3]):[0-5]\d$/

/** 按点分路径读取设置对象中的值 */
export function getAtPath(obj, path) {
  return path.split('.').reduce((o, k) => (o == null ? o : o[k]), obj)
}

/** 按点分路径写入（中途缺失的层不创建——设置项必须在 DEFAULT_SETTINGS 中登记） */
export function setAtPath(obj, path, value) {
  const keys = path.split('.')
  const last = keys.pop()
  const parent = keys.reduce((o, k) => (o == null ? o : o[k]), obj)
  if (parent != null) parent[last] = value
}

/** 取设置项定义 */
export function getItem(path) {
  return SETTING_ITEMS.find((it) => it.path === path)
}

/**
 * 输入校验：单键合法性。返回 null 表示合法，否则返回错误文案。
 * 与后端 UserSettingService.validate 同规则（双侧校验，本地先拦）。
 */
export function validateSetting(path, value) {
  const item = getItem(path)
  if (!item) return `不支持的设置项：${path}`
  switch (item.type) {
    case 'switch':
      return typeof value === 'boolean' ? null : `${item.label}需为开/关`
    case 'choice':
      return item.options.some((o) => o.value === value) ? null : `${item.label}的取值不合法`
    case 'time':
      return HHMM.test(value) ? null : `${item.label}需为 HH:mm 格式的时刻`
    default:
      return `未知设置类型：${item.type}`
  }
}

/**
 * 归一化：任意来源（localStorage / 服务端）的设置对象 → 默认值 ∪ 合法值。
 * 未知键丢弃、非法值回落默认，保证 store 里的数据永远完整可用。
 */
export function normalizeSettings(raw) {
  const merged = structuredClone(DEFAULT_SETTINGS)
  if (!raw || typeof raw !== 'object') return merged
  for (const item of SETTING_ITEMS) {
    const v = getAtPath(raw, item.path)
    if (v === undefined || validateSetting(item.path, v) !== null) continue
    setAtPath(merged, item.path, v)
  }
  return merged
}

/** 按分组输出 [{ group, items }]，供设置页按卡片渲染 */
export function groupedItems() {
  const groups = []
  for (const item of SETTING_ITEMS) {
    let g = groups.find((x) => x.group === item.group)
    if (!g) groups.push((g = { group: item.group, items: [] }))
    g.items.push(item)
  }
  return groups
}
