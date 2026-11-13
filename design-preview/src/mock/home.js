/**
 * 用户端首页演示数据（Mock）。
 * 接口接入后，此文件的数据来源替换为后端 /api/v1 下的用户端接口，页面结构不变。
 */

export const city = '深圳·南山'

export const banner = {
  tag: 'NEW USER GIFT',
  title: '首单立减 15 元',
  actions: [
    { label: '立即领取', primary: true },
    { label: '到店自取再减 3 元', primary: false }
  ]
}

export const categories = [
  { name: '快餐', emoji: '🍔', bg: 'linear-gradient(135deg,#FFE7C6,#FFC48A)' },
  { name: '饮品', emoji: '🥤', bg: 'linear-gradient(135deg,#DDEFFF,#B9DBFF)' },
  { name: '甜品', emoji: '🍰', bg: 'linear-gradient(135deg,#FFE1EC,#FFB7CE)' },
  { name: '烧烤', emoji: '🍤', bg: 'linear-gradient(135deg,#FFEBD2,#FFD08F)' },
  { name: '火锅', emoji: '🍲', bg: 'linear-gradient(135deg,#FFE0DA,#FFB2A6)' },
  { name: '超市', emoji: '🏪', bg: 'linear-gradient(135deg,#E6F7EF,#B7E7D2)' },
  { name: '生鲜', emoji: '🥬', bg: 'linear-gradient(135deg,#EAF8DC,#C8EBA6)' },
  { name: '药品', emoji: '💊', bg: 'linear-gradient(135deg,#E9F0FF,#C6D6FF)' },
  { name: '鲜花', emoji: '🌸', bg: 'linear-gradient(135deg,#FDE7F6,#F3C4EA)' },
  { name: '更多', emoji: '⋯', bg: 'linear-gradient(135deg,#F1F2F6,#E1E4EB)' }
]

export const seckill = {
  title: '限时秒杀',
  endAt: Date.now() + 1000 * 60 * 36 + 1000 * 20,
  items: [
    { name: '香辣鸡腿堡', price: 999, origin: 1800, emoji: '🍔', bg: 'linear-gradient(135deg,#FFE7C6,#FFC48A)' },
    { name: '杨枝甘露', price: 880, origin: 1600, emoji: '🧋', bg: 'linear-gradient(135deg,#FFF1D6,#FFDA9E)' },
    { name: '牛肉拉面', price: 1290, origin: 2200, emoji: '🍜', bg: 'linear-gradient(135deg,#FFE0DA,#FFB2A6)' }
  ]
}

export const coupons = [
  {
    key: 'everyday',
    title: '天天神券',
    amount: '12',
    desc: '满30减12',
    note: '每日限领 · 全平台通用'
  },
  {
    key: 'pickup',
    title: '到店自取',
    amount: '3',
    desc: '立减3元',
    note: '免配送费 · 到店即走'
  }
]

export const sorts = [
  { key: 'default', label: '综合' },
  { key: 'distance', label: '距离最近' },
  { key: 'sales', label: '销量最高' },
  { key: 'rating', label: '评分最高' }
]

export const shops = [
  {
    id: 1,
    name: '麦当劳（南山店）',
    emoji: '🍔',
    bg: 'linear-gradient(135deg,#FFE7C6,#FFC48A)',
    rating: 4.8,
    monthSales: 2314,
    minPrice: 2000,
    deliveryFee: 0,
    distance: 1.2,
    etaMinutes: 28,
    selfPickup: true,
    tags: ['满30减5', '新客立减3', '支持自取']
  },
  {
    id: 2,
    name: '喜茶（海岸城店）',
    emoji: '🧋',
    bg: 'linear-gradient(135deg,#DDEFFF,#B9DBFF)',
    rating: 4.7,
    monthSales: 1862,
    minPrice: 1500,
    deliveryFee: 300,
    distance: 0.8,
    etaMinutes: 22,
    selfPickup: true,
    tags: ['满25减6', '支持自取']
  },
  {
    id: 3,
    name: '海底捞（科技园店）',
    emoji: '🍲',
    bg: 'linear-gradient(135deg,#FFE0DA,#FFB2A6)',
    rating: 4.9,
    monthSales: 968,
    minPrice: 5000,
    deliveryFee: 600,
    distance: 2.6,
    etaMinutes: 45,
    selfPickup: false,
    tags: ['满100减20', '品牌商家']
  },
  {
    id: 4,
    name: '木屋烧烤（南油店）',
    emoji: '🍤',
    bg: 'linear-gradient(135deg,#FFEBD2,#FFD08F)',
    rating: 4.5,
    monthSales: 743,
    minPrice: 3000,
    deliveryFee: 400,
    distance: 1.9,
    etaMinutes: 35,
    selfPickup: false,
    tags: ['满50减8']
  },
  {
    id: 5,
    name: '百果园（登良路店）',
    emoji: '🍎',
    bg: 'linear-gradient(135deg,#EAF8DC,#C8EBA6)',
    rating: 4.6,
    monthSales: 452,
    minPrice: 1000,
    deliveryFee: 200,
    distance: 0.6,
    etaMinutes: 18,
    selfPickup: true,
    tags: ['满20减3', '支持自取']
  }
]
