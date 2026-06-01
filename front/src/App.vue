<script setup lang="ts">
import { nextTick, onMounted, ref } from "vue";
import Sidebar from "./components/Sidebar.vue";
import CustomizationModal from "./components/CustomizationModal.vue";
import PersonalInformationModal from "./components/PersonalInformationModal.vue";
import LogoutModal from "./components/LogoutModal.vue";
import AuthModal from "./components/AuthModal.vue";
import AdminDashboard from "./components/AdminDashboard.vue";
import {
  ArrowUp,
  ChevronDown,
  Loader2,
  Moon,
  MoreVertical,
  Pin,
  Plus,
  Quote,
  Sparkles,
  Sun,
  Trash2,
} from "lucide-vue-next";
import { fetchCurrentUser, logout, type AuthResponse, type UserProfile } from "./services/auth";
import { ApiError, getAuthToken } from "./services/api";
import { fetchHistory, historyToMessages, sendMessage, type ChatCitation, type UiMessage } from "./services/chat";

interface SessionSummary {
  id: string;
  title: string;
  updatedAt: number;
  pinned?: boolean;
}

interface Message {
  id: string;
  role: "user" | "model";
  content: string;
  timestamp: number;
  citations?: ChatCitation[];
}

interface AssistantBlock {
  type: "paragraph" | "list";
  items: string[];
}

type Workspace = "chat" | "admin";
type AdminView = "dashboard" | "knowledge" | "datasource" | "monitor" | "users" | "settings" | "evaluate";

const STORAGE_KEY = "ibot-chat-sessions";
const ACTIVE_SESSION_KEY = "ibot-active-session";
const THEME_STORAGE_KEY = "ibot-theme";

const messages = ref<Message[]>([]);
const sessions = ref<SessionSummary[]>([]);
const activeSessionId = ref("");
const input = ref("");
const isLoading = ref(false);
const errorMessage = ref("");
const messagesEndRef = ref<HTMLElement | null>(null);

const showCustomization = ref(false);
const showProfile = ref(false);
const showLogout = ref(false);
const showAuth = ref(false);
const isAuthReady = ref(false);
const currentUser = ref<UserProfile | null>(null);
const workspace = ref<Workspace>("chat");
const currentAdminView = ref<AdminView>("dashboard");

const isMoreMenuOpen = ref(false);
const isDarkMode = ref(true);
const expandedCitationMessageIds = ref<string[]>([]);

function applyTheme(isDark: boolean) {
  isDarkMode.value = isDark;
  document.documentElement.dataset.theme = isDark ? "dark" : "light";
  localStorage.setItem(THEME_STORAGE_KEY, isDark ? "dark" : "light");
}

function toggleTheme() {
  applyTheme(!isDarkMode.value);
}

function createSessionId() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `session-${Date.now()}`;
}

function buildSessionTitle(text: string) {
  const compact = text.replace(/\s+/g, " ").trim();
  return compact.length > 24 ? `${compact.slice(0, 24)}...` : compact;
}

function readSessions() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return [];
    }
    return (JSON.parse(raw) as SessionSummary[]).map((session) => ({
      ...session,
      pinned: Boolean(session.pinned),
    }));
  } catch {
    return [];
  }
}

function persistSessions() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions.value));
  localStorage.setItem(ACTIVE_SESSION_KEY, activeSessionId.value);
}

function ensureSessionSummary(sessionId: string, firstQuestion?: string) {
  const now = Date.now();
  const existing = sessions.value.find((item) => item.id === sessionId);

  if (existing) {
    existing.updatedAt = now;
    if (firstQuestion && existing.title === "新对话") {
      existing.title = buildSessionTitle(firstQuestion);
    }
    sessions.value = [...sessions.value];
    return;
  }

  sessions.value = [
    {
      id: sessionId,
      title: firstQuestion ? buildSessionTitle(firstQuestion) : "新对话",
      updatedAt: now,
      pinned: false,
    },
    ...sessions.value,
  ];
}

function toViewMessages(historyMessages: UiMessage[]): Message[] {
  return historyMessages.map((message) => ({
    id: message.id,
    role: message.role === "assistant" ? "model" : "user",
    content: message.content,
    timestamp: message.timestamp,
    citations: message.citations,
  }));
}

function formatAssistantBlocks(content: string): AssistantBlock[] {
  const normalized = content.replace(/\r/g, "").trim();
  if (!normalized) {
    return [];
  }

  const sections = normalized.split(/\n\s*\n/);

  return sections.map((section) => {
    const lines = section
      .split("\n")
      .map((line) => line.trim())
      .filter(Boolean);

    const isList = lines.length > 1 && lines.every((line) => /^([-*]\s+|\d+\.\s+)/.test(line));

    if (isList) {
      return {
        type: "list",
        items: lines.map((line) => line.replace(/^([-*]\s+|\d+\.\s+)/, "").trim()),
      };
    }

    return {
      type: "paragraph",
      items: [lines.join(" ")],
    };
  });
}

async function scrollToBottom() {
  await nextTick();
  messagesEndRef.value?.scrollIntoView({ behavior: "smooth" });
}

function handleUnauthorized() {
  logout();
  currentUser.value = null;
  showAuth.value = true;
  errorMessage.value = "登录状态已过期，请重新登录。";
}

async function loadHistory(sessionId: string) {
  activeSessionId.value = sessionId;
  errorMessage.value = "";

  try {
    const history = await fetchHistory(sessionId);
    messages.value = history ? toViewMessages(historyToMessages(history.history)) : [];
    ensureSessionSummary(sessionId, history?.history[0]?.question);
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      handleUnauthorized();
      return;
    }
    errorMessage.value = error instanceof Error ? error.message : "无法加载会话历史记录。";
    messages.value = [];
  } finally {
    persistSessions();
    scrollToBottom();
  }
}

async function handleSend(prefilledMessage?: string) {
  const currentInput = (prefilledMessage ?? input.value).trim();
  if (!currentInput || isLoading.value) {
    return;
  }

  if (!activeSessionId.value) {
    activeSessionId.value = createSessionId();
  }

  const userMessage: Message = {
    id: `user-${Date.now()}`,
    role: "user",
    content: currentInput,
    timestamp: Date.now(),
  };

  messages.value.push(userMessage);
  ensureSessionSummary(activeSessionId.value, currentInput);
  input.value = "";
  isLoading.value = true;
  errorMessage.value = "";
  persistSessions();
  scrollToBottom();

  try {
    const response = await sendMessage(currentInput, activeSessionId.value);
    const aiMessage: Message = {
      id: `model-${Date.now()}`,
      role: "model",
      content: response.answer,
      timestamp: Date.now(),
      citations: response.citations,
    };
    messages.value.push(aiMessage);
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      handleUnauthorized();
      return;
    }
    errorMessage.value = error instanceof Error ? error.message : "消息发送失败，请稍后重试。";
  } finally {
    isLoading.value = false;
    persistSessions();
    scrollToBottom();
  }
}

function handleComposerKeydown(event: KeyboardEvent) {
  if (event.key === "Enter" && !event.shiftKey) {
    event.preventDefault();
    handleSend();
  }
}

function handleNewChat() {
  activeSessionId.value = createSessionId();
  messages.value = [];
  input.value = "";
  errorMessage.value = "";
  isMoreMenuOpen.value = false;
  ensureSessionSummary(activeSessionId.value);
  persistSessions();
}

function toggleMoreMenu() {
  isMoreMenuOpen.value = !isMoreMenuOpen.value;
}

function toggleCitationPanel(messageId: string) {
  if (expandedCitationMessageIds.value.includes(messageId)) {
    expandedCitationMessageIds.value = expandedCitationMessageIds.value.filter((id) => id !== messageId);
    return;
  }
  expandedCitationMessageIds.value = [...expandedCitationMessageIds.value, messageId];
}

function isCitationPanelExpanded(messageId: string) {
  return expandedCitationMessageIds.value.includes(messageId);
}

function switchWorkspace(nextWorkspace: Workspace) {
  if (nextWorkspace === "admin" && currentUser.value?.role !== "ADMIN") {
    return;
  }
  workspace.value = nextWorkspace;
}

function togglePinActiveSession() {
  if (!activeSessionId.value) {
    return;
  }

  sessions.value = sessions.value.map((session) =>
    session.id === activeSessionId.value
      ? {
          ...session,
          pinned: !session.pinned,
          updatedAt: Date.now(),
        }
      : session
  );
  persistSessions();
  isMoreMenuOpen.value = false;
}

function deleteActiveSession() {
  if (!activeSessionId.value) {
    return;
  }

  const deletingSessionId = activeSessionId.value;
  sessions.value = sessions.value.filter((session) => session.id !== deletingSessionId);
  messages.value = [];
  input.value = "";
  errorMessage.value = "";
  isMoreMenuOpen.value = false;

  const nextSession = sessions.value[0];
  activeSessionId.value = nextSession?.id || "";
  persistSessions();

  if (nextSession) {
    loadHistory(nextSession.id);
    return;
  }

  handleNewChat();
}

function handleAuthSuccess(response: AuthResponse) {
  currentUser.value = response.user;
  showAuth.value = false;
  workspace.value = response.user.role === "ADMIN" ? "admin" : "chat";

  if (!activeSessionId.value) {
    handleNewChat();
  }
}

function handleLogout() {
  logout();
  currentUser.value = null;
  workspace.value = "chat";
  showLogout.value = false;
  showAuth.value = true;
  isMoreMenuOpen.value = false;
}

async function bootstrapAuth() {
  if (!getAuthToken()) {
    showAuth.value = true;
    isAuthReady.value = true;
    return;
  }

  try {
    const user = await fetchCurrentUser();
    currentUser.value = user;
    showAuth.value = false;
    workspace.value = user.role === "ADMIN" ? "admin" : "chat";
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      logout();
      showAuth.value = true;
    } else {
      errorMessage.value = error instanceof Error ? error.message : "认证初始化失败。";
      showAuth.value = true;
    }
  } finally {
    isAuthReady.value = true;
  }
}

onMounted(async () => {
  const savedTheme = localStorage.getItem(THEME_STORAGE_KEY);
  applyTheme(savedTheme !== "light");

  sessions.value = readSessions();
  const cachedActiveSessionId = localStorage.getItem(ACTIVE_SESSION_KEY);
  if (cachedActiveSessionId) {
    activeSessionId.value = cachedActiveSessionId;
  }

  await bootstrapAuth();

  if (!currentUser.value) {
    return;
  }

  if (activeSessionId.value) {
    await loadHistory(activeSessionId.value);
    if (messages.value.length > 0 || !errorMessage.value) {
      return;
    }
  }

  handleNewChat();
});
</script>

<template>
  <div class="flex h-screen bg-surface overflow-hidden">
    <Sidebar
      :user="currentUser"
      :sessions="sessions"
      :active-session-id="activeSessionId"
      :workspace="workspace"
      :is-dark-mode="isDarkMode"
      :current-admin-view="currentAdminView"
      @new-chat="handleNewChat"
      @select-session="loadHistory"
      @open-customization="showCustomization = true"
      @open-profile="showProfile = true"
      @open-logout="showLogout = true"
      @toggle-theme="toggleTheme"
      @change-admin-view="currentAdminView = $event"
      @switch-workspace="switchWorkspace"
    />

    <main class="flex-1 flex flex-col relative">
      <template v-if="!isAuthReady">
        <div class="flex h-full items-center justify-center text-on-surface-variant">
          <Loader2 class="h-5 w-5 animate-spin" />
          <span class="ml-3 text-sm">正在启动 ibot...</span>
        </div>
      </template>

      <template v-else-if="currentUser && workspace === 'admin'">
        <AdminDashboard :view="currentAdminView" />
      </template>

      <template v-else>
        <header class="h-16 flex items-center justify-between px-8 select-none border-b border-outline-variant/10">
          <div class="flex-1" />
          <div class="text-sm font-medium text-on-surface-variant">Ibot</div>
          <div class="flex-1 flex justify-end gap-4 relative">
            <button
              @click="toggleTheme"
              class="p-2 hover:bg-surface-container-high rounded-lg transition-colors text-on-surface-variant hover:text-on-surface"
              :aria-label="isDarkMode ? '切换为浅色模式' : '切换为深色模式'"
              :title="isDarkMode ? '切换为浅色模式' : '切换为深色模式'"
            >
              <Sun v-if="isDarkMode" class="w-4 h-4" />
              <Moon v-else class="w-4 h-4" />
            </button>
            <div class="relative">
              <button
                @click="toggleMoreMenu"
                :class="[
                  'p-2 rounded-lg transition-colors',
                  isMoreMenuOpen ? 'bg-surface-container-high text-on-surface' : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface'
                ]"
              >
                <MoreVertical class="w-4 h-4" />
              </button>

              <Transition
                enter-active-class="transition duration-200 ease-out"
                enter-from-class="translate-y-2 opacity-0 scale-95"
                enter-to-class="translate-y-0 opacity-100 scale-100"
                leave-active-class="transition duration-150 ease-in"
                leave-from-class="translate-y-0 opacity-100 scale-100"
                leave-to-class="translate-y-2 opacity-0 scale-95"
              >
                <div
                  v-if="isMoreMenuOpen"
                  class="absolute right-0 mt-2 w-48 bg-surface-container-highest/95 backdrop-blur-3xl rounded-2xl p-1.5 shadow-2xl border border-outline-variant/20 z-[60]"
                >
                  <div class="flex flex-col gap-0.5">
                    <button @click="togglePinActiveSession" class="flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-on-surface/5 transition-colors text-sm text-on-surface group w-full text-left">
                      <Pin class="w-4 h-4 text-on-surface-variant group-hover:text-on-surface" />
                      <span>置顶会话</span>
                    </button>
                    <div class="h-px bg-outline-variant/10 my-1 mx-2"></div>
                    <button @click="deleteActiveSession" class="flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-red-500/10 transition-colors text-sm text-red-400 group w-full text-left">
                      <Trash2 class="w-4 h-4 text-red-400/60 group-hover:text-red-400" />
                      <span>删除对话</span>
                    </button>
                  </div>
                </div>
              </Transition>
            </div>
          </div>
        </header>

        <div class="flex-1 overflow-y-auto px-4 py-8 scroll-smooth">
          <div class="max-w-3xl mx-auto w-full">
            <div v-if="messages.length === 0" class="h-[60vh] flex flex-col items-center justify-center text-center">
              <p class="text-[11px] uppercase tracking-[0.3em] text-on-surface-variant">企业智能问答客服</p>
              <h2 class="mt-4 text-4xl font-medium tracking-tight text-on-surface">输入企业知识相关问题，ibot 将先检索后回答。</h2>
              
              
              
            </div>

            <div v-else class="space-y-8 pb-32">
              <div v-for="msg in messages" :key="msg.id" :class="['flex w-full', msg.role === 'user' ? 'justify-end' : 'justify-start']">
                <div :class="[msg.role === 'user' ? 'flex w-full justify-end' : 'w-full max-w-[88%]']">
                  <div
                    v-if="msg.role === 'user'"
                    class="w-fit min-w-[8rem] max-w-[min(85vw,32rem)] rounded-[24px] bg-surface-container-highest px-5 py-3 text-left text-[15px] leading-7 text-on-surface shadow-[0_12px_36px_rgba(0,0,0,0.18)] whitespace-pre-wrap break-words"
                  >
                    {{ msg.content }}
                  </div>

                  <div v-else class="assistant-card rounded-[24px] border border-white/6 bg-white/[0.03] px-6 py-5 text-on-surface shadow-[0_18px_60px_rgba(0,0,0,0.18)] backdrop-blur-sm">
                    <div class="mb-4 flex items-center gap-3 text-on-surface-variant">
                      <div class="flex h-8 w-8 items-center justify-center rounded-full bg-white/6">
                        <Sparkles class="h-4 w-4" />
                      </div>
                      <div class="flex items-center">
                        <p class="text-sm font-medium text-on-surface">Ibot</p>
                        <p class="text-[11px] uppercase tracking-[0.22em] text-on-surface-variant/70">通义千问回复</p>
                      </div>
                    </div>

                    <div class="assistant-prose">
                      <template v-for="(block, blockIndex) in formatAssistantBlocks(msg.content)" :key="`${msg.id}-${blockIndex}`">
                        <p v-if="block.type === 'paragraph'">{{ block.items[0] }}</p>
                        <ul v-else>
                          <li v-for="(item, itemIndex) in block.items" :key="`${msg.id}-${blockIndex}-${itemIndex}`">{{ item }}</li>
                        </ul>
                      </template>
                    </div>

                    <div v-if="msg.citations && msg.citations.length > 0" class="mt-5 border-t border-white/6 pt-4">
                      <button
                        @click="toggleCitationPanel(msg.id)"
                        class="flex w-full items-center justify-between rounded-2xl px-1 py-1 text-left transition-colors hover:bg-white/[0.03]"
                      >
                        <div class="flex items-center gap-2 text-[11px] uppercase tracking-[0.22em] text-on-surface-variant/75">
                          <Quote class="h-3.5 w-3.5" />
                          <span>参考片段</span>
                          <span class="text-[10px] normal-case tracking-normal">{{ msg.citations.length }} 条</span>
                        </div>
                        <ChevronDown
                          :class="[
                            'h-4 w-4 text-on-surface-variant/75 transition-transform duration-200',
                            isCitationPanelExpanded(msg.id) ? 'rotate-180' : 'rotate-0'
                          ]"
                        />
                      </button>
                      <div v-if="isCitationPanelExpanded(msg.id)" class="mt-3 space-y-3">
                        <div v-for="(citation, citationIndex) in msg.citations" :key="`${msg.id}-citation-${citationIndex}`" class="rounded-2xl bg-white/[0.035] px-4 py-3">
                          <div class="flex items-center justify-between gap-3">
                            <p class="truncate text-sm font-medium text-on-surface">{{ citation.fileName }}</p>
                            <span class="shrink-0 text-[11px] text-on-surface-variant">{{ Number(citation.score).toFixed(2) }}</span>
                          </div>
                          <p class="mt-2 text-sm leading-6 text-on-surface-variant">{{ citation.snippet }}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="isLoading" class="flex justify-start">
                <div class="assistant-card rounded-[24px] border border-white/6 bg-white/[0.03] px-5 py-4 text-on-surface-variant flex items-center gap-3 backdrop-blur-sm">
                  <div class="flex h-8 w-8 items-center justify-center rounded-full bg-white/6">
                    <Loader2 class="w-4 h-4 animate-spin" />
                  </div>
                  <div class="flex items-center gap-1.5 text-sm">
                    <span>ibot 正在思考</span>
                    <span class="loading-dot"></span>
                    <span class="loading-dot"></span>
                    <span class="loading-dot"></span>
                  </div>
                </div>
              </div>

              <div v-if="errorMessage" class="flex justify-start">
                <div class="max-w-[85%] px-5 py-3 rounded-2xl bg-red-500/10 text-red-300 text-sm leading-relaxed">
                  {{ errorMessage }}
                </div>
              </div>

              <div ref="messagesEndRef" />
            </div>
          </div>
        </div>

        <div class="absolute bottom-0 left-0 right-0 p-8 bg-gradient-to-t from-surface via-surface to-transparent pointer-events-none">
          <div class="max-w-3xl mx-auto w-full pointer-events-auto">
            <div class="relative flex items-end">
              <button class="absolute bottom-3 left-4 p-2 hover:bg-surface-container-high rounded-full transition-colors text-on-surface-variant hover:text-on-surface" @click="handleNewChat">
                <Plus class="w-5 h-5" />
              </button>
              <textarea
                v-model="input"
                rows="1"
                @keydown="handleComposerKeydown"
                placeholder="请输入制度、报告、流程或产品知识相关问题"
                class="min-h-[56px] max-h-40 w-full resize-none overflow-y-auto rounded-[28px] border-none bg-surface-container-highest/80 py-4 pl-14 pr-14 text-[15px] leading-7 text-on-surface outline-none transition-all placeholder:text-on-surface-variant/50 focus:ring-1 focus:ring-outline-variant/30 backdrop-blur-xl"
              />
              <button
                @click="handleSend()"
                :disabled="!input.trim() || isLoading"
                :class="[
                  input.trim() && !isLoading ? 'bg-on-surface text-surface' : 'bg-surface-container-high text-on-surface-variant opacity-50'
                ]"
              >
                <ArrowUp class="w-5 h-5" />
              </button>
            </div>
            <p class="text-[10px] text-center text-on-surface-variant/40 mt-4 uppercase tracking-widest select-none">
              生成结果仅供参考，涉及重要业务信息时请再次核实。
            </p>
          </div>
        </div>
      </template>
    </main>

    <CustomizationModal :show="showCustomization" @close="showCustomization = false" />
    <PersonalInformationModal :show="showProfile" :user="currentUser" @close="showProfile = false" />
    <LogoutModal :show="showLogout" :email="currentUser?.email || ''" @close="showLogout = false" @logout="handleLogout" />
    <AuthModal :show="showAuth" @close="showAuth = false" @success="handleAuthSuccess" />
  </div>
</template>