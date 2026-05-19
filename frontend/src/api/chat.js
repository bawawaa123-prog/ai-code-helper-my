import { apiClient } from "./client";
import { getStoredToken } from "../utils/request";

function parseEventBlock(block) {
  const lines = block.split("\n");
  const dataLines = [];

  for (const rawLine of lines) {
    const line = rawLine.trimEnd();

    if (!line || line.startsWith(":")) {
      continue;
    }

    if (line.startsWith("data:")) {
      dataLines.push(line.slice(5).trimStart());
    }
  }

  return dataLines.join("\n");
}

async function consumeStream(reader, onChunk) {
  const decoder = new TextDecoder("utf-8");
  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done });

    const blocks = buffer.split(/\r?\n\r?\n/);
    buffer = blocks.pop() || "";

    for (const block of blocks) {
      const data = parseEventBlock(block);
      if (data) {
        onChunk(data);
      }
    }

    if (done) {
      const finalData = parseEventBlock(buffer);
      if (finalData) {
        onChunk(finalData);
      }
      break;
    }
  }
}

function buildStreamHeaders(extraHeaders = {}) {
  const headers = {
    Accept: "text/event-stream",
    ...extraHeaders
  };

  const token = getStoredToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

async function handleStreamResponse(response, onChunk) {
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`);
  }

  if (!response.body) {
    throw new Error("当前浏览器不支持流式读取响应");
  }

  const reader = response.body.getReader();
  await consumeStream(reader, onChunk);
}

async function streamChatByMemoryId({ memoryId, message, useRag, signal, onChunk }) {
  const params = {
    memoryId,
    message
  };

  if (typeof useRag === "boolean") {
    params.useRag = useRag;
  }

  const url = apiClient.getUri({
    url: "/ai/chat",
    params
  });

  const response = await fetch(url, {
    method: "GET",
    headers: buildStreamHeaders(),
    signal
  });

  await handleStreamResponse(response, onChunk);
}

async function streamChatBySession({ sessionId, message, useRag, signal, onChunk }) {
  const response = await fetch("/api/ai/chat/stream", {
    method: "POST",
    headers: buildStreamHeaders({
      "Content-Type": "application/json"
    }),
    body: JSON.stringify({
      sessionId,
      message,
      useRag
    }),
    signal
  });

  await handleStreamResponse(response, onChunk);
}

export async function streamChat(options) {
  if (options?.sessionId != null) {
    return streamChatBySession(options);
  }
  return streamChatByMemoryId(options);
}

export { streamChatBySession };
