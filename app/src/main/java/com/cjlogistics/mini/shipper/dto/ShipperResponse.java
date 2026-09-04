package com.cjlogistics.mini.shipper.dto;

import com.cjlogistics.mini.shipper.Shipper;

public record ShipperResponse(Long id, String name, String phone) {
    public static ShipperResponse from(Shipper s) {
        return new ShipperResponse(s.getId(), s.getName(), s.getPhone());
    }
}
