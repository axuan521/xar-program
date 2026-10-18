import request from '@/utils/request'

// 查询顾客列表
export function listWxUser(query) {
  return request({
    url: '/shop/wxuser/list',
    method: 'get',
    params: query
  })
}

// 查询顾客详细
export function getWxUser(wxUserId) {
  return request({
    url: '/shop/wxuser/' + wxUserId,
    method: 'get'
  })
}

// 修改顾客状态
export function changeWxUserStatus(wxUserId, status) {
  return request({
    url: '/shop/wxuser/changeStatus',
    method: 'put',
    data: { wxUserId, status }
  })
}

// 修改顾客信息
export function updateWxUser(data) {
  return request({
    url: '/shop/wxuser',
    method: 'put',
    data: data
  })
}

// 删除顾客
export function delWxUser(wxUserIds) {
  return request({
    url: '/shop/wxuser/' + wxUserIds,
    method: 'delete'
  })
}
