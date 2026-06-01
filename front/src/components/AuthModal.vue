<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { ShieldCheck, UserRound, Loader2 } from "lucide-vue-next";
import BaseModal from "./BaseModal.vue";
import { login, register, type AuthResponse } from "../services/auth";
import { ApiError } from "../services/api";

const props = defineProps<{
  show: boolean;
}>();

const emit = defineEmits<{
  close: [];
  success: [response: AuthResponse];
}>();

const mode = ref<"login" | "register">("login");
const loginAs = ref<"USER" | "ADMIN">("USER");
const form = ref({
  email: "",
  displayName: "",
  password: "",
});
const isSubmitting = ref(false);
const errorMessage = ref("");

watch(
  () => props.show,
  (show) => {
    if (show) {
      errorMessage.value = "";
      isSubmitting.value = false;
    }
  }
);

watch(mode, () => {
  errorMessage.value = "";
  form.value = {
    email: "",
    displayName: "",
    password: "",
  };
});

const title = computed(() => (mode.value === "login" ? "欢迎回来" : "创建账号"));
const subtitle = computed(() =>
  mode.value === "login"
    ? loginAs.value === "ADMIN"
      ? "使用已有管理员账号登录。"
      : "登录后继续使用企业知识检索与问答。"
    : "注册只会创建普通用户账号。"
);

async function handleSubmit() {
  if (!form.value.email || !form.value.password || (mode.value === "register" && !form.value.displayName)) {
    errorMessage.value = "请完整填写必填信息。";
    return;
  }

  errorMessage.value = "";
  isSubmitting.value = true;

  try {
    const response =
      mode.value === "login"
        ? await login({ email: form.value.email, password: form.value.password })
        : await register({
            email: form.value.email,
            displayName: form.value.displayName,
            password: form.value.password,
          });

    emit("success", response);
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : "暂时无法完成认证，请稍后重试。";
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <BaseModal :show="show" :title="title" @close="emit('close')">
    <div class="space-y-6">
      <div class="space-y-2">
        <p class="text-sm text-on-surface-variant">{{ subtitle }}</p>
      </div>

      <div class="rounded-full bg-surface-container-low p-1 flex border border-outline-variant/20">
        <button
          class="flex-1 rounded-full px-4 py-2 text-sm transition-colors"
          :class="mode === 'login' ? 'bg-on-surface text-surface' : 'text-on-surface-variant'"
          @click="mode = 'login'"
        >
          登录
        </button>
        <button
          class="flex-1 rounded-full px-4 py-2 text-sm transition-colors"
          :class="mode === 'register' ? 'bg-on-surface text-surface' : 'text-on-surface-variant'"
          @click="mode = 'register'; loginAs = 'USER'"
        >
          注册
        </button>
      </div>

      <div v-if="mode === 'login'" class="rounded-full bg-surface-container-low p-1 flex border border-outline-variant/20">
        <button
          class="flex-1 rounded-full px-4 py-2 text-sm transition-colors inline-flex items-center justify-center gap-2"
          :class="loginAs === 'USER' ? 'bg-on-surface text-surface' : 'text-on-surface-variant'"
          @click="loginAs = 'USER'"
        >
          <UserRound class="h-4 w-4" />
          用户
        </button>
        <button
          class="flex-1 rounded-full px-4 py-2 text-sm transition-colors inline-flex items-center justify-center gap-2"
          :class="loginAs === 'ADMIN' ? 'bg-on-surface text-surface' : 'text-on-surface-variant'"
          @click="loginAs = 'ADMIN'"
        >
          <ShieldCheck class="h-4 w-4" />
          管理员
        </button>
      </div>

      <div class="space-y-4">
        <label class="block space-y-2">
          <span class="text-xs uppercase tracking-[0.24em] text-on-surface-variant">邮箱</span>
          <input
            v-model="form.email"
            type="email"
            class="w-full rounded-2xl bg-surface-container-low px-4 py-3 outline-none ring-1 ring-transparent transition focus:ring-outline-variant/40"
            placeholder="you@company.com"
          />
        </label>

        <label v-if="mode === 'register'" class="block space-y-2">
          <span class="text-xs uppercase tracking-[0.24em] text-on-surface-variant">显示名称</span>
          <input
            v-model="form.displayName"
            type="text"
            class="w-full rounded-2xl bg-surface-container-low px-4 py-3 outline-none ring-1 ring-transparent transition focus:ring-outline-variant/40"
            placeholder="请输入你的称呼"
          />
        </label>

        <label class="block space-y-2">
          <span class="text-xs uppercase tracking-[0.24em] text-on-surface-variant">密码</span>
          <input
            v-model="form.password"
            type="password"
            class="w-full rounded-2xl bg-surface-container-low px-4 py-3 outline-none ring-1 ring-transparent transition focus:ring-outline-variant/40"
            placeholder="请输入密码"
          />
        </label>
      </div>

      <p v-if="mode === 'register'" class="text-xs text-on-surface-variant">
        新注册账号默认都是普通用户，管理员账号需要由系统内已有管理员创建。
      </p>

      <p v-if="errorMessage" class="rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-300">
        {{ errorMessage }}
      </p>

      <div class="flex items-center justify-end gap-3">
        <button
          class="rounded-full px-5 py-2.5 text-sm text-on-surface-variant transition hover:bg-surface-container-low hover:text-on-surface"
          @click="emit('close')"
        >
          稍后再说
        </button>
        <button
          class="inline-flex min-w-32 items-center justify-center gap-2 rounded-full bg-on-surface px-5 py-2.5 text-sm text-surface transition hover:opacity-90 disabled:opacity-60"
          :disabled="isSubmitting"
          @click="handleSubmit"
        >
          <Loader2 v-if="isSubmitting" class="h-4 w-4 animate-spin" />
          <span>{{ mode === 'login' ? '继续登录' : '创建账号' }}</span>
        </button>
      </div>
    </div>
  </BaseModal>
</template>
