package com.cjlogistics.mini.driver;

import com.cjlogistics.mini.driver.dto.DriverCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import com.cjlogistics.mini.security.JwtTokenService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriverController.class)
class DriverControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    DriverService driverService;
    @MockitoBean JwtTokenService jwtTokenService;

    @Test
    void create_returns_201_with_driver_vehicle_routes() throws Exception {
        Vehicle vehicle = new Vehicle(VehicleType.TRUCK_2_5T, 2500);
        ReflectionTestUtils.setField(vehicle, "id", 10L);
        Driver saved = new Driver("김운전", "010-2222-3333", vehicle);
        saved.addPreferredRoute(new PreferredRoute("서울", "부산"));
        ReflectionTestUtils.setField(saved, "id", 1L);

        given(driverService.create(anyString(), anyString(), eq(VehicleType.TRUCK_2_5T), eq(2500), any()))
                .willReturn(saved);

        DriverCreateRequest req = new DriverCreateRequest(
                "김운전", "010-2222-3333",
                new DriverCreateRequest.VehicleData(VehicleType.TRUCK_2_5T, 2500),
                List.of(new DriverCreateRequest.RouteData("서울", "부산"))
        );

        mockMvc.perform(post("/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/drivers/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("김운전"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.vehicle.vehicleType").value("TRUCK_2_5T"))
                .andExpect(jsonPath("$.vehicle.capacityKg").value(2500))
                .andExpect(jsonPath("$.preferredRoutes[0].originRegion").value("서울"))
                .andExpect(jsonPath("$.preferredRoutes[0].destinationRegion").value("부산"));
    }

    @Test
    void create_returns_400_when_vehicle_missing() throws Exception {
        String body = """
                { "name": "김운전", "phone": "010-2222-3333", "preferredRoutes": [] }
                """;

        mockMvc.perform(post("/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns_400_when_capacity_not_positive() throws Exception {
        DriverCreateRequest req = new DriverCreateRequest(
                "김운전", "010-2222-3333",
                new DriverCreateRequest.VehicleData(VehicleType.TRUCK_1T, 0),
                List.of()
        );

        mockMvc.perform(post("/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_returns_404_when_not_found() throws Exception {
        given(driverService.get(999L)).willThrow(new DriverNotFoundException(999L));

        mockMvc.perform(get("/drivers/999"))
                .andExpect(status().isNotFound());
    }
}
