package com.cjlogistics.mini.shipment;

import com.cjlogistics.mini.driver.VehicleType;
import com.cjlogistics.mini.security.JwtTokenService;
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
class ShipmentRequestListIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenService jwtTokenService;
    @Autowired ShipmentRequestRepository shipmentRequestRepository;

    @Test
    void listMine_returns_only_requests_owned_by_authenticated_shipper() throws Exception {
        shipmentRequestRepository.save(new ShipmentRequest(
                200L, "서울", "부산", List.of(new CargoItem("화물A", 100)), VehicleType.TRUCK_1T));
        shipmentRequestRepository.save(new ShipmentRequest(
                200L, "대구", "광주", List.of(new CargoItem("화물B", 200)), VehicleType.TRUCK_1T));
        shipmentRequestRepository.save(new ShipmentRequest(
                201L, "인천", "울산", List.of(new CargoItem("화물C", 300)), VehicleType.TRUCK_1T));

        String token = jwtTokenService.create("shipper-200@example.com", "SHIPPER", 200L);

        mockMvc.perform(get("/shipment-requests").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].shipperId").value(200))
                .andExpect(jsonPath("$[1].shipperId").value(200));
    }

    @Test
    void listMine_returns_empty_array_when_shipper_has_no_requests() throws Exception {
        String token = jwtTokenService.create("shipper-999@example.com", "SHIPPER", 999L);

        mockMvc.perform(get("/shipment-requests").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listMine_without_token_is_unauthorized() throws Exception {
        mockMvc.perform(get("/shipment-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listMine_with_driver_token_is_forbidden() throws Exception {
        String token = jwtTokenService.create("driver@example.com", "DRIVER", 1L);

        mockMvc.perform(get("/shipment-requests").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
