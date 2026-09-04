package com.cjlogistics.mini.auth;

import com.cjlogistics.mini.auth.dto.ShipperSignupRequest;
import com.cjlogistics.mini.auth.dto.DriverSignupRequest;
import com.cjlogistics.mini.auth.dto.LoginRequest;
import com.cjlogistics.mini.auth.dto.TokenResponse;
import com.cjlogistics.mini.driver.Driver;
import com.cjlogistics.mini.driver.DriverService;
import com.cjlogistics.mini.driver.PreferredRoute;
import com.cjlogistics.mini.driver.dto.DriverResponse;
import java.util.List;
import com.cjlogistics.mini.security.JwtTokenService;
import com.cjlogistics.mini.shipper.Shipper;
import com.cjlogistics.mini.shipper.ShipperService;
import com.cjlogistics.mini.shipper.dto.ShipperResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final ShipperService shipperService;
    private final DriverService driverService;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/auth/shippers/signup")
    public ResponseEntity<ShipperResponse> shipperSignup(@Valid @RequestBody ShipperSignupRequest request) {
        Shipper shipper = shipperService.signup(request.name(), request.phone(), request.email(), request.password());
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(shipper.getId()).toUri())
                .body(ShipperResponse.from(shipper));
    }

    @PostMapping("/auth/drivers/signup")
    public ResponseEntity<DriverResponse> driverSignup(@Valid @RequestBody DriverSignupRequest request) {
        List<PreferredRoute> routes = request.preferredRoutes() == null ? List.of() : request.preferredRoutes().stream()
                .map(route -> new PreferredRoute(route.originRegion(), route.destinationRegion())).toList();
        Driver driver = driverService.signup(request.name(), request.phone(), request.email(), request.password(),
                request.vehicle().vehicleType(), request.vehicle().capacityKg(), routes);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(driver.getId()).toUri()).body(DriverResponse.from(driver));
    }

    @PostMapping("/auth/shippers/login")
    public TokenResponse shipperLogin(@Valid @RequestBody LoginRequest request) {
        Shipper shipper = shipperService.login(request.email(), request.password());
        return token(jwtTokenService.create(shipper.getEmail(), "SHIPPER", shipper.getId()));
    }

    @PostMapping("/auth/drivers/login")
    public TokenResponse driverLogin(@Valid @RequestBody LoginRequest request) {
        Driver driver = driverService.login(request.email(), request.password());
        return token(jwtTokenService.create(driver.getEmail(), "DRIVER", driver.getId()));
    }

    private TokenResponse token(String value) { return new TokenResponse(value, "Bearer", jwtTokenService.expirationSeconds()); }
}
