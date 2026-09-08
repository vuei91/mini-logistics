# RabbitMQ 메시징 검증 가이드

CJ Logistics Mini 의 배차 알림 메시징은 **Transactional Outbox + RabbitMQ(Topic Exchange) + DLQ** 구조로 되어 있다.
이 문서는 두 가지 방식으로 검증하는 방법을 정리한다.

- 자동화 검증: Testcontainers 통합 테스트 (`RabbitMqMessagingIntegrationTest`)
- 수동 검증: 로컬 Docker RabbitMQ + 관리 UI

## 아키텍처 개요

```
[배차 확정 / 운송 완료 API]
        │  (같은 DB 트랜잭션)
        ▼
OutboxEventStore.store()  ──► outbox_events 테이블 (status=PENDING)
        │
        ▼
OutboxEventRelay (@Scheduled, 기본 1초)
        │  PENDING 조회 → RabbitTemplate.convertAndSend → status=PUBLISHED
        ▼
TopicExchange "dispatch.events"
        │  routingKey: dispatch.confirmed / shipment.completed
        ▼
Queue "dispatch.notification" ── 소비 실패(재시도 소진) ──► DLX "dispatch.dlx" ──► DLQ "dispatch.notification.dlq"
        ▼
DispatchNotificationConsumer (@RabbitListener) → 로그 출력
```

핵심 설계 포인트:

1. **Transactional Outbox** — 비즈니스 데이터와 이벤트가 하나의 트랜잭션에 저장되어 메시지 유실이 없다.
2. **재시도 + DLQ** — 컨슈머 실패 시 재시도 후 죽은 편지 큐로 격리된다. (`application.yml` 의 `spring.rabbitmq.listener.simple.retry`)
3. **at-least-once 전달** — 릴레이가 발행 후 PUBLISHED 로 마킹한다.

메시징 관련 빈은 `app.messaging.enabled=true` 일 때만 활성화된다. (`@ConditionalOnProperty`)

---

## 1. 자동화 검증 (Testcontainers)

실제 RabbitMQ 컨테이너를 띄워 파이프라인을 검증한다. Docker 데몬이 실행 중이어야 한다.

검증 시나리오 (`app/src/test/.../event/RabbitMqMessagingIntegrationTest.java`):

| 테스트 | 검증 내용 |
| --- | --- |
| `happyPath_outboxEvent_isPublishedAndConsumed` | PENDING 이벤트가 릴레이에 의해 발행되고 PUBLISHED 로 전이된다 |
| `outboxDurability_pendingEvent_transitionsToPublished` | 저장된 이벤트가 올바른 routingKey 로 발행된다 |
| `deadLetter_whenConsumerAlwaysFails_messageMovesToDlq` | 컨슈머가 반복 실패하면 메시지가 DLQ 로 이동한다 |

### 실행

```powershell
cd app
$env:DOCKER_HOST='npipe:////./pipe/dockerDesktopLinuxEngine'   # Docker Desktop(desktop-linux context) 사용 시
.\gradlew.bat test --tests "com.cjlogistics.mini.dispatch.event.RabbitMqMessagingIntegrationTest"
```

### Docker Engine 29+ 호환성 주의

Docker Engine 29 는 최소 API 버전 1.44 를 요구한다. Testcontainers 가 사용하는 docker-java 클라이언트는
기본으로 1.32 를 협상해 `HTTP 400 (Could not find a valid Docker environment)` 으로 실패한다.
이를 위해 `build.gradle` 의 test 태스크에 `systemProperty 'api.version', '1.44'` 를 지정해 두었다.
(참고: testcontainers-java 이슈 #11235)

---

## 2. 수동 검증 (Docker + 관리 UI)

### 2-1. RabbitMQ 기동

```powershell
docker compose up -d
```

- AMQP 포트: `5672`
- 관리 UI: http://localhost:15672 (계정: `guest` / `guest`)

### 2-2. 애플리케이션 실행 (rabbitmq 프로파일)

```powershell
cd app
.\gradlew.bat bootRun --args='--spring.profiles.active=rabbitmq'
```

`rabbitmq` 프로파일은 `app.messaging.enabled=true` 를 켜고 outbox 릴레이 주기를 설정한다.

### 2-3. 정상 흐름 확인

1. 배차 확정 API 를 호출한다. (배차 생성 → 확정 흐름은 `DispatchController` 참고)
2. **H2 콘솔**(http://localhost:8080/h2-console) 에서 `outbox_events` 조회 →
   `STATUS` 가 `PENDING` 에서 `PUBLISHED` 로 바뀌는지 확인한다.
3. **관리 UI → Queues → `dispatch.notification`** 에서 메시지 유입/소비를 확인한다.
4. 애플리케이션 로그에서 `Dispatch notification received: {...}` 를 확인한다.

### 2-4. Outbox 내구성 확인 (메시지 유실 없음)

1. `docker compose stop rabbitmq` 로 브로커를 내린다.
2. 배차 확정 API 를 호출한다 → `outbox_events` 에 `PENDING` 으로 쌓인다. (앱은 정상 동작)
3. `docker compose start rabbitmq` 로 다시 올린다.
4. 릴레이가 밀린 이벤트를 자동 발행해 모두 `PUBLISHED` 로 전이되는지 확인한다.

### 2-5. DLQ 동작 확인

컨슈머는 페이로드에 `FORCE_FAIL` 마커가 포함되면 강제로 예외를 던진다. (DLQ 검증용 테스트 훅)

관리 UI 의 `dispatch.events` exchange 에서 다음을 publish:

- Routing key: `dispatch.confirmed`
- Payload: `{"marker":"FORCE_FAIL"}`

재시도(`max-attempts: 3`) 소진 후 메시지가 `dispatch.notification.dlq` 로 이동하는 것을 관리 UI 에서 확인한다.

### 2-6. 정리

```powershell
docker compose down
```

---

## 참고: 실제 코드에 추가된 테스트 훅

`DispatchNotificationConsumer` 에는 DLQ 경로 검증을 위해 다음 훅이 있다.

```java
static final String FORCE_FAIL_MARKER = "FORCE_FAIL";
// payload 에 이 마커가 포함되면 소비를 강제 실패시킨다 (재시도 → DLQ 검증용).
```

정상 페이로드(배차/운송 이벤트 JSON)에는 이 마커가 없으므로 운영 동작에는 영향이 없다.
