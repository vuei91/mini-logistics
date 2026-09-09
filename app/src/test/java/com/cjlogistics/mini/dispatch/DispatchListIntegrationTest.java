package com.cjlogistics.mini.dispatch;

import com.cjlogistics.mini.security.JwtTokenService;
import com.cjlogistics.mini.shipment.CargoItem;
import com.cjlogistics.mini.shipment.ShipmentRequest;
import com.cjlogistics.mini.shipment.ShipmentRequestRepository;
import com.cjlogistics.mini.driver.VehicleType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.transaction.annotation.Transactional
class DispatchListIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenService jwtTokenService;
    @Autowired DispatchRepository dispatchRepository;
    @Autowired ShipmentRequestRepository shipmentRequestRepository;

    @Test
    void listMine_returns_only_dispatches_owned_by_authenticated_driver() throws Exception {
        ShipmentRequest req1 = shipmentRequestRepository.save(new ShipmentRequest(
                200L, "서울", "부산", List.of(new CargoItem("화물A", 100)), VehicleType.TRUCK_1T));
        ShipmentRequest req2 = shipmentRequestRepository.save(new ShipmentRequest(
                200L, "대구", "광주", List.of(new CargoItem("화물B", 200)), VehicleType.TRUCK_1T));
        ShipmentRequest req3 = shipmentRequestRepository.save(new ShipmentRequest(
                201L, "인천", "울산", List.of(new CargoItem("화물C", 300)), VehicleType.TRUCK_1T));

        dispatchRepository.save(new Dispatch(req1.getId(), 500L, 100.0));
        dispatchRepository.save(new Dispatch(req2.getId(), 500L, 90.0));
        dispatchRepository.save(new Dispatch(req3.getId(), 501L, 80.0));

        String token = jwtTokenService.create("driver-500@example.com", "DRIVER", 500L);

        mockMvc.perform(get("/api/dispatches").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].driverId").value(500))
                .andExpect(jsonPath("$[1].driverId").value(500));
    }

    @Test
    void listMine_returns_empty_array_when_driver_has_no_dispatches() throws Exception {
        String token = jwtTokenService.create("driver-999@example.com", "DRIVER", 999L);

        mockMvc.perform(get("/api/dispatches").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listMine_without_token_is_unauthorized() throws Exception {
        mockMvc.perform(get("/api/dispatches"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listMine_with_shipper_token_is_forbidden() throws Exception {
        String token = jwtTokenService.create("shipper@example.com", "SHIPPER", 1L);

        mockMvc.perform(get("/api/dispatches").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
