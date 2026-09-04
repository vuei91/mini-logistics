package com.cjlogistics.mini.shipment;

import com.cjlogistics.mini.driver.VehicleType;
import com.cjlogistics.mini.shipment.dto.CargoItemCreateRequest;
import com.cjlogistics.mini.shipment.dto.ShipmentRequestCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import com.cjlogistics.mini.security.JwtTokenService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShipmentRequestController.class)
class ShipmentRequestControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ShipmentRequestService shipmentRequestService;
    @MockitoBean JwtTokenService jwtTokenService;

    private ShipmentRequest request() {
        ShipmentRequest request = new ShipmentRequest(1L, "서울", "부산", List.of(
                new CargoItem("냉동식품 2박스", 200), new CargoItem("냉동식품 3박스", 300)), VehicleType.TRUCK_1T);
        ReflectionTestUtils.setField(request, "id", 1L);
        ReflectionTestUtils.setField(request, "createdAt", LocalDateTime.of(2026, 9, 1, 10, 0));
        return request;
    }

    @Test
    void create_returns_cargo_items_and_total_weight() throws Exception {
        given(shipmentRequestService.create(anyLong(), anyString(), anyString(), any(), any())).willReturn(request());
        ShipmentRequestCreateRequest body = new ShipmentRequestCreateRequest(1L, "서울", "부산", List.of(
                new CargoItemCreateRequest("냉동식품 2박스", 200), new CargoItemCreateRequest("냉동식품 3박스", 300)), VehicleType.TRUCK_1T);

        mockMvc.perform(post("/shipment-requests").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cargoItems").isArray())
                .andExpect(jsonPath("$.cargoItems.length()").value(2))
                .andExpect(jsonPath("$.totalCargoWeightKg").value(500));
    }

    @Test
    void create_rejects_empty_cargo_items() throws Exception {
        ShipmentRequestCreateRequest body = new ShipmentRequestCreateRequest(1L, "서울", "부산", List.of(), VehicleType.TRUCK_1T);
        mockMvc.perform(post("/shipment-requests").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_rejects_non_positive_cargo_weight() throws Exception {
        ShipmentRequestCreateRequest body = new ShipmentRequestCreateRequest(1L, "서울", "부산", List.of(new CargoItemCreateRequest("cargo", 0)), VehicleType.TRUCK_1T);
        mockMvc.perform(post("/shipment-requests").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
