import { get } from '@/utils/request'

/** 分类列表（仅正常状态） */
export function getCategoryList() {
  return get('/mp/product/category/list', {}, { auth: false })
}

/**
 * 可售商品列表
 * @param {number} [categoryId] 分类ID
 */
export function getProductList(categoryId) {
  return get('/mp/product/list', categoryId ? { categoryId } : {}, { auth: false })
}

/** 商品详情（含口味规格） */
export function getProductDetail(productId) {
  return get(`/mp/product/${productId}`, {}, { auth: false })
}
