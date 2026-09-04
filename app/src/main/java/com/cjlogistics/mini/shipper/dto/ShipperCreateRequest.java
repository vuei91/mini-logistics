package com.cjlogistics.mini.shipper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShipperCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 20) String phone
) {
}
