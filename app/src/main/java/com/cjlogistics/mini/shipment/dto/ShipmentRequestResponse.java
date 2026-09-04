package com.cjlogistics.mini.shipment.dto;
import com.cjlogistics.mini.driver.VehicleType;
import com.cjlogistics.mini.shipment.*;
import java.time.LocalDateTime;
import java.util.List;
public record ShipmentRequestResponse(Long id, Long shipperId, String originRegion, String destinationRegion, List<CargoItemResponse> cargoItems, Integer totalCargoWeightKg, VehicleType requiredVehicleType, ShipmentStatus status, LocalDateTime createdAt) {
 public static ShipmentRequestResponse from(ShipmentRequest s) { return new ShipmentRequestResponse(s.getId(),s.getShipperId(),s.getOriginRegion(),s.getDestinationRegion(),s.getCargoItems().stream().map(CargoItemResponse::from).toList(),s.getTotalCargoWeightKg(),s.getRequiredVehicleType(),s.getStatus(),s.getCreatedAt()); }
}
