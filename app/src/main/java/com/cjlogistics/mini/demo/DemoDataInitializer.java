package com.cjlogistics.mini.demo;

import com.cjlogistics.mini.dispatch.Dispatch;
import com.cjlogistics.mini.dispatch.DispatchService;
import com.cjlogistics.mini.dispatch.dto.DispatchResponse;
import com.cjlogistics.mini.driver.Driver;
import com.cjlogistics.mini.driver.DriverService;
import com.cjlogistics.mini.driver.PreferredRoute;
import com.cjlogistics.mini.driver.VehicleType;
import com.cjlogistics.mini.driver.dto.DriverResponse;
import com.cjlogistics.mini.shipment.ShipmentRequest;
import com.cjlogistics.mini.shipment.ShipmentRequestService;
import com.cjlogistics.mini.shipment.dto.CargoItemCreateRequest;
import com.cjlogistics.mini.shipment.dto.ShipmentRequestResponse;
import com.cjlogistics.mini.shipper.Shipper;
import com.cjlogistics.mini.shipper.ShipperService;
import com.cjlogistics.mini.shipper.dto.ShipperResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private final ShipperService shipperService;
    private final DriverService driverService;
    private final ShipmentRequestService shipmentRequestService;
    private final DispatchService dispatchService;

    private DemoIds demoIds;

    @Override
    public void run(ApplicationArguments args) {
        Shipper shipper = shipperService.create("CJ 데모 화주", "010-1000-2000");
        Driver driver = driverService.create(
                "김 데모 차주", "010-3000-4000", VehicleType.TRUCK_1T, 1000,
                List.of(new PreferredRoute("서울", "부산")));
        ShipmentRequest shipment = shipmentRequestService.create(
                shipper.getId(), "서울", "부산",
                List.of(
                        new CargoItemCreateRequest("데모 화물 · 전자제품 6박스", 300),
                        new CargoItemCreateRequest("데모 화물 · 전자제품 4박스", 200)
                ),
                VehicleType.TRUCK_1T);
        Dispatch dispatch = dispatchService.matchAndDispatch(shipment.getId());
        demoIds = new DemoIds(shipper.getId(), driver.getId(), shipment.getId(), dispatch.getId());
    }

    public DemoStateResponse currentState() {
        if (demoIds == null) {
            throw new IllegalStateException("데모 데이터가 아직 초기화되지 않았습니다.");
        }
        return new DemoStateResponse(
                ShipperResponse.from(shipperService.get(demoIds.shipperId())),
                DriverResponse.from(driverService.get(demoIds.driverId())),
                ShipmentRequestResponse.from(shipmentRequestService.get(demoIds.shipmentRequestId())),
                DispatchResponse.from(dispatchService.get(demoIds.dispatchId()))
        );
    }

    private record DemoIds(Long shipperId, Long driverId, Long shipmentRequestId, Long dispatchId) {
    }
}