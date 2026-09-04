package com.cjlogistics.mini.dispatch.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class DispatchNotificationConsumer {

    @RabbitListener(queues = RabbitMqConfig.NOTIFICATION_QUEUE)
    public void consume(String payload) {
        log.info("Dispatch notification received: {}", payload);
    }
}
