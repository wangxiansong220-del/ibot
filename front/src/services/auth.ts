import { apiRequest, clearAuthToken, setAuthToken } from "./api";

export interface UserProfile {
  id: number;
  email: string;
  displayName: string;
  role: "USER" | "ADMIN";
  enabled: boolean;
  createdAt?: string;
  lastLoginAt?: string;
}

export interface AuthResponse {
  token: string;
  user: UserProfile;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  email: string;
  displayName: string;
  password: string;
}

export async function login(payload: LoginPayload) {
  const response = await apiRequest<AuthResponse>("/api/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  setAuthToken(response.token);
  return response;
}

export async function register(payload: RegisterPayload) {
  const response = await apiRequest<AuthResponse>("/api/auth/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  setAuthToken(response.token);
  return response;
}

export function logout() {
  clearAuthToken();
}

export function fetchCurrentUser() {
  return apiRequest<UserProfile>("/api/auth/me");
}
