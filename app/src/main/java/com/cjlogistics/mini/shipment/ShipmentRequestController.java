package com.cjlogistics.mini.shipment;
import com.cjlogistics.mini.dispatch.FareCalculator;
import com.cjlogistics.mini.shipment.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import com.cjlogistics.mini.security.AuthenticatedMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController @RequestMapping("/shipment-requests") @RequiredArgsConstructor
public class ShipmentRequestController {
 private final ShipmentRequestService shipmentRequestService;
 private final FareCalculator fareCalculator;
 private ShipmentRequestResponse toResponse(ShipmentRequest request) {
  BigDecimal estimatedFare = fareCalculator.calculate(request);
  return ShipmentRequestResponse.from(request, estimatedFare);
 }
 @PostMapping public ResponseEntity<ShipmentRequestResponse> create(@Valid @RequestBody ShipmentRequestCreateRequest request) {
  ShipmentRequest created=shipmentRequestService.create(request.shipperId(),request.originRegion(),request.destinationRegion(),request.cargoItems(),request.requiredVehicleType());
  URI location=ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri(); return ResponseEntity.created(location).body(toResponse(created)); }
 @GetMapping public List<ShipmentRequestResponse> listMine(@AuthenticationPrincipal AuthenticatedMember member) {
  return shipmentRequestService.getByShipper(member.profileId()).stream().map(this::toResponse).toList(); }
 @GetMapping("/{id}") public ShipmentRequestResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedMember member) { shipmentRequestService.verifyShipperOwnership(id, member.profileId()); return toResponse(shipmentRequestService.get(id)); }
 @PostMapping("/{id}/cancel") public ShipmentRequestResponse cancel(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedMember member) { shipmentRequestService.verifyShipperOwnership(id, member.profileId()); return toResponse(shipmentRequestService.cancel(id)); }
}
