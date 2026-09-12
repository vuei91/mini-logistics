import type { ErrorResponse } from "./types";

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export const TOKEN_STORAGE_KEY = "cj_access_token";

/** 서버 에러 응답을 감싸는 예외 클래스 */
export class ApiError extends Error {
  readonly status: number;
  readonly body?: ErrorResponse;

  constructor(status: number, message: string, body?: ErrorResponse) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function setToken(token: string): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(TOKEN_STORAGE_KEY, token);
}

export function clearToken(): void {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(TOKEN_STORAGE_KEY);
}

interface RequestOptions {
  method?: "GET" | "POST" | "PATCH" | "PUT" | "DELETE";
  body?: unknown;
  /** 인증 헤더 첨부 여부 (기본 true). 로그인/회원가입은 false */
  auth?: boolean;
}

/**
 * fetch 래퍼.
 * - Base URL 자동 결합
 * - Authorization: Bearer 자동 첨부
 * - 에러 응답을 ApiError 로 변환
 * - 401 발생 시 토큰 삭제 후 /login 이동
 */
export async function apiFetch<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { method = "GET", body, auth = true } = options;

  const headers: Record<string, string> = {};
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (auth) {
    const token = getToken();
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }

  let res: Response;
  try {
    res = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(
      0,
      "서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인해주세요.",
    );
  }

  if (res.status === 401 && auth) {
    clearToken();
    if (typeof window !== "undefined" && window.location.pathname !== "/login") {
      // 공통 HTTP 계층에서는 Next Router 훅을 사용할 수 없어 전체 이동으로 인증 상태를 초기화한다.
      // eslint-disable-next-line @next/next/no-location-assign-relative-destination
      window.location.href = "/login";
    }
    throw new ApiError(401, "인증이 만료되었습니다. 다시 로그인해주세요.");
  }

  if (!res.ok) {
    let errorBody: ErrorResponse | undefined;
    let message = `요청에 실패했습니다. (${res.status})`;
    try {
      errorBody = (await res.json()) as ErrorResponse;
      if (errorBody?.message) message = errorBody.message;
    } catch {
      // 본문이 없거나 JSON 이 아닌 경우 기본 메시지 유지
    }
    throw new ApiError(res.status, message, errorBody);
  }

  // 204 No Content 또는 빈 본문 처리
  if (res.status === 204) {
    return undefined as T;
  }
  const text = await res.text();
  if (!text) return undefined as T;
  return JSON.parse(text) as T;
}
