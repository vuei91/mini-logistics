package com.cjlogistics.mini.shipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CargoItemCreateRequest(
        @NotBlank @Size(max = 500) String description,
        @NotNull @Positive Integer weightKg
) {
}
