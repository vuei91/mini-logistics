# Swagger + RabbitMQ 검증 시나리오

## 목표
Swagger UI에서 화주·차주 등록부터 배차 확정과 운송 완료까지 수행하고, Transactional Outbox를 거쳐 RabbitMQ 이벤트가 소비되는지 확인한다.

## 0. 실행 준비

### RabbitMQ 실행

```cmd
docker run -d --name cj-rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management
```

- 관리 화면: `http://localhost:15672`
- 로그인: `guest` / `guest`

### 애플리케이션 실행

`app` 폴더에서 실행한다.

```cmd
gradlew.bat bootRun --args="--spring.profiles.active=rabbitmq"
```

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- RabbitMQ 프로필을 켜야 Exchange, Queue, Consumer, Outbox 릴레이가 활성화된다.

## 1. 화주 등록

Swagger에서 `POST /shippers`를 실행한다.

```json
{"name":"CJ 화주","phone":"010-1111-2222"}
```

- 기대 결과: `201 Created`, 응답 `id`를 `shipperId`로 사용한다. (예: `1`)

## 2. 차주 등록

Swagger에서 `POST /drivers`를 실행한다.

```json
{"name":"김운전","phone":"010-3333-4444","vehicle":{"vehicleType":"TRUCK_1T","capacityKg":1000},"preferredRoutes":[{"originRegion":"서울","destinationRegion":"부산"}]}
```

- 기대 결과: `201 Created`, 차주 상태 `AVAILABLE`.

## 3. 운송 요청 등록

Swagger에서 `POST /shipment-requests`를 실행한다.

```json
{"shipperId":1,"originRegion":"서울","destinationRegion":"부산","cargoItems":[{"description":"전자제품 6박스","weightKg":300},{"description":"전자제품 4박스","weightKg":200}],"requiredVehicleType":"TRUCK_1T"}
```

- `cargoItems`는 비어 있을 수 없고, 각 품목의 `description`과 양수 `weightKg`가 필요하다.
- 기대 결과: `201 Created`, 운송 요청 상태 `REQUESTED`, 응답의 `totalCargoWeightKg`는 `500`.
- 응답 `id`를 `shipmentRequestId`로 사용한다. (예: `1`)

## 4. 배차 매칭 생성

Swagger에서 `POST /shipment-requests/{shipmentRequestId}/dispatch`를 실행한다.

- Path Variable: `shipmentRequestId = 1`
- 기대 결과: 운송 요청 `REQUESTED → MATCHING`, 배차 상태 `PROPOSED`.
- 응답 `id`를 `dispatchId`로 사용한다. (예: `1`)

## 5. 배차 수락과 RabbitMQ 이벤트 확인

Swagger에서 `POST /dispatches/{id}/accept`를 실행한다.

- Path Variable: `id = 1`
- 상태 변경: 배차 `PROPOSED → ACCEPTED`, 운송 요청 `MATCHING → DISPATCHED`, 차주 `AVAILABLE → BUSY`.
- Outbox: `DispatchConfirmedEvent`가 `outbox_events`에 `PENDING`으로 저장된 후 최대 약 1초 내 `PUBLISHED`로 변경된다.
- RabbitMQ: `dispatch.events` Exchange → `dispatch.confirmed` Routing Key → `dispatch.notification` Queue.
- 앱 로그: `Dispatch notification received: { ... }`

RabbitMQ 관리 화면의 **Exchanges → `dispatch.events`**, **Queues and Streams → `dispatch.notification`**에서 확인한다. Consumer가 즉시 처리하면 Ready 메시지는 `0`일 수 있으므로 앱 로그도 확인한다.

## 6. 픽업과 운송 상태 변경

Swagger에서 `PATCH /dispatches/{id}/status`를 아래 순서대로 실행한다. Path Variable은 모두 `id = 1`이다.

```json
{"status":"EN_ROUTE_TO_PICKUP"}
```

```json
{"status":"PICKED_UP"}
```

```json
{"status":"IN_TRANSIT"}
```

- 기대 상태: `DISPATCHED → EN_ROUTE_TO_PICKUP → PICKED_UP → IN_TRANSIT`.

## 7. 운송 완료와 RabbitMQ 이벤트 확인

Swagger에서 `PATCH /dispatches/{id}/status`를 실행한다.

```json
{"status":"COMPLETED"}
```

- 상태 변경: 운송 요청 `IN_TRANSIT → COMPLETED`, 배차 `ACCEPTED → COMPLETED`, 차주 `BUSY → AVAILABLE`.
- Outbox: `ShipmentCompletedEvent`가 저장·발행된다.
- RabbitMQ: `dispatch.events` Exchange → `shipment.completed` Routing Key → `dispatch.notification` Queue.
- 앱 로그: `Dispatch notification received: { ... }`

## 8. DLQ 검증 (선택)

`DispatchNotificationConsumer.consume()`에 일시적으로 `throw new IllegalStateException("DLQ 검증용 강제 실패");`를 추가하고 5단계 또는 7단계를 다시 실행한다.

- 기대 결과: 최대 3회 재시도 후 `dispatch.notification.dlq`로 이동한다.
- RabbitMQ 관리 화면의 **Queues and Streams → `dispatch.notification.dlq`**에서 메시지를 확인한다.
- 검증 후 강제 예외 코드는 반드시 제거한다.
