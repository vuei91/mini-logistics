package com.cjlogistics.mini.shipment;

public class ShipmentRequestNotFoundException extends RuntimeException {
    public ShipmentRequestNotFoundException(Long id) {
        super("ShipmentRequest not found: id=" + id);
    }
}
