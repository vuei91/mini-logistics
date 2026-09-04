package com.cjlogistics.mini.dispatch;

public class DispatchNotFoundException extends RuntimeException {
    public DispatchNotFoundException(Long id) {
        super("Dispatch not found: id=" + id);
    }
}
