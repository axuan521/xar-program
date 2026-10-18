import request from "@/utils/request";

// 查询商品列表
export function listProduct(query) {
  return request({ url: "/shop/product/list", method: "get", params: query });
}

// 查询商品详细（含口味规格）
export function getProduct(productId) {
  return request({ url: "/shop/product/" + productId, method: "get" });
}

// 新增商品
export function addProduct(data) {
  return request({ url: "/shop/product", method: "post", data });
}

// 修改商品
export function updateProduct(data) {
  return request({ url: "/shop/product", method: "put", data });
}

// 删除商品
export function delProduct(productIds) {
  return request({ url: "/shop/product/" + productIds, method: "delete" });
}
