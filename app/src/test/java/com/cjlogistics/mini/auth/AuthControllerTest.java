package com.cjlogistics.mini.auth;

import com.cjlogistics.mini.shipper.Shipper;
import com.cjlogistics.mini.shipper.ShipperService;
import com.cjlogistics.mini.driver.Driver;
import com.cjlogistics.mini.driver.DriverService;
import com.cjlogistics.mini.driver.Vehicle;
import com.cjlogistics.mini.driver.VehicleType;
import com.cjlogistics.mini.security.JwtTokenService;
import com.cjlogistics.mini.shipper.DuplicateShipperEmailException;
import com.cjlogistics.mini.auth.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ShipperService shipperService;
    @MockitoBean DriverService driverService;
    @MockitoBean JwtTokenService jwtTokenService;

    @Test
    void shipper_signup_returns_created() throws Exception {
        given(shipperService.signup(any(), any(), any(), any()))
                .willReturn(Shipper.register("CJ 화주", "010-1111-2222", "cj@example.com", "hash"));

        mockMvc.perform(post("/auth/shippers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"CJ 화주\",\"phone\":\"010-1111-2222\",\"email\":\"cj@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void driver_signup_returns_created() throws Exception {
        given(driverService.signup(any(), any(), any(), any(), any(), any(), any()))
                .willReturn(Driver.register("김운전", "010-3333-4444", "driver@example.com", "hash",
                        new Vehicle(VehicleType.TRUCK_1T, 1000)));

        mockMvc.perform(post("/auth/drivers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"김운전\",\"phone\":\"010-3333-4444\",\"email\":\"driver@example.com\",\"password\":\"password123\",\"vehicle\":{\"vehicleType\":\"TRUCK_1T\",\"capacityKg\":1000},\"preferredRoutes\":[]}"))
                .andExpect(status().isCreated());
    }

    @Test
    void shipper_login_returns_bearer_token() throws Exception {
        given(shipperService.login("cj@example.com", "password123"))
                .willReturn(Shipper.register("CJ 화주", "010-1111-2222", "cj@example.com", "hash"));
        given(jwtTokenService.create(any(), any(), any())).willReturn("signed-token");
        given(jwtTokenService.expirationSeconds()).willReturn(3600L);

        mockMvc.perform(post("/auth/shippers/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cj@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void driver_login_returns_bearer_token() throws Exception {
        given(driverService.login("driver@example.com", "password123"))
                .willReturn(Driver.register("김운전", "010-3333-4444", "driver@example.com", "hash",
                        new Vehicle(VehicleType.TRUCK_1T, 1000)));
        given(jwtTokenService.create(any(), any(), any())).willReturn("signed-token");
        given(jwtTokenService.expirationSeconds()).willReturn(3600L);

        mockMvc.perform(post("/auth/drivers/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"driver@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void invalid_signup_request_returns_bad_request() throws Exception {
        mockMvc.perform(post("/auth/shippers/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"phone\":\"\",\"email\":\"invalid\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicate_shipper_email_returns_conflict() throws Exception {
        given(shipperService.signup(any(), any(), any(), any())).willThrow(new DuplicateShipperEmailException("cj@example.com"));
        mockMvc.perform(post("/auth/shippers/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"CJ 화주\",\"phone\":\"010-1111-2222\",\"email\":\"cj@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void invalid_login_returns_unauthorized() throws Exception {
        given(shipperService.login(any(), any())).willThrow(new InvalidCredentialsException());
        mockMvc.perform(post("/auth/shippers/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cj@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }
}
