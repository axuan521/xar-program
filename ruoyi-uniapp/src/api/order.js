import { get, post, put } from '@/utils/request'

/**
 * 创建订单
 * @param {Array<{productId:number, flavorIds:number[], quantity:number}>} items
 * @param {string} [remark]
 */
export function createOrder(items, remark = '') {
  return post('/mp/order/create', { items, remark }, { auth: 'customer' })
}

/** 我的订单列表 */
export function getMyOrders(orderStatus = '') {
  return get('/mp/order/my', orderStatus ? { orderStatus } : {}, { auth: 'customer' })
}

/** 订单详情 */
export function getOrderDetail(orderId) {
  return get(`/mp/order/${orderId}`, {}, { auth: 'customer' })
}

/** 取消订单 */
export function cancelOrder(orderId) {
  return put(`/mp/order/cancel/${orderId}`, {}, { auth: 'customer' })
}

/** 当前支付通道（是否支持微信支付 / 模拟支付） */
export function getPayType() {
  return get('/mp/order/payType', {}, { auth: false, showError: false })
}

/**
 * 发起支付
 * @returns {Promise<{payType:string, orderNo:string, amountYuan:string, params:Object, wxPayEnabled:boolean, mockPayEnabled:boolean}>}
 */
export function preparePay(orderId) {
  return get(`/mp/order/pay/${orderId}`, {}, { auth: 'customer' })
}

/** 模拟支付（开发联调，需后端开启 wx.pay.mock-enabled） */
export function mockPay(orderId) {
  return post(`/mp/order/mock-pay/${orderId}`, {}, { auth: 'customer' })
}

/** 按取餐码查单（顾客端核销辅助，仅当日可核销订单） */
export function findByPickupCode(pickupCode) {
  return get('/mp/order/find-by-pickup-code', { pickupCode }, { auth: 'customer', showError: false })
}
