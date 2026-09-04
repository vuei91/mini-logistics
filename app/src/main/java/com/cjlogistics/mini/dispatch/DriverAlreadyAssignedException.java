package com.cjlogistics.mini.dispatch;
public class DriverAlreadyAssignedException extends RuntimeException {
    public DriverAlreadyAssignedException(Long driverId) { super("차주에게 이미 활성 배차가 있습니다: " + driverId); }
}
