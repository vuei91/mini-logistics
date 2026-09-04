package com.cjlogistics.mini.dispatch.dto;

import com.cjlogistics.mini.dispatch.Dispatch;
import com.cjlogistics.mini.dispatch.DispatchStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DispatchResponse(
        Long id,
        Long shipmentRequestId,
        Long driverId,
        Double matchScore,
        BigDecimal fare,
        DispatchStatus status,
        LocalDateTime createdAt
) {
    public static DispatchResponse from(Dispatch d) {
        return new DispatchResponse(
                d.getId(),
                d.getShipmentRequestId(),
                d.getDriverId(),
                d.getMatchScore(),
                d.getFare(),
                d.getStatus(),
                d.getCreatedAt()
        );
    }
}
