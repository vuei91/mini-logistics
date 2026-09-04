# CJ대한통운 '더운반' 미니프로젝트 시각화

> 원본: [CJ대한통운_미니프로젝트_설계가이드.md](CJ대한통운_미니프로젝트_설계가이드.md)
> Mermaid 렌더링: VS Code Mermaid Preview 확장 또는 https://mermaid.live/
> **스코프 조정**: 정산(Settlement) + 실시간 위치 트래킹(Tracking)은 **P3 후순위**로 표시 (점선/노트로 구분).

---

## 1. 시스템 아키텍처 (전체 컴포넌트)

가이드 §3.1~§3.9 에서 언급된 컴포넌트를 4계층으로 배치.

```mermaid
flowchart LR
    subgraph Client["Client Layer"]
        Shipper[화주 App]
        Driver[차주 App]
    end

    subgraph API["API Layer - Spring Boot 3.x"]
        SR[ShipmentRequest API]
        DP[Dispatch API]
        LOC["Location API + SSE Stream<br/>(P3 후순위)"]:::deferred
        ST["Settlement API<br/>(P3 후순위)"]:::deferred
    end

    subgraph Domain["Domain Layer"]
        MS[MatchingStrategy]
        PS[PricingStrategy]
        DS[DispatchService]
        TS["TrackingService<br/>(P3 후순위)"]:::deferred
        SS["SettlementService<br/>(P3 후순위)"]:::deferred
    end
    classDef deferred stroke-dasharray: 5 5,color:#888

    subgraph Infra["Infrastructure"]
        PG[(PostgreSQL)]
        RD[(Redis<br/>캐시/락)]
        MQ{{RabbitMQ}}
    end

    Shipper --> SR
    Driver --> DP
    Driver -.위치 전송 (P3).-> LOC
    Shipper -.SSE 구독 (P3).-> LOC
    SR --> DS
    DP --> DS
    DS --> MS
    DS --> PS
    DS --> PG
    LOC -.-> TS
    TS -.현재 위치.-> RD
    DS -.이벤트 발행.-> MQ
    MQ -.정산 이벤트.-> SS
    SS --> PG
    ST --> PG
    MS --> RD
    PS --> RD
```

---

## 2. 도메인 모델 (ER 다이어그램)

가이드 §3.1 의 엔티티들 관계.

```mermaid
erDiagram
    SHIPPER ||--o{ SHIPMENT_REQUEST : "등록"
    SHIPMENT_REQUEST ||--|{ CARGO_ITEM : "포함"
    DRIVER ||--o{ DISPATCH : "배차받음"
    DRIVER ||--|| VEHICLE : "소유"
    SHIPMENT_REQUEST ||--o| DISPATCH : "매칭"
    DISPATCH ||--o| SETTLEMENT : "정산 생성"

    SHIPPER {
        long id PK
        string name
        string phone
    }
    DRIVER {
        long id PK
        string name
        string preferredRoutes "선호구간 JSON"
        string status "AVAILABLE/BUSY"
    }
    VEHICLE {
        long id PK
        long driverId FK
        string vehicleType "1t/2.5t/5t"
        int capacity
    }
    SHIPMENT_REQUEST {
        long id PK
        long shipperId FK
        string origin
        string destination
        string requiredVehicleType
        string status "REQUESTED..COMPLETED"
    }
    CARGO_ITEM {
        long id PK
        long shipmentRequestId FK
        string description
        int weightKg
    }
    DISPATCH {
        long id PK
        long requestId FK
        long driverId FK
        decimal fare
        decimal matchScore
        string status
    }
    SETTLEMENT {
        long id PK
        long dispatchId FK
        decimal fare
        decimal commission
        string status "PENDING/CONFIRMED/PAID"
    }
```

---

## 3. 상태 전이 (운송 요청 State Machine)

가이드 §3.1 상태 전이 규칙.

```mermaid
stateDiagram-v2
    [*] --> REQUESTED : 화주 등록
    REQUESTED --> MATCHING : 매칭 시작
    MATCHING --> DISPATCHED : 차주 수락
    MATCHING --> CANCELED : 매칭 실패/취소
    DISPATCHED --> EN_ROUTE_TO_PICKUP : 차주 픽업지<br/>이동 시작
    DISPATCHED --> CANCELED : 배차 취소
    EN_ROUTE_TO_PICKUP --> PICKED_UP : 픽업지 도착<br/>+ 화물 상차 완료
    EN_ROUTE_TO_PICKUP --> CANCELED : 픽업 실패
    PICKED_UP --> IN_TRANSIT : 목적지로 출발
    IN_TRANSIT --> COMPLETED : 배송 완료
    COMPLETED --> [*] : 정산 생성 이벤트<br/>(P3 후순위)
    CANCELED --> [*]

    note right of PICKED_UP
      화물 상차 완료 이벤트 발행
      → 화주 알림 Consumer
    end note

    note right of COMPLETED
      COMPLETED 진입 시
      RabbitMQ 이벤트 발행
      → Settlement Consumer (P3)
    end note
```

---

## 4. 정산 상태 전이 · **P3 후순위**

> 정산 모듈은 초기 스코프에서 제외. 아래는 향후 확장 시 참고용.

```mermaid
stateDiagram-v2
    [*] --> PENDING : 운송 완료 이벤트 수신
    PENDING --> CONFIRMED : 정산 확정
    CONFIRMED --> PAID : 지급 완료
    PAID --> [*]
```

---

## 5. 엔드투엔드 시퀀스 (요청 → 매칭 → 배차 → 정산)

가이드 §2 핵심 시나리오 + §3.5 비동기 이벤트 흐름.

```mermaid
sequenceDiagram
    autonumber
    actor Shipper as 화주
    actor Driver as 차주
    participant API as Spring API
    participant Match as MatchingService
    participant Price as PricingEngine
    participant DB as PostgreSQL
    participant MQ as RabbitMQ
    participant Settle as SettlementConsumer

    Shipper->>API: POST /shipment-requests (cargoItems[])
    API->>DB: INSERT ShipmentRequest + CargoItem N건 (REQUESTED)
    API->>Match: findCandidates(총중량, 차량타입, 요청)
    Match->>DB: 조건 + preferredRoutes 필터
    Match-->>API: 후보 차주 + 매칭 점수
    API->>Price: calculate(distance, time, demand, season)
    Price-->>API: 운임
    API->>DB: INSERT Dispatch (DISPATCHED 대기)
    API->>Driver: 배차 제안 알림

    Driver->>API: POST /dispatches/{id}/accept
    API->>DB: UPDATE Dispatch (DISPATCHED)
    API->>MQ: publish DispatchConfirmedEvent

    Note over Driver,DB: 픽업 단계
    Driver->>API: PATCH status → EN_ROUTE_TO_PICKUP
    API->>DB: UPDATE ShipmentRequest

    rect rgba(150,180,220,0.15)
        Note over Driver,Shipper: 실시간 트래킹 - P3 후순위
        loop 5~10초 간격
            Driver-->>API: POST /dispatches/{id}/location
            API-->>DB: Redis SET dispatch:{id}:location (TTL 30s)
            API-->>Shipper: SSE push (좌표)
        end
    end

    Driver->>API: PATCH status → PICKED_UP (상차 완료)
    API->>DB: UPDATE ShipmentRequest
    API->>MQ: publish CargoPickedUpEvent
    MQ-->>Shipper: 알림 (화물 픽업 완료)

    Note over Driver,DB: 운송 단계
    Driver->>API: PATCH status → IN_TRANSIT (목적지 출발)
    Driver->>API: PATCH status → COMPLETED (배송 완료)
    API->>DB: UPDATE ShipmentRequest (COMPLETED)
    API->>MQ: publish ShipmentCompletedEvent

    rect rgba(200,200,200,0.15)
        Note over MQ,Settle: 정산 흐름 - P3 후순위
        MQ-->>Settle: consume event
        Settle-->>DB: INSERT Settlement (PENDING)
    end
```

---

## 6. 매칭 파이프라인 (알고리즘 흐름)

가이드 §3.2 매칭/배차 알고리즘.

```mermaid
flowchart TD
    Start([운송 요청 + CargoItem N건 수신]) --> W[품목 중량 합산]
    W --> F1{차량 타입<br/>일치?}
    F1 -->|No| Drop1[제외]
    F1 -->|Yes| F2{차량 적재량 ≥<br/>총 화물중량?}
    F2 -->|No| Drop2[제외]
    F2 -->|Yes| F3{차주 가용 상태?}
    F3 -->|No| Drop3[제외]
    F3 -->|Yes| S1[기본 점수 계산]
    S1 --> S2[선호구간 매칭<br/>가중치 가산]
    S2 --> Sort[점수 내림차순 정렬]
    Sort --> Top[상위 후보 반환]
    Top --> Price[PricingEngine<br/>운임 산정]
    Price --> Dispatch([Dispatch 생성])
```

---

## 7. 우선순위 로드맵 (Gantt)

가이드 §5 우선순위를 3주 일정으로 배치.

```mermaid
gantt
    title 미니프로젝트 3주 로드맵
    dateFormat  YYYY-MM-DD
    axisFormat  %m/%d

    section P1 필수
    도메인 모델링              :p1a, 2026-09-07, 2d
    REST API 설계              :p1b, after p1a, 2d
    매칭/배차 로직             :p1c, after p1b, 3d
    테스트 코드                :p1d, after p1c, 2d

    section P2 차별화
    RabbitMQ 이벤트(알림)      :p2a, after p1d, 3d
    동시성 처리 (락)           :p2b, after p2a, 2d
    동적 프라이싱 엔진         :p2c, after p2b, 2d

    section P3 여유
    Redis 캐싱/분산락          :p3a, after p2c, 1d
    Docker Compose 통합        :p3b, after p3a, 1d
    문서/ERD/아키텍처 다이어그램 :p3c, after p3b, 2d
    실시간 트래킹 (SSE+Redis)  :p3d, after p3c, 3d
    정산 모듈 (여유 시)        :p3e, after p3d, 2d
```
