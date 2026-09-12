import assert from "node:assert/strict";
import test from "node:test";
import { ApiError, apiFetch } from "./api";
import { getLoginErrorMessage } from "./login-error";
import type { ErrorResponse } from "./types";

const response = (code: ErrorResponse["code"], message = "서버 원문") => ({
  timestamp: "2026-09-12T12:00:00",
  status: 400,
  error: "Bad Request",
  code,
  message,
  path: "/api/auth/shippers/login",
});

test("invalid credentials use a safe account-agnostic message", () => {
  const error = new ApiError(
    401,
    "서버 원문",
    response("INVALID_CREDENTIALS"),
  );
  assert.equal(
    getLoginErrorMessage(error),
    "이메일 또는 비밀번호가 올바르지 않습니다.",
  );
});

test("validation failures ask the user to review the form", () => {
  const error = new ApiError(400, "서버 원문", response("VALIDATION_FAILED"));
  assert.equal(
    getLoginErrorMessage(error),
    "이메일 형식과 비밀번호를 확인해주세요.",
  );
});

test("rate limits explain when retrying is appropriate", () => {
  const error = new ApiError(429, "서버 원문");
  assert.equal(
    getLoginErrorMessage(error),
    "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.",
  );
});

test("network and server failures have distinct recovery guidance", () => {
  assert.equal(
    getLoginErrorMessage(new ApiError(0, "연결 실패")),
    "서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.",
  );
  assert.equal(
    getLoginErrorMessage(new ApiError(503, "내부 정보")),
    "서버에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.",
  );
});

test("unexpected client failures do not expose exception details", () => {
  assert.equal(
    getLoginErrorMessage(new Error("token internals")),
    "로그인 처리 중 오류가 발생했습니다. 다시 시도해주세요.",
  );
});

test("unknown server codes cannot resolve inherited object properties", () => {
  const error = new ApiError(
    400,
    "서버 원문",
    response("toString" as ErrorResponse["code"]),
  );
  assert.equal(
    getLoginErrorMessage(error),
    "로그인에 실패했습니다. 입력한 정보를 확인해주세요.",
  );
});

test("a public login 401 preserves invalid-credentials details", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () =>
    new Response(JSON.stringify(response("INVALID_CREDENTIALS")), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    });

  try {
    await assert.rejects(
      apiFetch("/auth/shippers/login", { method: "POST", auth: false }),
      (error: unknown) =>
        error instanceof ApiError &&
        error.status === 401 &&
        error.body?.code === "INVALID_CREDENTIALS",
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});
