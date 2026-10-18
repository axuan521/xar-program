import { STORAGE_KEYS } from '@/config'

/**
 * 顾客登录态
 */
export function getCustomerToken() {
  return uni.getStorageSync(STORAGE_KEYS.CUSTOMER_TOKEN) || ''
}

export function getCustomerInfo() {
  return uni.getStorageSync(STORAGE_KEYS.CUSTOMER_INFO) || {}
}

export function setCustomerLogin(token, info) {
  uni.setStorageSync(STORAGE_KEYS.CUSTOMER_TOKEN, token || '')
  uni.setStorageSync(STORAGE_KEYS.CUSTOMER_INFO, info || {})
}

export function clearCustomerLogin() {
  uni.removeStorageSync(STORAGE_KEYS.CUSTOMER_TOKEN)
  uni.removeStorageSync(STORAGE_KEYS.CUSTOMER_INFO)
}

export function isCustomerLogin() {
  return !!getCustomerToken()
}

/**
 * 店员登录态（复用若依后台 JWT）
 */
export function getStaffToken() {
  return uni.getStorageSync(STORAGE_KEYS.STAFF_TOKEN) || ''
}

export function getStaffInfo() {
  return uni.getStorageSync(STORAGE_KEYS.STAFF_INFO) || {}
}

export function setStaffLogin(token, info) {
  uni.setStorageSync(STORAGE_KEYS.STAFF_TOKEN, token || '')
  uni.setStorageSync(STORAGE_KEYS.STAFF_INFO, info || {})
}

export function clearStaffLogin() {
  uni.removeStorageSync(STORAGE_KEYS.STAFF_TOKEN)
  uni.removeStorageSync(STORAGE_KEYS.STAFF_INFO)
}

export function isStaffLogin() {
  return !!getStaffToken()
}

/**
 * 需要顾客登录才能继续，未登录时引导到登录页
 *
 * @param {string} redirect 登录成功后要回到的页面（含参数）
 * @returns {boolean} 是否已登录
 */
export function requireCustomer(redirect) {
  if (isCustomerLogin()) return true
  const query = redirect ? `?redirect=${encodeURIComponent(redirect)}` : ''
  uni.navigateTo({ url: `/pages/login/index${query}` })
  return false
}

/**
 * 需要店员登录才能继续
 */
export function requireStaff() {
  if (isStaffLogin()) return true
  uni.navigateTo({ url: '/pages/staff/login' })
  return false
}
