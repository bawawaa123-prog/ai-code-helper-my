<script setup>
import { computed, reactive, ref } from "vue";
import { login, register } from "../api/auth";

const emit = defineEmits(["login-success"]);

const isLoginMode = ref(true);
const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

const form = reactive({
  userAccount: "",
  userPassword: "",
  checkPassword: ""
});

const panelTitle = computed(() =>
  isLoginMode.value ? "登录 AI 编程助手" : "注册 AI 编程助手账号"
);

const panelSubtitle = computed(() =>
  isLoginMode.value
    ? "登录后即可恢复你的身份状态，并继续使用当前聊天工作台。"
    : "先创建一个账号，后续就可以基于登录态接入会话、历史消息和流式聊天。"
);

function resetTips() {
  errorMessage.value = "";
  successMessage.value = "";
}

function switchMode(nextIsLoginMode) {
  if (loading.value) {
    return;
  }
  isLoginMode.value = nextIsLoginMode;
  resetTips();
}

async function handleSubmit() {
  resetTips();
  loading.value = true;

  try {
    if (isLoginMode.value) {
      const result = await login({
        userAccount: form.userAccount,
        userPassword: form.userPassword
      });
      localStorage.setItem("token", result.token);
      localStorage.setItem("loginUser", JSON.stringify(result.user));
      emit("login-success", result.user);
      return;
    }

    await register({
      userAccount: form.userAccount,
      userPassword: form.userPassword,
      checkPassword: form.checkPassword
    });
    successMessage.value = "注册成功，请使用刚刚的账号登录。";
    isLoginMode.value = true;
    form.userPassword = "";
    form.checkPassword = "";
  } catch (error) {
    errorMessage.value = error.message || "操作失败，请稍后重试";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="auth-card">
    <section class="auth-hero">
      <p class="eyebrow">AI Programming Coach</p>
      <h1>{{ panelTitle }}</h1>
      <p class="subtitle">
        {{ panelSubtitle }}
      </p>

      <div class="hero-tags">
        <span class="hero-tag">学习路线规划</span>
        <span class="hero-tag">项目实战建议</span>
        <span class="hero-tag">面试模拟陪练</span>
        <span class="hero-tag">登录态恢复</span>
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-tabs">
        <button
          class="auth-tab"
          :class="{ active: isLoginMode }"
          type="button"
          :disabled="loading"
          @click="switchMode(true)"
        >
          登录
        </button>
        <button
          class="auth-tab"
          :class="{ active: !isLoginMode }"
          type="button"
          :disabled="loading"
          @click="switchMode(false)"
        >
          注册
        </button>
      </div>

      <div class="auth-form">
        <label class="auth-field">
          <span>账号</span>
          <input
            v-model.trim="form.userAccount"
            type="text"
            autocomplete="username"
            placeholder="请输入账号"
          />
        </label>

        <label class="auth-field">
          <span>密码</span>
          <input
            v-model="form.userPassword"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
          />
        </label>

        <label v-if="!isLoginMode" class="auth-field">
          <span>确认密码</span>
          <input
            v-model="form.checkPassword"
            type="password"
            autocomplete="new-password"
            placeholder="请再次输入密码"
          />
        </label>

        <p v-if="errorMessage" class="auth-feedback error">{{ errorMessage }}</p>
        <p v-if="successMessage" class="auth-feedback success">{{ successMessage }}</p>

        <button
          class="auth-submit"
          type="button"
          :disabled="loading"
          @click="handleSubmit"
        >
          {{ loading ? "处理中..." : isLoginMode ? "立即登录" : "注册账号" }}
        </button>
      </div>
    </section>
  </main>
</template>
