import request from "@/utils/request";

// 查询商品分类列表
export function listCategory(query) {
  return request({ url: "/shop/category/list", method: "get", params: query });
}

// 查询全部分类（下拉选项）
export function optionselectCategory() {
  return request({ url: "/shop/category/optionselect", method: "get" });
}

// 查询分类详细
export function getCategory(categoryId) {
  return request({ url: "/shop/category/" + categoryId, method: "get" });
}

// 新增分类
export function addCategory(data) {
  return request({ url: "/shop/category", method: "post", data });
}

// 修改分类
export function updateCategory(data) {
  return request({ url: "/shop/category", method: "put", data });
}

// 删除分类
export function delCategory(categoryIds) {
  return request({ url: "/shop/category/" + categoryIds, method: "delete" });
}
