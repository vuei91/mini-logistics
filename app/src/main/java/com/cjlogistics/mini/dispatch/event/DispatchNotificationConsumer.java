package com.cjlogistics.mini.dispatch.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class DispatchNotificationConsumer {

    /**
     * 페이로드에 이 마커가 포함되면 소비를 강제로 실패시킨다.
     * 재시도 소진 후 메시지가 DLQ 로 이동하는 경로를 검증하기 위한 테스트 훅이다.
     */
    static final String FORCE_FAIL_MARKER = "FORCE_FAIL";

    @RabbitListener(queues = RabbitMqConfig.NOTIFICATION_QUEUE)
    public void consume(String payload) {
        if (payload.contains(FORCE_FAIL_MARKER)) {
            log.warn("Dispatch notification forced to fail for DLQ verification: {}", payload);
            throw new IllegalStateException("forced failure for DLQ verification");
        }
        log.info("Dispatch notification received: {}", payload);
    }
}
