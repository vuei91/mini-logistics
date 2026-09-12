# Login Error Handling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 로그인 실패를 안정적인 오류 코드로 분류하고 백엔드와 프론트엔드가 동일한 계약으로 처리한다.

**Architecture:** 백엔드 `ErrorResponse`에 기계 판독용 `code`를 추가하고 전역 예외 처리기가 HTTP 상태와 오류 코드를 함께 매핑한다. 프론트 fetch 래퍼는 응답 코드를 보존하며, 인증이 필요한 요청의 401만 세션 만료로 처리하고 로그인 화면은 오류 코드별 사용자 메시지를 선택한다.

**Tech Stack:** Java 21, Spring Boot MVC/Security, JUnit 5/MockMvc, Next.js 16, TypeScript

**Spec:** 사용자 요청(2026-09-12): 로그인 오류 처리 방식 목록을 만들고 백엔드와 프론트엔드에 적용

## Global Constraints

- 존재하지 않는 계정과 틀린 비밀번호는 계정 열거 공격을 막기 위해 `INVALID_CREDENTIALS`로 통합한다.
- 사용자에게 내부 예외 상세를 노출하지 않는다.
- 기존 정상 로그인 및 보호 API의 세션 만료 리다이렉트 동작은 유지한다.

---

### Task 1: 백엔드 오류 계약

**Files:**
- Create: `app/src/main/java/com/cjlogistics/mini/common/ErrorCode.java`
- Modify: `app/src/main/java/com/cjlogistics/mini/common/ErrorResponse.java`
- Modify: `app/src/main/java/com/cjlogistics/mini/common/GlobalExceptionHandler.java`
- Test: `app/src/test/java/com/cjlogistics/mini/auth/AuthControllerTest.java`

**Interfaces:**
- Produces: JSON 오류 응답 `{ timestamp, status, error, code, message, path }`

- [ ] **Step 1: Write failing MockMvc tests**

```java
.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
```

- [ ] **Step 2: Run tests and verify RED**

Run: `cd app && ./gradlew test --tests com.cjlogistics.mini.auth.AuthControllerTest`
Expected: FAIL because `$.code` does not exist.

- [ ] **Step 3: Add error codes and exception mappings**

Add `INVALID_CREDENTIALS`, `VALIDATION_FAILED`, `MALFORMED_REQUEST`, and general HTTP/domain codes to `ErrorCode`; pass the selected value through `ErrorResponse.of`.

- [ ] **Step 4: Run tests and verify GREEN**

Run: `cd app && ./gradlew test --tests com.cjlogistics.mini.auth.AuthControllerTest`
Expected: PASS.

### Task 2: 프론트엔드 로그인 오류 분기

**Files:**
- Modify: `view/lib/types.ts`
- Modify: `view/lib/api.ts`
- Create: `view/lib/login-error.ts`
- Modify: `view/app/login/page.tsx`
- Test: `view/lib/login-error.test.ts`

**Interfaces:**
- Consumes: backend `ErrorResponse.code`
- Produces: `getLoginErrorMessage(error: unknown): string`

- [ ] **Step 1: Write failing table-driven tests**

Test invalid credentials, validation, rate limit, server response, network failure, and unknown client errors using literal Korean messages.

- [ ] **Step 2: Run tests and verify RED**

Run: `cd view && npm test`
Expected: FAIL because `getLoginErrorMessage` is missing.

- [ ] **Step 3: Implement minimal mapping and correct 401 behavior**

Only clear credentials and redirect for `401` when `auth === true`; preserve public login response codes/messages in `ApiError`.

- [ ] **Step 4: Run tests and verify GREEN**

Run: `cd view && npm test`
Expected: PASS.

### Task 3: 계약 문서와 전체 검증

**Files:**
- Create: `docs/login-error-handling.md`

**Interfaces:**
- Produces: status/code/cause/frontend-action matrix

- [ ] **Step 1: Document the implemented matrix**

Document `VALIDATION_FAILED`, `MALFORMED_REQUEST`, `INVALID_CREDENTIALS`, `AUTHENTICATION_REQUIRED`, `ACCESS_DENIED`, `TOO_MANY_REQUESTS`, `INTERNAL_SERVER_ERROR`, network failure, and malformed success tokens.

- [ ] **Step 2: Run full verification**

Run: `cd app && ./gradlew test`
Run: `cd view && npm test && npm run lint && npm run build`
Expected: all commands exit 0 with no failures.
