# CJ대한통운 미니 물류 플랫폼 — 프론트엔드 기획서

## 1. 개요

`app` 폴더의 Spring Boot REST API(화주-기사 화물 매칭/배차 시스템)를 기반으로,
`view` 폴더에 React 기반 프론트엔드를 구축한다.

- **목표**: 화주(Shipper)와 기사(Driver)가 각각 회원가입/로그인 후, 화물 요청 생성 → 매칭/배차 → 상태 추적을 웹 UI에서 수행할 수 있게 한다.
- **디자인 원칙**: 심플하고 실용적인 대시보드 스타일. 화려한 장식 없이 정보 전달과 액션에 집중.
- **기술 스택**: Next.js (App Router) + TypeScript + React + 간단한 CSS(Tailwind CSS 권장, 별도 UI 라이브러리 없이 최소 구성)

## 2. 기술 스택 선정 근거

| 항목 | 선택 | 이유 |
|---|---|---|
| 프레임워크 | Next.js 15 (App Router) | 라우팅/레이아웃 기본 제공, 별도 라우터 설정 불필요, 포트폴리오용으로 배포(Vercel 등)도 용이 |
| 언어 | TypeScript | API 응답/요청 타입을 백엔드 DTO와 1:1로 맞춰 타입 안정성 확보 |
| 스타일링 | Tailwind CSS | 별도 컴포넌트 라이브러리 없이 빠르게 심플한 UI 구성 가능 |
| 상태관리 | React Context + `useState`/`useEffect` (전역 상태는 인증 토큰 정도만) | 앱 규모가 작아 Redux 등 과한 도구 불필요 |
| 데이터 fetching | 네이티브 `fetch` 래퍼 (`lib/api.ts`) | 별도 라이브러리(React Query 등) 없이 최소 구성. 필요 시 추후 도입 |
| 인증 저장 | `localStorage`에 JWT 저장 + Context로 로그인 상태 전파 | 백엔드가 Stateless JWT(Bearer) 방식이므로 쿠키/세션 불필요 |

## 3. 백엔드 API 분석 결과

### 3.1 공통 사항

- Base URL: `http://localhost:8080` (기본 포트 8080, context-path 없음)
- 인증 방식: JWT Bearer 토큰 (`Authorization: Bearer {accessToken}`)
- 인증 불필요 경로: `/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/h2-console/**`
- 나머지 대부분의 API는 로그인 필요 (`anyRequest().authenticated()`)
- 역할(Role) 기반 접근 제어:
  - `SHIPPER`: 화물요청 생성/취소/배차요청
  - `DRIVER`: 배차 수락/거절/상태 변경
- **✅ CORS 설정 완료**: `SecurityConfig`에 `http://localhost:3000`을 허용하는 CORS 설정 반영됨 (8장 참고, `CORS_ALLOWED_ORIGINS` 환경변수로 배포 도메인 추가 가능).

### 3.2 인증 API (`/auth`)

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| POST | `/auth/shippers/signup` | 불필요 | 화주 회원가입 |
| POST | `/auth/drivers/signup` | 불필요 | 기사 회원가입 |
| POST | `/auth/shippers/login` | 불필요 | 화주 로그인 |
| POST | `/auth/drivers/login` | 불필요 | 기사 로그인 |

**요청/응답 타입**

```ts
// 화주 가입
type ShipperSignupRequest = { name: string; phone: string; email: string; password: string };

// 기사 가입
type DriverSignupRequest = {
  name: string; phone: string; email: string; password: string;
  vehicle: { vehicleType: VehicleType; capacityKg: number };
  preferredRoutes?: { originRegion: string; destinationRegion: string }[];
};

// 로그인 (공통)
type LoginRequest = { email: string; password: string };

// 로그인 응답
type TokenResponse = { accessToken: string; tokenType: "Bearer"; expiresIn: number };

type VehicleType = "TRUCK_1T" | "TRUCK_2_5T" | "TRUCK_5T" | "TRUCK_11T";
```

### 3.3 화주 API (`/shippers`)

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| GET | `/shippers/{id}` | 필요 | 화주 정보 조회 |

```ts
type ShipperResponse = { id: number; name: string; phone: string };
```

### 3.4 기사 API (`/drivers`)

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| GET | `/drivers/{id}` | 필요 | 기사 정보 조회 |

```ts
type DriverStatus = "AVAILABLE" | "BUSY" | "OFFLINE";

type DriverResponse = {
  id: number; name: string; phone: string; status: DriverStatus;
  vehicle: { id: number; vehicleType: VehicleType; capacityKg: number };
  preferredRoutes: { originRegion: string; destinationRegion: string }[];
};
```

### 3.5 화물 요청 API (`/shipment-requests`)

| Method | Path | 인증/권한 | 설명 |
|---|---|---|---|
| POST | `/shipment-requests` | SHIPPER | 화물 요청 생성 |
| GET | `/shipment-requests` | SHIPPER | **[신설]** 내(로그인한 화주) 화물 요청 목록 조회 (최신순) |
| GET | `/shipment-requests/{id}` | 로그인(소유자 화주만) | 화물 요청 조회 |
| POST | `/shipment-requests/{id}/cancel` | SHIPPER(소유자) | 화물 요청 취소 |

```ts
type ShipmentStatus =
  | "REQUESTED" | "MATCHING" | "DISPATCHED" | "EN_ROUTE_TO_PICKUP"
  | "PICKED_UP" | "IN_TRANSIT" | "COMPLETED" | "CANCELED";

type ShipmentRequestCreateRequest = {
  shipperId: number;
  originRegion: string;
  destinationRegion: string;
  cargoItems: { description: string; weightKg: number }[];
  requiredVehicleType: VehicleType;
};

type ShipmentRequestResponse = {
  id: number; shipperId: number; originRegion: string; destinationRegion: string;
  cargoItems: { id: number; description: string; weightKg: number }[];
  totalCargoWeightKg: number; requiredVehicleType: VehicleType;
  status: ShipmentStatus; createdAt: string; // ISO LocalDateTime
};
```

### 3.6 배차 API (`/dispatches`, `/shipment-requests/{id}/dispatch`)

| Method | Path | 인증/권한 | 설명 |
|---|---|---|---|
| POST | `/shipment-requests/{shipmentRequestId}/dispatch` | SHIPPER | 매칭 실행 + 배차 생성 (201, Location 헤더) |
| GET | `/dispatches` | DRIVER | **[신설]** 내(로그인한 기사) 배차 목록 조회 (최신순) |
| GET | `/dispatches/{id}` | 로그인 | 배차 조회 |
| POST | `/dispatches/{id}/accept` | DRIVER(배정된 기사만) | 배차 수락 |
| POST | `/dispatches/{id}/reject` | DRIVER(배정된 기사만) | 배차 거절 |
| PATCH | `/dispatches/{id}/status` | DRIVER(배정된 기사만) | 운송 상태 변경 (바디: `{ status: ShipmentStatus }`) |

```ts
type DispatchStatus = "PROPOSED" | "ACCEPTED" | "REJECTED" | "EXPIRED" | "CANCELED" | "COMPLETED";

type DispatchResponse = {
  id: number; shipmentRequestId: number; driverId: number;
  matchScore: number; fare: number; status: DispatchStatus; createdAt: string;
};

type DispatchStatusUpdateRequest = { status: ShipmentStatus };
```

### 3.7 데모 데이터 API (`/demo`, `demo` 프로파일에서만 활성)

| Method | Path | 설명 |
|---|---|---|
| GET | `/demo/state` | 초기 데모 상태(화주/기사/화물요청/배차) 조회 |

이 API는 로컬 데모용이므로 프론트엔드 정식 기능에는 포함하지 않고, 개발 중 참고용으로만 활용.

### 3.8 에러 응답 형식

`GlobalExceptionHandler` / `ErrorResponse` 기준, 모든 에러는 아래 형식으로 통일 응답된다.

```ts
type ErrorResponse = {
  timestamp: string;   // LocalDateTime
  status: number;       // 404, 409, 400, 401, 403 등
  error: string;        // HTTP status reason phrase (예: "Not Found")
  message: string;      // 검증 오류 시 "field: message, field2: message2" 형태로 join됨
  path: string;          // 요청 경로
};
```

주요 매핑:
- 404 (Not Found): 화주/기사/화물요청/배차 조회 실패
- 409 (Conflict): 상태 전이 불가, 매칭 기사 없음, 이메일 중복(가입)
- 400 (Bad Request): 입력값 검증 실패(`MethodArgumentNotValidException`), 잘못된 상태 타겟
- 401 (Unauthorized): 로그인 자격 증명 오류, 인증되지 않은 요청
- 403 (Forbidden): 배차/화물요청 소유자가 아닌 경우 접근

> 프론트에서는 `message` 필드를 그대로 사용자에게 노출 가능 (검증 오류는 필드명이 포함되어 있어 다소 기술적이므로, 필요 시 한글 매핑 테이블을 추가로 고려).

## 4. 화면 구성 (IA)

```
/                          랜딩 페이지 (서비스 소개, 로그인/가입 유도)
/login                     로그인 (화주/기사 탭 전환)
/signup                    회원가입 (화주/기사 탭 전환)

[화주 전용]
/shipper/dashboard         내 화물 요청 목록 + "새 요청" 버튼
/shipper/requests/new      화물 요청 생성 폼
/shipper/requests/[id]     화물 요청 상세 (상태, 배차 정보, 취소/배차요청 버튼)

[기사 전용]
/driver/dashboard          내게 배정된 배차(제안/진행 중) 목록
/driver/dispatches/[id]    배차 상세 (수락/거절, 운송 상태 변경 스텝퍼)
```

### 4.1 공통 레이아웃

- 상단 헤더: 로고/서비스명, 로그인 상태에 따라 "로그인/가입" 또는 "내 정보 · 로그아웃" 표시
- 역할(SHIPPER/DRIVER)에 따라 접근 가능한 대시보드로 리다이렉트
- 심플한 컬러 팔레트 (예: 흰 배경 + CJ 브랜드 느낌의 포인트 컬러 1~2가지, 회색 톤 텍스트), 카드 기반 리스트 UI

### 4.2 화면별 상세

**로그인/가입 (`/login`, `/signup`)**
- 화주/기사 토글 탭
- 가입 시 기사는 차량 정보(차종, 적재량) 입력 섹션 추가
- 성공 시 토큰 저장 → 역할별 대시보드로 이동

**화주 대시보드 (`/shipper/dashboard`)**
- 내 화물 요청 리스트 (상태 배지: 요청됨/매칭중/배차완료/운송중/완료/취소)
- "새 화물 요청" CTA 버튼

**화물 요청 생성 (`/shipper/requests/new`)**
- 출발지/도착지 입력
- 필요 차량 타입 선택 (드롭다운)
- 화물 항목 리스트 추가/삭제 (설명 + 무게kg)
- 제출 시 총 무게 자동 계산 표시(옵션)

**화물 요청 상세 (`/shipper/requests/[id]`)**
- 요청 정보, 화물 항목 목록
- 상태에 따라 액션 버튼 조건부 노출:
  - `REQUESTED`: "기사 매칭 요청" 버튼 → 배차 생성 API 호출
  - `REQUESTED`/`MATCHING`: "취소" 버튼
  - 배차 존재 시 배차 상태/기사 정보/운임(fare) 표시

**기사 대시보드 (`/driver/dashboard`)**
- 나에게 제안된 배차(`PROPOSED`) 목록 강조
- 진행 중 배차 목록

**배차 상세 (`/driver/dispatches/[id]`)**
- 배차 정보(화물 요청 연결 정보, 매칭 점수, 운임)
- `PROPOSED` 상태: 수락/거절 버튼
- `ACCEPTED` 이후: 운송 상태 변경 스텝퍼 (배차완료 → 픽업지 이동중 → 픽업완료 → 운송중 → 완료)

## 5. 폴더 구조 (제안)

```
view/
├── package.json
├── tsconfig.json
├── next.config.ts
├── tailwind.config.ts
├── .env.local.example        # NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
├── src/
│   ├── app/
│   │   ├── layout.tsx
│   │   ├── page.tsx                       # 랜딩
│   │   ├── login/page.tsx
│   │   ├── signup/page.tsx
│   │   ├── shipper/
│   │   │   ├── dashboard/page.tsx
│   │   │   └── requests/
│   │   │       ├── new/page.tsx
│   │   │       └── [id]/page.tsx
│   │   └── driver/
│   │       ├── dashboard/page.tsx
│   │       └── dispatches/[id]/page.tsx
│   ├── components/
│   │   ├── Header.tsx
│   │   ├── StatusBadge.tsx
│   │   ├── CargoItemForm.tsx
│   │   └── DispatchStepper.tsx
│   ├── contexts/
│   │   └── AuthContext.tsx               # 토큰/역할/profileId 전역 상태
│   ├── lib/
│   │   ├── api.ts                        # fetch 래퍼(Authorization 헤더 자동 첨부)
│   │   └── types.ts                       # 백엔드 DTO와 매칭되는 TS 타입 전체
│   └── styles/
│       └── globals.css
```

## 6. 인증/인가 흐름

1. 로그인 성공 시 `TokenResponse.accessToken`을 `localStorage`(예: `cj_access_token`)에 저장.
2. JWT payload는 `{ sub: email, role: "SHIPPER"|"DRIVER", profileId: number, iat, exp }` 구조 (`JwtTokenService` 확인 결과). 프론트에서 `jwt-decode` 라이브러리로 디코딩해 `role`, `profileId`, `email`을 AuthContext에 저장.
3. 모든 API 요청은 `lib/api.ts`에서 `Authorization: Bearer {token}` 헤더 자동 첨부.
4. 401 응답 시 로그인 페이지로 강제 이동 + 토큰 삭제.
5. 라우트 가드: 화주 전용/기사 전용 페이지는 클라이언트 사이드에서 역할 체크 후 접근 제한 (미들웨어는 추후 확장 가능).

## 7. 구현 우선순위 (마일스톤)

1. **백엔드 선행 작업** (완료, 8장 참고): CORS 설정, 화주/기사 목록 조회 API
2. **환경 셋업**: Next.js 프로젝트 생성, Tailwind 설정, `lib/api.ts` fetch 래퍼, 타입 정의
3. **인증 플로우**: 회원가입/로그인 화면 + AuthContext
4. **화주 플로우**: 대시보드 → 요청 생성 → 요청 상세 → 배차 요청/취소
5. **기사 플로우**: 대시보드 → 배차 상세 → 수락/거절/상태 변경
6. **다듬기**: 에러 처리 통일, 로딩/빈 상태 UI, 반응형 점검

## 8. 백엔드 선행 작업 — 완료

아래 3가지 작업을 `app` 프로젝트에 반영했다.

1. **CORS 설정 추가** ✅
   - `SecurityConfig`에 `CorsConfigurationSource` 빈을 추가하고 `http.cors(...)`로 필터 체인에 적용.
   - 허용 Origin은 `application.yml`의 `app.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS` 환경변수로 오버라이드 가능) 값으로 설정, 기본값 `http://localhost:3000`.
   - `Authorization`, `Content-Type` 헤더와 `GET/POST/PUT/PATCH/DELETE/OPTIONS` 메서드를 허용.

2. **화주 화물 요청 목록 조회 API 신설** ✅
   - `GET /shipment-requests` (SHIPPER 권한 필요) — **쿼리 파라미터로 shipperId를 받지 않고**, JWT의 `profileId`로 로그인한 화주 본인의 요청만 반환. 다른 화주의 목록을 조회할 수 없도록 설계.
   - `ShipmentRequestRepository.findByShipperIdOrderByCreatedAtDesc`, `ShipmentRequestService.getByShipper` 추가.
   - 응답: `ShipmentRequestResponse[]` (최신 생성순).

3. **기사 배차 목록 조회 API 신설** ✅
   - `GET /dispatches` (DRIVER 권한 필요) — 동일하게 JWT의 `profileId`로 로그인한 기사 본인에게 배정된 배차만 반환.
   - `DispatchRepository.findByDriverIdOrderByCreatedAtDesc`, `DispatchService.getByDriver` 추가.
   - 응답: `DispatchResponse[]` (최신 생성순).

> **설계 노트**: 최초 초안에서는 `?shipperId={id}` / `?driverId={id}` 쿼리 파라미터 방식을 고려했으나, 이 경우 로그인한 사용자가 자신의 ID가 아닌 임의의 ID를 쿼리에 넣어 다른 사용자의 데이터를 조회할 수 있는 IDOR(권한 상승) 취약점이 발생한다. 대신 인증 토큰의 `profileId` claim만을 기준으로 "내 목록"을 반환하도록 구현해, 기존 단건 조회의 소유권 검증(`verifyShipperOwnership`, `verifyDriverOwnership`)과 동일한 보안 수준을 유지했다.

**검증 결과**: `compileJava`, `compileTestJava` 성공. `test` 실행 시 74개 중 6개 실패가 있으나, 변경 전 원본 코드에서도 동일하게 실패하는 기존 결함(`ShipperControllerTest`, `DriverControllerTest`의 라우팅 404 문제, `DispatchServiceTest`의 매칭 로직 문제)으로 확인되어 이번 변경과는 무관함. 새로 추가한 목록 조회 로직에 대한 별도 테스트는 작성하지 않았으므로(요청 시 별도로 진행 가능), 실제 동작은 애플리케이션 기동 후 수동 확인이 필요하다.

이제 프론트엔드 구현에 필요한 API가 모두 준비되었으므로 Next.js 프로젝트 셋업으로 진행 가능하다.
