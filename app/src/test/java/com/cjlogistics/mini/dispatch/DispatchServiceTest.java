package com.cjlogistics.mini.dispatch;

import com.cjlogistics.mini.dispatch.event.OutboxEventStore;
import com.cjlogistics.mini.driver.Driver;
import com.cjlogistics.mini.driver.DriverRepository;
import com.cjlogistics.mini.driver.DriverService;
import com.cjlogistics.mini.driver.DriverStatus;
import com.cjlogistics.mini.driver.Vehicle;
import com.cjlogistics.mini.driver.VehicleType;
import com.cjlogistics.mini.shipment.CargoItem;
import com.cjlogistics.mini.shipment.ShipmentRequest;
import com.cjlogistics.mini.shipment.ShipmentRequestService;
import com.cjlogistics.mini.shipment.ShipmentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {
    @Mock ShipmentRequestService shipmentRequestService;
    @Mock DriverRepository driverRepository;
    @Mock DriverService driverService;
    @Mock DispatchRepository dispatchRepository;
    @Mock MatchingStrategy matchingStrategy;
    @Mock OutboxEventStore outboxEventStore;
    @InjectMocks DispatchService dispatchService;

    private ShipmentRequest request(ShipmentStatus status) {
        ShipmentRequest request = new ShipmentRequest(1L, "서울", "부산", List.of(new CargoItem("cargo", 500)), VehicleType.TRUCK_1T);
        ReflectionTestUtils.setField(request, "id", 100L);
        ReflectionTestUtils.setField(request, "status", status);
        return request;
    }

    private Driver driver() {
        Driver driver = new Driver("김운전", "010-0000-0000", new Vehicle(VehicleType.TRUCK_1T, 1000));
        ReflectionTestUtils.setField(driver, "id", 50L);
        return driver;
    }

    @Test
    void match_and_dispatch_creates_proposed_dispatch() {
        ShipmentRequest request = request(ShipmentStatus.REQUESTED);
        Driver driver = driver();
        given(shipmentRequestService.get(100L)).willReturn(request);
        given(driverRepository.findByStatus(DriverStatus.AVAILABLE)).willReturn(List.of(driver));
        given(matchingStrategy.findCandidates(request, List.of(driver))).willReturn(List.of(new MatchCandidate(driver, 130.0)));
        given(dispatchRepository.save(any(Dispatch.class))).willAnswer(invocation -> invocation.getArgument(0));

        Dispatch result = dispatchService.matchAndDispatch(100L);

        assertThat(result.getStatus()).isEqualTo(DispatchStatus.PROPOSED);
        assertThat(request.getStatus()).isEqualTo(ShipmentStatus.MATCHING);
    }

    @Test
    void accept_and_complete_updates_aggregate_statuses() {
        Dispatch dispatch = new Dispatch(100L, 50L, 130.0);
        ReflectionTestUtils.setField(dispatch, "id", 1L);
        Driver driver = driver();
        ShipmentRequest shipment = request(ShipmentStatus.MATCHING);
        given(dispatchRepository.findById(1L)).willReturn(Optional.of(dispatch));
        given(shipmentRequestService.get(100L)).willReturn(shipment);
        given(driverService.get(50L)).willReturn(driver);

        dispatchService.accept(1L);
        ReflectionTestUtils.setField(shipment, "status", ShipmentStatus.IN_TRANSIT);
        dispatchService.updateShipmentStatus(1L, ShipmentStatus.COMPLETED);

        assertThat(dispatch.getStatus()).isEqualTo(DispatchStatus.COMPLETED);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.COMPLETED);
        assertThat(driver.getStatus()).isEqualTo(DriverStatus.AVAILABLE);
    }
}
