<script setup>
import { computed } from "vue";

const props = defineProps({
  collapsed: {
    type: Boolean,
    default: false
  },
  sessions: {
    type: Array,
    default: () => []
  },
  currentSessionId: {
    type: [Number, String],
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  },
  currentUserName: {
    type: String,
    default: ""
  }
});

defineEmits(["toggle-collapse", "create-session", "select-session", "rename-session", "delete-session"]);

const hasSessions = computed(() => props.sessions.length > 0);

function formatMeta(session) {
  if (session.isDraft) {
    return "未保存";
  }

  if (session.lastMessage) {
    return session.lastMessage;
  }

  if (!session.updateTime) {
    return "等待第一条消息";
  }

  const date = new Date(session.updateTime);
  if (Number.isNaN(date.getTime())) {
    return "等待第一条消息";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}
</script>

<template>
  <aside class="session-sidebar" :class="{ collapsed }">
    <div class="session-sidebar-head">
      <div class="session-sidebar-brand">
        <button
          class="sidebar-collapse-button"
          type="button"
          :aria-expanded="!collapsed"
          @click="$emit('toggle-collapse')"
        >
          {{ collapsed ? ">" : "<" }}
        </button>

        <div v-if="!collapsed">
          <p class="session-sidebar-kicker">Workspace</p>
          <h2>{{ currentUserName || "我的会话" }}</h2>
        </div>
      </div>

      <button
        class="session-create-button"
        type="button"
        :disabled="loading"
        @click="$emit('create-session')"
      >
        {{ collapsed ? "+" : "新建会话" }}
      </button>
    </div>

    <div v-if="loading" class="session-sidebar-state">正在加载会话...</div>

    <div v-else-if="!hasSessions" class="session-sidebar-state">
      <strong>还没有会话</strong>
      <p>先新建一个会话，后续历史消息和标题都会在这里管理。</p>
    </div>

    <div v-else class="session-list">
      <article
        v-for="session in sessions"
        :key="session.id"
        class="session-item"
        :class="{ active: session.id === currentSessionId }"
      >
        <button
          class="session-item-select"
          type="button"
          @click="$emit('select-session', session.id)"
        >
          <div class="session-item-main">
            <strong v-if="!collapsed" class="session-item-title" :title="session.title || '新会话'">
              {{ session.title || "新会话" }}
            </strong>
            <span v-if="!collapsed" class="session-item-meta" :title="formatMeta(session)">
              {{ formatMeta(session) }}
            </span>
            <span v-else class="session-item-dot"></span>
          </div>
        </button>

        <div v-if="!collapsed" class="session-item-actions">
          <button
            class="session-action-button"
            type="button"
            title="重命名"
            @click="$emit('rename-session', session.id)"
          >
            改名
          </button>
          <button
            class="session-action-button danger"
            type="button"
            title="删除"
            @click="$emit('delete-session', session.id)"
          >
            删除
          </button>
        </div>
      </article>
    </div>
  </aside>
</template>
