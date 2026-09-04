package com.cjlogistics.mini.shipment.dto;

import com.cjlogistics.mini.shipment.CargoItem;

public record CargoItemResponse(Long id, String description, Integer weightKg) {

    public static CargoItemResponse from(CargoItem cargoItem) {
        return new CargoItemResponse(cargoItem.getId(), cargoItem.getDescription(), cargoItem.getWeightKg());
    }
}
