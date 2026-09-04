# Core Logistics Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the transport request-to-delivery flow safe under concurrent dispatching, recoverable after rejection, observable through a usable API, and locally runnable with its required infrastructure.

**Architecture:** Keep `ShipmentRequest` as the transport aggregate and `Dispatch` as a separate proposal/assignment record. Enforce exclusive driver assignment with database locking and an active-dispatch uniqueness invariant. Preserve reliable event creation with the existing outbox, and make delivery retry-safe through explicit publication failure state and idempotent consumers.

**Tech Stack:** Java 17, Spring Boot 4, Spring MVC, Spring Data JPA/Hibernate, PostgreSQL, RabbitMQ, Docker Compose, JUnit 5, Mockito, Testcontainers.

**Spec:** `CJ대한통운_미니프로젝트_설계가이드.md`, `CJ대한통운_미니프로젝트_체크리스트.md`, `Swagger_RabbitMQ_검증_시나리오.md`

## Global Constraints

- Keep Java at version 17 unless the deployment target is changed deliberately.
- All new business behavior is test-first: run each focused test red before production implementation and green afterward.
- PostgreSQL is the production-like database; H2 remains only for fast local tests where compatible.
- RabbitMQ publishing stays opt-in behind the existing `app.messaging.enabled` property.
- HTTP errors use the existing `ErrorResponse` envelope.
- Do not claim a build or test result without a fresh successful command output.

---

## File Map

- `app/src/main/java/.../dispatch/Dispatch.java`: active-dispatch invariant fields and rejection/cancellation transitions.
- `app/src/main/java/.../dispatch/DispatchRepository.java`: locked lookups and active-dispatch queries.
- `app/src/main/java/.../driver/DriverRepository.java`: locked driver acquisition for assignment.
- `app/src/main/java/.../dispatch/DispatchService.java`: transactional match, reject, retry, and status-flow orchestration.
- `app/src/main/java/.../shipment/ShipmentRequest.java`: explicit rematch transition from `MATCHING` to `REQUESTED`.
- `app/src/main/java/.../dispatch/dto/*`: candidate list and rematch responses where the UI/API needs them.
- `app/src/main/java/.../dispatch/event/*`: outbox attempt state, retry-safe relay, consumer idempotency support.
- `app/src/main/resources/application-*.yml`, `compose.yaml`, `Dockerfile`: reproducible local PostgreSQL/RabbitMQ runtime.
- `app/src/test/java/...`: domain, service, controller, repository concurrency, and container-backed integration coverage.

### Task 1: Establish a runnable Java and test baseline

**Files:**
- Modify: `app/gradlew` (executable mode only)
- Modify: `app/build.gradle` only if Testcontainers dependencies are required by later tests
- Test: all existing tests under `app/src/test/java`

**Produces:** A Java 17 runtime recognized by Gradle and a recorded baseline test result.

- [ ] **Step 1: Verify the selected runtime**

Run: `java -version && cd app && sh gradlew --version`

Expected: Java major version `17` and Gradle wrapper metadata.

- [ ] **Step 2: Run the existing suite before changing behavior**

Run: `cd app && sh gradlew test`

Expected: either a zero-failure baseline or a captured failure list that is fixed before Task 2.

- [ ] **Step 3: Make the wrapper directly executable when repository policy permits it**

Run: `chmod +x app/gradlew`

Expected: `ls -l app/gradlew` begins with `-rwx`.

- [ ] **Step 4: Re-run the suite through the wrapper**

Run: `cd app && ./gradlew test`

Expected: all existing tests pass.

### Task 2: Prevent duplicate active dispatches

**Files:**
- Modify: `app/src/main/java/com/cjlogistics/mini/dispatch/Dispatch.java`
- Modify: `app/src/main/java/com/cjlogistics/mini/dispatch/DispatchRepository.java`
- Modify: `app/src/main/java/com/cjlogistics/mini/driver/DriverRepository.java`
- Modify: `app/src/main/java/com/cjlogistics/mini/dispatch/DispatchService.java`
- Modify: `app/src/main/java/com/cjlogistics/mini/common/GlobalExceptionHandler.java`
- Create: `app/src/main/java/com/cjlogistics/mini/dispatch/DriverAlreadyAssignedException.java`
- Test: `app/src/test/java/com/cjlogistics/mini/dispatch/DispatchServiceTest.java`
- Create: `app/src/test/java/com/cjlogistics/mini/dispatch/DispatchConcurrencyIntegrationTest.java`

**Consumes:** `DriverStatus.AVAILABLE`, `DispatchStatus.PROPOSED`, and `ShipmentStatus.REQUESTED`.

**Produces:** `DriverRepository.findByIdForUpdate(Long)` and `DispatchRepository.existsByDriverIdAndStatusIn(Long, Collection<DispatchStatus>)`.

- [ ] **Step 1: Write a failing service test for an already-active driver**

```java
@Test
void match_and_dispatch_rejects_a_driver_with_an_active_dispatch() {
    given(driverRepository.findByIdForUpdate(50L)).willReturn(Optional.of(driver));
    given(dispatchRepository.existsByDriverIdAndStatusIn(eq(50L), any())).willReturn(true);

    assertThatThrownBy(() -> dispatchService.matchAndDispatch(100L))
            .isInstanceOf(DriverAlreadyAssignedException.class);
}
```

- [ ] **Step 2: Run the focused test and verify red**

Run: `cd app && ./gradlew test --tests '*DispatchServiceTest.match_and_dispatch_rejects_a_driver_with_an_active_dispatch'`

Expected: compilation or assertion failure because locked lookup/invariant enforcement is absent.

- [ ] **Step 3: Add the locked repository methods and service guard**

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select d from Driver d where d.id = :id")
Optional<Driver> findByIdForUpdate(@Param("id") Long id);

boolean existsByDriverIdAndStatusIn(Long driverId, Collection<DispatchStatus> statuses);
```

In `matchAndDispatch`, lock the selected candidate, verify it remains `AVAILABLE`, reject when a `PROPOSED` or `ACCEPTED` dispatch exists, then persist the proposal in the same transaction.

- [ ] **Step 4: Run the focused test and verify green**

Run: `cd app && ./gradlew test --tests '*DispatchServiceTest.match_and_dispatch_rejects_a_driver_with_an_active_dispatch'`

Expected: PASS.

- [ ] **Step 5: Add and run a two-thread PostgreSQL integration test**

```java
assertThat(successfulDispatches.get()).isEqualTo(1);
assertThat(dispatchRepository.count()).isEqualTo(1);
```

Run: `cd app && ./gradlew test --tests '*DispatchConcurrencyIntegrationTest'`

Expected: exactly one concurrent assignment succeeds.

### Task 3: Make rejection lead to a deterministic re-match path

**Files:**
- Modify: `app/src/main/java/com/cjlogistics/mini/shipment/ShipmentRequest.java`
- Modify: `app/src/main/java/com/cjlogistics/mini/dispatch/DispatchService.java`
- Modify: `app/src/main/java/com/cjlogistics/mini/dispatch/DispatchController.java`
- Test: `app/src/test/java/com/cjlogistics/mini/shipment/ShipmentRequestStateMachineTest.java`
- Test: `app/src/test/java/com/cjlogistics/mini/dispatch/DispatchServiceTest.java`
- Test: `app/src/test/java/com/cjlogistics/mini/dispatch/DispatchControllerTest.java`

**Consumes:** A `PROPOSED` dispatch associated with a `MATCHING` shipment.

**Produces:** `ShipmentRequest.returnToRequested()` and `POST /dispatches/{id}/reject-and-rematch`.

- [ ] **Step 1: Write a failing state-machine test**

```java
request.startMatching();
request.returnToRequested();
assertThat(request.getStatus()).isEqualTo(ShipmentStatus.REQUESTED);
```

- [ ] **Step 2: Verify the state-machine test fails**

Run: `cd app && ./gradlew test --tests '*ShipmentRequestStateMachineTest.return_to_requested*'`

Expected: compilation failure because `returnToRequested` does not exist.

- [ ] **Step 3: Implement the guarded aggregate transition**

```java
public void returnToRequested() {
    if (status != ShipmentStatus.MATCHING) {
        throw new InvalidShipmentStatusTransitionException(status, ShipmentStatus.REQUESTED);
    }
    status = ShipmentStatus.REQUESTED;
}
```

- [ ] **Step 4: Write and run the failing service test for rejecting then selecting the next candidate**

```java
Dispatch rematched = dispatchService.rejectAndRematch(rejectedDispatchId);
assertThat(rejected.getStatus()).isEqualTo(DispatchStatus.REJECTED);
assertThat(rematched.getDriverId()).isEqualTo(nextDriver.getId());
```

Run: `cd app && ./gradlew test --tests '*DispatchServiceTest.reject_and_rematch*'`

Expected: FAIL because the orchestration method and endpoint do not exist.

- [ ] **Step 5: Implement `rejectAndRematch` and its controller route**

The method rejects the current proposal, returns the shipment to `REQUESTED`, and invokes the same guarded matching workflow. Return `201 Created` with the replacement dispatch location.

- [ ] **Step 6: Run the focused service/controller tests**

Run: `cd app && ./gradlew test --tests '*DispatchServiceTest.reject_and_rematch*' --tests '*DispatchControllerTest.reject_and_rematch*'`

Expected: PASS.

### Task 4: Add transparent rule-based fare calculation

**Files:**
- Create: `app/src/main/java/com/cjlogistics/mini/pricing/PricingStrategy.java`
- Create: `app/src/main/java/com/cjlogistics/mini/pricing/RuleBasedPricingStrategy.java`
- Create: `app/src/main/java/com/cjlogistics/mini/pricing/PricingResult.java`
- Modify: `app/src/main/java/com/cjlogistics/mini/dispatch/Dispatch.java`
- Modify: `app/src/main/java/com/cjlogistics/mini/dispatch/DispatchService.java`
- Modify: `app/src/main/java/com/cjlogistics/mini/dispatch/dto/DispatchResponse.java`
- Test: `app/src/test/java/com/cjlogistics/mini/pricing/RuleBasedPricingStrategyTest.java`

**Consumes:** origin/destination regions and total cargo weight from `ShipmentRequest`.

**Produces:** `PricingStrategy.calculate(ShipmentRequest request, LocalDateTime requestedAt)` returning a `PricingResult` with final fare and named factors.

- [ ] **Step 1: Write the failing pure unit test for a base fare**

```java
PricingResult result = strategy.calculate(request("서울", "부산", 500), LocalDateTime.of(2026, 9, 4, 10, 0));
assertThat(result.finalFare()).isEqualByComparingTo("150000");
assertThat(result.breakdown().get("baseFare")).isEqualByComparingTo("150000");
```

- [ ] **Step 2: Verify red**

Run: `cd app && ./gradlew test --tests '*RuleBasedPricingStrategyTest.base_fare*'`

Expected: compilation failure because the pricing package is absent.

- [ ] **Step 3: Implement the minimal strategy**

Use an explicit route-rate map with a default rate; multiply by a peak-hour factor only when 07:00–09:59 or 17:00–19:59. Do not claim distance precision without coordinates or a geocoding source.

- [ ] **Step 4: Add boundary tests and verify green**

```java
assertThat(strategy.calculate(request, LocalDateTime.of(2026, 9, 4, 6, 59)).timeFactor())
        .isEqualByComparingTo("1.00");
assertThat(strategy.calculate(request, LocalDateTime.of(2026, 9, 4, 7, 0)).timeFactor())
        .isEqualByComparingTo("1.15");
```

Run: `cd app && ./gradlew test --tests '*RuleBasedPricingStrategyTest'`

Expected: PASS.

- [ ] **Step 5: Persist the calculated fare when creating a dispatch and expose its breakdown**

Run: `cd app && ./gradlew test --tests '*DispatchServiceTest.match_and_dispatch*'`

Expected: the returned dispatch has a non-null fare equal to the strategy result.

### Task 5: Complete settlement from shipment-completed events

**Files:**
- Create: `app/src/main/java/com/cjlogistics/mini/settlement/Settlement.java`
- Create: `app/src/main/java/com/cjlogistics/mini/settlement/SettlementStatus.java`
- Create: `app/src/main/java/com/cjlogistics/mini/settlement/SettlementRepository.java`
- Create: `app/src/main/java/com/cjlogistics/mini/settlement/SettlementService.java`
- Create: `app/src/main/java/com/cjlogistics/mini/settlement/SettlementController.java`
- Create: `app/src/main/java/com/cjlogistics/mini/settlement/dto/SettlementResponse.java`
- Modify: `app/src/main/java/com/cjlogistics/mini/dispatch/event/DispatchNotificationConsumer.java`
- Test: `app/src/test/java/com/cjlogistics/mini/settlement/SettlementServiceTest.java`
- Test: `app/src/test/java/com/cjlogistics/mini/dispatch/event/DispatchNotificationConsumerTest.java`

**Consumes:** `ShipmentCompletedEvent(dispatchId, shipmentRequestId, driverId, occurredAt)` and the dispatch fare.

**Produces:** one `Settlement` per completed dispatch with `PENDING → CONFIRMED → PAID` transitions, `GET /settlements/{id}`, and `GET /drivers/{driverId}/settlements`.

- [ ] **Step 1: Write a failing unit test for creation from a completion event**

```java
Settlement settlement = settlementService.createForCompletedShipment(event);
assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PENDING);
assertThat(settlement.getNetAmount()).isEqualByComparingTo("135000");
```

- [ ] **Step 2: Verify red**

Run: `cd app && ./gradlew test --tests '*SettlementServiceTest.create_for_completed_shipment*'`

Expected: compilation failure because settlement components are absent.

- [ ] **Step 3: Implement a unique dispatch-based settlement and 10% commission**

`Settlement` stores `dispatchId`, `driverId`, `grossFare`, `commission`, `netAmount`, and `status`; `dispatchId` has a unique database constraint. When a duplicate completion message arrives, return the existing record instead of creating another.

- [ ] **Step 4: Verify green and add status-transition coverage**

Run: `cd app && ./gradlew test --tests '*SettlementServiceTest'`

Expected: creation, duplicate handling, confirmation, payment, and invalid transitions pass.

- [ ] **Step 5: Add consumer deserialization routing and endpoint tests**

Run: `cd app && ./gradlew test --tests '*DispatchNotificationConsumerTest' --tests '*SettlementControllerTest'`

Expected: only `ShipmentCompletedEvent` creates settlement data; `DispatchConfirmedEvent` remains notification-only.

### Task 6: Make outbox delivery observable and retry-safe

**Files:**
- Modify: `app/src/main/java/com/cjlogistics/mini/dispatch/event/OutboxEvent.java`
- Modify: `app/src/main/java/com/cjlogistics/mini/dispatch/event/OutboxEventStatus.java`
- Modify: `app/src/main/java/com/cjlogistics/mini/dispatch/event/OutboxEventRelay.java`
- Modify: `app/src/main/java/com/cjlogistics/mini/dispatch/event/OutboxEventRepository.java`
- Test: `app/src/test/java/com/cjlogistics/mini/dispatch/event/OutboxEventRelayTest.java`

**Consumes:** `PENDING` events created within the dispatch transaction.

**Produces:** attempt count, last error, `PUBLISHED` and `FAILED` outbox states, plus a retryable `PENDING` state on broker failure.

- [ ] **Step 1: Write a failing relay test for broker failure**

```java
willThrow(new AmqpException("broker unavailable"))
        .given(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

relay.publishPendingEvents();

assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
assertThat(event.getAttemptCount()).isEqualTo(1);
assertThat(event.getLastError()).contains("broker unavailable");
```

- [ ] **Step 2: Verify red**

Run: `cd app && ./gradlew test --tests '*OutboxEventRelayTest.broker_failure*'`

Expected: compilation failure because error metadata is absent.

- [ ] **Step 3: Implement retry metadata without swallowing the scheduled batch**

Add `recordFailure(String message)` to `OutboxEvent`; catch `AmqpException` per event in the relay, record its failed attempt, and continue with other pending events. Mark an event `FAILED` after a configured maximum attempt count.

- [ ] **Step 4: Run relay tests**

Run: `cd app && ./gradlew test --tests '*OutboxEventRelayTest'`

Expected: successful sends publish once; failures retain retry state; exhausted attempts become failed.

### Task 7: Provide reproducible local infrastructure and profile separation

**Files:**
- Create: `compose.yaml`
- Create: `app/Dockerfile`
- Modify: `app/src/main/resources/application.yml`
- Create: `app/src/main/resources/application-local.yml`
- Modify: `app/src/main/resources/application-rabbitmq.yml`
- Modify: `README.md`
- Test: `app/src/test/java/com/cjlogistics/mini/CjLogisticsMiniApplicationTests.java`

**Produces:** `docker compose up --build` runs PostgreSQL, RabbitMQ management UI, and the API; local profile defaults to H2 and messaging disabled.

- [ ] **Step 1: Write the failing configuration-load test**

```java
@SpringBootTest(properties = {"spring.profiles.active=local", "app.messaging.enabled=false"})
class LocalProfileContextTest {
    @Test void contextLoads() { }
}
```

- [ ] **Step 2: Verify red**

Run: `cd app && ./gradlew test --tests '*LocalProfileContextTest'`

Expected: failure until the local profile is supplied.

- [ ] **Step 3: Define profiles and Compose health checks**

`local` uses H2 with messaging disabled. `rabbitmq` uses environment variables for host credentials. Compose exposes PostgreSQL only on `5432`, RabbitMQ AMQP on `5672`, management UI on `15672`, and starts the application after database/RabbitMQ health checks.

- [ ] **Step 4: Verify profile and container startup**

Run: `cd app && ./gradlew test --tests '*LocalProfileContextTest' && docker compose config`

Expected: test passes and Compose configuration resolves without errors.

### Task 8: Execute end-to-end verification and update the operational scenario

**Files:**
- Modify: `Swagger_RabbitMQ_검증_시나리오.md`
- Modify: `CJ대한통운_미니프로젝트_체크리스트.md`
- Test: `app/src/test/java/com/cjlogistics/mini/dispatch/DispatchControllerTest.java`
- Test: `app/src/test/java/com/cjlogistics/mini/settlement/SettlementControllerTest.java`

**Produces:** an accurate manual verification script covering create, match, reject/rematch, accept, progress, completion, outbox publication, and settlement lookup.

- [ ] **Step 1: Add the failing API-level test for completed dispatch settlement lookup**

```java
mockMvc.perform(get("/drivers/{driverId}/settlements", 50L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("PENDING"));
```

- [ ] **Step 2: Verify red**

Run: `cd app && ./gradlew test --tests '*SettlementControllerTest.list_by_driver*'`

Expected: endpoint missing or response mismatch.

- [ ] **Step 3: Implement only the endpoint behavior required by the test**

- [ ] **Step 4: Run the complete suite and build**

Run: `cd app && ./gradlew clean test bootJar`

Expected: `BUILD SUCCESSFUL` with zero test failures.

- [ ] **Step 5: Perform the documented RabbitMQ scenario**

Run: `docker compose up --build -d && docker compose ps`

Expected: all required services are healthy; carry out the Swagger sequence and confirm `DispatchConfirmedEvent` and `ShipmentCompletedEvent` are consumed and settlement data exists.

## Plan Review

- Spec coverage: Tasks 2–3 cover safe matching and dispatch lifecycle; Task 4 covers rule-based pricing; Task 5 covers settlement; Task 6 covers outbox recovery; Task 7 covers PostgreSQL/RabbitMQ/Docker; Task 8 covers documented end-to-end verification. SSE/Redis tracking is intentionally deferred because the existing spec marks it P3 and it is independent of the core transport/settlement lifecycle.
- Placeholder scan: all task steps specify concrete behavior, target files, tests, and commands.
- Type consistency: `PricingStrategy.calculate`, `ShipmentRequest.returnToRequested`, `DispatchService.rejectAndRematch`, and settlement event inputs are defined before dependent tasks use them.
