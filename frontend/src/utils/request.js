const TOKEN_KEY = "token";

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function buildHeaders(customHeaders = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...customHeaders
  };

  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

export async function request(url, options = {}) {
  const {
    method = "GET",
    headers,
    body,
    ...restOptions
  } = options;

  const finalOptions = {
    method,
    headers: buildHeaders(headers),
    ...restOptions
  };

  if (body !== undefined && body !== null) {
    finalOptions.body =
      typeof body === "string" ? body : JSON.stringify(body);
  }

  const response = await fetch(url, finalOptions);
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

request.get = (url, options = {}) => request(url, { ...options, method: "GET" });
request.post = (url, body, options = {}) => request(url, { ...options, method: "POST", body });
request.put = (url, body, options = {}) => request(url, { ...options, method: "PUT", body });
request.delete = (url, options = {}) => request(url, { ...options, method: "DELETE" });

export function getStoredToken() {
  return getToken();
}
