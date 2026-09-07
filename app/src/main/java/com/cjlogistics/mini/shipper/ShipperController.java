package com.cjlogistics.mini.shipper;

import com.cjlogistics.mini.security.AuthenticatedMember;
import com.cjlogistics.mini.shipper.dto.ShipperResponse;
import com.cjlogistics.mini.shipper.dto.ShipperUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shippers")
@RequiredArgsConstructor
public class ShipperController {

    private final ShipperService shipperService;

    @GetMapping("/me")
    public ShipperResponse getMe(@AuthenticationPrincipal AuthenticatedMember member) {
        return ShipperResponse.from(shipperService.get(member.profileId()));
    }

    @PatchMapping("/me")
    public ShipperResponse updateMe(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody ShipperUpdateRequest request
    ) {
        Shipper updated = shipperService.updateProfile(
                member.profileId(), request.name(), request.phone());
        return ShipperResponse.from(updated);
    }

    @GetMapping("/{id}")
    public ShipperResponse get(@PathVariable Long id) {
        return ShipperResponse.from(shipperService.get(id));
    }
}
