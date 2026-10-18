import { get, post } from '@/utils/request'

/**
 * 小程序顾客登录
 * @param {string} code wx.login 得到的临时凭证
 * @param {string} [nickName]
 * @param {string} [avatarUrl]
 */
export function wxLogin(code, nickName = '', avatarUrl = '') {
  return post('/mp/auth/login', { code, nickName, avatarUrl }, { auth: false, showError: false })
}

/** 获取当前顾客信息 */
export function getCustomerInfo() {
  return post('/mp/auth/info', {}, { auth: 'customer', showError: false })
}

/** 顾客退出登录 */
export function customerLogout() {
  return post('/mp/auth/logout', {}, { auth: 'customer', showError: false })
}

/** 后台登录（店员复用若依账号） */
export function staffLogin(data) {
  return post('/login', data, { auth: false, showError: false })
}

/** 后台登录用户信息（含角色与权限） */
export function staffGetInfo() {
  return get('/getInfo', {}, { auth: 'staff', showError: false })
}

/** 获取图形验证码配置 */
export function getCaptchaImage() {
  return get('/captchaImage', {}, { auth: false, showError: false })
}
