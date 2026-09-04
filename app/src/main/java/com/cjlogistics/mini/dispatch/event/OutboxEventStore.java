package com.cjlogistics.mini.dispatch.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventStore {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventStore(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    public void store(DispatchConfirmedEvent event) {
        store(event, "dispatch.confirmed");
    }

    public void store(ShipmentCompletedEvent event) {
        store(event, "shipment.completed");
    }

    private void store(Object event, String routingKey) {
        try {
            outboxEventRepository.save(new OutboxEvent(
                    event.getClass().getSimpleName(), routingKey, OBJECT_MAPPER.writeValueAsString(event)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("이벤트 직렬화에 실패했습니다.", exception);
        }
    }
}
