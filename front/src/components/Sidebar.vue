<script setup lang="ts">
import { computed, ref } from "vue";
import {
  ChevronRight,
  History,
  LayoutDashboard,
  LayoutGrid,
  LogOut,
  MessageSquare,
  Palette,
  Plus,
  Settings,
  ShieldCheck,
  Sun,
  Moon,
  User,
  Workflow,
  Database,
  Search,
  ClipboardCheck,
} from "lucide-vue-next";
import type { UserProfile } from "../services/auth";

interface SessionSummary {
  id: string;
  title: string;
  updatedAt: number;
  pinned?: boolean;
}

type AdminView = "dashboard" | "knowledge" | "datasource" | "monitor" | "users" | "settings" | "evaluate";
type Workspace = "chat" | "admin";

const props = defineProps<{
  user: UserProfile | null;
  sessions: SessionSummary[];
  activeSessionId: string;
  workspace: Workspace;
  isDarkMode: boolean;
  currentAdminView: AdminView;
}>();

const emit = defineEmits<{
  "new-chat": [];
  "select-session": [sessionId: string];
  "open-customization": [];
  "open-profile": [];
  "open-settings": [];
  "open-logout": [];
  "toggle-theme": [];
  "change-admin-view": [view: AdminView];
  "switch-workspace": [workspace: "chat" | "admin"];
}>();

const isUserMenuOpen = ref(false);
const isHistoryOpen = ref(false);

const historyItems = computed(() =>
  [...props.sessions].sort((a, b) => {
    if (Boolean(a.pinned) !== Boolean(b.pinned)) {
      return Number(Boolean(b.pinned)) - Number(Boolean(a.pinned));
    }
    return b.updatedAt - a.updatedAt;
  })
);

const adminItems: Array<{ key: AdminView; label: string; icon: any }> = [
  { key: "dashboard", label: "仪表盘", icon: LayoutDashboard },
  { key: "knowledge", label: "知识库", icon: Search },
  { key: "datasource", label: "数据源", icon: Database },
  { key: "monitor", label: "任务监控", icon: Workflow },
  { key: "users", label: "用户管理", icon: User },
  { key: "settings", label: "系统设置", icon: Settings },
  { key: "evaluate", label: "检索评估", icon: ClipboardCheck },
];

function toggleUserMenu() {
  isUserMenuOpen.value = !isUserMenuOpen.value;
  if (isUserMenuOpen.value) {
    isHistoryOpen.value = false;
  }
}

function toggleHistory() {
  isHistoryOpen.value = !isHistoryOpen.value;
  if (isHistoryOpen.value) {
    isUserMenuOpen.value = false;
  }
}

function selectSession(sessionId: string) {
  emit("select-session", sessionId);
  isHistoryOpen.value = false;
}

function openAdminView(view: AdminView) {
  emit("change-admin-view", view);
  emit("switch-workspace", "admin");
  isHistoryOpen.value = false;
  isUserMenuOpen.value = false;
}
</script>

<template>
  <aside class="w-64 h-screen bg-surface-container-low flex flex-col p-6 select-none relative z-50 border-r border-outline-variant/10">
    <div class="flex items-center gap-3 mb-10">
      <div class="w-9 h-9 bg-on-surface rounded-full flex items-center justify-center">
        <LayoutGrid class="w-5 h-5 text-surface" />
      </div>
      <div>
        <h1 class="text-lg font-medium leading-none tracking-tight">Ibot</h1>
        <p class="text-[10px] text-on-surface-variant uppercase tracking-widest mt-1">企业知识服务</p>
      </div>
    </div>

    <template v-if="user?.role === 'ADMIN' && workspace === 'admin'">
      <div class="space-y-1">
        <button
          v-for="item in adminItems"
          :key="item.key"
          class="w-full rounded-full px-4 py-2 text-left text-sm transition"
          :class="currentAdminView === item.key ? 'bg-surface text-on-surface' : 'text-on-surface-variant hover:bg-surface hover:text-on-surface'"
          @click="openAdminView(item.key)"
        >
          <span class="inline-flex items-center gap-3">
            <component :is="item.icon" class="h-4 w-4" />
            {{ item.label }}
          </span>
        </button>
      </div>

      <div class="mt-6">
        <button
          class="flex w-full items-center gap-3 rounded-full bg-surface px-4 py-2.5 text-sm text-on-surface transition hover:opacity-90"
          @click="emit('switch-workspace', 'chat')"
        >
          <MessageSquare class="h-4 w-4" />
          打开聊天界面
        </button>
      </div>
    </template>

    <template v-else>
      <button
        @click="emit('new-chat')"
        class="flex items-center gap-3 px-6 py-3 bg-surface-container-highest rounded-full hover:bg-surface-container-high transition-colors text-sm font-medium mb-auto group w-full"
      >
        <Plus class="w-4 h-4 text-on-surface-variant group-hover:text-on-surface transition-colors" />
        <span>新建对话</span>
      </button>
    </template>

    <div class="mt-auto pt-2 border-t border-outline-variant/10 relative">
      <Transition
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="translate-y-2 opacity-0 scale-95"
        enter-to-class="translate-y-0 opacity-100 scale-100"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="translate-y-0 opacity-100 scale-100"
        leave-to-class="translate-y-2 opacity-0 scale-95"
      >
        <div
          v-if="isUserMenuOpen"
          class="absolute bottom-0 left-0 w-full bg-surface-container-highest/95 backdrop-blur-3xl rounded-2xl p-1.5 shadow-2xl border border-outline-variant/20 z-50"
        >
          <div class="flex flex-col gap-0.5">
            <button
              class="flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-on-surface/5 transition-colors text-sm text-on-surface group w-full text-left"
              @click="emit('open-customization'); isUserMenuOpen = false"
            >
              <Palette class="w-4 h-4 text-on-surface-variant group-hover:text-on-surface" />
              <span>个性化</span>
            </button>
            <button
              class="flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-on-surface/5 transition-colors text-sm text-on-surface group w-full text-left"
              @click="emit('open-profile'); isUserMenuOpen = false"
            >
              <User class="w-4 h-4 text-on-surface-variant group-hover:text-on-surface" />
              <span>个人资料</span>
            </button>
            <button
              class="flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-on-surface/5 transition-colors text-sm text-on-surface group w-full text-left"
              @click="emit('open-settings'); isUserMenuOpen = false"
            >
              <Settings class="w-4 h-4 text-on-surface-variant group-hover:text-on-surface" />
              <span>设置</span>
            </button>
            <div v-if="user?.role === 'ADMIN' && workspace === 'chat'" class="h-px bg-outline-variant/10 my-1 mx-2"></div>
            <button
              v-if="user?.role === 'ADMIN' && workspace === 'chat'"
              class="flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-on-surface/5 transition-colors text-sm text-on-surface group w-full text-left"
              @click="emit('switch-workspace', 'admin'); isUserMenuOpen = false"
            >
              <ShieldCheck class="w-4 h-4 text-on-surface-variant group-hover:text-on-surface" />
              <span>管理工作台</span>
            </button>
            <div class="h-px bg-outline-variant/10 my-1 mx-2"></div>
            <button
              class="flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-on-surface/5 transition-colors text-sm text-on-surface group w-full text-left"
              @click="emit('open-logout'); isUserMenuOpen = false"
            >
              <LogOut class="w-4 h-4 text-on-surface-variant group-hover:text-on-surface" />
              <span>退出登录</span>
            </button>
          </div>
        </div>
      </Transition>

      <div class="flex flex-col gap-0.5 relative">
        <Transition
          enter-active-class="transition duration-200 ease-out"
          enter-from-class="translate-y-2 opacity-0 scale-95"
          enter-to-class="translate-y-0 opacity-100 scale-100"
          leave-active-class="transition duration-150 ease-in"
          leave-from-class="translate-y-0 opacity-100 scale-100"
          leave-to-class="translate-y-2 opacity-0 scale-95"
        >
          <div
            v-if="isHistoryOpen && workspace === 'chat'"
            class="absolute bottom-full left-0 w-full mb-2 bg-surface-container-highest/95 backdrop-blur-3xl rounded-2xl p-1.5 shadow-2xl border border-outline-variant/20 z-50 max-h-48 overflow-y-auto scrollbar-hide"
          >
            <div class="flex flex-col gap-0.5">
              <div class="px-3 py-1.5 text-[10px] text-on-surface-variant uppercase tracking-widest flex items-center justify-between">
                <span>历史记录</span>
                <ChevronRight class="w-3 h-3 rotate-90" />
              </div>
              <button
                v-for="item in historyItems"
                :key="item.id"
                class="flex items-center gap-3 px-3 py-2 rounded-xl transition-colors text-sm group w-full text-left truncate"
                :class="item.id === activeSessionId ? 'bg-on-surface/5 text-on-surface' : 'hover:bg-on-surface/5 text-on-surface'"
                @click="selectSession(item.id)"
              >
                <span v-if="item.pinned" class="shrink-0 text-[10px] text-on-surface-variant">置顶</span>
                {{ item.title }}
              </button>
              <div v-if="historyItems.length === 0" class="px-3 py-2 text-sm text-on-surface-variant">暂无保存的会话记录。</div>
            </div>
          </div>
        </Transition>

        <button
          v-if="workspace === 'chat'"
          @click="toggleHistory"
          class="flex items-center justify-between px-3 py-2 rounded-lg hover:bg-surface-container-high transition-colors text-sm text-on-surface-variant hover:text-on-surface group w-full text-left"
        >
          <div class="flex items-center gap-3">
            <History class="w-4 h-4" />
            <span>历史记录</span>
          </div>
          <ChevronRight :class="['w-4 h-4 transition-transform duration-200', isHistoryOpen ? '-rotate-90' : 'rotate-0']" />
        </button>
        <button
          @click="emit('toggle-theme')"
          class="flex items-center gap-3 px-3 py-2 rounded-lg transition-colors text-sm text-on-surface-variant hover:text-on-surface hover:bg-surface-container-high group w-full text-left"
        >
          <Sun v-if="isDarkMode" class="w-4 h-4" />
          <Moon v-else class="w-4 h-4" />
          <span>{{ isDarkMode ? '浅色模式' : '深色模式' }}</span>
        </button>
        <div class="mt-2 pt-2 border-t border-outline-variant/10">
          <button
            @click="toggleUserMenu"
            class="flex items-center gap-3 px-3 py-2 rounded-lg transition-colors text-sm text-on-surface-variant hover:text-on-surface hover:bg-surface-container-high group w-full text-left"
          >
            <div class="w-7 h-7 rounded-full bg-surface-container-highest flex items-center justify-center text-[11px] font-medium text-on-surface">
              {{ user?.displayName?.slice(0, 1)?.toUpperCase() || 'I' }}
            </div>
            <div class="min-w-0 flex-1">
              <div class="truncate text-on-surface">{{ user?.displayName || '访客' }}</div>
              <div class="truncate text-[10px] uppercase tracking-[0.18em] text-on-surface-variant">{{ user?.role || '未登录' }}</div>
            </div>
          </button>
        </div>
      </div>
   
    </div>
  </aside>
</template>
