package com.cjlogistics.mini.dispatch;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.cjlogistics.mini.security.JwtTokenService;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(DispatchController.class)
class ScratchProbeTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean DispatchService dispatchService;
    @MockitoBean JwtTokenService jwtTokenService;

    @Test
    void probe() throws Exception {
        org.mockito.BDDMockito.given(dispatchService.accept(1L)).willReturn(new Dispatch(100L, 50L, 1.0));
        var result = mockMvc.perform(post("/dispatches/1/accept")).andReturn();
        System.out.println("STATUS=" + result.getResponse().getStatus());
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> profileCaptor = ArgumentCaptor.forClass(Long.class);
        verify(dispatchService).verifyDriverOwnership(idCaptor.capture(), profileCaptor.capture());
        System.out.println("CAPTURED_ID=" + idCaptor.getValue());
        System.out.println("CAPTURED_PROFILE_ID=" + profileCaptor.getValue());
    }
}
