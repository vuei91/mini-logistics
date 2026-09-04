package com.cjlogistics.mini.dispatch;

import com.cjlogistics.mini.driver.Driver;
import com.cjlogistics.mini.driver.DriverStatus;
import com.cjlogistics.mini.driver.PreferredRoute;
import com.cjlogistics.mini.driver.Vehicle;
import com.cjlogistics.mini.driver.VehicleType;
import com.cjlogistics.mini.shipment.CargoItem;
import com.cjlogistics.mini.shipment.ShipmentRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultMatchingStrategyTest {
    private final DefaultMatchingStrategy strategy = new DefaultMatchingStrategy();

    private ShipmentRequest request(int... weights) {
        return new ShipmentRequest(1L, "서울", "부산", java.util.Arrays.stream(weights)
                .mapToObj(weight -> new CargoItem("일반화물", weight)).toList(), VehicleType.TRUCK_1T);
    }

    private Driver driver(String name, int capacity, List<PreferredRoute> routes) {
        Driver driver = new Driver(name, "010-0000-0000", new Vehicle(VehicleType.TRUCK_1T, capacity));
        routes.forEach(driver::addPreferredRoute);
        return driver;
    }

    @Test
    void filters_by_total_weight_of_multiple_cargo_items() {
        ShipmentRequest request = request(200, 300);
        Driver tooSmall = driver("A", 400, List.of());
        Driver enough = driver("B", 500, List.of());

        List<MatchCandidate> result = strategy.findCandidates(request, List.of(tooSmall, enough));

        assertThat(request.getTotalCargoWeightKg()).isEqualTo(500);
        assertThat(result).extracting(candidate -> candidate.driver().getName()).containsExactly("B");
    }

    @Test
    void preferred_route_bonus_sorts_candidate_first() {
        ShipmentRequest request = request(500);
        Driver preferred = driver("A", 1000, List.of(new PreferredRoute("서울", "부산")));
        Driver normal = driver("B", 1000, List.of());

        List<MatchCandidate> result = strategy.findCandidates(request, List.of(normal, preferred));

        assertThat(result).extracting(MatchCandidate::score).containsExactly(130.0, 100.0);
        assertThat(result.get(0).driver().getName()).isEqualTo("A");
    }

    @Test
    void excludes_busy_or_wrong_vehicle_drivers() {
        ShipmentRequest request = request(500);
        Driver busy = driver("A", 1000, List.of());
        busy.updateStatus(DriverStatus.BUSY);
        Driver wrongType = new Driver("B", "010-0000-0000", new Vehicle(VehicleType.TRUCK_5T, 5000));

        assertThat(strategy.findCandidates(request, List.of(busy, wrongType))).isEmpty();
    }
}
