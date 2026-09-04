package com.cjlogistics.mini.shipper;

import com.cjlogistics.mini.shipper.dto.ShipperCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShipperController.class)
class ShipperControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    ShipperService shipperService;

    @Test
    void create_returns_201_and_body() throws Exception {
        Shipper saved = new Shipper("홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(saved, "id", 1L);
        given(shipperService.create(anyString(), anyString())).willReturn(saved);

        ShipperCreateRequest req = new ShipperCreateRequest("홍길동", "010-1234-5678");

        mockMvc.perform(post("/shippers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/shippers/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.phone").value("010-1234-5678"));
    }

    @Test
    void create_returns_400_when_name_blank() throws Exception {
        ShipperCreateRequest req = new ShipperCreateRequest("", "010-1234-5678");

        mockMvc.perform(post("/shippers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_returns_404_when_not_found() throws Exception {
        given(shipperService.get(999L)).willThrow(new ShipperNotFoundException(999L));

        mockMvc.perform(get("/shippers/999"))
                .andExpect(status().isNotFound());
    }
}
