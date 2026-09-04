package com.cjlogistics.mini.shipment;
import com.cjlogistics.mini.shipment.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
@RestController @RequestMapping("/shipment-requests") @RequiredArgsConstructor
public class ShipmentRequestController {
 private final ShipmentRequestService shipmentRequestService;
 @PostMapping public ResponseEntity<ShipmentRequestResponse> create(@Valid @RequestBody ShipmentRequestCreateRequest request) {
  ShipmentRequest created=shipmentRequestService.create(request.shipperId(),request.originRegion(),request.destinationRegion(),request.cargoItems(),request.requiredVehicleType());
  URI location=ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri(); return ResponseEntity.created(location).body(ShipmentRequestResponse.from(created)); }
 @GetMapping("/{id}") public ShipmentRequestResponse get(@PathVariable Long id) { return ShipmentRequestResponse.from(shipmentRequestService.get(id)); }
 @PostMapping("/{id}/cancel") public ShipmentRequestResponse cancel(@PathVariable Long id) { return ShipmentRequestResponse.from(shipmentRequestService.cancel(id)); }
}
