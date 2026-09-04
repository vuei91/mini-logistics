package com.cjlogistics.mini.shipment;
import com.cjlogistics.mini.driver.VehicleType;
import com.cjlogistics.mini.shipment.dto.CargoItemCreateRequest;
import com.cjlogistics.mini.shipper.ShipperService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class ShipmentRequestService {
 private final ShipmentRequestRepository shipmentRequestRepository; private final ShipperService shipperService;
 @Transactional public ShipmentRequest create(Long shipperId,String origin,String destination,List<CargoItemCreateRequest> cargoItems,VehicleType vehicleType) {
  shipperService.get(shipperId);
  return shipmentRequestRepository.save(new ShipmentRequest(shipperId,origin,destination,cargoItems.stream().map(c -> new CargoItem(c.description(),c.weightKg())).toList(),vehicleType));
 }
 public ShipmentRequest get(Long id) { return shipmentRequestRepository.findById(id).orElseThrow(() -> new ShipmentRequestNotFoundException(id)); }
 public void verifyShipperOwnership(Long shipmentRequestId, Long shipperId) { if (!get(shipmentRequestId).getShipperId().equals(shipperId)) throw new ShipmentAccessDeniedException(shipmentRequestId); }
 @Transactional public ShipmentRequest cancel(Long id) { ShipmentRequest request=get(id); request.cancel(); return request; }
}
