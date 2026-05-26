import { apiClient } from "./client";
import { getStoredToken } from "../utils/request";

function parseEventBlock(block) {
  const lines = block.split("\n");
  let event = "";
  const dataLines = [];

  for (const rawLine of lines) {
    const line = rawLine.trimEnd();

    if (!line || line.startsWith(":")) {
      continue;
    }

    if (line.startsWith("event:")) {
      event = line.slice(6).trimStart();
      continue;
    }

    if (line.startsWith("data:")) {
      dataLines.push(line.slice(5).trimStart());
    }
  }

  return {
    event,
    data: dataLines.join("\n")
  };
}

function normalizeStreamHandlers(handlersOrOnChunk) {
  if (typeof handlersOrOnChunk === "function") {
    return {
      onChunk: handlersOrOnChunk
    };
  }
  return handlersOrOnChunk || {};
}

function parseSourcesData(data) {
  if (!data) {
    return [];
  }

  try {
    return JSON.parse(data);
  } catch {
    return [];
  }
}

function dispatchEventBlock(block, handlers) {
  const { event, data } = parseEventBlock(block);
  const eventType = event || "message";

  if (eventType === "sources") {
    handlers.onSources?.(parseSourcesData(data));
    return;
  }

  if (eventType === "done") {
    handlers.onDone?.(data);
    return;
  }

  if (data) {
    handlers.onChunk?.(data);
  }
}

async function consumeStream(reader, handlersOrOnChunk) {
  const handlers = normalizeStreamHandlers(handlersOrOnChunk);
  const decoder = new TextDecoder("utf-8");
  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done });

    const blocks = buffer.split(/\r?\n\r?\n/);
    buffer = blocks.pop() || "";

    for (const block of blocks) {
      dispatchEventBlock(block, handlers);
    }

    if (done) {
      if (buffer) {
        dispatchEventBlock(buffer, handlers);
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

async function handleStreamResponse(response, handlersOrOnChunk) {
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`);
  }

  if (!response.body) {
    throw new Error("当前浏览器不支持流式读取响应");
  }

  const reader = response.body.getReader();
  await consumeStream(reader, handlersOrOnChunk);
}

async function streamChatByMemoryId({ memoryId, message, useRag, signal, onChunk, onSources, onDone }) {
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

  await handleStreamResponse(response, {
    onChunk,
    onSources,
    onDone
  });
}

async function streamChatBySession({ sessionId, message, useRag, knowledgeBaseId, signal, onChunk, onSources, onDone }) {
  const body = {
    sessionId,
    message,
    useRag
  };

  if (knowledgeBaseId != null) {
    body.knowledgeBaseId = knowledgeBaseId;
  }

  const response = await fetch("/api/ai/chat/stream", {
    method: "POST",
    headers: buildStreamHeaders({
      "Content-Type": "application/json"
    }),
    body: JSON.stringify(body),
    signal
  });

  await handleStreamResponse(response, {
    onChunk,
    onSources,
    onDone
  });
}

export async function streamChat(options) {
  if (options?.sessionId != null) {
    return streamChatBySession(options);
  }
  return streamChatByMemoryId(options);
}

export { streamChatBySession };
