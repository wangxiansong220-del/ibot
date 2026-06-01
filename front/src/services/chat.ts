import { getAuthToken } from "./api";

export interface ChatCitation {
  fileName: string;
  sourcePath: string;
  chunkIndex: string;
  score: number;
  snippet: string;
}

export interface ChatResponse {
  sessionId: string;
  answer: string;
  citations: ChatCitation[];
}

export interface ChatHistoryItem {
  question: string;
  answer: string;
}

export interface ChatHistoryResponse {
  sessionId: string;
  createdAt: string;
  updatedAt: string;
  history: ChatHistoryItem[];
}

export interface UiMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  timestamp: number;
  citations?: ChatCitation[];
}

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim() || "";

async function apiRequestFromChat<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers || {});
  const token = getAuthToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  const response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });
  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;
  if (!response.ok) {
    const message = (payload && payload.message) || response.statusText || "Request failed";
    throw new Error(message);
  }
  return payload as T;
}

export function sendMessage(message: string, sessionId: string) {
  return apiRequestFromChat<ChatResponse>("/api/chat", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ sessionId, message }),
  });
}

export async function fetchHistory(sessionId: string): Promise<ChatHistoryResponse | null> {
  try {
    return await apiRequestFromChat<ChatHistoryResponse>(`/api/chat/history?sessionId=${encodeURIComponent(sessionId)}`);
  } catch (error) {
    if (error instanceof Error && "status" in error && Number((error as any).status) === 404) {
      return null;
    }
    throw error;
  }
}

export function historyToMessages(history: ChatHistoryItem[]): UiMessage[] {
  return history.flatMap((item, index) => [
    {
      id: "q-" + index,
      role: "user" as const,
      content: item.question,
      timestamp: Date.now() + index,
    },
    {
      id: "a-" + index,
      role: "assistant" as const,
      content: item.answer,
      timestamp: Date.now() + index + 1,
    },
  ]);
}
