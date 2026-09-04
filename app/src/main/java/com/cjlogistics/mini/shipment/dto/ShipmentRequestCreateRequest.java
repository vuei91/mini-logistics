package com.cjlogistics.mini.shipment.dto;
import com.cjlogistics.mini.driver.VehicleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record ShipmentRequestCreateRequest(@NotNull Long shipperId, @NotBlank @Size(max = 50) String originRegion, @NotBlank @Size(max = 50) String destinationRegion, @NotEmpty @Valid List<CargoItemCreateRequest> cargoItems, @NotNull VehicleType requiredVehicleType) {}
