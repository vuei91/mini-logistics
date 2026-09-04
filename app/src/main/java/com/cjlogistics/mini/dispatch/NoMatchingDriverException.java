package com.cjlogistics.mini.dispatch;

public class NoMatchingDriverException extends RuntimeException {
    public NoMatchingDriverException(Long shipmentRequestId) {
        super("No matching driver for shipmentRequestId=" + shipmentRequestId);
    }
}
