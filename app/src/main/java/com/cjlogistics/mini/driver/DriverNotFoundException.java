package com.cjlogistics.mini.driver;

public class DriverNotFoundException extends RuntimeException {
    public DriverNotFoundException(Long id) {
        super("Driver not found: id=" + id);
    }
}
