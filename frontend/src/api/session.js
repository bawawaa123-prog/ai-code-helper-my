import { request } from "../utils/request";

export function createSession(data) {
  return request.post("/api/chat/session", data);
}

export function listSessions() {
  return request.get("/api/chat/session/list");
}

export function getSessionMessages(sessionId) {
  return request.get(`/api/chat/session/${sessionId}/messages`);
}

export function updateSessionTitle(sessionId, data) {
  return request.put(`/api/chat/session/${sessionId}`, data);
}

export function deleteSession(sessionId) {
  return request.delete(`/api/chat/session/${sessionId}`);
}
