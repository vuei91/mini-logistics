package com.cjlogistics.mini.dispatch;

import com.cjlogistics.mini.shipment.ShipmentStatus;

public class IllegalStatusTargetException extends RuntimeException {
    public IllegalStatusTargetException(ShipmentStatus target) {
        super("Status target not allowed via this endpoint: " + target);
    }
}
