package com.cjlogistics.mini.shipment;

import com.cjlogistics.mini.driver.VehicleType;
import com.cjlogistics.mini.shipment.dto.CargoItemCreateRequest;
import com.cjlogistics.mini.shipper.Shipper;
import com.cjlogistics.mini.shipper.ShipperService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ShipmentRequestServiceTest {

    @Test
    void verify_shipper_ownership_rejects_a_different_shipper() {
        ShipmentRequest request = new ShipmentRequest(10L, "서울", "부산", List.of(new CargoItem("화물", 1)), VehicleType.TRUCK_1T);
        given(shipmentRequestRepository.findById(1L)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> shipmentRequestService.verifyShipperOwnership(1L, 11L))
                .isInstanceOf(ShipmentAccessDeniedException.class);
    }
    @Mock ShipmentRequestRepository shipmentRequestRepository;
    @Mock ShipperService shipperService;
    @InjectMocks ShipmentRequestService shipmentRequestService;

    @Test
    void create_persists_all_cargo_items_and_calculates_total_weight() {
        given(shipperService.get(1L)).willReturn(new Shipper("화주1", "010-1111-2222"));
        given(shipmentRequestRepository.save(any(ShipmentRequest.class))).willAnswer(invocation -> invocation.getArgument(0));

        ShipmentRequest result = shipmentRequestService.create(1L, "서울", "부산", List.of(
                new CargoItemCreateRequest("상자 A", 200), new CargoItemCreateRequest("상자 B", 300)), VehicleType.TRUCK_1T);

        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.REQUESTED);
        assertThat(result.getCargoItems()).hasSize(2);
        assertThat(result.getTotalCargoWeightKg()).isEqualTo(500);
        assertThat(result.getCargoItems()).allSatisfy(item -> assertThat(item.getShipmentRequest()).isSameAs(result));
    }
}
