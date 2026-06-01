<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch, computed } from "vue";
import {
  CheckCircle2,
  Database,
  Edit3,
  FolderPlus,
  Loader2,
  Plus,
  RefreshCw,
  RotateCcw,
  Save,
  Search,
  Shield,
  Tag,
  Trash2,
  Upload,
  Users,
  Workflow,
  Wrench,
  X,
  ClipboardCheck,
  FileText,
  DatabaseZap,
} from "lucide-vue-next";
import {
  createDataSource,
  createUser,
  deleteDataSource,
  deleteKnowledgeDocument,
  deleteUser,
  getRagStatus,
  getRebuildTask,
  getSettings,
  getTask,
  listDataSources,
  listKnowledgeDocuments,
  listTasks,
  listUsers,
  rebuildKnowledgeBase,
  resetUserPassword,
  testDataSource,
  type AdminSettings,
  type AdminTask,
  type DataSource,
  type DataSourcePayload,
  type KnowledgeDocument,
  type RagStatus,
  type RebuildResponse,
  type UserProfileItem,
  updateDataSource,
  updateSettings,
  updateUser,
  uploadKnowledgeDocuments,
  getDashboard,
  listKnowledgeBases,
  createKnowledgeBase,
  deleteKnowledgeBase,
  renameKnowledgeBase,
  updateDocumentMetadata,
  listKnowledgeBaseTags,
  listKnowledgeBaseFolders,
  type KnowledgeBaseInfo,
  type DocumentMetadataPayload,
  type DashboardData,
  seedSampleData,
  seedAllKnowledgeBases,
  seedDomain,
  previewDocument,
  evaluateKnowledgeBase,
  getEvaluationResult,
  type DocumentPreview,
  type EvaluationResult,
} from "../services/admin";
import { ApiError } from "../services/api";

const props = defineProps<{
  view: "dashboard" | "knowledge" | "datasource" | "monitor" | "users" | "settings" | "evaluate";
}>();

const knowledgeBase = ref("default");
const knowledgeDocuments = ref<KnowledgeDocument[]>([]);
const ragStatus = ref<RagStatus | null>(null);
const latestRebuild = ref<RebuildResponse | null>(null);
const selectedFiles = ref<File[]>([]);
const uploadBusy = ref(false);
const rebuildBusy = ref(false);
const viewBusy = ref(false);
const panelMessage = ref("");
const panelError = ref("");

// Dashboard data
const dashboardData = ref<DashboardData | null>(null);
const dashboardBusy = ref(false);

// Sample data & preview & evaluation
const seedBusy = ref(false);
const seedBusyAll = ref(false);
const seedMessage = ref("");
const previewDoc = ref<DocumentPreview | null>(null);
const showPreview = ref(false);
const evalFile = ref<File | null>(null);
const evalBusy = ref(false);
const evalResult = ref<EvaluationResult | null>(null);
const evalKnowledgeBase = ref("default");

async function handleSeed(kbName: string) {
  seedBusy.value = true;
  try {
    const result = await seedSampleData(kbName);
    panelMessage.value = result.message;
    await loadKnowledgePanel();
  } catch (error) {
    setError(error, "无法导入示例数据。");
  } finally {
    seedBusy.value = false;
  }
}

async function handleSeedAll() {
  seedBusyAll.value = true;
  seedMessage.value = "";
  try {
    const results = await seedAllKnowledgeBases();
    const total = results.reduce((sum: number, r: any) => sum + r.documentCount, 0);
    seedMessage.value = "已创建 " + results.length + " 个知识库，共 " + total + " 个文档";
    await Promise.all([loadKnowledgePanel(), loadKnowledgeBases()]);
  } catch (error) {
    setError(error, "批量导入失败。");
  } finally {
    seedBusyAll.value = false;
  }
}

async function handlePreview(docId: string) {
  try {
    previewDoc.value = await previewDocument(knowledgeBase.value, docId);
    showPreview.value = true;
  } catch (error) {
    setError(error, "无法预览文档。");
  }
}

function closePreview() {
  showPreview.value = false;
  previewDoc.value = null;
}

async function handleEvaluate() {
  if (!evalFile.value) {
    panelError.value = "请先选择评估文件（CSV格式）。";
    return;
  }
  evalBusy.value = true;
  panelError.value = "";
  panelMessage.value = "";
  evalResult.value = null;
  try {
    const result = await evaluateKnowledgeBase(evalKnowledgeBase.value, evalFile.value);
    evalResult.value = result;
    panelMessage.value = "评估任务已启动，任务ID: " + result.taskId;
    // Poll for completion
    pollEvaluation(result.taskId);
  } catch (error) {
    setError(error, "无法启动评估。");
  } finally {
    evalBusy.value = false;
  }
}

function pollEvaluation(taskId: string) {
  const timer = window.setInterval(async () => {
    try {
      const result = await getEvaluationResult(taskId);
      evalResult.value = result;
      if (result.status === "COMPLETED" || result.status === "FAILED") {
        window.clearInterval(timer);
        if (result.status === "COMPLETED") {
          panelMessage.value = "评估完成：共" + result.totalQuestions + "题，匹配" + result.matchedCount + "题，平均分" + result.avgScore.toFixed(2);
        }
      }
    } catch {
      window.clearInterval(timer);
    }
  }, 2000);
}

async function loadDashboard() {
  dashboardBusy.value = true;
  try {
    dashboardData.value = await getDashboard();
  } catch (error) {
    setError(error, "无法加载仪表盘数据。");
  } finally {
    dashboardBusy.value = false;
  }
}

const memoryUsagePercent = computed(() => {
  if (!dashboardData.value?.systemStats) return 0;
  const { usedMemoryMb, maxMemoryMb } = dashboardData.value.systemStats;
  if (maxMemoryMb <= 0) return 0;
  return Math.round((usedMemoryMb / maxMemoryMb) * 100);
});

// Knowledge base management
const knowledgeBases = ref<KnowledgeBaseInfo[]>([]);
const kbSelectorOpen = ref(false);
const showCreateKb = ref(false);
const newKbName = ref("");
const showDeleteKbConfirm = ref(false);
const showRenameKb = ref(false);
const renameKbName = ref("");

// Search and filter
const searchQuery = ref("");
const availableTags = ref<string[]>([]);
const availableFolders = ref<string[]>([]);
const selectedTag = ref("__all__");
const selectedFolder = ref("__all__");

// Tag / folder inline editing
const editingTagDocId = ref<string | null>(null);
const editingTagInput = ref("");
const editingFolderDocId = ref<string | null>(null);
const editingFolderInput = ref("");

const dataSources = ref<DataSource[]>([]);
const editingDataSourceId = ref<number | null>(null);
const dataSourceForm = ref<DataSourcePayload>({
  name: "",
  type: "MYSQL",
  host: "127.0.0.1",
  port: 3306,
  databaseName: "",
  username: "",
  password: "",
  apiBaseUrl: "",
  notes: "",
  enabled: true,
});

const users = ref<UserProfileItem[]>([]);
const createUserForm = ref({
  email: "",
  displayName: "",
  password: "",
  role: "USER" as "USER" | "ADMIN",
});
const tasks = ref<AdminTask[]>([]);
const selectedTask = ref<AdminTask | null>(null);
const settings = ref<AdminSettings | null>(null);
const settingsForm = ref({
  chatModelName: "qwen-max",
  defaultKnowledgeBase: "default",
  retrievalMaxResults: 3,
  retrievalMinScore: 0.6,
  uploadMaxSizeMb: 20,
});

const dataSourceTypeOptions = ["MYSQL", "POSTGRESQL", "MONGODB", "ELASTICSEARCH", "REDIS", "API_ENDPOINT"] as const;
let rebuildPollTimer: number | null = null;

// Computed
const selectedFileCount = ref(0);

function updateSelectedFiles(event: Event) {
  const input = event.target as HTMLInputElement;
  selectedFiles.value = Array.from(input.files || []);
  selectedFileCount.value = selectedFiles.value.length;
}

const filteredDocuments = computed(() => {
  return knowledgeDocuments.value;
});

function getBarHeight(value: number, allDays: Array<{ date: string; sessionCount: number; messageCount: number }>) {
  const maxVal = Math.max(...allDays.map((d) => Math.max(d.sessionCount, d.messageCount)), 1);
  return Math.max(4, (value / maxVal) * 100);
}

function setError(error: unknown, fallback: string) {
  panelError.value = error instanceof ApiError ? error.message : fallback;
}

async function loadSettingsPanel() {
  const response = await getSettings();
  settings.value = response;
  settingsForm.value = {
    chatModelName: response.chatModelName,
    defaultKnowledgeBase: response.defaultKnowledgeBase,
    retrievalMaxResults: response.retrievalMaxResults,
    retrievalMinScore: response.retrievalMinScore,
    uploadMaxSizeMb: response.uploadMaxSizeMb,
  };
  if (!knowledgeBase.value || knowledgeBase.value === "default") {
    knowledgeBase.value = response.defaultKnowledgeBase || "default";
  }
}

async function loadKnowledgeBases() {
  try {
    knowledgeBases.value = await listKnowledgeBases();
  } catch {
    // Silently ignore — we'll fall back to the current KB name
  }
}

async function loadKnowledgeBaseMeta() {
  try {
    availableTags.value = await listKnowledgeBaseTags(knowledgeBase.value);
    availableFolders.value = await listKnowledgeBaseFolders(knowledgeBase.value);
  } catch {
    availableTags.value = [];
    availableFolders.value = [];
  }
}

async function loadKnowledgePanel() {
  const [documents, status] = await Promise.all([
    listKnowledgeDocuments(knowledgeBase.value, searchQuery.value || undefined, selectedTag.value !== "__all__" ? selectedTag.value : undefined, selectedFolder.value !== "__all__" ? selectedFolder.value : undefined),
    getRagStatus(knowledgeBase.value),
  ]);
  knowledgeDocuments.value = documents;
  ragStatus.value = status;
  await loadKnowledgeBaseMeta();
}

function applyFilters() {
  loadKnowledgePanel().catch((error) => setError(error, "无法刷新文档列表。"));
}

async function loadDataSourcePanel() {
  dataSources.value = await listDataSources();
}

async function loadUsersPanel() {
  users.value = await listUsers();
}

async function loadTasksPanel() {
  tasks.value = await listTasks();
}

async function refreshCurrentView() {
  panelError.value = "";
  panelMessage.value = "";
  viewBusy.value = true;

  try {
    if (!settings.value || props.view === "settings") {
      await loadSettingsPanel();
    }

    if (props.view === "dashboard") {
      await loadDashboard();
    } else if (props.view === "knowledge") {
      await Promise.all([loadKnowledgePanel(), loadKnowledgeBases()]);
    } else if (props.view === "datasource") {
      await loadDataSourcePanel();
    } else if (props.view === "users") {
      await loadUsersPanel();
    } else if (props.view === "monitor") {
      await loadTasksPanel();
    }
  } catch (error) {
    setError(error, "当前管理模块加载失败。");
  } finally {
    viewBusy.value = false;
  }
}

// ====== Knowledge base CRUD ======

function selectKnowledgeBase(name: string) {
  knowledgeBase.value = name;
  selectedTag.value = "__all__";
  selectedFolder.value = "__all__";
  searchQuery.value = "";
  kbSelectorOpen.value = false;
  loadKnowledgePanel().catch((error) => setError(error, "无法切换到知识库。"));
}

async function handleCreateKb() {
  const name = newKbName.value.trim();
  if (!name) return;
  try {
    const kb = await createKnowledgeBase(name);
    knowledgeBases.value.push(kb);
    selectKnowledgeBase(kb.name);
    showCreateKb.value = false;
    newKbName.value = "";
    panelMessage.value = `知识库 "${kb.name}" 创建成功。`;
  } catch (error) {
    setError(error, "无法创建知识库。");
  }
}

async function handleDeleteKb() {
  try {
    await deleteKnowledgeBase(knowledgeBase.value);
    panelMessage.value = `知识库 "${knowledgeBase.value}" 已删除。`;
    showDeleteKbConfirm.value = false;
    // Switch to the first available KB or default
    await loadKnowledgeBases();
    const firstKb = knowledgeBases.value[0];
    if (firstKb) {
      selectKnowledgeBase(firstKb.name);
    } else {
      // Create default
      try {
        await createKnowledgeBase("default");
        await loadKnowledgeBases();
        selectKnowledgeBase("default");
      } catch {
        knowledgeBase.value = "default";
        knowledgeDocuments.value = [];
      }
    }
  } catch (error) {
    setError(error, "无法删除知识库。");
  }
}

async function handleRenameKb() {
  const newName = renameKbName.value.trim();
  if (!newName || newName === knowledgeBase.value) {
    showRenameKb.value = false;
    return;
  }
  try {
    await renameKnowledgeBase(knowledgeBase.value, newName);
    panelMessage.value = `知识库已重命名为 "${newName}"。`;
    showRenameKb.value = false;
    renameKbName.value = "";
    knowledgeBase.value = newName;
    await loadKnowledgeBases();
    await loadKnowledgePanel();
  } catch (error) {
    setError(error, "无法重命名知识库。");
  }
}

// ====== Document operations ======

async function handleUpload() {
  if (!selectedFiles.value.length) {
    panelError.value = "请先选择文件后再上传。";
    return;
  }

  uploadBusy.value = true;
  panelError.value = "";
  panelMessage.value = "";

  try {
    const response = await uploadKnowledgeDocuments(knowledgeBase.value, selectedFiles.value);
    selectedFiles.value = [];
    panelMessage.value = response.message || `已向 ${response.knowledgeBase} 上传 ${response.uploadedCount} 个文档。`;
    selectedFileCount.value = 0;
    await loadKnowledgePanel();
    await loadTasksPanel();
  } catch (error) {
    setError(error, "文档上传失败。");
  } finally {
    uploadBusy.value = false;
  }
}

async function handleDeleteDocument(documentId: string) {
  try {
    const response = await deleteKnowledgeDocument(knowledgeBase.value, documentId);
    panelMessage.value = response.message || "文档删除成功。";
    await loadKnowledgePanel();
    await loadTasksPanel();
  } catch (error) {
    setError(error, "无法删除该文档。");
  }
}

// ====== Tag / Folder editing ======

function startEditTags(doc: KnowledgeDocument) {
  editingTagDocId.value = doc.documentId;
  editingTagInput.value = (doc.tags || []).join(", ");
}

function cancelEditTags() {
  editingTagDocId.value = null;
  editingTagInput.value = "";
}

async function saveTags(doc: KnowledgeDocument) {
  const tags = editingTagInput.value
    .split(",")
    .map((t) => t.trim())
    .filter(Boolean);
  try {
    await updateDocumentMetadata(knowledgeBase.value, doc.documentId, { tags });
    panelMessage.value = "标签已更新。";
    editingTagDocId.value = null;
    editingTagInput.value = "";
    await loadKnowledgePanel();
  } catch (error) {
    setError(error, "无法保存标签。");
  }
}

function startEditFolder(doc: KnowledgeDocument) {
  editingFolderDocId.value = doc.documentId;
  editingFolderInput.value = doc.folder || "";
}

function cancelEditFolder() {
  editingFolderDocId.value = null;
  editingFolderInput.value = "";
}

async function saveFolder(doc: KnowledgeDocument) {
  const folder = editingFolderInput.value.trim();
  try {
    await updateDocumentMetadata(knowledgeBase.value, doc.documentId, { folder: folder || "" });
    panelMessage.value = folder ? `已归入文件夹 "${folder}"。` : "已移除文件夹分类。";
    editingFolderDocId.value = null;
    editingFolderInput.value = "";
    await loadKnowledgePanel();
  } catch (error) {
    setError(error, "无法保存文件夹。");
  }
}

async function handleRebuild() {
  rebuildBusy.value = true;
  panelError.value = "";
  panelMessage.value = "";

  try {
    latestRebuild.value = await rebuildKnowledgeBase(knowledgeBase.value);
    panelMessage.value = "知识库重建任务已启动。";
    startRebuildPolling();
    await loadTasksPanel();
  } catch (error) {
    setError(error, "暂时无法启动重建任务。");
  } finally {
    rebuildBusy.value = false;
  }
}

async function refreshRebuildTask() {
  if (!latestRebuild.value?.taskId) {
    return;
  }

  try {
    latestRebuild.value = await getRebuildTask(latestRebuild.value.taskId);
    if (latestRebuild.value.status === "COMPLETED" || latestRebuild.value.status === "FAILED") {
      stopRebuildPolling();
    }
    await loadTasksPanel();
  } catch (error) {
    stopRebuildPolling();
    setError(error, "无法刷新重建进度。");
  }
}

function stopRebuildPolling() {
  if (rebuildPollTimer !== null) {
    window.clearInterval(rebuildPollTimer);
    rebuildPollTimer = null;
  }
}

function startRebuildPolling() {
  stopRebuildPolling();
  rebuildPollTimer = window.setInterval(() => {
    refreshRebuildTask().catch(() => undefined);
  }, 3000);
}

function resetDataSourceForm() {
  editingDataSourceId.value = null;
  dataSourceForm.value = {
    name: "",
    type: "MYSQL",
    host: "127.0.0.1",
    port: 3306,
    databaseName: "",
    username: "",
    password: "",
    apiBaseUrl: "",
    notes: "",
    enabled: true,
  };
}

async function submitDataSource() {
  try {
    if (editingDataSourceId.value) {
      await updateDataSource(editingDataSourceId.value, dataSourceForm.value);
      panelMessage.value = "数据源更新成功。";
    } else {
      await createDataSource(dataSourceForm.value);
      panelMessage.value = "数据源创建成功。";
    }
    resetDataSourceForm();
    await loadDataSourcePanel();
    await loadTasksPanel();
  } catch (error) {
    setError(error, "无法保存数据源配置。");
  }
}

function startEditDataSource(item: DataSource) {
  editingDataSourceId.value = item.id;
  dataSourceForm.value = {
    name: item.name,
    type: item.type,
    host: item.host,
    port: item.port,
    databaseName: item.databaseName,
    username: item.username,
    password: "",
    apiBaseUrl: item.apiBaseUrl,
    notes: item.notes,
    enabled: item.enabled,
  };
}

async function removeDataSource(id: number) {
  try {
    await deleteDataSource(id);
    panelMessage.value = "数据源已删除。";
    await loadDataSourcePanel();
  } catch (error) {
    setError(error, "无法删除该数据源。");
  }
}

async function runDataSourceTest(id: number) {
  try {
    const result = await testDataSource(id);
    panelMessage.value = result.message;
    await loadDataSourcePanel();
    await loadTasksPanel();
  } catch (error) {
    setError(error, "连接测试失败。");
  }
}

async function submitUser() {
  try {
    await createUser(createUserForm.value);
    createUserForm.value = {
      email: "",
      displayName: "",
      password: "",
      role: "USER",
    };
    panelMessage.value = "用户创建成功。";
    await loadUsersPanel();
  } catch (error) {
    setError(error, "无法创建用户。");
  }
}

async function toggleUserEnabled(user: UserProfileItem) {
  try {
    await updateUser(user.id, { enabled: !user.enabled });
    await loadUsersPanel();
  } catch (error) {
    setError(error, "无法更新账号状态。");
  }
}

async function toggleUserRole(user: UserProfileItem) {
  try {
    await updateUser(user.id, { role: user.role === "ADMIN" ? "USER" : "ADMIN" });
    await loadUsersPanel();
  } catch (error) {
    setError(error, "无法更新用户角色。");
  }
}

async function handleResetPassword(user: UserProfileItem) {
  const nextPassword = window.prompt(`请为 ${user.email} 设置新密码`);
  if (!nextPassword) {
    return;
  }

  try {
    await resetUserPassword(user.id, nextPassword);
    panelMessage.value = `${user.email} 的密码已更新。`;
  } catch (error) {
    setError(error, "无法重置该用户密码。");
  }
}

async function handleSaveSettings() {
  try {
    settings.value = await updateSettings(settingsForm.value);
    knowledgeBase.value = settingsForm.value.defaultKnowledgeBase;
    panelMessage.value = "系统设置保存成功。";
  } catch (error) {
    setError(error, "无法保存系统设置。");
  }
}

async function handleDeleteUser(user: UserProfileItem) {
  const confirmed = window.confirm(`确认删除用户 ${user.email} 吗？此操作不可撤销。`);
  if (!confirmed) {
    return;
  }

  try {
    const response = await deleteUser(user.id);
    panelMessage.value = response.message || `已删除 ${user.email}`;
    await loadUsersPanel();
  } catch (error) {
    setError(error, "无法删除该用户。");
  }
}

function refreshSelectedTask() {
  if (!selectedTask.value) {
    return;
  }

  getTask(selectedTask.value.id)
    .then((task) => {
      selectedTask.value = task;
    })
    .catch((error) => {
      setError(error, "无法刷新该任务详情。");
    });
}

watch(
  () => props.view,
  () => {
    refreshCurrentView();
  },
  { immediate: true }
);

watch(knowledgeBase, () => {
  if (props.view === "knowledge" && settings.value) {
    loadKnowledgePanel().catch((error) => setError(error, "无法刷新当前知识库数据。"));
  }
});

onMounted(async () => {
  await loadTasksPanel().catch(() => undefined);
  if (props.view === "knowledge") {
    await loadKnowledgeBases().catch(() => undefined);
  }
});

onBeforeUnmount(() => {
  stopRebuildPolling();
});
</script>

<template>
  <div class="flex h-full flex-col bg-surface overflow-hidden">
    <header class="flex items-center justify-between border-b border-outline-variant/10 px-8 py-5">
      <div>
        <p class="text-[11px] uppercase tracking-[0.28em] text-on-surface-variant">管理工作台</p>
        <h2 class="mt-2 text-2xl font-medium tracking-tight text-on-surface">在同一界面内完成系统管理与知识维护。</h2>
      </div>
      <button class="inline-flex items-center gap-2 rounded-full border border-outline-variant/20 px-4 py-2 text-xs uppercase tracking-[0.22em] text-on-surface-variant transition hover:bg-surface-container-low hover:text-on-surface" @click="refreshCurrentView">
        <RefreshCw class="h-3.5 w-3.5" />
        刷新
      </button>
    </header>

    <div class="flex-1 px-8 py-8 overflow-hidden flex flex-col">
      <div class="mx-auto max-w-6xl w-full flex-1 min-h-0 flex flex-col space-y-6">
        <div v-if="panelError" class="rounded-3xl bg-red-500/10 px-5 py-4 text-sm text-red-300">{{ panelError }}</div>
        <div v-if="panelMessage" class="rounded-3xl bg-emerald-500/10 px-5 py-4 text-sm text-emerald-300">{{ panelMessage }}</div>

        <div v-if="viewBusy" class="flex items-center gap-3 rounded-3xl bg-surface-container-low px-5 py-4 text-sm text-on-surface-variant">
          <Loader2 class="h-4 w-4 animate-spin" />
          正在加载管理数据...
        </div>

        <template v-else>
          <!-- ==================== DASHBOARD VIEW ==================== -->
          <section v-if="view === 'dashboard'" class="space-y-6 overflow-y-auto flex-1 min-h-0">

            <div v-if="dashboardBusy && !dashboardData" class="flex items-center gap-3 rounded-3xl bg-surface-container-low px-5 py-4 text-sm text-on-surface-variant">
              <Loader2 class="h-4 w-4 animate-spin" />
              正在加载仪表盘数据...
            </div>

            <template v-if="dashboardData">
              <!-- Stats cards row -->
              <div class="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
                  <p class="text-[11px] uppercase tracking-[0.24em] text-on-surface-variant">知识库</p>
                  <p class="mt-3 text-4xl font-medium tracking-tight text-on-surface">{{ dashboardData.kbStats.totalKnowledgeBases }}</p>
                  <p class="mt-2 text-xs text-on-surface-variant">共 {{ dashboardData.kbStats.totalDocuments }} 个文档 / {{ dashboardData.kbStats.totalChunks }} 个分块</p>
                </div>
                <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
                  <p class="text-[11px] uppercase tracking-[0.24em] text-on-surface-variant">文档总量</p>
                  <p class="mt-3 text-4xl font-medium tracking-tight text-on-surface">{{ dashboardData.kbStats.totalDocuments }}</p>
                  <p class="mt-2 text-xs text-on-surface-variant">分布于 {{ dashboardData.kbStats.totalKnowledgeBases }} 个知识库</p>
                </div>
                <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
                  <p class="text-[11px] uppercase tracking-[0.24em] text-on-surface-variant">会话总数</p>
                  <p class="mt-3 text-4xl font-medium tracking-tight text-on-surface">{{ dashboardData.chatStats.totalSessions }}</p>
                  <p class="mt-2 text-xs text-on-surface-variant">共 {{ dashboardData.chatStats.totalMessages }} 条消息</p>
                </div>
                <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
                  <p class="text-[11px] uppercase tracking-[0.24em] text-on-surface-variant">系统内存</p>
                  <p class="mt-3 text-4xl font-medium tracking-tight text-on-surface">{{ dashboardData.systemStats.usedMemoryMb }}<span class="text-xl text-on-surface-variant">MB</span></p>
                  <p class="mt-2 text-xs text-on-surface-variant">/ {{ dashboardData.systemStats.maxMemoryMb }} MB ({{ memoryUsagePercent }}%)</p>
                </div>
              </div>

              <!-- KB Breakdown + Activity Chart -->
              <div class="grid gap-6 lg:grid-cols-[1fr_1fr]">
                <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
                  <p class="text-[11px] uppercase tracking-[0.24em] text-on-surface-variant">知识库概览</p>
                  <div class="mt-4 space-y-3">
                    <div v-if="dashboardData.kbStats.knowledgeBases.length === 0" class="rounded-2xl bg-surface px-4 py-6 text-center text-sm text-on-surface-variant">暂无知识库。</div>
                    <div v-for="kb in dashboardData.kbStats.knowledgeBases" :key="kb.name" class="rounded-2xl bg-surface px-4 py-3.5">
                      <div class="flex items-center justify-between gap-3">
                        <p class="text-sm font-medium text-on-surface truncate">{{ kb.name }}</p>
                        <div class="flex gap-3 text-xs text-on-surface-variant shrink-0">
                          <span>{{ kb.documentCount }} 文档</span>
                          <span>{{ kb.chunkCount }} 分块</span>
                        </div>
                      </div>
                      <div class="mt-2 h-1.5 rounded-full bg-surface-container-high overflow-hidden">
                        <div class="h-full rounded-full bg-on-surface/20 transition-all" :style="{ width: dashboardData.kbStats.totalDocuments > 0 ? (kb.documentCount / dashboardData.kbStats.totalDocuments * 100) + '%' : '0%' }" />
                      </div>
                    </div>
                  </div>
                </div>

                <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
                  <p class="text-[11px] uppercase tracking-[0.24em] text-on-surface-variant">近期活跃度</p>
                  <p class="mt-1 text-xs text-on-surface-variant">最近 7 天会话与消息趋势</p>
                  <div class="mt-5 flex items-end justify-between gap-3" style="height: 140px;">
                    <div v-for="day in dashboardData.chatStats.dailyActivity" :key="day.date" class="flex flex-col items-center gap-2 flex-1 min-w-0">
                      <div class="flex flex-col items-center gap-0.5 w-full" style="flex: 1; justify-content: flex-end;">
                        <div class="w-full rounded-sm bg-on-surface/15 transition-all" :style="{ height: getBarHeight(day.messageCount, dashboardData!.chatStats.dailyActivity) + 'px' }" />
                        <div class="w-full rounded-sm bg-on-surface/35 transition-all" :style="{ height: getBarHeight(day.sessionCount, dashboardData!.chatStats.dailyActivity) + 'px' }" />
                      </div>
                      <p class="text-[10px] text-on-surface-variant mt-1">{{ day.date }}</p>
                    </div>
                  </div>
                  <div class="mt-4 flex items-center justify-center gap-5 text-[10px] text-on-surface-variant">
                    <span class="inline-flex items-center gap-1.5"><span class="w-2.5 h-2.5 rounded-sm bg-on-surface/35" />会话数</span>
                    <span class="inline-flex items-center gap-1.5"><span class="w-2.5 h-2.5 rounded-sm bg-on-surface/15" />消息数</span>
                  </div>
                </div>
              </div>

              <div class="grid gap-6 lg:grid-cols-[1.3fr_0.7fr]">
                <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
                  <p class="text-[11px] uppercase tracking-[0.24em] text-on-surface-variant">热门查询</p>
                  <p class="mt-1 text-xs text-on-surface-variant">用户最常提出的问题（按提问次数排序）</p>
                  <div class="mt-5 flex flex-wrap gap-2">
                    <div v-if="dashboardData.popularQueries.length === 0" class="rounded-2xl bg-surface px-4 py-6 text-sm text-on-surface-variant">暂无用户查询记录。</div>
                    <span v-for="q in dashboardData.popularQueries" :key="q.query" class="inline-flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-xs transition-colors" :class="q.count >= (dashboardData.popularQueries[0]?.count || 1) * 0.6 ? 'bg-on-surface/15 text-on-surface' : q.count >= (dashboardData.popularQueries[0]?.count || 1) * 0.3 ? 'bg-on-surface/8 text-on-surface' : 'bg-surface text-on-surface-variant'">
                      <span class="truncate max-w-[14rem]">{{ q.query }}</span>
                      <span class="text-[10px] opacity-60 shrink-0">x{{ q.count }}</span>
                    </span>
                  </div>
                </div>

                <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
                  <p class="text-[11px] uppercase tracking-[0.24em] text-on-surface-variant">系统信息</p>
                  <div class="mt-5 space-y-4">
                    <div class="rounded-2xl bg-surface px-4 py-4">
                      <div class="flex items-center justify-between gap-3">
                        <p class="text-xs text-on-surface-variant">堆内存使用</p>
                        <p class="text-sm text-on-surface">{{ dashboardData.systemStats.usedMemoryMb }} / {{ dashboardData.systemStats.maxMemoryMb }} MB</p>
                      </div>
                      <div class="mt-2 h-1.5 rounded-full bg-surface-container-high overflow-hidden">
                        <div class="h-full rounded-full transition-all" :class="memoryUsagePercent > 80 ? 'bg-red-500/50' : memoryUsagePercent > 50 ? 'bg-yellow-500/40' : 'bg-emerald-500/30'" :style="{ width: memoryUsagePercent + '%' }" />
                      </div>
                    </div>
                    <div class="rounded-2xl bg-surface px-4 py-4">
                      <p class="text-xs text-on-surface-variant">处理器核心</p>
                      <p class="mt-1 text-sm text-on-surface">{{ dashboardData.systemStats.availableProcessors }} 核</p>
                    </div>
                    <div class="rounded-2xl bg-surface px-4 py-4">
                      <p class="text-xs text-on-surface-variant">操作系统</p>
                      <p class="mt-1 text-sm text-on-surface truncate">{{ dashboardData.systemStats.os }}</p>
                    </div>
                    <div class="rounded-2xl bg-surface px-4 py-4">
                      <p class="text-xs text-on-surface-variant">Java 版本</p>
                      <p class="mt-1 text-sm text-on-surface">{{ dashboardData.systemStats.javaVersion }}</p>
                    </div>
                  </div>
                </div>
              </div>
              

  <!-- Document preview modal -->
  <Teleport to="body">
    <div v-if="showPreview && previewDoc" class="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="closePreview">
      <div class="w-full max-w-3xl max-h-[80vh] mx-4 rounded-[28px] border border-outline-variant/10 bg-surface-container-low flex flex-col overflow-hidden">
        <div class="flex items-center justify-between px-6 py-4 border-b border-outline-variant/10">
          <div class="min-w-0 flex-1">
            <p class="text-sm font-medium text-on-surface truncate">{{ previewDoc.fileName }}</p>
            <p class="text-[10px] text-on-surface-variant mt-0.5">{{ (previewDoc.fileSize / 1024).toFixed(1) }} KB</p>
          </div>
          <button class="p-2 hover:bg-surface-container-high rounded-full transition-colors text-on-surface-variant hover:text-on-surface" @click="closePreview"><X class="h-4 w-4" /></button>
        </div>
        <div class="flex-1 overflow-y-auto p-6">
          <pre class="text-sm leading-7 text-on-surface whitespace-pre-wrap font-sans">{{ previewDoc.content }}</pre>
        </div>
      </div>
    </div>
  </Teleport>

</template>

            <div v-if="!dashboardBusy && !dashboardData" class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-10 text-center">
              <p class="text-sm text-on-surface-variant">无法加载仪表盘数据，请确保后端服务正常运行。</p>
            </div>
          </section>

          <!-- ==================== KNOWLEDGE VIEW ==================== -->
          <section v-if="view === 'knowledge'" class="space-y-6 overflow-y-auto flex-1 min-h-0">

            <!-- Knowledge base selector bar -->
            <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
              <div class="flex items-start justify-between gap-4 flex-wrap">
                <div class="min-w-0 flex-1">
                  <p class="text-[11px] uppercase tracking-[0.24em] text-on-surface-variant">知识库管理</p>
                  <div class="mt-2 flex items-center gap-3 flex-wrap">
                    <div class="relative">
                      <button @click="kbSelectorOpen = !kbSelectorOpen" class="inline-flex items-center gap-2 rounded-2xl bg-surface px-4 py-2.5 text-sm text-on-surface outline-none hover:opacity-90 min-w-[11rem] justify-between">
                        <span>{{ knowledgeBase }}</span>
                        <svg class="w-3.5 h-3.5 text-on-surface-variant" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
                      </button>
                      <Transition enter-active-class="transition duration-200 ease-out" enter-from-class="translate-y-2 opacity-0 scale-95" enter-to-class="translate-y-0 opacity-100 scale-100" leave-active-class="transition duration-150 ease-in" leave-from-class="translate-y-0 opacity-100 scale-100" leave-to-class="translate-y-2 opacity-0 scale-95">
                        <div v-if="kbSelectorOpen" class="absolute left-0 top-full mt-1 w-full bg-surface-container-highest/95 backdrop-blur-3xl rounded-2xl p-1.5 shadow-2xl border border-outline-variant/20 z-50 min-w-[12rem]">
                          <div class="flex flex-col gap-0.5 max-h-48 overflow-y-auto scrollbar-hide">
                            <div v-if="knowledgeBases.length === 0" class="px-3 py-2 text-xs text-on-surface-variant">暂无知识库</div>
                            <button v-for="kb in knowledgeBases" :key="kb.name" class="flex items-center justify-between px-3 py-2 rounded-xl transition-colors text-sm w-full text-left" :class="kb.name === knowledgeBase ? 'bg-on-surface/10 text-on-surface' : 'hover:bg-on-surface/5 text-on-surface'" @click="selectKnowledgeBase(kb.name)">
                              <span>{{ kb.name }}</span>
                              <span class="text-[10px] text-on-surface-variant">{{ kb.documentCount }} 文档</span>
                            </button>
                          </div>
                        </div>
                      </Transition>
                    </div>
                    <button class="inline-flex items-center gap-1.5 rounded-full border border-outline-variant/20 px-3.5 py-2 text-xs text-on-surface-variant transition hover:bg-surface hover:text-on-surface" @click="showCreateKb = true"><Plus class="h-3.5 w-3.5" />新建</button>
                    <button class="inline-flex items-center gap-1.5 rounded-full border border-outline-variant/20 px-3.5 py-2 text-xs text-on-surface-variant transition hover:bg-surface hover:text-on-surface" @click="renameKbName = knowledgeBase; showRenameKb = true"><Edit3 class="h-3.5 w-3.5" />重命名</button>
                    <button class="inline-flex items-center gap-1.5 rounded-full border border-red-500/20 px-3.5 py-2 text-xs text-red-300 transition hover:bg-red-500/10" @click="showDeleteKbConfirm = true"><Trash2 class="h-3.5 w-3.5" />删除</button>
                  </div>
                </div>
                <div class="shrink-0 rounded-full bg-surface px-3 py-1 text-xs text-on-surface-variant">{{ ragStatus?.documentCount || 0 }} 个文档</div>
              </div>

              <div v-if="showCreateKb" class="mt-4 flex items-center gap-3">
                <input v-model="newKbName" placeholder="输入知识库名称（小写字母、数字、短横线）" class="flex-1 rounded-2xl bg-surface px-4 py-3 text-sm text-on-surface outline-none placeholder:text-on-surface-variant/50" @keydown.enter="handleCreateKb" />
                <button class="inline-flex items-center gap-1.5 rounded-full bg-on-surface px-4 py-2.5 text-xs text-surface transition hover:opacity-90" @click="handleCreateKb">确认创建</button>
                <button class="rounded-full border border-outline-variant/20 px-4 py-2.5 text-xs text-on-surface-variant transition hover:bg-surface" @click="showCreateKb = false; newKbName = ''">取消</button>
              </div>

              <div v-if="showDeleteKbConfirm" class="mt-4 rounded-2xl bg-surface px-5 py-4">
                <p class="text-sm text-on-surface">确认删除知识库 <strong>{{ knowledgeBase }}</strong> 吗？所有文档将被永久删除，此操作不可撤销。</p>
                <div class="mt-3 flex gap-3">
                  <button class="inline-flex items-center gap-1.5 rounded-full bg-red-500 px-4 py-2 text-xs text-white transition hover:opacity-90" @click="handleDeleteKb">确认删除</button>
                  <button class="rounded-full border border-outline-variant/20 px-4 py-2 text-xs text-on-surface-variant transition hover:bg-surface" @click="showDeleteKbConfirm = false">取消</button>
                </div>
              </div>

              <div v-if="showRenameKb" class="mt-4 flex items-center gap-3">
                <input v-model="renameKbName" placeholder="输入新名称" class="flex-1 rounded-2xl bg-surface px-4 py-3 text-sm text-on-surface outline-none placeholder:text-on-surface-variant/50" @keydown.enter="handleRenameKb" />
                <button class="inline-flex items-center gap-1.5 rounded-full bg-on-surface px-4 py-2.5 text-xs text-surface transition hover:opacity-90" @click="handleRenameKb">确认重命名</button>
                <button class="rounded-full border border-outline-variant/20 px-4 py-2.5 text-xs text-on-surface-variant transition hover:bg-surface" @click="showRenameKb = false">取消</button>
              </div>
            </div>

            <!-- Upload + RAG status -->
            <div class="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
              <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
                <div class="flex items-start justify-between gap-4">
                  <div>
                    <p class="text-[11px] uppercase tracking-[0.24em] text-on-surface-variant">文档导入</p>
                    <h3 class="mt-2 text-xl font-medium text-on-surface">上传文件到 {{ knowledgeBase }}</h3>
                  </div>
                </div>
                <div class="mt-5 grid gap-4 md:grid-cols-[0.8fr_1.2fr]">
                  <label class="space-y-2">
                    <span class="text-xs uppercase tracking-[0.24em] text-on-surface-variant">知识库</span>
                    <div class="w-full rounded-2xl bg-surface px-4 py-3 text-sm text-on-surface outline-none">{{ knowledgeBase }}</div>
                  </label>
                  <label class="space-y-2">
                    <span class="text-xs uppercase tracking-[0.24em] text-on-surface-variant">选择文件</span>
                    <input type="file" multiple class="w-full rounded-2xl bg-surface px-4 py-3 text-sm text-on-surface-variant" @change="updateSelectedFiles($event)" />
                    <span class="text-xs uppercase tracking-[0.24em] text-on-surface-variant mt-2">或选择文件夹</span>
                  <input type="file" webkitdirectory class="w-full rounded-2xl bg-surface px-4 py-3 text-sm text-on-surface-variant" @change="updateSelectedFiles($event)" />
                  </label>
                </div>
                <div class="mt-5 flex flex-wrap gap-3">
                  <button class="inline-flex items-center gap-2 rounded-full bg-on-surface px-5 py-2.5 text-sm text-surface transition hover:opacity-90 disabled:opacity-60" :disabled="uploadBusy" @click="handleUpload"><Upload class="h-4 w-4" />{{ uploadBusy ? '上传中...' : '上传文档' }} {{ selectedFileCount > 0 ? '(' + selectedFileCount + '个文件)' : '' }}</button>
                  <button class="inline-flex items-center gap-2 rounded-full border border-outline-variant/20 px-5 py-2.5 text-sm text-on-surface transition hover:bg-surface" :disabled="rebuildBusy" @click="handleRebuild"><RotateCcw class="h-4 w-4" />{{ rebuildBusy ? '启动中...' : '重建知识库' }}</button>
                  <button v-if="latestRebuild?.taskId" class="inline-flex items-center gap-2 rounded-full border border-outline-variant/20 px-5 py-2.5 text-sm text-on-surface-variant transition hover:bg-surface hover:text-on-surface" @click="refreshRebuildTask"><RefreshCw class="h-4 w-4" />刷新重建进度</button>
                  <button class="inline-flex items-center gap-2 rounded-full border border-outline-variant/20 px-5 py-2.5 text-sm text-on-surface-variant transition hover:bg-surface hover:text-on-surface" :disabled="seedBusy" @click="handleSeed(knowledgeBase)"><DatabaseZap class="h-4 w-4" />{{ seedBusy ? '导入中...' : '导入示例数据' }}</button>
                  <button class="inline-flex items-center gap-2 rounded-full border border-outline-variant/20 px-5 py-2.5 text-sm text-on-surface-variant transition hover:bg-surface hover:text-on-surface" :disabled="seedBusyAll" @click="handleSeedAll"><DatabaseZap class="h-4 w-4" />{{ seedBusyAll ? '生成中...' : '批量生成6个领域知识库' }}</button>
                </div>
              </div>

              <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
                <p class="text-[11px] uppercase tracking-[0.24em] text-on-surface-variant">RAG 状态</p>
                <div class="mt-4 grid gap-3 sm:grid-cols-2">
                  <div class="rounded-2xl bg-surface px-4 py-3"><p class="text-xs text-on-surface-variant">服务提供方</p><p class="mt-2 text-sm leading-6 text-on-surface break-words">{{ ragStatus?.embeddingProvider || '--' }}</p></div>
                  <div class="rounded-2xl bg-surface px-4 py-3"><p class="text-xs text-on-surface-variant">向量存储</p><p class="mt-2 text-sm leading-6 text-on-surface break-words">{{ ragStatus?.vectorStore || '--' }}</p></div>
                  <div class="rounded-2xl bg-surface px-4 py-3 sm:col-span-2"><p class="text-xs text-on-surface-variant">检索状态</p><p class="mt-2 text-sm leading-6 text-on-surface break-all">{{ ragStatus?.retrievalStatus || '--' }}</p></div>
                  <div class="rounded-2xl bg-surface px-4 py-3 sm:col-span-2"><p class="text-xs text-on-surface-variant">分块参数</p><p class="mt-2 text-sm leading-6 text-on-surface">{{ ragStatus?.chunkSize || '--' }} / {{ ragStatus?.chunkOverlap || '--' }}</p></div>
                </div>
                <div v-if="latestRebuild" class="mt-6 rounded-2xl bg-surface px-4 py-4">
                  <div class="flex items-center justify-between gap-4">
                    <div><p class="text-sm font-medium text-on-surface">最近一次重建</p><p class="mt-1 text-xs text-on-surface-variant">{{ latestRebuild.status }} · {{ latestRebuild.message || '任务执行中' }}</p></div>
                    <span class="text-sm text-on-surface">{{ latestRebuild.progress }}%</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Search and document table -->
            <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
              <div class="flex items-center justify-between gap-4">
                <div><p class="text-[11px] uppercase tracking-[0.24em] text-on-surface-variant">文档列表</p><h3 class="mt-2 text-xl font-medium text-on-surface">已索引内容</h3></div>
                <div class="inline-flex items-center gap-2 rounded-full bg-surface px-3 py-1 text-xs text-on-surface-variant"><Search class="h-3.5 w-3.5" />{{ knowledgeBase }}</div>
              </div>
              <div class="mt-4 flex flex-wrap items-center gap-3">
                <div class="relative flex-1 min-w-[12rem] max-w-sm">
                  <Search class="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-on-surface-variant/60" />
                  <input v-model="searchQuery" placeholder="搜索文件名..." class="w-full rounded-2xl bg-surface pl-10 pr-4 py-2.5 text-sm text-on-surface outline-none placeholder:text-on-surface-variant/50" @input="applyFilters" />
                  <button v-if="searchQuery" @click="searchQuery = ''; applyFilters()" class="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant/60 hover:text-on-surface"><X class="h-3.5 w-3.5" /></button>
                </div>
                <select v-model="selectedTag" class="rounded-2xl bg-surface px-4 py-2.5 text-sm text-on-surface outline-none min-w-[8rem]" @change="applyFilters">
                  <option value="__all__">全部标签</option>
                  <option v-for="tag in availableTags" :key="tag" :value="tag">{{ tag }}</option>
                </select>
                <select v-model="selectedFolder" class="rounded-2xl bg-surface px-4 py-2.5 text-sm text-on-surface outline-none min-w-[8rem]" @change="applyFilters">
                  <option value="__all__">全部文件夹</option>
                  <option value="__none__">未分类</option>
                  <option v-for="folder in availableFolders" :key="folder" :value="folder">{{ folder }}</option>
                </select>
              </div>
              <div class="mt-5 overflow-hidden rounded-3xl border border-outline-variant/10">
                <table class="min-w-full text-left text-sm">
                  <thead class="bg-surface text-on-surface-variant">
                    <tr><th class="px-4 py-3 font-medium whitespace-nowrap">文件名</th><th class="px-4 py-3 font-medium whitespace-nowrap">分块</th><th class="px-4 py-3 font-medium whitespace-nowrap">标签</th><th class="px-4 py-3 font-medium whitespace-nowrap">文件夹</th><th class="px-4 py-3 font-medium whitespace-nowrap">状态</th><th class="px-4 py-3 font-medium text-right whitespace-nowrap">操作</th></tr>
                  </thead>
                  <tbody>
                    <tr v-if="knowledgeDocuments.length === 0"><td colspan="6" class="px-4 py-8 text-center text-on-surface-variant">{{ searchQuery || selectedTag !== '__all__' || selectedFolder !== '__all__' ? '没有匹配的文档。' : '暂无文档。' }}</td></tr>
                    <tr v-for="document in knowledgeDocuments" :key="document.documentId" class="border-t border-outline-variant/10">
                      <td class="px-4 py-4"><p class="font-medium text-on-surface truncate max-w-[18rem] cursor-pointer hover:text-on-surface-variant transition-colors" :title="document.fileName" @click="handlePreview(document.documentId)">{{ document.fileName }}</p><p class="mt-1 text-xs text-on-surface-variant truncate max-w-[18rem]">{{ document.storagePath }}</p></td>
                      <td class="px-4 py-4 text-on-surface">{{ document.chunkCount }}</td>
                      <td class="px-4 py-4">
                        <div v-if="editingTagDocId === document.documentId" class="flex flex-col gap-1.5">
                          <input v-model="editingTagInput" placeholder="标签1, 标签2, ..." class="w-32 rounded-xl bg-surface px-3 py-1.5 text-xs text-on-surface outline-none" @keydown.enter="saveTags(document)" />
                          <div class="flex gap-1.5"><button class="text-[10px] text-emerald-400 hover:text-emerald-300" @click="saveTags(document)">保存</button><button class="text-[10px] text-on-surface-variant hover:text-on-surface" @click="cancelEditTags">取消</button></div>
                        </div>
                        <div v-else class="flex flex-wrap gap-1">
                          <span v-for="tag in (document.tags || [])" :key="tag" class="inline-flex items-center rounded-full bg-surface px-2 py-0.5 text-[10px] text-on-surface-variant">{{ tag }}</span>
                          <button class="inline-flex items-center rounded-full border border-dashed border-outline-variant/20 px-2 py-0.5 text-[10px] text-on-surface-variant/60 hover:text-on-surface hover:border-outline-variant/40 transition-colors" @click="startEditTags(document)"><Tag class="h-3 w-3 mr-0.5" />{{ (document.tags || []).length ? '编辑' : '添加' }}</button>
                        </div>
                      </td>
                      <td class="px-4 py-4">
                        <div v-if="editingFolderDocId === document.documentId" class="flex flex-col gap-1.5">
                          <input v-model="editingFolderInput" placeholder="例如: 合同/2024" class="w-32 rounded-xl bg-surface px-3 py-1.5 text-xs text-on-surface outline-none" @keydown.enter="saveFolder(document)" />
                          <div class="flex gap-1.5"><button class="text-[10px] text-emerald-400 hover:text-emerald-300" @click="saveFolder(document)">保存</button><button class="text-[10px] text-on-surface-variant hover:text-on-surface" @click="cancelEditFolder">取消</button></div>
                        </div>
                        <div v-else>
                          <button class="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] transition-colors" :class="document.folder ? 'bg-surface text-on-surface-variant hover:bg-surface-container-high' : 'border border-dashed border-outline-variant/20 text-on-surface-variant/60 hover:text-on-surface hover:border-outline-variant/40'" @click="startEditFolder(document)"><FolderPlus class="h-3 w-3" />{{ document.folder || '归入文件夹' }}</button>
                        </div>
                      </td>
                      <td class="px-4 py-4 text-on-surface-variant">{{ document.indexingStatus }}</td>
                      <td class="px-4 py-4 text-right"><button class="rounded-full px-3 py-1.5 text-xs text-red-300 transition hover:bg-red-500/10" @click="handleDeleteDocument(document.documentId)">删除</button></td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </section>

          <!-- ==================== DATASOURCE VIEW ==================== -->
          <section v-else-if="view === 'datasource'" class="grid gap-6 xl:grid-cols-[0.9fr_1.1fr] overflow-y-auto flex-1 min-h-0">
            <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
              <div class="flex items-center gap-3"><Database class="h-5 w-5 text-on-surface-variant" /><h3 class="text-xl font-medium text-on-surface">{{ editingDataSourceId ? '编辑数据源' : '新建数据源' }}</h3></div>
              <p class="mt-3 text-sm leading-6 text-on-surface-variant">在这里维护业务系统、数据库或外部接口连接，后续可作为知识增强或结构化查询的数据来源。</p>
              <div class="mt-5 grid gap-4">
                <input v-model="dataSourceForm.name" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none" placeholder="数据源名称" />
                <select v-model="dataSourceForm.type" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none"><option v-for="type in dataSourceTypeOptions" :key="type" :value="type">{{ type }}</option></select>
                <div class="grid gap-4 md:grid-cols-2"><input v-model="dataSourceForm.host" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none" placeholder="主机地址" /><input v-model.number="dataSourceForm.port" type="number" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none" placeholder="端口" /></div>
                <input v-model="dataSourceForm.databaseName" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none" placeholder="数据库名称" />
                <input v-model="dataSourceForm.apiBaseUrl" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none" placeholder="接口基础地址" />
                <div class="grid gap-4 md:grid-cols-2"><input v-model="dataSourceForm.username" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none" placeholder="用户名" /><input v-model="dataSourceForm.password" type="password" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none" placeholder="密码" /></div>
                <textarea v-model="dataSourceForm.notes" rows="4" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none" placeholder="备注说明"></textarea>
                <label class="inline-flex items-center gap-3 text-sm text-on-surface-variant"><input v-model="dataSourceForm.enabled" type="checkbox" class="h-4 w-4 rounded border-outline-variant/30 bg-surface" />启用该数据源</label>
                <div class="flex gap-3">
                  <button class="inline-flex items-center gap-2 rounded-full bg-on-surface px-5 py-2.5 text-sm text-surface transition hover:opacity-90" @click="submitDataSource"><Save class="h-4 w-4" />{{ editingDataSourceId ? '保存修改' : '创建数据源' }}</button>
                  <button class="rounded-full border border-outline-variant/20 px-5 py-2.5 text-sm text-on-surface-variant transition hover:bg-surface" @click="resetDataSourceForm">清空表单</button>
                </div>
              </div>
            </div>
            <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
              <div class="flex items-center gap-3"><Wrench class="h-5 w-5 text-on-surface-variant" /><h3 class="text-xl font-medium text-on-surface">已配置数据源</h3></div>
              <div class="mt-5 space-y-4">
                <div v-if="dataSources.length === 0" class="rounded-2xl bg-surface px-4 py-6 text-center text-sm text-on-surface-variant">当前还没有配置任何数据源。</div>
                <div v-for="item in dataSources" :key="item.id" class="rounded-3xl bg-surface px-5 py-5">
                  <div class="flex items-start justify-between gap-4">
                    <div><p class="text-sm font-medium text-on-surface">{{ item.name }}</p><p class="mt-1 text-xs text-on-surface-variant">{{ item.type }} · {{ item.host || item.apiBaseUrl || '--' }}</p></div>
                    <span class="rounded-full px-2.5 py-1 text-[11px]" :class="item.enabled ? 'bg-emerald-500/10 text-emerald-300' : 'bg-white/5 text-on-surface-variant'">{{ item.enabled ? '已启用' : '已停用' }}</span>
                  </div>
                  <div class="mt-4 flex flex-wrap gap-2 text-xs">
                    <button class="rounded-full border border-outline-variant/20 px-3 py-1.5 text-on-surface-variant transition hover:bg-surface-container-low hover:text-on-surface" @click="startEditDataSource(item)">编辑</button>
                    <button class="rounded-full border border-outline-variant/20 px-3 py-1.5 text-on-surface-variant transition hover:bg-surface-container-low hover:text-on-surface" @click="runDataSourceTest(item.id)">测试连接</button>
                    <button class="rounded-full border border-red-500/20 px-3 py-1.5 text-red-300 transition hover:bg-red-500/10" @click="removeDataSource(item.id)">删除</button>
                  </div>
                  <p v-if="item.lastTestMessage" class="mt-3 text-xs text-on-surface-variant">最近测试：{{ item.lastTestMessage }}</p>
                </div>
              </div>
            </div>
          </section>

          <!-- ==================== MONITOR VIEW ==================== -->
          <section v-else-if="view === 'monitor'" class="space-y-6 overflow-y-auto flex-1 min-h-0">
            <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
              <div class="flex items-center gap-3"><Workflow class="h-5 w-5 text-on-surface-variant" /><h3 class="text-xl font-medium text-on-surface">最近任务</h3></div>
              <div class="mt-5 space-y-3">
                <div v-if="tasks.length === 0" class="rounded-2xl bg-surface px-4 py-6 text-center text-sm text-on-surface-variant">当前还没有管理任务记录。</div>
                <button v-for="task in tasks.slice(0, 10)" :key="task.id" class="flex w-full items-center justify-between rounded-3xl bg-surface px-5 py-4 text-left transition hover:bg-surface-container-high" @click="selectedTask = task">
                  <div><p class="text-sm font-medium text-on-surface">{{ task.taskType }} · {{ task.targetName }}</p><p class="mt-1 text-xs text-on-surface-variant">{{ task.status }} · {{ task.message || '暂无任务说明' }}</p></div>
                  <span class="text-sm text-on-surface">{{ task.progress }}%</span>
                </button>
              </div>
            </div>
            <div v-if="selectedTask" class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
              <div class="flex items-center justify-between gap-4"><div><p class="text-[11px] uppercase tracking-[0.24em] text-on-surface-variant">任务详情</p><h3 class="mt-2 text-xl font-medium text-on-surface">{{ selectedTask.taskType }}</h3></div><button class="rounded-full border border-outline-variant/20 px-4 py-2 text-xs text-on-surface-variant transition hover:bg-surface" @click="refreshSelectedTask">刷新任务</button></div>
              <dl class="mt-5 grid gap-4 md:grid-cols-2">
                <div><dt class="text-xs text-on-surface-variant">目标对象</dt><dd class="mt-1 text-sm text-on-surface">{{ selectedTask.targetType }} / {{ selectedTask.targetName }}</dd></div>
                <div><dt class="text-xs text-on-surface-variant">任务状态</dt><dd class="mt-1 text-sm text-on-surface">{{ selectedTask.status }}</dd></div>
                <div><dt class="text-xs text-on-surface-variant">执行进度</dt><dd class="mt-1 text-sm text-on-surface">{{ selectedTask.progress }}%</dd></div>
                <div><dt class="text-xs text-on-surface-variant">最近更新</dt><dd class="mt-1 text-sm text-on-surface">{{ selectedTask.updatedAt || '--' }}</dd></div>
              </dl>
              <p class="mt-5 rounded-2xl bg-surface px-4 py-4 text-sm text-on-surface-variant">{{ selectedTask.message || '暂无任务说明。' }}</p>
            </div>
          </section>

          <!-- ==================== USERS VIEW ==================== -->
          <section v-else-if="view === 'users'" class="grid gap-6 xl:grid-cols-[0.85fr_1.15fr] overflow-hidden flex-1 min-h-0">
            <div class="self-start rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
              <div class="flex items-center gap-3"><Users class="h-5 w-5 text-on-surface-variant" /><h3 class="text-xl font-medium text-on-surface">创建用户</h3></div>
              <div class="mt-5 grid gap-4">
                <input v-model="createUserForm.displayName" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none" placeholder="显示名称" />
                <input v-model="createUserForm.email" type="email" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none" placeholder="邮箱" />
                <input v-model="createUserForm.password" type="password" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none" placeholder="临时密码" />
                <select v-model="createUserForm.role" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none"><option value="USER">普通用户</option><option value="ADMIN">管理员</option></select>
                <button class="inline-flex items-center justify-center gap-2 rounded-full bg-on-surface px-5 py-2.5 text-sm text-surface transition hover:opacity-90" @click="submitUser"><Shield class="h-4 w-4" />创建用户</button>
              </div>
            </div>
            <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6 flex flex-col overflow-hidden">
              <h3 class="text-xl font-medium text-on-surface shrink-0">用户列表</h3>
              <div class="mt-5 flex-1 overflow-y-auto space-y-3">
                <div v-for="user in users" :key="user.id" class="rounded-3xl bg-surface px-5 py-5">
                  <div class="flex items-start justify-between gap-4">
                    <div><p class="text-sm font-medium text-on-surface">{{ user.displayName }}</p><p class="mt-1 text-xs text-on-surface-variant">{{ user.email }}</p></div>
                    <span class="rounded-full px-2.5 py-1 text-[11px]" :class="user.enabled ? 'bg-emerald-500/10 text-emerald-300' : 'bg-white/5 text-on-surface-variant'">{{ user.enabled ? '已启用' : '已禁用' }}</span>
                  </div>
                  <div class="mt-4 flex flex-wrap gap-2 text-xs">
                    <button class="rounded-full border border-outline-variant/20 px-3 py-1.5 text-on-surface-variant transition hover:bg-surface-container-low hover:text-on-surface" @click="toggleUserRole(user)">切换为{{ user.role === 'ADMIN' ? '普通用户' : '管理员' }}</button>
                    <button class="rounded-full border border-outline-variant/20 px-3 py-1.5 text-on-surface-variant transition hover:bg-surface-container-low hover:text-on-surface" @click="toggleUserEnabled(user)">{{ user.enabled ? '禁用' : '启用' }}</button>
                    <button class="rounded-full border border-outline-variant/20 px-3 py-1.5 text-on-surface-variant transition hover:bg-surface-container-low hover:text-on-surface" @click="handleResetPassword(user)">重置密码</button>
                    <button class="rounded-full border border-red-500/20 px-3 py-1.5 text-red-300 transition hover:bg-red-500/10" @click="handleDeleteUser(user)">删除</button>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <!-- ==================== EVALUATE VIEW ==================== -->
          <section v-else-if="view === 'evaluate'" class="space-y-6 overflow-y-auto flex-1 min-h-0">
            <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
              <div class="flex items-center gap-3"><ClipboardCheck class="h-5 w-5 text-on-surface-variant" /><h3 class="text-xl font-medium text-on-surface">RAG 检索评估</h3></div>
              <p class="mt-3 text-sm leading-6 text-on-surface-variant">上传 CSV 测试集（格式：question,expected_answer），系统将自动评估知识库的检索质量和回答匹配度。</p>
              <div class="mt-5 flex flex-wrap items-end gap-4">
                <label class="space-y-2">
                  <span class="text-xs uppercase tracking-[0.24em] text-on-surface-variant">知识库</span>
                  <select v-model="evalKnowledgeBase" class="rounded-2xl bg-surface px-4 py-2.5 text-sm text-on-surface outline-none min-w-[10rem]">
                    <option v-for="kb in knowledgeBases" :key="kb.name" :value="kb.name">{{ kb.name }}</option>
                  </select>
                </label>
                <label class="space-y-2 flex-1 min-w-[12rem]">
                  <span class="text-xs uppercase tracking-[0.24em] text-on-surface-variant">测试集文件（CSV）</span>
                  <input type="file" accept=".csv" class="w-full rounded-2xl bg-surface px-4 py-3 text-sm text-on-surface-variant" @change="evalFile = ($event.target as HTMLInputElement).files?.[0] || null" />
                </label>
                <button class="inline-flex items-center gap-2 rounded-full bg-on-surface px-5 py-2.5 text-sm text-surface transition hover:opacity-90 disabled:opacity-60" :disabled="evalBusy" @click="handleEvaluate"><ClipboardCheck class="h-4 w-4" />{{ evalBusy ? '评估中...' : '开始评估' }}</button>
              </div>
            </div>

            <div v-if="evalResult" class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
              <div class="flex items-center gap-3"><ClipboardCheck class="h-5 w-5 text-on-surface-variant" /><h3 class="text-xl font-medium text-on-surface">评估结果</h3></div>
              <div class="mt-5 grid gap-4 sm:grid-cols-3">
                <div class="rounded-2xl bg-surface px-4 py-4">
                  <p class="text-xs text-on-surface-variant">状态</p>
                  <p class="mt-2 text-lg font-medium text-on-surface">{{ evalResult.status }}</p>
                </div>
                <div class="rounded-2xl bg-surface px-4 py-4">
                  <p class="text-xs text-on-surface-variant">测试题目</p>
                  <p class="mt-2 text-lg font-medium text-on-surface">{{ evalResult.totalQuestions }}</p>
                </div>
                <div class="rounded-2xl bg-surface px-4 py-4">
                  <p class="text-xs text-on-surface-variant">检索命中</p>
                  <p class="mt-2 text-lg font-medium text-on-surface">{{ evalResult.matchedCount }} / {{ evalResult.totalQuestions }}</p>
                </div>
              </div>
              <div v-if="evalResult.status === 'COMPLETED' && evalResult.items" class="mt-5 overflow-hidden rounded-3xl border border-outline-variant/10">
                <table class="min-w-full text-left text-sm">
                  <thead class="bg-surface text-on-surface-variant">
                    <tr><th class="px-4 py-3 font-medium">问题</th><th class="px-4 py-3 font-medium">命中</th><th class="px-4 py-3 font-medium">来源文档</th></tr>
                  </thead>
                  <tbody>
                    <tr v-for="(item, i) in evalResult.items" :key="i" class="border-t border-outline-variant/10">
                      <td class="px-4 py-3 text-on-surface max-w-[20rem] truncate">{{ item.question }}</td>
                      <td class="px-4 py-3"><span class="rounded-full px-2 py-0.5 text-[11px]" :class="item.retrieved ? 'bg-emerald-500/10 text-emerald-300' : 'bg-red-500/10 text-red-300'">{{ item.retrieved ? '是' : '否' }}</span></td>
                      <td class="px-4 py-3 text-on-surface-variant text-xs">{{ item.retrievedDocs?.join(', ') || '--' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </section>

          <!-- ==================== SETTINGS VIEW ==================== -->
          <section v-else-if="view === 'settings'" class="grid gap-6 lg:grid-cols-[1fr_0.7fr] overflow-y-auto flex-1 min-h-0">
            <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
              <div class="flex items-center gap-3"><Save class="h-5 w-5 text-on-surface-variant" /><h3 class="text-xl font-medium text-on-surface">系统设置</h3></div>
              <p class="mt-3 text-sm leading-6 text-on-surface-variant">这里用于控制聊天模型、默认知识库和检索策略。修改后会直接影响用户提问时的检索效果和最终回答内容。</p>
              <div class="mt-5 grid gap-4">
                <label class="space-y-2"><span class="text-sm font-medium text-on-surface">对话模型</span><p class="text-xs leading-5 text-on-surface-variant">用于生成最终回答的大模型名称，例如 `qwen-max`。</p><input v-model="settingsForm.chatModelName" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none" placeholder="例如：qwen-max" /></label>
                <label class="space-y-2"><span class="text-sm font-medium text-on-surface">默认知识库</span><p class="text-xs leading-5 text-on-surface-variant">当用户没有主动指定知识库时，系统默认检索的知识库标识。</p><input v-model="settingsForm.defaultKnowledgeBase" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none" placeholder="例如：default" /></label>
                <div class="grid gap-4 md:grid-cols-3">
                  <label class="space-y-2"><span class="text-sm font-medium text-on-surface">检索条数</span><p class="text-xs leading-5 text-on-surface-variant">每次问答最多取回多少条知识片段参与生成。</p><input v-model.number="settingsForm.retrievalMaxResults" type="number" min="1" max="10" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none" placeholder="1 - 10" /></label>
                  <label class="space-y-2"><span class="text-sm font-medium text-on-surface">最低相似度</span><p class="text-xs leading-5 text-on-surface-variant">低于该分数的片段不会进入上下文，范围 `0 - 1`。</p><input v-model.number="settingsForm.retrievalMinScore" type="number" step="0.05" min="0" max="1" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none" placeholder="例如：0.6" /></label>
                  <label class="space-y-2"><span class="text-sm font-medium text-on-surface">上传大小限制</span><p class="text-xs leading-5 text-on-surface-variant">管理员单次上传文件允许的最大体积，单位 MB。</p><input v-model.number="settingsForm.uploadMaxSizeMb" type="number" min="1" max="100" class="w-full rounded-2xl bg-surface px-4 py-3 outline-none" placeholder="例如：20" /></label>
                </div>
                <button class="inline-flex items-center justify-center gap-2 rounded-full bg-on-surface px-5 py-2.5 text-sm text-surface transition hover:opacity-90" @click="handleSaveSettings"><Save class="h-4 w-4" />保存设置</button>
              </div>
            </div>
            <div class="rounded-[28px] border border-outline-variant/10 bg-surface-container-low p-6">
              <div class="flex items-center gap-3"><CheckCircle2 class="h-5 w-5 text-on-surface-variant" /><h3 class="text-xl font-medium text-on-surface">配置状态</h3></div>
              <div class="mt-5 space-y-4">
                <div class="rounded-2xl bg-surface px-4 py-4"><p class="text-xs uppercase tracking-[0.24em] text-on-surface-variant">DashScope</p><p class="mt-2 text-sm text-on-surface">{{ settings?.dashscopeApiConfigured ? '已配置' : '缺少密钥' }}</p><p class="mt-2 text-xs leading-5 text-on-surface-variant">负责大模型对话与部分向量化能力，未配置时聊天功能会受影响。</p></div>
                <div class="rounded-2xl bg-surface px-4 py-4"><p class="text-xs uppercase tracking-[0.24em] text-on-surface-variant">Pinecone</p><p class="mt-2 text-sm text-on-surface">{{ settings?.pineconeApiConfigured ? '已配置' : '缺少密钥' }}</p><p class="mt-2 text-xs leading-5 text-on-surface-variant">负责知识片段的向量存储与检索，未配置时 RAG 检索无法正常工作。</p></div>
                <div class="rounded-2xl bg-surface px-4 py-4 text-sm leading-6 text-on-surface-variant">这些设置会同步写回后端配置中心，并立即影响后台默认运行参数。建议修改后做一次知识库重建和问答验证。</div>
              </div>
            </div>
          </section>
          

  <!-- Document preview modal -->
  <Teleport to="body">
    <div v-if="showPreview && previewDoc" class="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="closePreview">
      <div class="w-full max-w-3xl max-h-[80vh] mx-4 rounded-[28px] border border-outline-variant/10 bg-surface-container-low flex flex-col overflow-hidden">
        <div class="flex items-center justify-between px-6 py-4 border-b border-outline-variant/10">
          <div class="min-w-0 flex-1">
            <p class="text-sm font-medium text-on-surface truncate">{{ previewDoc.fileName }}</p>
            <p class="text-[10px] text-on-surface-variant mt-0.5">{{ (previewDoc.fileSize / 1024).toFixed(1) }} KB</p>
          </div>
          <button class="p-2 hover:bg-surface-container-high rounded-full transition-colors text-on-surface-variant hover:text-on-surface" @click="closePreview"><X class="h-4 w-4" /></button>
        </div>
        <div class="flex-1 overflow-y-auto p-6">
          <pre class="text-sm leading-7 text-on-surface whitespace-pre-wrap font-sans">{{ previewDoc.content }}</pre>
        </div>
      </div>
    </div>
  </Teleport>

</template>
      </div>
    </div>
  </div>
  

  <!-- Document preview modal -->
  <Teleport to="body">
    <div v-if="showPreview && previewDoc" class="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="closePreview">
      <div class="w-full max-w-3xl max-h-[80vh] mx-4 rounded-[28px] border border-outline-variant/10 bg-surface-container-low flex flex-col overflow-hidden">
        <div class="flex items-center justify-between px-6 py-4 border-b border-outline-variant/10">
          <div class="min-w-0 flex-1">
            <p class="text-sm font-medium text-on-surface truncate">{{ previewDoc.fileName }}</p>
            <p class="text-[10px] text-on-surface-variant mt-0.5">{{ (previewDoc.fileSize / 1024).toFixed(1) }} KB</p>
          </div>
          <button class="p-2 hover:bg-surface-container-high rounded-full transition-colors text-on-surface-variant hover:text-on-surface" @click="closePreview"><X class="h-4 w-4" /></button>
        </div>
        <div class="flex-1 overflow-y-auto p-6">
          <pre class="text-sm leading-7 text-on-surface whitespace-pre-wrap font-sans">{{ previewDoc.content }}</pre>
        </div>
      </div>
    </div>
  </Teleport>

</template>
