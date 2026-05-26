import { getStoredToken, request } from "../utils/request";

async function requestFormData(url, formData) {
  const token = getStoredToken();
  const headers = token ? { Authorization: `Bearer ${token}` } : undefined;
  const response = await fetch(url, {
    method: "POST",
    headers,
    body: formData
  });
  const text = await response.text();
  const result = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const errorMessage = result?.message || `Request failed: ${response.status}`;
    throw new Error(errorMessage);
  }

  if (!result) {
    return null;
  }

  if (result.code !== 0) {
    throw new Error(result.message || "Request failed");
  }

  return result.data;
}

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

export function getKnowledgeDocumentList(knowledgeBaseId) {
  return request.get(`/api/knowledge/base/${knowledgeBaseId}/document/list`);
}

export function uploadKnowledgeDocument(knowledgeBaseId, file) {
  const formData = new FormData();
  formData.append("file", file);
  return requestFormData(`/api/knowledge/base/${knowledgeBaseId}/document/upload`, formData);
}

export function parseKnowledgeDocument(knowledgeBaseId, documentId) {
  return request.post(`/api/knowledge/base/${knowledgeBaseId}/document/${documentId}/parse`);
}

export function vectorizeKnowledgeDocument(knowledgeBaseId, documentId) {
  return request.post(`/api/knowledge/base/${knowledgeBaseId}/document/${documentId}/vectorize`);
}

export function deleteKnowledgeDocument(knowledgeBaseId, documentId) {
  return request.delete(`/api/knowledge/base/${knowledgeBaseId}/document/${documentId}`);
}

export function getKnowledgeSegmentList(knowledgeBaseId, documentId) {
  return request.get(`/api/knowledge/base/${knowledgeBaseId}/document/${documentId}/segment/list`);
}
