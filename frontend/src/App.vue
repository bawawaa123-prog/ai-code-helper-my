<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { streamChatBySession } from "@/api/chat";
import { getCurrentUser, logout } from "@/api/auth";
import {
  createKnowledgeBase,
  deleteKnowledgeBase,
  getKnowledgeBaseList,
  updateKnowledgeBase
} from "@/api/knowledge";
import {
  createSession,
  deleteSession,
  getSessionMessages,
  listSessions,
  updateSessionTitle
} from "@/api/session";
import LoginPanel from "@/components/LoginPanel.vue";
import SessionSidebar from "@/components/SessionSidebar.vue";

const DEFAULT_SESSION_TITLE = "新会话";
const DRAFT_SESSION_ID = "__draft_session__";
const DEFAULT_ASSISTANT_MESSAGE =
  "你好，我是 AI 编程助手。你可以直接问我学习路线、项目实战、面试题，或者让我帮你模拟面试。";

const capabilityTags = ["学习路线规划", "项目实战建议", "面试模拟陪练", "简历优化提示"];
const ragModes = [
  { label: "跟随后端", value: "auto" },
  { label: "前端开启", value: "on" },
  { label: "前端关闭", value: "off" }
];
const quickPrompts = [
  "给我一份 Java 学习路线，适合零基础入门。",
  "模拟一场前端面试，先问我三个常见问题。",
  "帮我分析简历项目，怎么写更容易通过校招筛选。"
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
const draftSessionActive = ref(false);
const draftSessionTitle = ref(DEFAULT_SESSION_TITLE);
const currentSessionId = ref(null);
const messages = ref(createDefaultMessages());
const isLeftSidebarCollapsed = ref(false);
const isRightSidebarCollapsed = ref(false);
const knowledgeBases = ref([]);
const selectedKnowledgeBaseId = ref("");
const knowledgeLoading = ref(false);
const knowledgeError = ref("");
const newKnowledgeBaseName = ref("");
const newKnowledgeBaseDescription = ref("");
const editingKnowledgeBaseId = ref(null);
const editingKnowledgeBaseName = ref("");
const editingKnowledgeBaseDescription = ref("");
const knowledgeActionLoading = ref(false);
const knowledgeActionError = ref("");

let currentController = null;

const isLoggedIn = computed(() => Boolean(currentUser.value));
const canSend = computed(() => Boolean(draft.value.trim()) && !isStreaming.value);
const messageCount = computed(() => messages.value.length);
const statusText = computed(() => (isStreaming.value ? "AI 正在回复" : "准备好开始对话"));
const currentUserName = computed(
  () => currentUser.value?.userName || currentUser.value?.userAccount || "已登录用户"
);
const isKnowledgeSelectorDisabled = computed(
  () => ragMode.value === "off" || knowledgeLoading.value || isStreaming.value
);
const isKnowledgeActionDisabled = computed(
  () => knowledgeActionLoading.value || knowledgeLoading.value || isStreaming.value
);
const ragDescription = computed(() => {
  if (ragMode.value === "on") {
    return "前端强制开启 RAG";
  }
  if (ragMode.value === "off") {
    return "前端强制关闭 RAG";
  }
  return "跟随后端默认配置";
});

const displaySessions = computed(() => {
  if (!draftSessionActive.value) {
    return sessions.value;
  }

  return [
    {
      id: DRAFT_SESSION_ID,
      title: draftSessionTitle.value || DEFAULT_SESSION_TITLE,
      lastMessage: "",
      messageCount: 0,
      isDraft: true
    },
    ...sessions.value
  ];
});

const currentSessionTitle = computed(() => {
  const currentSession = displaySessions.value.find((item) => item.id === currentSessionId.value);
  return currentSession?.title || DEFAULT_SESSION_TITLE;
});

function createDefaultMessages() {
  return [createMessage("assistant", DEFAULT_ASSISTANT_MESSAGE)];
}

function createMessage(role, content = "") {
  return {
    id: crypto.randomUUID(),
    role,
    content,
    sources: [],
    sourcesExpanded: false
  };
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

function resolveKnowledgeBaseIdForRequest() {
  if (ragMode.value === "off") {
    return undefined;
  }
  if (!selectedKnowledgeBaseId.value) {
    return undefined;
  }
  return Number(selectedKnowledgeBaseId.value);
}

function scrollToBottom() {
  nextTick(() => {
    const el = chatBodyRef.value;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  });
}

function activateDraftSession() {
  stopStreaming();
  draftSessionActive.value = true;
  draftSessionTitle.value = DEFAULT_SESSION_TITLE;
  currentSessionId.value = DRAFT_SESSION_ID;
  messages.value = createDefaultMessages();
  draft.value = "";
  sessionError.value = "";
  scrollToBottom();
}

function discardDraftSession() {
  draftSessionActive.value = false;
  draftSessionTitle.value = DEFAULT_SESSION_TITLE;
  if (currentSessionId.value === DRAFT_SESSION_ID) {
    currentSessionId.value = null;
  }
}

function resetKnowledgeForm() {
  newKnowledgeBaseName.value = "";
  newKnowledgeBaseDescription.value = "";
}

function resetKnowledgeEditing() {
  editingKnowledgeBaseId.value = null;
  editingKnowledgeBaseName.value = "";
  editingKnowledgeBaseDescription.value = "";
}

function clearKnowledgeSelection() {
  knowledgeBases.value = [];
  selectedKnowledgeBaseId.value = "";
  knowledgeError.value = "";
  knowledgeLoading.value = false;
  knowledgeActionError.value = "";
  knowledgeActionLoading.value = false;
  resetKnowledgeForm();
  resetKnowledgeEditing();
}

function selectKnowledgeBaseOrFallback(knowledgeBaseId) {
  const exists = knowledgeBases.value.some((item) => String(item.id) === String(knowledgeBaseId));
  selectedKnowledgeBaseId.value = exists ? String(knowledgeBaseId) : "";
}

function startEditingKnowledgeBase(knowledgeBase) {
  editingKnowledgeBaseId.value = knowledgeBase.id;
  editingKnowledgeBaseName.value = knowledgeBase.name || "";
  editingKnowledgeBaseDescription.value = knowledgeBase.description || "";
  knowledgeActionError.value = "";
}

function convertHistoryMessages(historyList = []) {
  if (!Array.isArray(historyList) || historyList.length === 0) {
    return createDefaultMessages();
  }

  return historyList.map((item) => ({
    id: item.id || crypto.randomUUID(),
    role: item.role === "user" ? "user" : "assistant",
    content: item.content || "",
    sources: [],
    sourcesExpanded: false
  }));
}

function toggleSources(message) {
  if (!message || !Array.isArray(message.sources) || message.sources.length === 0) {
    return;
  }
  message.sourcesExpanded = !message.sourcesExpanded;
}

async function loadKnowledgeBases() {
  knowledgeLoading.value = true;
  knowledgeError.value = "";

  try {
    const list = await getKnowledgeBaseList();
    knowledgeBases.value = Array.isArray(list) ? list : [];

    if (
      selectedKnowledgeBaseId.value &&
      !knowledgeBases.value.some((item) => String(item.id) === String(selectedKnowledgeBaseId.value))
    ) {
      selectedKnowledgeBaseId.value = "";
    }
  } catch (error) {
    knowledgeBases.value = [];
    selectedKnowledgeBaseId.value = "";
    knowledgeError.value = error.message || "加载知识库列表失败";
  } finally {
    knowledgeLoading.value = false;
  }
}

async function refreshKnowledgeBases({ selectKnowledgeBaseId } = {}) {
  await loadKnowledgeBases();
  if (selectKnowledgeBaseId != null) {
    selectKnowledgeBaseOrFallback(selectKnowledgeBaseId);
  }
}

async function handleCreateKnowledgeBase() {
  const name = newKnowledgeBaseName.value.trim();
  const description = newKnowledgeBaseDescription.value.trim();

  if (!name) {
    knowledgeActionError.value = "知识库名称不能为空";
    return;
  }

  knowledgeActionLoading.value = true;
  knowledgeActionError.value = "";

  try {
    const knowledgeBaseId = await createKnowledgeBase({
      name,
      description: description || undefined
    });
    resetKnowledgeForm();
    await refreshKnowledgeBases({ selectKnowledgeBaseId: knowledgeBaseId });
  } catch (error) {
    knowledgeActionError.value = error.message || "创建知识库失败";
  } finally {
    knowledgeActionLoading.value = false;
  }
}

async function handleSaveKnowledgeBaseEdit() {
  const knowledgeBaseId = editingKnowledgeBaseId.value;
  const name = editingKnowledgeBaseName.value.trim();
  const description = editingKnowledgeBaseDescription.value.trim();

  if (!knowledgeBaseId) {
    return;
  }
  if (!name) {
    knowledgeActionError.value = "知识库名称不能为空";
    return;
  }

  knowledgeActionLoading.value = true;
  knowledgeActionError.value = "";

  try {
    await updateKnowledgeBase(knowledgeBaseId, {
      name,
      description: description || undefined
    });
    await refreshKnowledgeBases({ selectKnowledgeBaseId: knowledgeBaseId });
    resetKnowledgeEditing();
  } catch (error) {
    knowledgeActionError.value = error.message || "重命名知识库失败";
  } finally {
    knowledgeActionLoading.value = false;
  }
}

async function handleDeleteKnowledgeBase(knowledgeBase) {
  const confirmed = window.confirm(`确认删除知识库“${knowledgeBase.name}”吗？`);
  if (!confirmed) {
    return;
  }

  knowledgeActionLoading.value = true;
  knowledgeActionError.value = "";

  try {
    await deleteKnowledgeBase(knowledgeBase.id);
    await refreshKnowledgeBases();
    if (String(selectedKnowledgeBaseId.value) === String(knowledgeBase.id)) {
      selectedKnowledgeBaseId.value = "";
    }
    if (editingKnowledgeBaseId.value === knowledgeBase.id) {
      resetKnowledgeEditing();
    }
  } catch (error) {
    knowledgeActionError.value = error.message || "删除知识库失败";
  } finally {
    knowledgeActionLoading.value = false;
  }
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
  discardDraftSession();

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
      if (!draftSessionActive.value) {
        currentSessionId.value = null;
        messages.value = createDefaultMessages();
      }
      return;
    }

    if (!autoSelect || draftSessionActive.value) {
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
  discardDraftSession();
  await Promise.all([refreshSessions({ autoSelect: false }), loadKnowledgeBases()]);
  currentSessionId.value = null;
  messages.value = createDefaultMessages();
  sessionError.value = "";
  knowledgeActionError.value = "";
  scrollToBottom();
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
    clearKnowledgeSelection();
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
  clearKnowledgeSelection();
  discardDraftSession();
  currentSessionId.value = null;
  sessionError.value = "";
  messages.value = createDefaultMessages();
}

async function persistDraftSession(title = DEFAULT_SESSION_TITLE) {
  const sessionId = await createSession({
    title,
    useRag: false
  });

  discardDraftSession();
  currentSessionId.value = sessionId;
  await refreshSessions({ autoSelect: false });

  const createdSession = sessions.value.find((item) => item.id === sessionId);
  if (createdSession) {
    sessions.value = [createdSession, ...sessions.value.filter((item) => item.id !== sessionId)];
  }

  return sessionId;
}

function handleCreateSession() {
  activateDraftSession();
}

async function handleSelectSession(sessionId) {
  if (!sessionId || sessionId === currentSessionId.value) {
    return;
  }
  if (sessionId === DRAFT_SESSION_ID) {
    activateDraftSession();
    return;
  }
  await loadSessionMessages(sessionId);
}

async function handleRenameSession(sessionId) {
  const targetSession = displaySessions.value.find((item) => item.id === sessionId);
  if (!targetSession) {
    return;
  }

  const nextTitle = window.prompt("请输入新的会话标题", targetSession.title || DEFAULT_SESSION_TITLE);
  if (!nextTitle || !nextTitle.trim()) {
    return;
  }

  try {
    const trimmedTitle = nextTitle.trim();

    if (targetSession.isDraft) {
      draftSessionTitle.value = trimmedTitle;
      await persistDraftSession(trimmedTitle);
      return;
    }

    await updateSessionTitle(sessionId, { title: trimmedTitle });
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
    if (sessionId === DRAFT_SESSION_ID) {
      discardDraftSession();

      if (sessions.value.length > 0) {
        await loadSessionMessages(sessions.value[0].id);
      } else {
        currentSessionId.value = null;
        messages.value = createDefaultMessages();
        draft.value = "";
        sessionError.value = "";
        scrollToBottom();
      }
      return;
    }

    await deleteSession(sessionId);
    await refreshSessions({ autoSelect: false });

    if (currentSessionId.value !== sessionId) {
      return;
    }

    if (sessions.value.length > 0) {
      await loadSessionMessages(sessions.value[0].id);
      return;
    }

    activateDraftSession();
  } catch (error) {
    sessionError.value = error.message || "删除会话失败";
  }
}

async function ensureCurrentSession() {
  if (currentSessionId.value && currentSessionId.value !== DRAFT_SESSION_ID) {
    return currentSessionId.value;
  }
  if (!draftSessionActive.value) {
    activateDraftSession();
  }
  return persistDraftSession(draftSessionTitle.value || DEFAULT_SESSION_TITLE);
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
      knowledgeBaseId: resolveKnowledgeBaseIdForRequest(),
      signal: currentController.signal,
      onChunk(chunk) {
        assistantMessage.content += chunk;
        scrollToBottom();
      },
      onSources(sources) {
        assistantMessage.sources = Array.isArray(sources) ? sources : [];
        scrollToBottom();
      }
    });

    if (!assistantMessage.content.trim()) {
      assistantMessage.content = "这次没有返回内容，你可以换个问法再试一次。";
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

function toggleLeftSidebar() {
  isLeftSidebarCollapsed.value = !isLeftSidebarCollapsed.value;
}

function toggleRightSidebar() {
  isRightSidebarCollapsed.value = !isRightSidebarCollapsed.value;
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
    <div class="page-bg"></div>

    <main v-if="isInitializing" class="auth-loading-card">
      <div class="auth-loading">
        <p class="eyebrow">AI Programming Coach</p>
        <h1>正在恢复登录状态</h1>
        <p class="subtitle">请稍候，我们正在校验你的登录信息并恢复页面状态。</p>
      </div>
    </main>

    <LoginPanel v-else-if="!isLoggedIn" @login-success="handleLoginSuccess" />

    <main
      v-else
      class="chat-shell"
      :class="{
        'left-collapsed': isLeftSidebarCollapsed,
        'right-collapsed': isRightSidebarCollapsed
      }"
    >
      <SessionSidebar
        :collapsed="isLeftSidebarCollapsed"
        :sessions="displaySessions"
        :current-session-id="currentSessionId"
        :loading="sessionLoading"
        :current-user-name="currentUserName"
        @toggle-collapse="toggleLeftSidebar"
        @create-session="handleCreateSession"
        @select-session="handleSelectSession"
        @rename-session="handleRenameSession"
        @delete-session="handleDeleteSession"
      />

      <section class="chat-workspace">
        <header class="chat-header">
          <div class="hero-block">
            <p class="eyebrow">AI Programming Coach</p>
            <h1>AI 编程小助手</h1>
            <p class="subtitle">
              为编程学习和求职准备打造的实时对话工作台，你可以把它当作课程顾问、项目搭子和面试陪练。
            </p>

            <div class="hero-tags">
              <span v-for="tag in capabilityTags" :key="tag" class="hero-tag">{{ tag }}</span>
            </div>
          </div>
        </header>

        <div class="workspace-grid">
          <section class="conversation-column">
            <div class="conversation-topbar">
              <div class="workspace-heading">
                <p class="section-kicker">Conversation Workspace</p>
                <h2>{{ currentSessionTitle }}</h2>
                <p class="workspace-summary">
                  在当前会话里持续追问，历史消息会参与上下文，让对话更连贯。
                </p>
              </div>

              <div class="workspace-status">
                <span class="status-pill">{{ statusText }}</span>
                <button
                  class="rail-toggle-button mobile-only"
                  type="button"
                  :aria-expanded="!isRightSidebarCollapsed"
                  @click="toggleRightSidebar"
                >
                  {{ isRightSidebarCollapsed ? "展开信息栏" : "收起信息栏" }}
                </button>
              </div>
            </div>

            <p v-if="sessionError" class="page-feedback error">{{ sessionError }}</p>

            <section ref="chatBodyRef" class="chat-body">
              <div class="welcome-strip">
                <div class="welcome-copy">
                  <span class="welcome-badge">推荐用法</span>
                  <p>
                    先说清你的目标和背景，比如“我准备校招前端面试”或“我想补齐 Java 基础”，回答会更贴近你的阶段。
                  </p>
                </div>
              </div>

              <article
                v-for="message in messages"
                :key="message.id"
                class="message-row"
                :class="message.role"
              >
                <div class="avatar">{{ message.role === "assistant" ? "AI" : "我" }}</div>

                <div class="bubble-wrap">
                  <div class="message-meta">
                    {{ message.role === "assistant" ? "AI 编程小助手" : "你自己" }}
                  </div>
                  <div class="message-bubble">
                    {{ message.content || "正在思考中..." }}
                  </div>
                  <section
                    v-if="
                      message.role === 'assistant' &&
                      Array.isArray(message.sources) &&
                      message.sources.length > 0
                    "
                    class="source-card-list"
                  >
                    <button
                      class="source-card-toggle"
                      type="button"
                      @click="toggleSources(message)"
                    >
                      <span class="source-card-header">参考来源（{{ message.sources.length }}）</span>
                      <span class="source-card-indicator">
                        {{ message.sourcesExpanded ? "收起" : "展开" }}
                      </span>
                    </button>
                    <div v-if="message.sourcesExpanded" class="source-card-body">
                      <article
                        v-for="(source, index) in message.sources"
                        :key="`${message.id}-source-${index}`"
                        class="source-card"
                      >
                        <div class="source-card-top">
                          <strong class="source-name">{{ source?.sourceName || "未知来源" }}</strong>
                          <span v-if="source?.score != null" class="source-score">
                            相似度 {{ Number(source.score).toFixed(3) }}
                          </span>
                        </div>
                        <p v-if="source?.content" class="source-content">
                          {{ source.content }}
                        </p>
                      </article>
                    </div>
                  </section>
                </div>
              </article>
            </section>

            <footer class="composer">
              <div class="input-panel">
                <section class="quick-prompts-panel">
                  <div class="quick-prompts-head">
                    <span class="section-kicker">Quick Starters</span>
                    <p>试试这些常用问题，快速进入状态。</p>
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

                <div class="input-shell">
                  <span class="input-label">输入你的问题</span>
                  <textarea
                    v-model="draft"
                    rows="3"
                    placeholder="例如：怎么准备 Java 八股和项目面试？"
                    @keydown.enter="handleEnter"
                  ></textarea>
                </div>

                <div class="composer-actions">
                  <span class="composer-tip">
                    {{ isStreaming ? "正在生成回答..." : "Enter 发送，Shift + Enter 换行" }}
                  </span>

                  <div class="action-group">
                    <button v-if="isStreaming" class="stop-button" type="button" @click="stopStreaming">
                      停止生成
                    </button>
                    <button class="send-button" type="button" :disabled="!canSend" @click="sendMessage()">
                      发送消息
                    </button>
                  </div>
                </div>
              </div>
            </footer>
          </section>

          <aside class="context-column">
            <div class="context-panel" :class="{ collapsed: isRightSidebarCollapsed }">
              <div class="session-panel-head">
                <div class="session-panel-top">
                  <span class="session-label">当前会话</span>
                  <span class="status-dot"></span>
                </div>

                <button class="rail-toggle-button" type="button" @click="toggleRightSidebar">
                  {{ isRightSidebarCollapsed ? "展开" : "收起" }}
                </button>
              </div>

              <template v-if="!isRightSidebarCollapsed">
                <strong class="session-id">{{ currentSessionTitle }}</strong>
                <span class="workspace-hint">{{ ragDescription }}</span>

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

                  <div class="knowledge-select-panel">
                    <label class="knowledge-select-label" for="knowledge-base-select">
                      知识库选择
                    </label>
                    <select
                      id="knowledge-base-select"
                      v-model="selectedKnowledgeBaseId"
                      class="knowledge-select"
                      :disabled="isKnowledgeSelectorDisabled"
                    >
                      <option value="">静态内置知识库</option>
                      <option
                        v-for="knowledgeBase in knowledgeBases"
                        :key="knowledgeBase.id"
                        :value="String(knowledgeBase.id)"
                      >
                        {{ knowledgeBase.name }}
                      </option>
                    </select>
                    <p v-if="knowledgeLoading" class="knowledge-select-hint">
                      正在加载我的知识库...
                    </p>
                    <p v-else-if="knowledgeError" class="knowledge-select-hint">
                      {{ knowledgeError }}
                    </p>
                    <p v-else-if="knowledgeBases.length === 0" class="knowledge-select-hint">
                      暂无我的知识库，可继续使用静态内置知识库。
                    </p>
                  </div>

                  <div class="knowledge-manager">
                    <div class="knowledge-manager-head">
                      <span class="stat-label">我的知识库</span>
                      <span class="knowledge-manager-summary">
                        创建、重命名和删除都会自动刷新列表
                      </span>
                    </div>

                    <p v-if="knowledgeActionError" class="knowledge-manager-error">
                      {{ knowledgeActionError }}
                    </p>

                    <div class="knowledge-create-form">
                      <input
                        v-model="newKnowledgeBaseName"
                        class="knowledge-text-input"
                        type="text"
                        maxlength="64"
                        placeholder="输入知识库名称"
                        :disabled="isKnowledgeActionDisabled"
                      />
                      <textarea
                        v-model="newKnowledgeBaseDescription"
                        class="knowledge-textarea"
                        rows="2"
                        maxlength="512"
                        placeholder="可选：输入知识库描述"
                        :disabled="isKnowledgeActionDisabled"
                      ></textarea>
                      <button
                        class="knowledge-primary-button"
                        type="button"
                        :disabled="isKnowledgeActionDisabled"
                        @click="handleCreateKnowledgeBase"
                      >
                        {{ knowledgeActionLoading ? "处理中..." : "新建知识库" }}
                      </button>
                    </div>

                    <div class="knowledge-list">
                      <article
                        v-for="knowledgeBase in knowledgeBases"
                        :key="knowledgeBase.id"
                        class="knowledge-item"
                        :class="{ selected: String(knowledgeBase.id) === String(selectedKnowledgeBaseId) }"
                      >
                        <template v-if="editingKnowledgeBaseId === knowledgeBase.id">
                          <input
                            v-model="editingKnowledgeBaseName"
                            class="knowledge-text-input"
                            type="text"
                            maxlength="64"
                            placeholder="知识库名称"
                            :disabled="isKnowledgeActionDisabled"
                          />
                          <textarea
                            v-model="editingKnowledgeBaseDescription"
                            class="knowledge-textarea"
                            rows="2"
                            maxlength="512"
                            placeholder="知识库描述"
                            :disabled="isKnowledgeActionDisabled"
                          ></textarea>
                          <div class="knowledge-item-actions">
                            <button
                              class="knowledge-secondary-button"
                              type="button"
                              :disabled="isKnowledgeActionDisabled"
                              @click="handleSaveKnowledgeBaseEdit"
                            >
                              保存
                            </button>
                            <button
                              class="knowledge-link-button"
                              type="button"
                              :disabled="isKnowledgeActionDisabled"
                              @click="resetKnowledgeEditing"
                            >
                              取消
                            </button>
                          </div>
                        </template>

                        <template v-else>
                          <div class="knowledge-item-head">
                            <strong class="knowledge-item-name">{{ knowledgeBase.name }}</strong>
                            <span
                              v-if="String(knowledgeBase.id) === String(selectedKnowledgeBaseId)"
                              class="knowledge-item-badge"
                            >
                              当前使用
                            </span>
                          </div>
                          <p v-if="knowledgeBase.description" class="knowledge-item-description">
                            {{ knowledgeBase.description }}
                          </p>
                          <div class="knowledge-item-actions">
                            <button
                              class="knowledge-link-button"
                              type="button"
                              :disabled="isKnowledgeActionDisabled"
                              @click="startEditingKnowledgeBase(knowledgeBase)"
                            >
                              重命名
                            </button>
                            <button
                              class="knowledge-danger-button"
                              type="button"
                              :disabled="isKnowledgeActionDisabled"
                              @click="handleDeleteKnowledgeBase(knowledgeBase)"
                            >
                              删除
                            </button>
                          </div>
                        </template>
                      </article>
                    </div>
                  </div>
                </div>

                <button class="ghost-button" type="button" @click="resetConversation">
                  重置当前聊天画布
                </button>
                <button class="logout-button" type="button" @click="handleLogout">
                  退出登录
                </button>
              </template>
            </div>
          </aside>
        </div>
      </section>
    </main>
  </div>
</template>
