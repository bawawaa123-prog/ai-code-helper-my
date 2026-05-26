import { request } from "../utils/request";

export function getKnowledgeBaseList() {
  return request.get("/api/knowledge/base/list");
}

export function createKnowledgeBase(data) {
  return request.post("/api/knowledge/base", data);
}

export function updateKnowledgeBase(knowledgeBaseId, data) {
  return request.put(`/api/knowledge/base/${knowledgeBaseId}`, data);
}

export function deleteKnowledgeBase(knowledgeBaseId) {
  return request.delete(`/api/knowledge/base/${knowledgeBaseId}`);
}
