package com.cjlogistics.mini.dispatch.dto;

import com.cjlogistics.mini.dispatch.Dispatch;
import com.cjlogistics.mini.dispatch.DispatchStatus;
import com.cjlogistics.mini.shipment.ShipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DispatchResponse(
        Long id,
        Long shipmentRequestId,
        Long driverId,
        Double matchScore,
        BigDecimal fare,
        DispatchStatus status,
        ShipmentStatus shipmentStatus,
        LocalDateTime createdAt
) {
    public static DispatchResponse from(Dispatch d) {
        return from(d, null);
    }

    public static DispatchResponse from(Dispatch d, ShipmentStatus shipmentStatus) {
        return new DispatchResponse(
                d.getId(),
                d.getShipmentRequestId(),
                d.getDriverId(),
                d.getMatchScore(),
                d.getFare(),
                d.getStatus(),
                shipmentStatus,
                d.getCreatedAt()
        );
    }
}
