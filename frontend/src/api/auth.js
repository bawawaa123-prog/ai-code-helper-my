import { request } from "../utils/request";

const TOKEN_KEY = "token";
const USER_KEY = "loginUser";

export function register(data) {
  return request.post("/api/user/register", data);
}

export function login(data) {
  return request.post("/api/user/login", data);
}

export function getCurrentUser() {
  return request.get("/api/user/me");
}

export function logout() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}
