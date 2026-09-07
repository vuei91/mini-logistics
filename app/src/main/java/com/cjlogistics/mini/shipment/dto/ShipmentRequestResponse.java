package com.cjlogistics.mini.shipment.dto;
import com.cjlogistics.mini.driver.VehicleType;
import com.cjlogistics.mini.shipment.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record ShipmentRequestResponse(Long id, Long shipperId, String originRegion, String destinationRegion, List<CargoItemResponse> cargoItems, Integer totalCargoWeightKg, VehicleType requiredVehicleType, ShipmentStatus status, BigDecimal estimatedFare, LocalDateTime createdAt) {
 public static ShipmentRequestResponse from(ShipmentRequest s) { return from(s, null); }
 public static ShipmentRequestResponse from(ShipmentRequest s, BigDecimal estimatedFare) { return new ShipmentRequestResponse(s.getId(),s.getShipperId(),s.getOriginRegion(),s.getDestinationRegion(),s.getCargoItems().stream().map(CargoItemResponse::from).toList(),s.getTotalCargoWeightKg(),s.getRequiredVehicleType(),s.getStatus(),estimatedFare,s.getCreatedAt()); }
}
