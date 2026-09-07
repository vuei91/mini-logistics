# CJ대한통운 미니 물류 플랫폼 — 프론트엔드 구현 체크리스트

> 기준 문서: `PLANNING.md`
> 대상: `view/` 폴더에 Next.js(App Router) + TypeScript + Tailwind CSS 프론트엔드 구축

---

## 0. 백엔드 선행 작업 (완료 — PLANNING.md 8장)

- [x] `SecurityConfig`에 CORS 설정 추가 (`http://localhost:3000` 허용, `CORS_ALLOWED_ORIGINS`로 오버라이드)
- [x] `GET /shipment-requests` 화주 본인 화물 요청 목록 조회 API 신설 (JWT `profileId` 기준)
- [x] `GET /dispatches` 기사 본인 배차 목록 조회 API 신설 (JWT `profileId` 기준)
- [x] `compileJava` / `compileTestJava` 성공 확인
- [x] `DispatchResponse`에 `shipmentStatus` 필드 추가 (기사 배차 상세 스텝퍼용, 프론트 구현 중 필요해 추가) — `compileJava` 성공
- [ ] (선택) 신규 목록 조회 API에 대한 테스트 작성
- [ ] (선택) 애플리케이션 기동 후 신규 API 수동 동작 확인

---

## 1. 환경 셋업 (마일스톤 2)

> 참고: 스캐폴딩된 프로젝트는 Next.js 16.3.4 (App Router, Turbopack) + Tailwind v4 기반이며, `src/` 없이 루트 `app/` 디렉터리를 사용한다. Tailwind v4는 CSS-first 설정(`@import "tailwindcss"` + `@theme`)이라 `tailwind.config.ts`가 필요 없다.

- [x] `view/` 폴더에 Next.js (App Router) + TypeScript 프로젝트 생성 (실제 16.3.4)
- [x] Tailwind CSS 설치 및 설정 (v4 CSS-first, `app/globals.css`)
- [x] `.env.local.example` 작성 (`NEXT_PUBLIC_API_BASE_URL=http://localhost:8080`)
- [x] `jwt-decode` 등 필요한 의존성 설치
- [x] 폴더 구조 스캐폴딩 (`app`, `components`, `contexts`, `lib`)
- [x] `lib/types.ts` — 백엔드 DTO와 1:1 매칭되는 TS 타입 전체 정의
  - [x] 인증 타입 (`ShipperSignupRequest`, `DriverSignupRequest`, `LoginRequest`, `TokenResponse`, `VehicleType`)
  - [x] 화주/기사 타입 (`ShipperResponse`, `DriverResponse`, `DriverStatus`)
  - [x] 화물 요청 타입 (`ShipmentStatus`, `ShipmentRequestCreateRequest`, `ShipmentRequestResponse`)
  - [x] 배차 타입 (`DispatchStatus`, `DispatchResponse`, `DispatchStatusUpdateRequest`)
  - [x] 에러 타입 (`ErrorResponse`)
- [x] `lib/api.ts` — `fetch` 래퍼 (Base URL, `Authorization: Bearer` 자동 첨부, 에러 파싱, 401 처리)
- [x] `lib/endpoints.ts` — 도메인별 API 함수 정리

---

## 2. 인증 플로우 (마일스톤 3)

- [x] `contexts/AuthContext.tsx` — 토큰/역할/profileId/email 전역 상태
  - [x] 로그인 시 `localStorage`(`cj_access_token`)에 토큰 저장
  - [x] `jwt-decode`로 payload(`sub`, `role`, `profileId`, `exp`) 파싱 + 만료 검사
  - [x] 로그아웃 시 토큰 삭제 및 상태 초기화
- [x] `components/Header.tsx` — 로그인 상태별 헤더 (로그인/가입 ↔ 이메일·로그아웃)
- [x] `app/layout.tsx` — 공통 레이아웃 + AuthProvider 적용
- [x] `app/page.tsx` — 랜딩 페이지 (서비스 소개, 로그인/가입 유도, 로그인 시 대시보드 리다이렉트)
- [x] `app/login/page.tsx` — 로그인 (화주/기사 탭 전환), 성공 시 역할별 대시보드 이동
- [x] `app/signup/page.tsx` — 회원가입 (화주/기사 탭 전환, 기사는 차량 정보 섹션 추가, 가입 후 자동 로그인)
- [x] 라우트 가드 (`components/RoleGuard.tsx`) — 화주/기사 전용 페이지 클라이언트 사이드 역할 체크
- [x] 401 응답 시 로그인 페이지 강제 이동 + 토큰 삭제

---

## 3. 화주 플로우 (마일스톤 4)

- [x] `components/StatusBadge.tsx` — 상태 배지 (화물/배차 상태 색상 매핑)
- [x] `app/shipper/dashboard/page.tsx` — 내 화물 요청 목록 + "새 요청" CTA
- [x] `components/CargoItemForm.tsx` — 화물 항목 추가/삭제 (설명 + 무게kg, 총 무게 표시)
- [x] `app/shipper/requests/new/page.tsx` — 화물 요청 생성 폼
  - [x] 출발지/도착지 입력
  - [x] 필요 차량 타입 드롭다운
  - [x] 화물 항목 리스트 추가/삭제 + 총 무게 자동 계산
- [x] `app/shipper/requests/[id]/page.tsx` — 화물 요청 상세
  - [x] 요청 정보 / 화물 항목 목록 표시
  - [x] `REQUESTED`: "기사 매칭 요청" 버튼 → 배차 생성 API 호출
  - [x] `REQUESTED`/`MATCHING`: "취소" 버튼
  - [x] 배차 존재 시 배차 상태/기사 ID/매칭점수/운임(fare) 표시 (매칭 요청 응답으로 표시)

---

## 4. 기사 플로우 (마일스톤 5)

- [x] `components/DispatchStepper.tsx` — 운송 상태 변경 스텝퍼
- [x] `app/driver/dashboard/page.tsx` — 배정된 배차 목록 (`PROPOSED` 강조 + 진행/완료 분리)
- [x] `app/driver/dispatches/[id]/page.tsx` — 배차 상세
  - [x] 배차 정보 (화물 요청 연결, 매칭 점수, 운임, 운송 상태)
  - [x] `PROPOSED`: 수락/거절 버튼
  - [x] `ACCEPTED` 이후: 운송 상태 스텝퍼 (배차완료 → 픽업지 이동중 → 픽업완료 → 운송중 → 완료)

---

## 5. 다듬기 (마일스톤 6)

- [x] 에러 처리 통일 (`ErrorResponse.message` 노출, `ApiError` 클래스로 상태/메시지 관리, `ErrorAlert`)
- [x] 로딩 상태 UI (`Spinner`)
- [x] 빈 상태(Empty State) UI (`EmptyState`)
- [x] 심플 컬러 팔레트 적용 (흰 배경 + CJ 레드 포인트 컬러, 회색 톤 텍스트, 카드 기반 UI)
- [x] 반응형 세부 점검 (기본 반응형 적용됨, 실기기 확인 권장)

---

## 6. 마무리 / 배포 (선택)

- [x] README 작성 (`view/README.md` — 실행 방법, 환경변수, 화면/구조 안내)
- [x] 프론트엔드 프로덕션 빌드 성공 (`npm run build`)
- [ ] 백엔드 컴파일 성공 (`compileJava`)
- [ ] 로컬 통합 테스트 (백엔드 기동 → 회원가입 → 로그인 → 화주/기사 전체 플로우 수동 확인)
- [ ] Vercel 등 배포 + `CORS_ALLOWED_ORIGINS`에 배포 도메인 추가
