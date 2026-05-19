<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { streamChatBySession } from "@/api/chat";
import { getCurrentUser, logout } from "@/api/auth";
import {
  createSession,
  deleteSession,
  getSessionMessages,
  listSessions,
  updateSessionTitle
} from "@/api/session";
import LoginPanel from "@/components/LoginPanel.vue";
import SessionSidebar from "@/components/SessionSidebar.vue";

const capabilityTags = ["学习路线规划", "项目实战建议", "面试模拟陪练", "简历优化提示"];
const ragModes = [
  { label: "跟随后端", value: "auto" },
  { label: "前端开启", value: "on" },
  { label: "前端关闭", value: "off" }
];
const quickPrompts = [
  "给我一份 Java 学习路线，适合零基础入门。",
  "模拟一场前端面试，先问我三个常见问题。",
  "帮我分析简历项目怎么写更容易通过校招筛选。"
];

const chatBodyRef = ref(null);
const draft = ref("");
const isStreaming = ref(false);
const ragMode = ref("auto");
const isInitializing = ref(true);
const sessionLoading = ref(false);
const sessionError = ref("");
const currentUser = ref(readStoredUser());
const sessions = ref([]);
const currentSessionId = ref(null);
const messages = ref(createDefaultMessages());

let currentController = null;

const isLoggedIn = computed(() => Boolean(currentUser.value));
const canSend = computed(() => draft.value.trim() && !isStreaming.value);
const messageCount = computed(() => messages.value.length);
const statusText = computed(() => (isStreaming.value ? "AI 正在实时思考..." : "随时可以开始新的提问"));
const currentUserName = computed(() => currentUser.value?.userName || currentUser.value?.userAccount || "已登录用户");
const currentSessionTitle = computed(() => {
  const currentSession = sessions.value.find((item) => item.id === currentSessionId.value);
  return currentSession?.title || "新会话";
});
const ragDescription = computed(() => {
  if (ragMode.value === "on") {
    return "当前由前端强制开启 RAG";
  }
  if (ragMode.value === "off") {
    return "当前由前端强制关闭 RAG";
  }
  return "当前跟随后端默认配置";
});

function createDefaultMessages() {
  return [
    {
      id: crypto.randomUUID(),
      role: "assistant",
      content:
        "你好，我是 AI 编程小助手。你可以问我编程学习路线、项目实战建议、八股准备方法，或者直接让我陪你模拟面试。"
    }
  ];
}

function readStoredUser() {
  const raw = localStorage.getItem("loginUser");
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

function resolveUseRag() {
  if (ragMode.value === "on") {
    return true;
  }
  if (ragMode.value === "off") {
    return false;
  }
  return undefined;
}

function scrollToBottom() {
  nextTick(() => {
    const el = chatBodyRef.value;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  });
}

function createMessage(role, content = "") {
  return {
    id: crypto.randomUUID(),
    role,
    content
  };
}

function convertHistoryMessages(historyList = []) {
  if (!Array.isArray(historyList) || historyList.length === 0) {
    return createDefaultMessages();
  }

  return historyList.map((item) => ({
    id: item.id || crypto.randomUUID(),
    role: item.role === "user" ? "user" : "assistant",
    content: item.content || ""
  }));
}

async function loadSessionMessages(sessionId) {
  if (!sessionId) {
    messages.value = createDefaultMessages();
    return;
  }

  sessionLoading.value = true;
  sessionError.value = "";
  stopStreaming();
  isStreaming.value = false;

  try {
    const historyList = await getSessionMessages(sessionId);
    messages.value = convertHistoryMessages(historyList);
    currentSessionId.value = sessionId;
  } catch (error) {
    sessionError.value = error.message || "加载历史消息失败";
    messages.value = createDefaultMessages();
  } finally {
    sessionLoading.value = false;
    scrollToBottom();
  }
}

async function refreshSessions({ autoSelect = true } = {}) {
  sessionLoading.value = true;
  sessionError.value = "";

  try {
    const sessionList = await listSessions();
    sessions.value = sessionList || [];

    if (!sessions.value.length) {
      currentSessionId.value = null;
      messages.value = createDefaultMessages();
      return;
    }

    if (!autoSelect) {
      return;
    }

    const matchedSession = sessions.value.find((item) => item.id === currentSessionId.value);
    const nextSessionId = matchedSession?.id || sessions.value[0].id;
    await loadSessionMessages(nextSessionId);
  } catch (error) {
    sessionError.value = error.message || "加载会话列表失败";
  } finally {
    sessionLoading.value = false;
  }
}

async function ensureSessionOnLogin() {
  await refreshSessions({ autoSelect: false });
  if (sessions.value.length > 0) {
    await loadSessionMessages(sessions.value[0].id);
    return;
  }
  await handleCreateSession();
}

async function restoreLoginState() {
  const token = localStorage.getItem("token");
  if (!token) {
    currentUser.value = null;
    isInitializing.value = false;
    return;
  }

  try {
    const user = await getCurrentUser();
    currentUser.value = user;
    localStorage.setItem("loginUser", JSON.stringify(user));
    await ensureSessionOnLogin();
  } catch {
    logout();
    currentUser.value = null;
  } finally {
    isInitializing.value = false;
  }
}

async function handleLoginSuccess(user) {
  currentUser.value = user;
  await ensureSessionOnLogin();
}

function handleLogout() {
  stopStreaming();
  logout();
  currentUser.value = null;
  sessions.value = [];
  currentSessionId.value = null;
  sessionError.value = "";
  messages.value = createDefaultMessages();
}

async function handleCreateSession() {
  sessionLoading.value = true;
  sessionError.value = "";

  try {
    const sessionId = await createSession({
      title: "新会话",
      useRag: false
    });
    await refreshSessions({ autoSelect: false });
    currentSessionId.value = sessionId;
    messages.value = createDefaultMessages();
    const createdSession = sessions.value.find((item) => item.id === sessionId);
    if (createdSession) {
      sessions.value = [createdSession, ...sessions.value.filter((item) => item.id !== sessionId)];
    }
  } catch (error) {
    sessionError.value = error.message || "创建会话失败";
  } finally {
    sessionLoading.value = false;
    scrollToBottom();
  }
}

async function handleSelectSession(sessionId) {
  if (!sessionId || sessionId === currentSessionId.value) {
    return;
  }
  await loadSessionMessages(sessionId);
}

async function handleRenameSession(sessionId) {
  const targetSession = sessions.value.find((item) => item.id === sessionId);
  if (!targetSession) {
    return;
  }

  const nextTitle = window.prompt("请输入新的会话标题", targetSession.title || "新会话");
  if (!nextTitle || !nextTitle.trim()) {
    return;
  }

  try {
    await updateSessionTitle(sessionId, { title: nextTitle.trim() });
    await refreshSessions({ autoSelect: false });
  } catch (error) {
    sessionError.value = error.message || "重命名会话失败";
  }
}

async function handleDeleteSession(sessionId) {
  const confirmed = window.confirm("确认删除这个会话吗？删除后将无法在列表中继续查看。");
  if (!confirmed) {
    return;
  }

  try {
    await deleteSession(sessionId);
    await refreshSessions({ autoSelect: false });

    if (currentSessionId.value !== sessionId) {
      return;
    }

    if (sessions.value.length > 0) {
      await loadSessionMessages(sessions.value[0].id);
      return;
    }

    await handleCreateSession();
  } catch (error) {
    sessionError.value = error.message || "删除会话失败";
  }
}

async function ensureCurrentSession() {
  if (currentSessionId.value) {
    return currentSessionId.value;
  }
  await handleCreateSession();
  return currentSessionId.value;
}

async function sendMessage(prefilledMessage) {
  if (!isLoggedIn.value) {
    sessionError.value = "请先登录后再发送消息";
    return;
  }

  const content = (prefilledMessage ?? draft.value).trim();
  if (!content || isStreaming.value) {
    return;
  }

  const sessionId = await ensureCurrentSession();
  if (!sessionId) {
    sessionError.value = "当前会话创建失败，请稍后重试";
    return;
  }

  draft.value = "";
  sessionError.value = "";

  const userMessage = createMessage("user", content);
  const assistantMessage = createMessage("assistant");
  messages.value.push(userMessage, assistantMessage);
  isStreaming.value = true;
  scrollToBottom();

  currentController = new AbortController();

  try {
    await streamChatBySession({
      sessionId,
      message: content,
      useRag: resolveUseRag(),
      signal: currentController.signal,
      onChunk(chunk) {
        assistantMessage.content += chunk;
        scrollToBottom();
      }
    });

    if (!assistantMessage.content.trim()) {
      assistantMessage.content = "这次返回了空内容，你可以换个问法再试一次。";
    }

    await refreshSessions({ autoSelect: false });
  } catch (error) {
    if (error.name === "AbortError") {
      assistantMessage.content =
        assistantMessage.content || "当前回答已停止，你可以继续追问新的问题。";
    } else {
      assistantMessage.content =
        assistantMessage.content || "连接中断了，请确认后端服务已启动后重试。";
      sessionError.value = error.message || "发送消息失败";
    }
  } finally {
    isStreaming.value = false;
    currentController = null;
    scrollToBottom();
  }
}

function stopStreaming() {
  currentController?.abort();
}

function resetConversation() {
  stopStreaming();
  messages.value = createDefaultMessages();
  draft.value = "";
}

function handleEnter(event) {
  if (event.shiftKey) {
    return;
  }

  event.preventDefault();
  sendMessage();
}

watch(messages, scrollToBottom, { deep: true });

onMounted(() => {
  scrollToBottom();
  restoreLoginState();
});
onBeforeUnmount(() => {
  stopStreaming();
});
</script>

<template>
  <div class="page-shell">
    <div class="grid-overlay"></div>
    <div class="ambient ambient-left"></div>
    <div class="ambient ambient-right"></div>
    <div class="ambient ambient-center"></div>

    <main v-if="isInitializing" class="chat-card auth-loading-card">
      <div class="auth-loading">
        <p class="eyebrow">AI Programming Coach</p>
        <h1>正在恢复登录状态</h1>
        <p class="subtitle">请稍等，我们正在验证你的登录信息并恢复页面状态。</p>
      </div>
    </main>

    <LoginPanel
      v-else-if="!isLoggedIn"
      @login-success="handleLoginSuccess"
    />

    <main v-else class="chat-card app-with-sidebar">
      <SessionSidebar
        :sessions="sessions"
        :current-session-id="currentSessionId"
        :loading="sessionLoading"
        :current-user-name="currentUserName"
        @create-session="handleCreateSession"
        @select-session="handleSelectSession"
        @rename-session="handleRenameSession"
        @delete-session="handleDeleteSession"
      />

      <div class="chat-main">
        <header class="chat-header">
          <div class="hero-block">
            <p class="eyebrow">AI Programming Coach</p>
            <h1>AI 编程小助手</h1>
            <p class="subtitle">
              为编程学习和求职准备打造的实时对话工作台。你可以把它当作路线顾问、项目搭子和面试陪练。
            </p>

            <div class="hero-tags">
              <span v-for="tag in capabilityTags" :key="tag" class="hero-tag">
                {{ tag }}
              </span>
            </div>
          </div>

          <div class="session-panel">
            <div class="session-panel-top">
              <span class="session-label">当前会话</span>
              <span class="status-dot"></span>
            </div>
            <strong class="session-id">{{ currentSessionTitle }}</strong>

            <div class="stat-card user-card">
              <span class="stat-label">当前账号</span>
              <strong>{{ currentUserName }}</strong>
            </div>

            <div class="session-stats">
              <div class="stat-card">
                <span class="stat-label">当前状态</span>
                <strong>{{ isStreaming ? "生成中" : "已就绪" }}</strong>
              </div>
              <div class="stat-card">
                <span class="stat-label">消息数量</span>
                <strong>{{ messageCount }}</strong>
              </div>
            </div>

            <div class="rag-panel">
              <div class="rag-panel-head">
                <span class="stat-label">RAG 策略</span>
                <span class="rag-summary">{{ ragDescription }}</span>
              </div>

              <div class="rag-switches">
                <button
                  v-for="mode in ragModes"
                  :key="mode.value"
                  class="rag-option"
                  :class="{ active: ragMode === mode.value }"
                  type="button"
                  :disabled="isStreaming"
                  @click="ragMode = mode.value"
                >
                  {{ mode.label }}
                </button>
              </div>
            </div>

            <button class="ghost-button" type="button" @click="resetConversation">
              重置当前聊天画布
            </button>
            <button class="logout-button" type="button" @click="handleLogout">
              退出登录
            </button>
          </div>
        </header>

        <section class="workspace-shell">
          <div class="workspace-topbar">
            <div>
              <p class="section-kicker">Conversation Workspace</p>
              <h2>围绕你的问题，持续往下深挖</h2>
            </div>

            <div class="workspace-status">
              <span class="status-pill">{{ statusText }}</span>
              <span class="workspace-hint">{{ ragMode === "auto" ? "RAG 跟随后端" : ragMode === "on" ? "RAG 已由前端开启" : "RAG 已由前端关闭" }}</span>
            </div>
          </div>

          <p v-if="sessionError" class="page-feedback error">{{ sessionError }}</p>

          <section ref="chatBodyRef" class="chat-body">
            <div class="welcome-strip">
              <div class="welcome-copy">
                <span class="welcome-badge">推荐用法</span>
                <p>
                  先讲清你的目标和背景，比如“我准备校招前端面试”或“我想补齐 Java 基础”，回复会更贴近你的阶段。
                </p>
              </div>
            </div>

            <article
              v-for="message in messages"
              :key="message.id"
              class="message-row"
              :class="message.role"
            >
              <div class="avatar">
                {{ message.role === "assistant" ? "AI" : "我" }}
              </div>

              <div class="bubble-wrap">
                <div class="message-meta">
                  {{ message.role === "assistant" ? "AI 编程小助手" : "你自己" }}
                </div>
                <div class="message-bubble">
                  {{ message.content || "正在思考中..." }}
                </div>
              </div>
            </article>
          </section>

          <section class="quick-prompts-panel">
            <div class="quick-prompts-head">
              <span class="section-kicker">Quick Starters</span>
              <p>试试这些高频问题，快速进入状态。</p>
            </div>

            <div class="quick-prompts">
              <button
                v-for="prompt in quickPrompts"
                :key="prompt"
                class="prompt-chip"
                type="button"
                :disabled="isStreaming"
                @click="sendMessage(prompt)"
              >
                {{ prompt }}
              </button>
            </div>
          </section>

          <footer class="composer">
            <div class="input-panel">
              <div class="input-shell">
                <span class="input-label">输入你的问题</span>
                <textarea
                  v-model="draft"
                  rows="3"
                  placeholder="输入你想咨询的问题，例如：怎么准备 Java 八股和项目面试？"
                  @keydown.enter="handleEnter"
                ></textarea>
              </div>

              <div class="composer-actions">
                <span class="composer-tip">
                  {{ isStreaming ? "正在基于上下文连续生成内容..." : "Enter 发送，Shift + Enter 换行" }}
                </span>

                <div class="action-group">
                  <button
                    v-if="isStreaming"
                    class="stop-button"
                    type="button"
                    @click="stopStreaming"
                  >
                    停止生成
                  </button>
                  <button
                    class="send-button"
                    type="button"
                    :disabled="!canSend"
                    @click="sendMessage()"
                  >
                    发送消息
                  </button>
                </div>
              </div>
            </div>
          </footer>
        </section>
      </div>
    </main>
  </div>
</template>
