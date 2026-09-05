package com.cjlogistics.mini.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.cjlogistics.mini.dispatch.Dispatch;
import com.cjlogistics.mini.dispatch.DispatchRepository;
import com.cjlogistics.mini.shipment.ShipmentRequest;
import com.cjlogistics.mini.shipment.ShipmentRequestRepository;
import com.cjlogistics.mini.shipment.CargoItem;
import com.cjlogistics.mini.driver.VehicleType;
import com.cjlogistics.mini.shipper.Shipper;
import com.cjlogistics.mini.shipper.ShipperRepository;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenService jwtTokenService;
    @Autowired DispatchRepository dispatchRepository;
    @Autowired ShipmentRequestRepository shipmentRequestRepository;
    @Autowired ShipperRepository shipperRepository;

    @Test
    void mutation_without_bearer_token_is_unauthorized() throws Exception {
        mockMvc.perform(post("/shipment-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shipperId\":1,\"originRegion\":\"서울\",\"destinationRegion\":\"부산\",\"requiredVehicleType\":\"TRUCK_1T\",\"cargoItems\":[{\"description\":\"화물\",\"weightKg\":1}]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void driver_token_cannot_create_shipment() throws Exception {
        String token = jwtTokenService.create("driver@example.com", "DRIVER", 1L);
        mockMvc.perform(post("/shipment-requests").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shipperId\":1,\"originRegion\":\"서울\",\"destinationRegion\":\"부산\",\"requiredVehicleType\":\"TRUCK_1T\",\"cargoItems\":[{\"description\":\"화물\",\"weightKg\":1}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void driver_token_cannot_start_dispatch_matching() throws Exception {
        String token = jwtTokenService.create("driver@example.com", "DRIVER", 1L);

        mockMvc.perform(post("/shipment-requests/1/dispatch")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shipper_token_cannot_accept_dispatch() throws Exception {
        String token = jwtTokenService.create("cj@example.com", "SHIPPER", 1L);
        mockMvc.perform(post("/dispatches/1/accept").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shipper_token_cannot_update_dispatch_status() throws Exception {
        String token = jwtTokenService.create("cj@example.com", "SHIPPER", 1L);
        mockMvc.perform(patch("/dispatches/1/status").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"EN_ROUTE_TO_PICKUP\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void different_driver_cannot_accept_dispatch() throws Exception {
        Dispatch dispatch = dispatchRepository.save(new Dispatch(100L, 50L, 130.0));
        String token = jwtTokenService.create("other-driver@example.com", "DRIVER", 51L);

        mockMvc.perform(post("/dispatches/{id}/accept", dispatch.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void different_shipper_cannot_read_shipment_request() throws Exception {
        ShipmentRequest shipment = shipmentRequestRepository.save(new ShipmentRequest(
                10L, "서울", "부산", List.of(new CargoItem("화물", 1)), VehicleType.TRUCK_1T));
        String token = jwtTokenService.create("other-shipper@example.com", "SHIPPER", 11L);

        mockMvc.perform(get("/shipment-requests/{id}", shipment.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shipper_token_can_create_own_shipment_request() throws Exception {
        Shipper shipper = shipperRepository.save(Shipper.register("CJ 화주", "010-1111-2222", "cj@example.com", "hash"));
        String token = jwtTokenService.create("cj@example.com", "SHIPPER", shipper.getId());

        mockMvc.perform(post("/shipment-requests").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shipperId\":" + shipper.getId() + ",\"originRegion\":\"서울\",\"destinationRegion\":\"부산\",\"requiredVehicleType\":\"TRUCK_1T\",\"cargoItems\":[{\"description\":\"화물\",\"weightKg\":1}]}"))
                .andExpect(status().isCreated());
    }
}
