import request from "@/utils/request";

// 查询知识库文档列表
export function listDocument(query) {
  return request({
    url: "/kb/document/list",
    method: "get",
    params: query,
  });
}

// 查询文档详细（含切片）
export function getDocument(docId) {
  return request({
    url: "/kb/document/" + docId,
    method: "get",
  });
}

// 查询文档切片
export function listChunks(docId) {
  return request({
    url: "/kb/document/" + docId + "/chunks",
    method: "get",
  });
}

// 删除文档
export function delDocument(docIds) {
  return request({
    url: "/kb/document/" + docIds,
    method: "delete",
  });
}

// 重新入库
export function retryDocument(docId) {
  return request({
    url: "/kb/document/retry/" + docId,
    method: "post",
  });
}
