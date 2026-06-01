import { apiRequest } from "./api";

export interface AdminTask {
  id: number;
  taskType: string;
  targetType: string;
  targetName: string;
  status: string;
  progress: number;
  message: string;
  detailsJson?: string;
  createdBy?: string;
  startedAt?: string;
  completedAt?: string;
  updatedAt?: string;
}

export interface KnowledgeDocument {
  documentId: string;
  knowledgeBase: string;
  namespace: string;
  fileName: string;
  fileSize: number;
  chunkCount: number;
  storagePath: string;
  indexingStatus: string;
  uploadedAt?: string;
  tags?: string[];
  folder?: string;
}

export interface KnowledgeUploadResponse {
  knowledgeBase: string;
  uploadedCount: number;
  message?: string;
  documents: KnowledgeDocument[];
}

export interface KnowledgeBaseInfo {
  name: string;
  documentCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentMetadataPayload {
  tags?: string[];
  folder?: string;
}

export interface RagStatus {
  knowledgeBase: string;
  storageRoot: string;
  documentCount: number;
  ragEnabled: boolean;
  retrievalStatus: string;
  embeddingProvider: string;
  vectorStore: string;
  chunkSize: number;
  chunkOverlap: number;
}

export interface RebuildDocument {
  fileName: string;
  sourcePath: string;
  chunkCount: number;
  status: string;
  message: string;
}

export interface RebuildResponse {
  taskId: string;
  knowledgeBase: string;
  namespace: string;
  status: string;
  progress: number;
  documentCount: number;
  processedDocuments: number;
  chunkCount: number;
  indexingStatus: string;
  message: string;
  triggeredAt?: string;
  completedAt?: string;
  documents: RebuildDocument[];
}

export interface DataSource {
  id: number;
  name: string;
  type: "MYSQL" | "POSTGRESQL" | "MONGODB" | "ELASTICSEARCH" | "REDIS" | "API_ENDPOINT";
  host?: string;
  port?: number;
  databaseName?: string;
  username?: string;
  apiBaseUrl?: string;
  notes?: string;
  enabled: boolean;
  hasPassword: boolean;
  lastTestStatus?: string;
  lastTestMessage?: string;
  lastTestedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface DataSourcePayload {
  name: string;
  type: DataSource["type"];
  host?: string;
  port?: number;
  databaseName?: string;
  username?: string;
  password?: string;
  apiBaseUrl?: string;
  notes?: string;
  enabled?: boolean;
}

export interface DataSourceTestResponse {
  taskId: number;
  dataSourceId: number;
  status: string;
  message: string;
  testedAt: string;
}

export interface UserProfileItem {
  id: number;
  email: string;
  displayName: string;
  role: "USER" | "ADMIN";
  enabled: boolean;
  createdAt?: string;
  lastLoginAt?: string;
}

export interface CreateUserPayload {
  email: string;
  displayName: string;
  password: string;
  role?: "USER" | "ADMIN";
  enabled?: boolean;
}

export interface UpdateUserPayload {
  displayName?: string;
  role?: "USER" | "ADMIN";
  enabled?: boolean;
}

export interface AdminSettings {
  chatModelName: string;
  defaultKnowledgeBase: string;
  retrievalMaxResults: number;
  retrievalMinScore: number;
  uploadMaxSizeMb: number;
  dashscopeApiConfigured: boolean;
  pineconeApiConfigured: boolean;
}

export interface UpdateAdminSettingsPayload {
  chatModelName: string;
  defaultKnowledgeBase: string;
  retrievalMaxResults: number;
  retrievalMinScore: number;
  uploadMaxSizeMb: number;
}

// ====== Dashboard ======

export interface DashboardData {
  kbStats: {
    totalKnowledgeBases: number;
    totalDocuments: number;
    totalChunks: number;
    knowledgeBases: Array<{
      name: string;
      documentCount: number;
      chunkCount: number;
    }>;
  };
  chatStats: {
    totalSessions: number;
    totalMessages: number;
    dailyActivity: Array<{
      date: string;
      sessionCount: number;
      messageCount: number;
    }>;
  };
  popularQueries: Array<{
    query: string;
    count: number;
  }>;
  systemStats: {
    usedMemoryMb: number;
    maxMemoryMb: number;
    availableProcessors: number;
    os: string;
    javaVersion: string;
  };
}

export function getDashboard() {
  return apiRequest<DashboardData>("/api/admin/dashboard");
}

// ====== Knowledge Base CRUD ======

// ====== Sample Data ======

export interface SeedResponse {
  knowledgeBase: string;
  documentCount: number;
  fileNames: string[];
  message: string;
}

export function seedSampleData(name: string) {
  return apiRequest<SeedResponse>("/api/admin/knowledge/bases/" + encodeURIComponent(name) + "/seed", {
    method: "POST",
  });
}

// ====== Document Preview ======

export interface DocumentPreview {
  documentId: string;
  fileName: string;
  content: string;
  fileSize: number;
  knowledgeBase: string;
}

export function previewDocument(knowledgeBase: string, documentId: string) {
  return apiRequest<DocumentPreview>("/api/admin/knowledge/documents/" + encodeURIComponent(documentId) + "/content?knowledgeBase=" + encodeURIComponent(knowledgeBase));
}

// ====== RAG Evaluation ======

export interface EvalItem {
  question: string;
  expectedAnswer: string;
  actualAnswer: string;
  score: number;
  retrieved: boolean;
  retrievedDocs: string[];
}

export interface EvaluationResult {
  taskId: string;
  status: string;
  totalQuestions: number;
  matchedCount: number;
  avgScore: number;
  knowledgeBase: string;
  items: EvalItem[];
}

export function evaluateKnowledgeBase(knowledgeBase: string, file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return apiRequest<EvaluationResult>("/api/admin/knowledge/evaluate?knowledgeBase=" + encodeURIComponent(knowledgeBase), {
    method: "POST",
    body: formData,
  });
}

export function getEvaluationResult(taskId: string) {
  return apiRequest<EvaluationResult>("/api/admin/knowledge/evaluate/" + encodeURIComponent(taskId));
}

export function seedAllKnowledgeBases() {
  return apiRequest<SeedResponse[]>("/api/admin/knowledge/seed-all", {
    method: "POST",
  });
}

export function seedDomain(kbName: string, domain: string) {
  return apiRequest<SeedResponse>("/api/admin/knowledge/bases/" + encodeURIComponent(kbName) + "/seed-domain?domain=" + encodeURIComponent(domain));
}

export function listKnowledgeBases() {
  return apiRequest<KnowledgeBaseInfo[]>("/api/admin/knowledge/bases");
}

export function createKnowledgeBase(name: string) {
  return apiRequest<KnowledgeBaseInfo>("/api/admin/knowledge/bases?name=" + encodeURIComponent(name), {
    method: "POST",
  });
}

export function deleteKnowledgeBase(name: string) {
  return apiRequest<{ message: string }>("/api/admin/knowledge/bases/" + encodeURIComponent(name), {
    method: "DELETE",
  });
}

export function renameKnowledgeBase(name: string, newName: string) {
  return apiRequest<KnowledgeBaseInfo>("/api/admin/knowledge/bases/" + encodeURIComponent(name) + "?newName=" + encodeURIComponent(newName), {
    method: "PUT",
  });
}

// ====== Knowledge Documents ======

export function listKnowledgeDocuments(knowledgeBase: string, search?: string, tag?: string, folder?: string) {
  const params = new URLSearchParams({ knowledgeBase });
  if (search) params.set("search", search);
  if (tag) params.set("tag", tag);
  if (folder) params.set("folder", folder);
  return apiRequest<KnowledgeDocument[]>("/api/admin/knowledge/documents?" + params.toString());
}

export async function uploadKnowledgeDocuments(knowledgeBase: string, files: File[]) {
  const formData = new FormData();
  files.forEach((file) => formData.append("files", file));
  return apiRequest<KnowledgeUploadResponse>("/api/admin/knowledge/documents?knowledgeBase=" + encodeURIComponent(knowledgeBase), {
    method: "POST",
    body: formData,
  });
}

export function deleteKnowledgeDocument(knowledgeBase: string, documentId: string) {
  return apiRequest<{ message: string }>("/api/admin/knowledge/documents/" + encodeURIComponent(documentId) + "?knowledgeBase=" + encodeURIComponent(knowledgeBase), {
    method: "DELETE",
  });
}

// ====== Document Metadata ======

export function updateDocumentMetadata(knowledgeBase: string, documentId: string, payload: DocumentMetadataPayload) {
  return apiRequest<KnowledgeDocument>("/api/admin/knowledge/documents/" + encodeURIComponent(documentId) + "/metadata?knowledgeBase=" + encodeURIComponent(knowledgeBase), {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export function listKnowledgeBaseTags(knowledgeBase: string) {
  return apiRequest<string[]>("/api/admin/knowledge/tags?knowledgeBase=" + encodeURIComponent(knowledgeBase));
}

export function listKnowledgeBaseFolders(knowledgeBase: string) {
  return apiRequest<string[]>("/api/admin/knowledge/folders?knowledgeBase=" + encodeURIComponent(knowledgeBase));
}

// ====== RAG Operations ======

export function getRagStatus(knowledgeBase: string) {
  return apiRequest<RagStatus>("/api/admin/knowledge/rag/status?knowledgeBase=" + encodeURIComponent(knowledgeBase));
}

export function rebuildKnowledgeBase(knowledgeBase: string) {
  return apiRequest<RebuildResponse>("/api/admin/knowledge/rebuild?knowledgeBase=" + encodeURIComponent(knowledgeBase), {
    method: "POST",
  });
}

export function getRebuildTask(taskId: string) {
  return apiRequest<RebuildResponse>("/api/admin/knowledge/rebuild/" + encodeURIComponent(taskId));
}

// ====== Tasks ======

export function listTasks() {
  return apiRequest<AdminTask[]>("/api/admin/tasks");
}

export function getTask(taskId: number) {
  return apiRequest<AdminTask>("/api/admin/tasks/" + taskId);
}

// ====== Users ======

export function listUsers() {
  return apiRequest<UserProfileItem[]>("/api/admin/users");
}

export function createUser(payload: CreateUserPayload) {
  return apiRequest<UserProfileItem>("/api/admin/users", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export function updateUser(userId: number, payload: UpdateUserPayload) {
  return apiRequest<UserProfileItem>("/api/admin/users/" + userId, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export function resetUserPassword(userId: number, newPassword: string) {
  return apiRequest<{ message: string }>("/api/admin/users/" + userId + "/reset-password", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ newPassword }),
  });
}

export function deleteUser(userId: number) {
  return apiRequest<{ message: string }>("/api/admin/users/" + userId, {
    method: "DELETE",
  });
}

// ====== Data Sources ======

export function listDataSources() {
  return apiRequest<DataSource[]>("/api/admin/datasources");
}

export function createDataSource(payload: DataSourcePayload) {
  return apiRequest<DataSource>("/api/admin/datasources", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export function updateDataSource(id: number, payload: DataSourcePayload) {
  return apiRequest<DataSource>("/api/admin/datasources/" + id, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export function deleteDataSource(id: number) {
  return apiRequest<{ message: string }>("/api/admin/datasources/" + id, {
    method: "DELETE",
  });
}

export function testDataSource(id: number) {
  return apiRequest<DataSourceTestResponse>("/api/admin/datasources/" + id + "/test", {
    method: "POST",
  });
}

// ====== Settings ======

export function getSettings() {
  return apiRequest<AdminSettings>("/api/admin/settings");
}

export function updateSettings(payload: UpdateAdminSettingsPayload) {
  return apiRequest<AdminSettings>("/api/admin/settings", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}
