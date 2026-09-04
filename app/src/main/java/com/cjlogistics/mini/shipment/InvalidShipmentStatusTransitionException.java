package com.cjlogistics.mini.shipment;

public class InvalidShipmentStatusTransitionException extends RuntimeException {
    public InvalidShipmentStatusTransitionException(ShipmentStatus from, ShipmentStatus to) {
        super("Invalid status transition: " + from + " -> " + to);
    }
}
