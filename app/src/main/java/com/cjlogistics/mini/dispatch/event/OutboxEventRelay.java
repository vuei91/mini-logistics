package com.cjlogistics.mini.dispatch.event;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class OutboxEventRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelayString = "${app.messaging.outbox.fixed-delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING)
                .forEach(this::publish);
    }

    private void publish(OutboxEvent event) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIFICATION_EXCHANGE, event.getRoutingKey(), event.getPayload());
        event.markPublished();
    }
}
