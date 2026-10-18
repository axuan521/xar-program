import { get, post, put } from '@/utils/request'

/** 待处理订单（待收款 / 待制作 / 制作中 / 待取餐） */
export function getPendingOrders() {
  return get('/shop/order/pending', {}, { auth: 'staff' })
}

/** 订单分页列表（后台口径，含历史订单） */
export function getOrderList(params = {}) {
  return get('/shop/order/list', params, { auth: 'staff' })
}

/** 订单详情 */
export function getStaffOrderDetail(orderId) {
  return get(`/shop/order/${orderId}`, {}, { auth: 'staff' })
}

/**
 * 按取餐码查单（店员扫码/手输）
 */
export function getOrderByPickupCode(pickupCode) {
  return get(`/shop/order/pickup/${pickupCode}`, {}, { auth: 'staff', showError: false })
}

/** 确认收款 / 核销取餐 */
export function verifyOrder(orderId) {
  return post(`/shop/order/verify/${orderId}`, {}, { auth: 'staff' })
}

/** 更新订单状态（1待制作 2制作中 3待取餐） */
export function updateOrderStatus(orderId, orderStatus) {
  return put('/shop/order/status', { orderId, orderStatus }, { auth: 'staff' })
}

/** 取消订单 */
export function cancelStaffOrder(orderId, cancelReason) {
  return put(`/shop/order/cancel/${orderId}`, { cancelReason }, { auth: 'staff' })
}

/** 完成订单 */
export function finishOrder(orderId) {
  return put(`/shop/order/finish/${orderId}`, {}, { auth: 'staff' })
}
