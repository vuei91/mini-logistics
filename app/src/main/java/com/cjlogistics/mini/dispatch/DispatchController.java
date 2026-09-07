package com.cjlogistics.mini.dispatch;

import com.cjlogistics.mini.dispatch.dto.DispatchResponse;
import com.cjlogistics.mini.dispatch.dto.DispatchStatusUpdateRequest;
import com.cjlogistics.mini.shipment.ShipmentRequestService;
import com.cjlogistics.mini.shipment.ShipmentStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import com.cjlogistics.mini.security.AuthenticatedMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;
    private final ShipmentRequestService shipmentRequestService;

    @PostMapping("/shipment-requests/{shipmentRequestId}/dispatch")
    public ResponseEntity<DispatchResponse> matchAndDispatch(@PathVariable Long shipmentRequestId) {
        Dispatch dispatch = dispatchService.matchAndDispatch(shipmentRequestId);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/dispatches/{id}")
                .buildAndExpand(dispatch.getId())
                .toUri();
        return ResponseEntity.created(location).body(toResponse(dispatch));
    }

    @GetMapping("/dispatches")
    public java.util.List<DispatchResponse> listMine(@AuthenticationPrincipal AuthenticatedMember member) {
        return dispatchService.getByDriver(member.profileId()).stream().map(this::toResponse).toList();
    }

    @GetMapping("/dispatches/{id}")
    public DispatchResponse get(@PathVariable Long id) {
        return toResponse(dispatchService.get(id));
    }

    @PostMapping("/dispatches/{id}/accept")
    public DispatchResponse accept(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedMember member) {
        dispatchService.verifyDriverOwnership(id, member.profileId());
        return toResponse(dispatchService.accept(id));
    }

    @PostMapping("/dispatches/{id}/reject")
    public DispatchResponse reject(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedMember member) {
        dispatchService.verifyDriverOwnership(id, member.profileId());
        return toResponse(dispatchService.reject(id));
    }

    @PatchMapping("/dispatches/{id}/status")
    public DispatchResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody DispatchStatusUpdateRequest request, @AuthenticationPrincipal AuthenticatedMember member
    ) {
        dispatchService.verifyDriverOwnership(id, member.profileId());
        return toResponse(dispatchService.updateShipmentStatus(id, request.status()));
    }

    private DispatchResponse toResponse(Dispatch dispatch) {
        ShipmentStatus shipmentStatus = shipmentRequestService.get(dispatch.getShipmentRequestId()).getStatus();
        return DispatchResponse.from(dispatch, shipmentStatus);
    }
}
