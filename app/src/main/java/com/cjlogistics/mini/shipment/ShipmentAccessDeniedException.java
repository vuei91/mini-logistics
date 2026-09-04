package com.cjlogistics.mini.shipment;
public class ShipmentAccessDeniedException extends RuntimeException {
    public ShipmentAccessDeniedException(Long shipmentId) { super("운송 요청에 접근할 권한이 없습니다: " + shipmentId); }
}
