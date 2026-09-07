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


## 9. 에러 케이스 테스트 케이스 (TC)

> **사전 조건**: 현재 보안 설정상 보호 API에는 `Authorization: Bearer {token}` 헤더가 필요하다. 아래에서 `SHIPPER_A`, `SHIPPER_B`, `DRIVER_A`, `DRIVER_B`는 서로 다른 계정의 JWT를 뜻한다. 회원가입·로그인은 `POST /auth/shippers/signup`, `POST /auth/drivers/signup`, `POST /auth/shippers/login`, `POST /auth/drivers/login`을 사용한다.
>
> 공통 오류 응답(`@RestControllerAdvice` 적용 구간)은 다음 구조를 확인한다. `timestamp` 값은 실행 시점에 따라 달라진다.
>
> ```json
> {"timestamp":"...","status":400,"error":"Bad Request","message":"...","path":"/..."}
> ```

| TC ID | 구분 | 사전 조건 / 요청 | 기대 결과 |
|---|---|---|---|
| ERR-01 | 미인증 접근 | 헤더 없이 `POST /shipment-requests` 실행 | `401 Unauthorized`. 요청이 생성되지 않는다. |
| ERR-02 | 역할 권한 오류 | `DRIVER_A` 토큰으로 `POST /shipment-requests` 실행 | `403 Forbidden`. 요청이 생성되지 않는다. |
| ERR-03 | 역할 권한 오류 | `SHIPPER_A` 토큰으로 `POST /dispatches/{dispatchId}/accept` 실행 | `403 Forbidden`. 배차 상태가 `PROPOSED` 그대로다. |
| ERR-04 | 화주 입력 검증 | `SHIPPER_A` 토큰으로 `POST /shipment-requests`, `cargoItems: []` 전송 | `400 Bad Request`; `message`에 `cargoItems` 검증 오류가 포함되고 요청이 생성되지 않는다. |
| ERR-05 | 화주 입력 검증 | `SHIPPER_A` 토큰으로 아래처럼 품목 중량을 0 또는 음수로 전송 | `400 Bad Request`; `message`에 `cargoItems[0].weightKg` 검증 오류가 포함되고 요청이 생성되지 않는다. |
| ERR-06 | 화주 입력 검증 | `SHIPPER_A` 토큰으로 `originRegion`을 빈 문자열로 보내거나 `requiredVehicleType`을 생략 | `400 Bad Request`; 해당 필드의 검증 오류가 포함된다. |
| ERR-07 | 존재하지 않는 운송 요청 | `SHIPPER_A` 토큰으로 `POST /shipment-requests/999999/dispatch` 실행 | `404 Not Found`; `message`는 `ShipmentRequest not found: id=999999`. |
| ERR-08 | 매칭 후보 없음 | 가용 차주가 없거나, 모든 가용 차주의 차량 타입 또는 적재량이 요청과 맞지 않는 상태에서 배차 생성 | `409 Conflict`; `No matching driver...` 메시지. 운송 요청은 `REQUESTED` 상태를 유지한다. (`preferredRoutes`는 후보 제외 조건이 아니라 점수 가산 조건이다.) |
| ERR-09 | 중복 배차 생성 | 동일 `shipmentRequestId`로 배차 생성 후, 같은 요청에 다시 `POST /shipment-requests/{id}/dispatch` 실행 | `409 Conflict`; 두 번째 요청은 `MATCHING → MATCHING` 상태 전이 실패이며 추가 Dispatch가 생성되지 않는다. |
| ERR-10 | 타 화주의 운송 요청 접근 | `SHIPPER_A`가 만든 요청을 `SHIPPER_B` 토큰으로 `GET /shipment-requests/{id}` 또는 `POST /shipment-requests/{id}/cancel` 실행 | `403 Forbidden`; `message`는 `운송 요청에 접근할 권한이 없습니다: {id}`. 상태가 변경되지 않는다. |
| ERR-11 | 존재하지 않는 배차 | `DRIVER_A` 토큰으로 `POST /dispatches/999999/accept` 실행 | `404 Not Found`; `message`는 `Dispatch not found: id=999999`. |
| ERR-12 | 타 차주의 배차 조작 | `DRIVER_A`에게 배정된 `dispatchId`를 `DRIVER_B` 토큰으로 수락·거절·상태 변경 | `403 Forbidden`; `message`는 `배차를 조작할 권한이 없습니다: {id}`. 배차 및 운송 요청 상태가 유지된다. |
| ERR-13 | 중복 수락 | `DRIVER_A`로 배차를 한 번 수락한 뒤 같은 `POST /dispatches/{id}/accept`를 재실행 | `409 Conflict`; 배차는 `ACCEPTED`, 요청은 `DISPATCHED`, 차주는 `BUSY`를 유지하고 Outbox 이벤트가 추가 저장되지 않는다. |
| ERR-14 | 잘못된 상태 순서 | 수락된 배차에서 `PATCH /dispatches/{id}/status`에 `{"status":"PICKED_UP"}` 전송 | `409 Conflict`; 현재 요청 상태가 `DISPATCHED`이므로 `PICKED_UP`으로 전이되지 않는다. |
| ERR-15 | 허용되지 않는 상태 목표 | 수락된 배차에서 `PATCH /dispatches/{id}/status`에 `{"status":"CANCELED"}` 전송 | `400 Bad Request`; `IllegalStatusTargetException` 오류. 상태가 변경되지 않는다. |
| ERR-16 | 완료 후 재변경 | 정상 완료된 배차에 다시 `PATCH /dispatches/{id}/status` 실행 | `409 Conflict`; Dispatch는 `COMPLETED`, ShipmentRequest는 `COMPLETED`, Driver는 `AVAILABLE`을 유지한다. 추가 완료 이벤트는 발행되지 않는다. |
| ERR-17 | 로그인 실패 | 잘못된 비밀번호로 `POST /auth/shippers/login` 또는 `POST /auth/drivers/login` 실행 | `401 Unauthorized`; 토큰이 발급되지 않는다. |
| ERR-18 | 이메일 중복 가입 | 같은 이메일로 화주 또는 차주 회원가입을 두 번 실행 | `409 Conflict`; 두 번째 계정이 생성되지 않는다. |

### ERR-04 ~ ERR-06 요청 예시

`POST /shipment-requests` (`Authorization: Bearer {SHIPPER_A}`)

```json
{
  "shipperId": 1,
  "originRegion": "서울",
  "destinationRegion": "부산",
  "cargoItems": [{"description":"테스트 화물","weightKg":0}],
  "requiredVehicleType": "TRUCK_1T"
}
```

### ERR-14 재현 순서

1. 5단계까지 정상 수행해 배차를 `ACCEPTED`, 운송 요청을 `DISPATCHED`로 만든다.
2. `DRIVER_A` 토큰으로 아래 요청을 보낸다.

```json
{"status":"PICKED_UP"}
```

3. `409 Conflict`와 상태 전이 오류 메시지를 확인한다.
4. `GET /dispatches/{id}` 및 해당 운송 요청 조회로 배차 `ACCEPTED`, 운송 요청 `DISPATCHED`가 유지되는지 확인한다.

### 구현 확인 필요 항목: 활성 배차 동시성 오류

동시에 두 운송 요청이 같은 차주를 후보로 선택하는 경합에서는 서비스가 `DriverAlreadyAssignedException`을 발생시킨다. 그러나 현재 `GlobalExceptionHandler`에 이 예외가 등록되어 있지 않아, 현 구현의 실제 응답은 **500 Internal Server Error**가 될 수 있다. 의도한 계약은 `409 Conflict`이며, 아래 TC는 해당 보완 후 검증한다.

| TC ID | 사전 조건 / 요청 | 기대 결과(보완 후) |
|---|---|---|
| ERR-19 | 동일 차주만 후보가 되도록 만든 서로 다른 운송 요청 2건에 대해 `POST /shipment-requests/{id}/dispatch`를 동시에 실행 | 한 요청만 `201 Created`; 나머지는 `409 Conflict`, 활성 배차는 차주당 1건만 존재한다. |

> 보완 방법: `GlobalExceptionHandler.handleConflict(...)`의 예외 목록에 `DriverAlreadyAssignedException.class`를 추가한다.
