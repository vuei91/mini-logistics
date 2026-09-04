package com.cjlogistics.mini.shipment;

import com.cjlogistics.mini.driver.VehicleType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentRequestStateMachineTest {
    private ShipmentRequest newRequest() {
        return new ShipmentRequest(1L, "서울", "부산", List.of(new CargoItem("냉동식품 3박스", 500)), VehicleType.TRUCK_1T);
    }

    @Test
    void cargo_items_are_owned_by_shipment_request_and_total_weight_is_calculated() {
        ShipmentRequest request = new ShipmentRequest(1L, "서울", "부산", List.of(
                new CargoItem("상자 A", 200), new CargoItem("상자 B", 300)), VehicleType.TRUCK_1T);

        assertThat(request.getCargoItems()).hasSize(2);
        assertThat(request.getTotalCargoWeightKg()).isEqualTo(500);
        assertThat(request.getCargoItems()).allSatisfy(item -> assertThat(item.getShipmentRequest()).isSameAs(request));
    }

    @Test
    void full_happy_path_transitions() {
        ShipmentRequest request = newRequest();
        request.startMatching(); request.confirmDispatch(); request.startPickup();
        request.arriveAtPickup(); request.startTransit(); request.complete();
        assertThat(request.getStatus()).isEqualTo(ShipmentStatus.COMPLETED);
    }

    @Test
    void cancel_from_requested_or_matching_succeeds() {
        ShipmentRequest requested = newRequest();
        requested.cancel();
        ShipmentRequest matching = newRequest();
        matching.startMatching(); matching.cancel();
        assertThat(requested.getStatus()).isEqualTo(ShipmentStatus.CANCELED);
        assertThat(matching.getStatus()).isEqualTo(ShipmentStatus.CANCELED);
    }
}
