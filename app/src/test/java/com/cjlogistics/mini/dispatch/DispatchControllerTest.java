package com.cjlogistics.mini.dispatch;

import com.cjlogistics.mini.dispatch.dto.DispatchStatusUpdateRequest;
import com.cjlogistics.mini.shipment.ShipmentRequestNotFoundException;
import com.cjlogistics.mini.shipment.ShipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DispatchController.class)
class DispatchControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    DispatchService dispatchService;

    private Dispatch savedDispatch(Long id, Long shipmentRequestId, Long driverId, double score) {
        Dispatch dispatch = new Dispatch(shipmentRequestId, driverId, score);
        ReflectionTestUtils.setField(dispatch, "id", id);
        return dispatch;
    }

    @Test
    void dispatch_endpoint_returns_201_with_body() throws Exception {
        Dispatch dispatch = savedDispatch(1L, 100L, 50L, 130.0);
        given(dispatchService.matchAndDispatch(100L)).willReturn(dispatch);

        mockMvc.perform(post("/shipment-requests/100/dispatch"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/dispatches/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.shipmentRequestId").value(100))
                .andExpect(jsonPath("$.driverId").value(50))
                .andExpect(jsonPath("$.matchScore").value(130.0))
                .andExpect(jsonPath("$.status").value("PROPOSED"));
    }

    @Test
    void dispatch_endpoint_returns_404_when_shipment_not_found() throws Exception {
        given(dispatchService.matchAndDispatch(999L))
                .willThrow(new ShipmentRequestNotFoundException(999L));

        mockMvc.perform(post("/shipment-requests/999/dispatch"))
                .andExpect(status().isNotFound());
    }

    @Test
    void dispatch_endpoint_returns_409_when_no_matching_driver() throws Exception {
        given(dispatchService.matchAndDispatch(100L))
                .willThrow(new NoMatchingDriverException(100L));

        mockMvc.perform(post("/shipment-requests/100/dispatch"))
                .andExpect(status().isConflict());
    }

    @Test
    void get_dispatch_returns_body() throws Exception {
        Dispatch dispatch = savedDispatch(1L, 100L, 50L, 100.0);
        given(dispatchService.get(1L)).willReturn(dispatch);

        mockMvc.perform(get("/dispatches/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void get_dispatch_returns_404_when_not_found() throws Exception {
        given(dispatchService.get(999L)).willThrow(new DispatchNotFoundException(999L));

        mockMvc.perform(get("/dispatches/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void accept_returns_200_with_ACCEPTED_status() throws Exception {
        Dispatch dispatch = savedDispatch(1L, 100L, 50L, 130.0);
        ReflectionTestUtils.setField(dispatch, "status", DispatchStatus.ACCEPTED);
        given(dispatchService.accept(1L)).willReturn(dispatch);

        mockMvc.perform(post("/dispatches/1/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void accept_returns_409_when_dispatch_already_accepted() throws Exception {
        given(dispatchService.accept(1L))
                .willThrow(new InvalidDispatchStatusTransitionException(DispatchStatus.ACCEPTED, DispatchStatus.ACCEPTED));

        mockMvc.perform(post("/dispatches/1/accept"))
                .andExpect(status().isConflict());
    }

    @Test
    void reject_returns_200_with_REJECTED_status() throws Exception {
        Dispatch dispatch = savedDispatch(1L, 100L, 50L, 130.0);
        ReflectionTestUtils.setField(dispatch, "status", DispatchStatus.REJECTED);
        given(dispatchService.reject(1L)).willReturn(dispatch);

        mockMvc.perform(post("/dispatches/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void patch_status_progresses_shipment_status() throws Exception {
        Dispatch dispatch = savedDispatch(1L, 100L, 50L, 130.0);
        ReflectionTestUtils.setField(dispatch, "status", DispatchStatus.ACCEPTED);
        given(dispatchService.updateShipmentStatus(1L, ShipmentStatus.PICKED_UP)).willReturn(dispatch);

        DispatchStatusUpdateRequest body = new DispatchStatusUpdateRequest(ShipmentStatus.PICKED_UP);

        mockMvc.perform(patch("/dispatches/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED")); // dispatch.status stays ACCEPTED
    }

    @Test
    void patch_status_returns_400_when_target_is_illegal() throws Exception {
        given(dispatchService.updateShipmentStatus(1L, ShipmentStatus.REQUESTED))
                .willThrow(new IllegalStatusTargetException(ShipmentStatus.REQUESTED));

        DispatchStatusUpdateRequest body = new DispatchStatusUpdateRequest(ShipmentStatus.REQUESTED);

        mockMvc.perform(patch("/dispatches/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patch_status_returns_400_when_status_missing() throws Exception {
        mockMvc.perform(patch("/dispatches/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
