import config, { STORAGE_KEYS } from '@/config'

/** 同一时刻只弹一次错误提示，避免并发请求刷屏 */
let lastToastAt = 0

function toast(title) {
  const now = Date.now()
  if (now - lastToastAt < 1200) return
  lastToastAt = now
  uni.showToast({ title: title || '请求失败', icon: 'none', duration: 2000 })
}

function readToken(auth) {
  if (auth === 'staff') return uni.getStorageSync(STORAGE_KEYS.STAFF_TOKEN) || ''
  if (auth === 'customer') return uni.getStorageSync(STORAGE_KEYS.CUSTOMER_TOKEN) || ''
  return ''
}

function buildHeader(options) {
  const header = { 'Content-Type': 'application/json', ...(options.header || {}) }
  const auth = options.auth === undefined ? 'customer' : options.auth
  const token = readToken(auth)
  if (token) {
    // 顾客端走自定义头，店员端复用若依后台 JWT
    if (auth === 'staff') header['Authorization'] = 'Bearer ' + token
    else header['X-Wx-Token'] = token
  }
  return header
}

function handleUnauthorized(auth) {
  if (auth === 'staff') {
    uni.removeStorageSync(STORAGE_KEYS.STAFF_TOKEN)
    uni.removeStorageSync(STORAGE_KEYS.STAFF_INFO)
    // 店员登录态失效时回到店员登录页，避免在工作台反复报错
    const pages = getCurrentPages()
    const route = pages.length ? pages[pages.length - 1].route : ''
    if (route !== 'pages/staff/login') {
      uni.reLaunch({ url: '/pages/staff/login' })
    }
  } else {
    uni.removeStorageSync(STORAGE_KEYS.CUSTOMER_TOKEN)
    uni.removeStorageSync(STORAGE_KEYS.CUSTOMER_INFO)
    // 通知 App.vue 清理 Pinia 中的登录态，避免页面仍显示“已登录”
    if (typeof uni.$emit === 'function') {
      uni.$emit('shop:customer-logout')
    }
  }
}

/**
 * 统一请求封装
 *
 * @param {Object}  options
 * @param {string}  options.url        接口路径（不含域名）
 * @param {string}  [options.method]   GET/POST/PUT/DELETE
 * @param {Object}  [options.data]     请求体或查询参数
 * @param {'customer'|'staff'|false} [options.auth] 鉴权方式，默认顾客端
 * @param {boolean} [options.showError] 失败是否自动提示，默认 true
 * @param {boolean} [options.loading]  是否展示全屏 loading
 * @param {string}  [options.loadingText]
 * @returns {Promise<any>} resolve 后端 data 字段（分页接口返回 { rows, total }）
 */
export function request(options) {
  const auth = options.auth === undefined ? 'customer' : options.auth

  if (options.loading) {
    uni.showLoading({ title: options.loadingText || '加载中', mask: true })
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: config.baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: buildHeader({ ...options, auth }),
      timeout: config.timeout,
      success(res) {
        if (options.loading) uni.hideLoading()

        const body = res.data || {}

        if (res.statusCode === 401) {
          handleUnauthorized(auth)
          if (options.showError !== false) toast('登录已过期，请重新登录')
          reject({ code: 401, msg: '未登录或登录已过期' })
          return
        }

        if (res.statusCode !== 200) {
          if (options.showError !== false) toast(body.msg || `服务异常(${res.statusCode})`)
          reject({ code: res.statusCode, msg: body.msg || '服务异常' })
          return
        }

        if (body.code === 200) {
          // 分页接口：把 rows/total 一起返回，方便调用方直接使用
          if (body.rows !== undefined) {
            resolve({ rows: body.rows, total: body.total, data: body.rows })
            return
          }
          // 若依 AjaxResult 有两种用法：success(data) 与 success().put(k, v)。
          // 后者没有 data 字段，业务字段平铺在响应根上，这里一并兼容。
          if (body.data !== undefined && body.data !== null) {
            resolve(body.data)
          } else {
            resolve(body)
          }
          return
        }

        if (body.code === 401) {
          handleUnauthorized(auth)
          if (options.showError !== false) toast(body.msg || '请先登录')
          reject({ code: 401, msg: body.msg || '请先登录' })
          return
        }

        if (options.showError !== false) toast(body.msg || '请求失败')
        reject({ code: body.code, msg: body.msg || '请求失败' })
      },
      fail(err) {
        if (options.loading) uni.hideLoading()
        if (options.showError !== false) {
          toast('网络连接失败，请检查后端服务')
        }
        reject({ code: -1, msg: '网络连接失败', raw: err })
      }
    })
  })
}

export const get = (url, data, options = {}) => request({ url, method: 'GET', data, ...options })
export const post = (url, data, options = {}) => request({ url, method: 'POST', data, ...options })
export const put = (url, data, options = {}) => request({ url, method: 'PUT', data, ...options })
export const del = (url, data, options = {}) => request({ url, method: 'DELETE', data, ...options })

export default request
