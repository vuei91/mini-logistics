package com.cjlogistics.mini.auth.dto;

import com.cjlogistics.mini.driver.VehicleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DriverSignupRequest(
        @NotBlank @Size(max = 100) String name, @NotBlank @Size(max = 20) String phone,
        @NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull @Valid VehicleData vehicle, @Valid List<RouteData> preferredRoutes
) {
    public record VehicleData(@NotNull VehicleType vehicleType, @NotNull @Positive Integer capacityKg) {}
    public record RouteData(@NotBlank @Size(max = 50) String originRegion, @NotBlank @Size(max = 50) String destinationRegion) {}
}
