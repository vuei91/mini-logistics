package com.cjlogistics.mini.shipper;

public class ShipperNotFoundException extends RuntimeException {
    public ShipperNotFoundException(Long id) {
        super("Shipper not found: id=" + id);
    }
}
