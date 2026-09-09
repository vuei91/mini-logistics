package com.cjlogistics.mini.driver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import com.cjlogistics.mini.security.JwtTokenService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void get_returns_404_when_not_found() throws Exception {
        given(driverService.get(999L)).willThrow(new DriverNotFoundException(999L));

        mockMvc.perform(get("/api/drivers/999"))
                .andExpect(status().isNotFound());
    }
}
