package com.cjlogistics.mini.shipper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import com.cjlogistics.mini.security.JwtTokenService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShipperController.class)
class ShipperControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    ShipperService shipperService;
    @MockitoBean JwtTokenService jwtTokenService;

    @Test
    void get_returns_404_when_not_found() throws Exception {
        org.mockito.BDDMockito.given(shipperService.get(999L)).willThrow(new ShipperNotFoundException(999L));

        mockMvc.perform(get("/api/shippers/999"))
                .andExpect(status().isNotFound());
    }
}
