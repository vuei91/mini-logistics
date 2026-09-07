package com.cjlogistics.mini.driver;

import com.cjlogistics.mini.driver.dto.DriverResponse;
import com.cjlogistics.mini.driver.dto.DriverUpdateRequest;
import com.cjlogistics.mini.security.AuthenticatedMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping("/me")
    public DriverResponse getMe(@AuthenticationPrincipal AuthenticatedMember member) {
        return DriverResponse.from(driverService.get(member.profileId()));
    }

    @PatchMapping("/me")
    public DriverResponse updateMe(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody DriverUpdateRequest request
    ) {
        List<PreferredRoute> routes = request.preferredRoutes() == null ? List.of()
                : request.preferredRoutes().stream()
                        .map(r -> new PreferredRoute(r.originRegion(), r.destinationRegion()))
                        .toList();
        Driver updated = driverService.updateProfile(
                member.profileId(),
                request.name(),
                request.phone(),
                request.vehicle().vehicleType(),
                request.vehicle().capacityKg(),
                routes
        );
        return DriverResponse.from(updated);
    }

    @GetMapping("/{id}")
    public DriverResponse get(@PathVariable Long id) {
        return DriverResponse.from(driverService.get(id));
    }
}
