package com.cjlogistics.mini.dispatch.dto;

import com.cjlogistics.mini.shipment.ShipmentStatus;
import jakarta.validation.constraints.NotNull;

public record DispatchStatusUpdateRequest(
        @NotNull ShipmentStatus status
) {
}
