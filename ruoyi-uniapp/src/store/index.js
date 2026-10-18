import { createPinia, defineStore } from 'pinia'
import { STORAGE_KEYS, SHOP_INFO } from '@/config'
import { sumMoney } from '@/utils'
import { setCustomerLogin, clearCustomerLogin } from '@/utils/auth'

const pinia = createPinia()

/** 本地缓存可能因旧版本格式损坏，读取时做一次类型兜底 */
function readStoredCart() {
  const stored = uni.getStorageSync(STORAGE_KEYS.CART)
  return Array.isArray(stored) ? stored : []
}

/**
 * 购物车
 * 每项结构：
 * {
 *   key,            // 商品ID + 口味组合，用于合并同款
 *   productId,
 *   productName,
 *   productImage,
 *   price,          // 基础单价（元）
 *   flavorIds: [],
 *   flavorText,     // "少冰/三分糖/加珍珠(+2)"
 *   flavorAmount,   // 口味加价合计（元）
 *   quantity
 * }
 */
export const useCartStore = defineStore('cart', {
  state: () => ({
    items: readStoredCart(),
    tableNo: uni.getStorageSync(STORAGE_KEYS.TABLE_NO) || '',
    shopInfo: SHOP_INFO
  }),

  getters: {
    totalCount: state => state.items.reduce((sum, item) => sum + (item.quantity || 0), 0),

    /** 商品件数（用于“共 N 件”） */
    totalQuantity: state => state.items.reduce((sum, item) => sum + (item.quantity || 0), 0),

    totalPrice: state => sumMoney(state.items.map(item => (Number(item.price) + Number(item.flavorAmount || 0)) * (item.quantity || 0))),

    isEmpty: state => state.items.length === 0,

    /** 按商品聚合的数量，用于菜单页显示“已选 N” */
    countByProduct: state => {
      const map = {}
      state.items.forEach(item => {
        map[item.productId] = (map[item.productId] || 0) + (item.quantity || 0)
      })
      return map
    }
  },

  actions: {
    buildKey(productId, flavorIds) {
      const ids = [...(flavorIds || [])].map(Number).sort((a, b) => a - b)
      return `${productId}_${ids.join('-')}`
    },

    addItem(item) {
      const key = this.buildKey(item.productId, item.flavorIds)
      const exist = this.items.find(i => i.key === key)
      if (exist) {
        exist.quantity += item.quantity || 1
      } else {
        this.items.push({
          key,
          productId: item.productId,
          productName: item.productName,
          productImage: item.productImage || '',
          price: Number(item.price) || 0,
          flavorIds: item.flavorIds || [],
          flavorText: item.flavorText || '',
          flavorAmount: Number(item.flavorAmount) || 0,
          quantity: item.quantity || 1
        })
      }
      this.save()
    },

    increaseItem(index, step = 1) {
      const item = this.items[index]
      if (!item) return
      item.quantity += step
      this.save()
    },

    decreaseItem(index, step = 1) {
      const item = this.items[index]
      if (!item) return
      if (item.quantity > step) {
        item.quantity -= step
      } else {
        this.items.splice(index, 1)
      }
      this.save()
    },

    removeItem(index) {
      this.items.splice(index, 1)
      this.save()
    },

    /** 移除某商品的全部规格（菜单页长按/清空用） */
    removeByProduct(productId) {
      this.items = this.items.filter(i => i.productId !== productId)
      this.save()
    },

    clear() {
      this.items = []
      this.save()
    },

    setTableNo(tableNo) {
      this.tableNo = tableNo || ''
      uni.setStorageSync(STORAGE_KEYS.TABLE_NO, this.tableNo)
    },

    /** 购物车换算成下单请求体 */
    toOrderItems() {
      return this.items.map(item => ({
        productId: item.productId,
        flavorIds: item.flavorIds || [],
        quantity: item.quantity
      }))
    },

    save() {
      uni.setStorageSync(STORAGE_KEYS.CART, this.items)
    }
  }
})

/**
 * 顾客信息
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: uni.getStorageSync(STORAGE_KEYS.CUSTOMER_TOKEN) || '',
    info: uni.getStorageSync(STORAGE_KEYS.CUSTOMER_INFO) || {}
  }),

  getters: {
    isLogin: state => !!state.token,
    nickName: state => state.info.nickName || '微信顾客',
    avatarUrl: state => state.info.avatarUrl || '',
    wxUserId: state => state.info.wxUserId || null
  },

  actions: {
    login(token, info) {
      this.token = token
      this.info = info || {}
      setCustomerLogin(token, this.info)
    },

    updateInfo(info) {
      this.info = { ...this.info, ...(info || {}) }
      uni.setStorageSync(STORAGE_KEYS.CUSTOMER_INFO, this.info)
    },

    logout() {
      this.token = ''
      this.info = {}
      clearCustomerLogin()
    }
  }
})

export default pinia
