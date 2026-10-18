import request from '@/utils/request'

// 查询订单列表
export function listOrder(query) {
  return request({
    url: '/shop/order/list',
    method: 'get',
    params: query
  })
}

// 查询订单详细
export function getOrder(orderId) {
  return request({
    url: '/shop/order/' + orderId,
    method: 'get'
  })
}

// 查询待处理订单
export function listPendingOrders() {
  return request({
    url: '/shop/order/pending',
    method: 'get'
  })
}

// 核销订单
export function verifyOrder(orderId) {
  return request({
    url: '/shop/order/verify/' + orderId,
    method: 'post'
  })
}

// 更新订单状态
export function updateOrderStatus(data) {
  return request({
    url: '/shop/order/status',
    method: 'put',
    data: data
  })
}

// 完成订单
export function finishOrder(orderId) {
  return request({
    url: '/shop/order/finish/' + orderId,
    method: 'put'
  })
}

// 取消订单
export function cancelOrder(orderId, cancelReason) {
  return request({
    url: '/shop/order/cancel/' + orderId,
    method: 'put',
    data: { cancelReason }
  })
}

// 删除订单
export function delOrder(orderIds) {
  return request({
    url: '/shop/order/' + orderIds,
    method: 'delete'
  })
}
