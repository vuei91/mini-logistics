package com.cjlogistics.mini.dispatch.event;

import java.time.LocalDateTime;

public record ShipmentCompletedEvent(
        Long dispatchId,
        Long shipmentRequestId,
        Long driverId,
        LocalDateTime occurredAt
) {
}
