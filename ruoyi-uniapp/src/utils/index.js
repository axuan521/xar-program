import { BASE_URL } from '@/config'

/**
 * 格式化时间
 * @param {Date|string|number} date 时间
 * @param {string} fmt 格式模板
 * @returns {string}
 */
export function formatTime(date, fmt = 'YYYY-MM-DD HH:mm:ss') {
  if (!date) return ''
  // iOS 不识别 "2026-10-18 13:00:00"，统一替换成 "/"
  const d = new Date(typeof date === 'string' ? date.replace(/-/g, '/') : date)
  if (isNaN(d.getTime())) return ''

  const o = {
    'M+': d.getMonth() + 1,
    'D+': d.getDate(),
    'H+': d.getHours(),
    'm+': d.getMinutes(),
    's+': d.getSeconds()
  }

  if (/(Y+)/.test(fmt)) {
    fmt = fmt.replace(RegExp.$1, (d.getFullYear() + '').substr(4 - RegExp.$1.length))
  }

  for (const k in o) {
    if (new RegExp(`(${k})`).test(fmt)) {
      const str = o[k] + ''
      fmt = fmt.replace(RegExp.$1, RegExp.$1.length === 1 ? str : padLeftZero(str))
    }
  }

  return fmt
}

function padLeftZero(str) {
  return ('00' + str).substr(str.length)
}

/** 只显示时分 */
export function formatTimeHM(date) {
  return formatTime(date, 'HH:mm')
}

/** 友好时间：今天 12:30 / 昨天 12:30 / 10-16 12:30 */
export function formatFriendlyTime(date) {
  if (!date) return ''
  const d = new Date(typeof date === 'string' ? date.replace(/-/g, '/') : date)
  if (isNaN(d.getTime())) return ''
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  if (sameDay) return formatTime(d, 'HH:mm')
  const yesterday = new Date(now.getTime() - 86400000)
  if (d.toDateString() === yesterday.toDateString()) return '昨天 ' + formatTime(d, 'HH:mm')
  return formatTime(d, 'MM-DD HH:mm')
}

/**
 * 金额格式化：后端 BigDecimal 可能返回 "15" / "15.0" / 15
 * @param {number|string} value 金额（元）
 * @returns {string} 保留两位小数的字符串
 */
export function formatMoney(value) {
  const num = Number(value)
  return isNaN(num) ? '0.00' : num.toFixed(2)
}

/**
 * 金额计算（避免浮点误差）：累加后再四舍五入到分
 * @param {Array<number|string>} values
 */
export function sumMoney(values) {
  return Math.round(values.reduce((sum, v) => sum + Number(v || 0) * 100, 0)) / 100
}

/**
 * 商品图片地址处理：数据库存的是相对路径，需要拼上后端域名
 * @param {string} path
 * @returns {string} 可直接给 <image> 使用的地址
 */
export function resolveImage(path) {
  if (!path) return ''
  if (/^(https?:)?\/\//.test(path) || path.startsWith('data:')) return path
  if (path.startsWith('/static/')) return path
  return BASE_URL + (path.startsWith('/') ? path : '/' + path)
}

/**
 * 取餐码归一化：用户可能输入小写或带空格
 */
export function normalizePickupCode(code) {
  return (code || '').replace(/\s/g, '').toUpperCase()
}

/**
 * 防抖函数
 */
export function debounce(fn, delay = 300) {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

/**
 * 节流函数
 */
export function throttle(fn, interval = 300) {
  let lastTime = 0
  return function (...args) {
    const now = Date.now()
    if (now - lastTime >= interval) {
      lastTime = now
      fn.apply(this, args)
    }
  }
}

/** 深拷贝 */
export function deepClone(obj) {
  if (obj === null || typeof obj !== 'object') return obj
  if (obj instanceof Date) return new Date(obj)
  if (obj instanceof Array) return obj.map(item => deepClone(item))
  const copy = {}
  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
      copy[key] = deepClone(obj[key])
    }
  }
  return copy
}

/** 生成唯一 ID */
export function generateId() {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

/** 手机号校验 */
export function isValidPhone(phone) {
  return /^1[3-9]\d{9}$/.test(phone)
}

/**
 * 复制文本
 */
export function copyText(text, tip = '已复制') {
  return new Promise(resolve => {
    uni.setClipboardData({
      data: String(text == null ? '' : text),
      success: () => {
        uni.showToast({ title: tip, icon: 'none' })
        resolve(true)
      },
      fail: () => resolve(false)
    })
  })
}
