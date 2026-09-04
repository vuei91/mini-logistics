# CJ대한통운 '더운반' 미니프로젝트 기능 체크리스트

> 원본: [CJ대한통운_미니프로젝트_설계가이드.md](CJ대한통운_미니프로젝트_설계가이드.md)
> 우선순위 표기: **P1** 필수 · **P2** 차별화 · **P3** 여유 있을 때
> **스코프 조정**:
> - 정산(Settlement) 모듈은 **P3 후순위**. 엔드투엔드 흐름은 운송 완료(`COMPLETED`)까지 우선 구현.
> - 실시간 위치 트래킹(SSE + Redis)도 **P3 후순위**. 백엔드 차별화 포인트로 여유 시 확장.

---

## 1. 도메인 모델링 (P1)

- [ ] `Shipper` (화주) 엔티티 정의
- [ ] `Driver` (차주) 엔티티 정의 + `preferredRoutes` 선호구간 속성
- [ ] `Vehicle` (차량) 엔티티 정의 (차량 타입 포함)
- [x] `ShipmentRequest` (운송요청) 엔티티: 출발지/도착지/요구 차량타입/상태
- [x] `CargoItem` (화물 품목) 엔티티: `ShipmentRequest 1 : N`, 설명·중량·총중량 계산
- [x] 총 화물중량과 차량 적재량을 비교하는 매칭 필터
- [x] `Dispatch` (배차) 엔티티: 매칭된 화주-차주-운송 정보
- [ ] `Settlement` (정산) 엔티티: 운임/수수료/정산상태 · **P3**
- [ ] 운송 상태 enum: `REQUESTED → MATCHING → DISPATCHED → EN_ROUTE_TO_PICKUP → PICKED_UP → IN_TRANSIT → COMPLETED / CANCELED`
- [ ] 픽업 이벤트 처리: 픽업지 이동 시작 / 상차 완료 상태 전이 API
- [ ] 정산 상태 enum: `PENDING → CONFIRMED → PAID` · **P3**
- [ ] 상태 전이 로직을 엔티티 내부 메서드로 캡슐화 (DDD 스타일)
- [ ] 잘못된 상태 전이 시 예외 발생 처리

---

## 2. REST API (P1)

- [ ] 운송 요청 등록 API: `POST /shipment-requests`
- [ ] 운송 요청 조회 API: `GET /shipment-requests/{id}`
- [ ] 차주 배차 목록 조회 API: `GET /drivers/{id}/dispatches`
- [ ] 배차 수락 API: `POST /dispatches/{id}/accept`
- [ ] 배차 거절 API: `POST /dispatches/{id}/reject`
- [ ] 운송 상태 변경 API: `PATCH /dispatches/{id}/status`
- [ ] 정산 내역 조회 API: `GET /settlements/{id}` · **P3**
- [ ] 요청/응답 DTO 분리
- [ ] Bean Validation 적용 (`@NotNull`, `@Size` 등)
- [ ] 공통 예외 핸들러 (`@RestControllerAdvice`) 로 에러 응답 표준화
- [ ] Swagger/OpenAPI 문서화 (springdoc-openapi)

---

## 3. 매칭 / 배차 알고리즘 (P1)

- [ ] `MatchingStrategy` 인터페이스 정의 (전략 패턴)
- [ ] 기본 필터링 구현: 지역 / 차량타입 / 가용시간
- [ ] 거리 기반 스코어링 확장
- [ ] 차주 선호구간(`preferredRoutes`) 가중치 반영
- [ ] `DispatchService` 로 배차 로직 분리
- [ ] 매칭 점수 계산 결과를 로그/응답에 포함 (검증 용이성)
- [ ] 매칭 알고리즘 단위 테스트 (경계값, 동률, 후보 없음)

---

## 4. 테스트 코드 (P1)

- [ ] JUnit5 + Mockito 세팅
- [ ] 매칭 알고리즘 단위 테스트
- [ ] 상태 전이 로직 단위 테스트 (정상 흐름 + 잘못된 전이)
- [ ] 운임 산정 로직 단위 테스트 (가중치별 케이스)
- [ ] 서비스 계층 Mock 기반 단위 테스트
- [ ] 컨트롤러 통합 테스트 (`@SpringBootTest` 또는 `@WebMvcTest`)

---

## 5. 비동기 / 이벤트 처리 - RabbitMQ (P2)

- [x] RabbitMQ 연결 설정 (Spring AMQP)
- [x] `DispatchConfirmedEvent` 도메인 이벤트 정의
- [x] `ShipmentCompletedEvent` 도메인 이벤트 정의
- [x] Exchange / Queue / Binding 설정 (Java Config)
- [x] 배차 확정 시 이벤트 발행 Publisher
- [x] 알림 Consumer (로그 출력 수준으로 충분)
- [ ] 정산 Consumer (운송 완료 이벤트 수신 → Settlement 생성) · **P3**
- [x] Transactional Outbox 패턴 적용 (트랜잭션-발행 원자성)
- [x] Consumer 실패 시 재처리 / DLQ 설정

---

## 6-A. 실시간 위치 트래킹 (P3 후순위)

> 초기 스코프에서 제외. 픽업/운송 중 화주가 차주 현재 위치를 실시간 확인. 백엔드 차별화 포인트(SSE + Redis GEO + 이벤트 스트리밍).

- [ ] 위치 수집 API: `POST /dispatches/{id}/location` (좌표 + timestamp)
- [ ] Redis 현재 위치 저장 (Key: `dispatch:{id}:location`, TTL 30초)
- [ ] Redis GEO 명령 활용 (선택): 반경 검색으로 근접 차주 조회 확장 여지
- [ ] SSE 스트림 엔드포인트: `GET /dispatches/{id}/location/stream`
- [ ] Spring `SseEmitter` 또는 WebFlux `Flux<ServerSentEvent>` 구현
- [ ] 화주 인가 (JWT/세션 → 해당 배차 화주만 구독 허용)
- [ ] 레이트 리미팅: 최소 인터벌 5초 or 최소 이동거리 20m 임계값
- [ ] 스트림 브로드캐스트 단위 테스트 (좌표 mock)

## 6-B. 정산 모듈 (P3 후순위)

> 초기 스코프에서 제외. P1/P2 완주 후 시간 여유가 있을 때 착수.

- [ ] `COMPLETED` 이벤트 수신 → Settlement 자동 생성
- [ ] 운임 / 수수료 계산 로직
- [ ] 정산 상태 전이 API (`PENDING → CONFIRMED → PAID`)
- [ ] 정산 조회 API (차주별 / 기간별)
- [ ] 정산 생성 단위 테스트

---

## 7. 동적 프라이싱 엔진 (P3)

- [ ] `PricingStrategy` 인터페이스 정의
- [ ] 기본 운임 계산: `거리 × 단가`
- [ ] 시간대 가중치 (피크/오프피크 배율 테이블)
- [ ] 노선 수요 가중치 (Redis 카운터 또는 DB 집계)
- [ ] 계절 가중치 (월별 설정 테이블)
- [ ] 최종 계산식 조립: `기본운임 × 시간대 × 노선수요 × 계절`
- [ ] 프라이싱 결과 API 응답에 상세 breakdown 포함
- [ ] 각 가중치별 단위 테스트

---

## 8. 데이터베이스 / 동시성 (P1~P2)

- [ ] PostgreSQL 스키마 설계 (ERD 문서 포함)
- [ ] JPA/Hibernate 엔티티 매핑
- [ ] 지역 / 상태 컬럼 인덱스 설계
- [ ] N+1 문제 방지 (fetch join, `@BatchSize`)
- [ ] 중복 배차 방지: 비관적 락 (`PESSIMISTIC_WRITE`) 또는 unique 제약
- [ ] Flyway / Liquibase 로 마이그레이션 관리
- [ ] 동시성 테스트 (같은 차주 2건 동시 배차 시도 → 1건만 성공)

---

## 9. 인프라 / 운영 (P3)

- [ ] Docker Compose: App + PostgreSQL + RabbitMQ + Redis
- [ ] Redis 캐싱: 가용 차주 목록
- [ ] Redis 분산 락: 중복 배차 방지 (Redisson)
- [ ] `application.yml` 프로파일 분리 (local / dev / prod)
- [ ] GitHub Actions CI (빌드 + 테스트)
- [ ] AWS 배포 (EC2/ECS) 또는 K8S manifest 작성

---

## 10. 문서 / 포트폴리오 자산 (P1)

- [ ] README: **공고 요구사항 → 구현 매핑표**
- [ ] ERD 다이어그램
- [ ] 아키텍처 다이어그램 (컴포넌트 / 이벤트 흐름)
- [ ] Swagger API 명세 캡처 또는 링크
- [ ] 매칭 알고리즘 설계 트레이드오프 문서화
- [ ] 동시성 / 이벤트 처리 문제 해결 과정 기록
- [ ] 실행 방법 (docker-compose up 예시 포함)

---

## 진행 순서 권장

1. **1주차**: 도메인 모델 + REST API + 매칭/배차 로직 + 테스트 (P1 완주)
2. **2주차**: RabbitMQ 이벤트(알림 Consumer) + 동시성 처리 + 동적 프라이싱 (P2 차별화)
3. **3주차**: Redis + Docker + 문서화, 여유 있으면 **실시간 트래킹(SSE)** → **정산 모듈** 순으로 확장 (P3)
