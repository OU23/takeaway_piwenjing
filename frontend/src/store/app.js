import { defineStore } from 'pinia'
import { getCartCount, getDict, getUserProfile } from '../api'

/** 全局状态：购物车角标、字典、当前用户。 */
export const useAppStore = defineStore('app', {
  state: () => ({
    cartCount: 0,
    dict: null,
    user: null,
    loaded: false
  }),
  getters: {
    orderStatusLabel: (s) => (code) => s.dict?.orderStatus?.[code] || code,
    orderGroups: (s) => s.dict?.orderGroup || ['全部', '待付款', '待接单', '进行中', '待评价', '退款售后']
  },
  actions: {
    async bootstrap() {
      const tasks = [this.refreshCartCount(), this.loadDict(), this.loadUser()]
      await Promise.allSettled(tasks)
      this.loaded = true
    },
    async refreshCartCount() {
      try {
        const r = await getCartCount()
        this.cartCount = r.data || 0
      } catch (e) {
        /* 静默失败，不影响主流程 */
      }
    },
    async loadDict() {
      try {
        const r = await getDict()
        this.dict = r.data
      } catch (e) {
        /* ignore */
      }
    },
    async loadUser() {
      try {
        const r = await getUserProfile()
        this.user = r.data
      } catch (e) {
        /* ignore */
      }
    }
  }
})
