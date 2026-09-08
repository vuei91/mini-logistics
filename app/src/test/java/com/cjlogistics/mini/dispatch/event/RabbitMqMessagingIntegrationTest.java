package com.cjlogistics.mini.dispatch.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 실제 RabbitMQ 컨테이너를 띄워서 메시징 파이프라인을 검증하는 통합 테스트.
 *
 * <p>검증 시나리오
 * <ol>
 *   <li>정상 흐름: Outbox 릴레이가 PENDING 이벤트를 발행하고 컨슈머가 정상 소비한다.</li>
 *   <li>Outbox 내구성: store 로 저장된 PENDING 이벤트가 릴레이에 의해 PUBLISHED 로 전이된다.</li>
 *   <li>DLQ 동작: 컨슈머가 반복 실패하면 재시도 소진 후 메시지가 DLQ 로 이동한다.</li>
 * </ol>
 *
 * <p>Docker 데몬이 필요하다. 실행 전 Docker Desktop 을 기동해야 한다.
 */
@Testcontainers
@SpringBootTest
@TestPropertySource(properties = {
        "app.messaging.enabled=true",
        "app.messaging.outbox.fixed-delay-ms=200",
        // DLQ 검증을 빠르게 하기 위해 재시도 간격을 최소화한다.
        "spring.rabbitmq.listener.simple.retry.enabled=true",
        "spring.rabbitmq.listener.simple.retry.max-attempts=3",
        "spring.rabbitmq.listener.simple.retry.initial-interval=100ms",
        "spring.rabbitmq.listener.simple.retry.max-interval=200ms",
        "spring.rabbitmq.listener.simple.retry.multiplier=1",
        "spring.rabbitmq.listener.simple.default-requeue-rejected=false"
})
class RabbitMqMessagingIntegrationTest {

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBIT = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.13-management"));

    @Autowired OutboxEventStore outboxEventStore;
    @Autowired OutboxEventRepository outboxEventRepository;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired TransactionTemplate transactionTemplate;

    @BeforeEach
    void clearOutbox() {
        inTransaction(() -> {
            outboxEventRepository.deleteAll();
            return null;
        });
    }

    /**
     * 트랜잭션 경계 안에서 실행한다.
     * OutboxEventStore.store 저장과, 비관적 락(@Lock)이 걸린 조회 메서드는
     * 모두 활성 트랜잭션을 전제로 하기 때문이다.
     */
    private <T> T inTransaction(java.util.function.Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }

    @Test
    void happyPath_outboxEvent_isPublishedAndConsumed() {
        inTransaction(() -> {
            outboxEventStore.store(new DispatchConfirmedEvent(1L, 10L, 100L, LocalDateTime.now()));
            return null;
        });

        // 릴레이가 PENDING 을 집어 발행 -> 상태가 PUBLISHED 로 전이될 때까지 대기
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            int pending = inTransaction(() -> outboxEventRepository
                    .findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING).size());
            assertThat(pending).isZero();
        });

        assertThat(inTransaction(() -> outboxEventRepository.findAll()))
                .isNotEmpty()
                .allSatisfy(event -> {
                    assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
                    assertThat(event.getPublishedAt()).isNotNull();
                });
    }

    @Test
    void outboxDurability_pendingEvent_transitionsToPublished() {
        inTransaction(() -> {
            outboxEventStore.store(new ShipmentCompletedEvent(2L, 20L, 200L, LocalDateTime.now()));
            return null;
        });

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(inTransaction(() -> outboxEventRepository
                        .findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PUBLISHED)))
                        .anySatisfy(event ->
                                assertThat(event.getRoutingKey()).isEqualTo("shipment.completed")));
    }

    @Test
    void deadLetter_whenConsumerAlwaysFails_messageMovesToDlq() {
        // 컨슈머가 강제 실패하도록 마커가 포함된 페이로드를 직접 발행한다.
        String failingPayload = "{\"marker\":\"" + DispatchNotificationConsumer.FORCE_FAIL_MARKER + "\"}";
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.NOTIFICATION_EXCHANGE, "dispatch.confirmed", failingPayload);

        // 재시도 소진 후 DLQ 로 이동한 메시지를 수신할 때까지 대기
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Message dead = rabbitTemplate.receive(RabbitMqConfig.DEAD_LETTER_QUEUE, 500);
            assertThat(dead).isNotNull();
            assertThat(new String(dead.getBody())).contains(DispatchNotificationConsumer.FORCE_FAIL_MARKER);
        });
    }
}
