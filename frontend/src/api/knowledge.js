import { request } from "../utils/request";

export function getKnowledgeBaseList() {
  return request.get("/api/knowledge/base/list");
}
