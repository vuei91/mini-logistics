# CJ Logistics Mini — 백엔드 포트폴리오

> **화주-차주 매칭·배차**를 축소 구현하며 **이벤트 기반 아키텍처(RabbitMQ), 동시성 제어, 트랜잭션 일관성, AWS 클라우드 배포**를 직접 다룬 사이드 프로젝트입니다.

- 라이브 데모: https://d1igl0x2mkyq92.cloudfront.net
- 데모 계정 — 화주 `a@yopmail.com` / `1q2w3e4r!` · 기사 `c@yopmail.com` / `1q2w3e4r!`

---

## 한눈에 보기

| | |
|---|---|
| **도메인** | 물류 화주-차주 매칭 및 배차 (수요응답형 온디맨드 매칭 성격) |
| **역할** | 백엔드 설계·구현 및 AWS 인프라 구축 (개인 프로젝트) |
| **핵심 스택** | Java 17 · Spring Boot · Spring Security(JWT) · JPA · **RabbitMQ** · **PostgreSQL** |
| **인프라** | AWS EC2 · RDS · S3 · CloudFront · ECR (Docker) |
| **테스트** | JUnit5 · Mockito · **Testcontainers** · 80+ 테스트 |

---

## 이 프로젝트가 다루는 문제

물류 배차는 **화주의 운송 요청**과 **가용 차주**를 조건(차량 종류·적재량·선호 노선)에 맞춰 매칭하고, 배차 확정부터 운송 완료까지의 상태를 추적하며 관련 이벤트를 비동기로 전파해야 하는 도메인입니다. 이 과정에서 실무에서 마주치는 세 가지 문제를 작은 스케일로 직접 구현·검증했습니다.

- **동시성** — 한 차주가 동시 요청으로 여러 배차에 중복 배정되면 안 됨
- **상태 전이** — 요청 → 매칭 → 배차 → 운송 → 완료의 단방향 흐름 보장
- **비동기 이벤트 일관성** — 배차 확정/운송 완료 이벤트를 유실 없이 전파

---

## 기술적으로 집중한 것

### 1. Outbox 패턴으로 DB–메시지 일관성 보장
배차 확정·운송 완료 시 RabbitMQ로 이벤트를 발행하는데, "DB 커밋은 됐는데 메시지 발행은 실패" 같은 **이중 쓰기 불일치**를 막아야 했습니다.

- 비즈니스 트랜잭션 안에서 이벤트를 `outbox_events` 테이블에 `PENDING`으로 함께 저장 → 원자성 확보
- 별도 스케줄러(`OutboxEventRelay`)가 PENDING 이벤트를 폴링해 발행하고 `PUBLISHED`로 전이
- Topic Exchange + **DLX/DLQ**로 소비 실패 메시지를 격리
- **Testcontainers로 실제 RabbitMQ를 띄워** 정상 발행·소비, Outbox 내구성, DLQ 이동까지 통합 테스트로 검증

### 2. 배차 동시성 제어
같은 기사가 동시 요청으로 중복 배차되는 경쟁 상태를 방어했습니다.

- 배차 확정 시 기사 레코드를 **비관적 락(`SELECT ... FOR UPDATE`)** 으로 조회
- 진행 중 배차(`PROPOSED`/`ACCEPTED`) 존재 여부를 재검증해 `DriverAlreadyAssignedException` 발생

### 3. 리치 도메인 모델 + 상태 머신
상태 전이를 서비스가 아닌 **엔티티 메서드에 캡슐화**하고, 허용되지 않은 전이는 예외로 차단했습니다. 상태 규칙이 도메인 객체 안에 응집되어 일관성이 깨질 여지를 줄였습니다.

### 4. 전략 패턴 기반 매칭
`MatchingStrategy` 인터페이스로 매칭 알고리즘을 분리해, 차량/적재량 필터링과 점수 계산(기본점 + 선호노선 가산)을 교체 가능한 구조로 만들었습니다.

### 5. 보안 이중 방어
- URL·HTTP 메서드별 역할 기반 접근제어(`SecurityConfig`)
- 서비스 계층에서 **리소스 소유권 검증** — 같은 역할이라도 남의 리소스는 접근 불가

---

## 트러블슈팅 (배포 과정에서 실제 해결한 문제)

프론트를 S3+CloudFront, API를 EC2로 분리 배포하면서 마주친 문제들을 직접 진단·해결했습니다.

| 문제 | 원인 | 해결 |
|---|---|---|
| API 응답이 JSON이 아닌 HTML로 옴 | CloudFront 커스텀 에러 응답(403/404→index.html)이 **전역 적용**되어 백엔드 정상 에러까지 가로챔 | 커스텀 에러 응답 제거 + `/api/*`는 통과시키는 CloudFront Function으로 SPA 폴백 대체 |
| 브라우저에서 403 `Invalid CORS request` | 백엔드 허용 Origin이 CloudFront 도메인과 불일치 | `CORS_ALLOWED_ORIGINS`를 CloudFront 도메인으로 교정 |
| 상세 페이지 `AccessDenied` | 정적 export는 빌드 시점에 없는 동적 경로(`/requests/[id]`)의 파일이 없음 | **동적 라우트를 쿼리스트링(`?id=`) 방식으로 재설계** — 인프라 특수 규칙 없이 근본 해결 |
| CloudFront가 오리진 접근 실패(504) | 보안그룹이 특정 IP만 허용해 CloudFront 엣지 차단 | 8080 인바운드를 CloudFront **관리형 prefix list**로 제한 허용 |

> 단순 우회 대신, "정적 호스팅에서 동적 라우트를 어떻게 다룰 것인가"라는 **근본 원인**을 파고들어 쿼리스트링 설계로 전환한 점이 이 과정의 핵심이었습니다.

---

## API에 `/api` 프리픽스를 코드로 일괄 부여

CloudFront에서 `/api/*`만 백엔드로 라우팅하기 위해 모든 컨트롤러에 프리픽스가 필요했습니다. 컨트롤러마다 `@RequestMapping`을 고치는 대신, `WebMvcConfigurer.configurePathMatch`로 **애플리케이션 패키지의 `@RestController`에만** 프리픽스를 자동 적용했습니다(springdoc 문서 엔드포인트는 제외). 설정 한 곳으로 전역 라우팅 규칙을 통제한 사례입니다.

---

## 배포 파이프라인

```
로컬: docker build → ECR Public push
EC2:  docker compose pull → up   (빌드 없이 이미지 실행, Spring Boot + RabbitMQ)
DB:   RDS PostgreSQL
프론트: next build(정적 export) → S3 sync → CloudFront invalidation
```

EC2에서 직접 빌드하던 방식을 **ECR 이미지 pull 방식으로 전환**해 EC2 부하와 배포 시간을 줄였습니다.

---

## 배운 점

- 메시징에서 "발행했다고 믿는 것"과 "실제로 일관되게 발행되는 것"은 다르며, Outbox 패턴이 그 간극을 메운다는 것을 코드와 테스트로 체득했습니다.
- 프론트/백엔드 분리 배포에서 CORS·Mixed Content·SPA 라우팅은 **어느 계층에서 푸느냐**가 설계의 핵심이라는 것을 배웠습니다.
- 문제를 우회로 덮기보다 원인을 규명하고 구조를 바꾸는 편이 장기적으로 단순하다는 것을 배포 트러블슈팅에서 확인했습니다.

---

## 직무 역량 매핑

물류 플랫폼(화주/차주 매칭·배차) 백엔드 직무에서 요구되는 역량을 이 프로젝트로 어떻게 다뤘는지 정리했습니다.

| 요구 역량 | 프로젝트에서 다룬 내용 |
|---|---|
| 화주/차주 매칭·배차 알고리즘 | 차량 종류·적재량 필터링 + 선호노선 가산 점수화 매칭(`MatchingStrategy` 전략 패턴), 배차 생성/수락/거절/운송상태 진행 |
| Java · Spring(Boot/JPA) 서버 개발 | Spring Boot + JPA 기반 계층형 설계, 리치 도메인 모델(상태 전이 캡슐화) |
| RESTful API 설계 | 역할 기반 REST API, `WebMvcConfigurer`로 `/api` 전역 라우팅 통제, springdoc(OpenAPI) 문서화 |
| PostgreSQL / RDBMS 모델링 | RDS PostgreSQL 운영, 엔티티·인덱스 설계, 비관적 락 활용 쿼리 |
| RabbitMQ 비동기 메시징·이벤트 처리 | Outbox 패턴 + Topic Exchange + DLX/DLQ, 스케줄러 릴레이, Testcontainers 통합 검증 |
| AWS·Docker 컨테이너 운영 | EC2(Docker Compose) · RDS · S3 · CloudFront · ECR 직접 구축·배포 |
| 테스트 코드(JUnit/Mockito) | 80+ 테스트 — 단위·컨트롤러 슬라이스·통합·메시징(Testcontainers) |

---

## 확장 방향 (진행/계획)

이 프로젝트를 실무 요구 수준으로 끌어올린다면 다음을 다룰 계획입니다. (현재 미구현 항목을 정직하게 기록)

- **런타임 상향**: 현재 Java 17 / Spring Boot 4 → 팀 표준(JDK 21 / Spring Boot 3.x)에 맞춘 정렬
- **컨테이너 오케스트레이션**: 현재 EC2 + Docker Compose → **Kubernetes(EKS)** 로 이전, 무중단 배포
- **캐시/조회 성능**: 매칭 후보·알림 조회에 **Redis** 도입
- **관측성**: 구조적 로깅·메트릭·트레이싱(분산 추적)으로 배차 파이프라인 모니터링

---

## 화면

| 랜딩 | 로그인 | 화주 대시보드 |
|---|---|---|
| ![landing](./images/01-landing.png) | ![login](./images/02-login.png) | ![shipper](./images/04-shipper-dashboard.png) |

| 화물 요청 생성 | 기사 대시보드 | 알림 |
|---|---|---|
| ![new](./images/05-shipper-request-new.png) | ![driver](./images/07-driver-dashboard.png) | ![noti](./images/06-shipper-notifications.png) |

---

## 더 보기

- 상세 구현 문서: [IMPLEMENTATION.md](./IMPLEMENTATION.md)
- 라이브 데모: https://d1igl0x2mkyq92.cloudfront.net
