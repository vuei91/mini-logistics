package com.cjlogistics.mini.driver;

import com.cjlogistics.mini.driver.dto.DriverCreateRequest;
import com.cjlogistics.mini.driver.dto.DriverResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PostMapping
    public ResponseEntity<DriverResponse> create(@Valid @RequestBody DriverCreateRequest request) {
        List<PreferredRoute> routes = request.preferredRoutes() == null
                ? List.of()
                : request.preferredRoutes().stream()
                        .map(r -> new PreferredRoute(r.originRegion(), r.destinationRegion()))
                        .toList();

        Driver driver = driverService.create(
                request.name(),
                request.phone(),
                request.vehicle().vehicleType(),
                request.vehicle().capacityKg(),
                routes
        );

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(driver.getId())
                .toUri();
        return ResponseEntity.created(location).body(DriverResponse.from(driver));
    }

    @GetMapping("/{id}")
    public DriverResponse get(@PathVariable Long id) {
        return DriverResponse.from(driverService.get(id));
    }
}
