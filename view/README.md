# CJ대한통운 미니 물류 플랫폼 — 프론트엔드 (view)

`app` 폴더의 Spring Boot REST API를 기반으로 한 화주-기사 화물 매칭/배차 웹 프론트엔드입니다.
Next.js 16 (App Router) + TypeScript + Tailwind CSS v4로 구성되어 있습니다.

## 실행 방법

1. 백엔드 실행 (`app` 폴더, 기본 포트 8080)

2. 환경변수 설정

   ```bash
   cp .env.local.example .env.local
   # 필요 시 NEXT_PUBLIC_API_BASE_URL 수정 (기본값 http://localhost:8080)
   ```

3. 의존성 설치 및 개발 서버 실행

   ```bash
   npm install
   npm run dev
   ```

   기본 접속 주소: http://localhost:3000
   (백엔드 `SecurityConfig`의 CORS 허용 Origin과 일치해야 합니다.)

4. 프로덕션 빌드

   ```bash
   npm run build
   npm run start
   ```

## 환경변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080` | 백엔드 API Base URL |

## 화면 구성

| 경로 | 설명 | 접근 |
|---|---|---|
| `/` | 랜딩 (로그인 시 역할별 대시보드로 리다이렉트) | 공개 |
| `/login` | 로그인 (화주/기사 탭) | 공개 |
| `/signup` | 회원가입 (기사는 차량 정보 추가, 가입 후 자동 로그인) | 공개 |
| `/shipper/dashboard` | 내 화물 요청 목록 | 화주 |
| `/shipper/requests/new` | 화물 요청 생성 | 화주 |
| `/shipper/requests/[id]` | 화물 요청 상세 (매칭 요청/취소) | 화주 |
| `/driver/dashboard` | 배정된 배차 목록 (제안/진행·완료 분리) | 기사 |
| `/driver/dispatches/[id]` | 배차 상세 (수락/거절, 운송 상태 스텝퍼) | 기사 |

## 폴더 구조

```
view/
├── app/
│   ├── layout.tsx              # AuthProvider + 공통 헤더 레이아웃
│   ├── page.tsx                # 랜딩
│   ├── login/page.tsx
│   ├── signup/page.tsx
│   ├── shipper/
│   │   ├── dashboard/page.tsx
│   │   └── requests/{new,[id]}/page.tsx
│   └── driver/
│       ├── dashboard/page.tsx
│       └── dispatches/[id]/page.tsx
├── components/                 # Header, RoleGuard, StatusBadge, CargoItemForm,
│                               #   DispatchStepper, Spinner, EmptyState, ErrorAlert
├── contexts/AuthContext.tsx    # JWT 토큰/역할/profileId 전역 상태
└── lib/
    ├── types.ts                # 백엔드 DTO 매칭 타입
    ├── api.ts                  # fetch 래퍼 (Bearer 자동 첨부, ApiError, 401 처리)
    ├── endpoints.ts            # 도메인별 API 함수
    └── labels.ts               # 열거형 한글 라벨 / 포맷 유틸
```

## 인증 흐름

- 로그인/가입 성공 시 JWT를 `localStorage`(`cj_access_token`)에 저장합니다.
- `jwt-decode`로 payload(`sub`, `role`, `profileId`, `exp`)를 파싱해 `AuthContext`에 반영하고 만료를 검사합니다.
- 모든 인증 요청은 `lib/api.ts`에서 `Authorization: Bearer` 헤더를 자동 첨부합니다.
- 401 응답 시 토큰을 삭제하고 `/login`으로 이동합니다.
- 화주/기사 전용 페이지는 `RoleGuard`로 클라이언트 사이드 역할 체크를 수행합니다.
