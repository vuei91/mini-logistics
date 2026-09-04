package com.cjlogistics.mini.dispatch;

public class InvalidDispatchStatusTransitionException extends RuntimeException {
    public InvalidDispatchStatusTransitionException(DispatchStatus from, DispatchStatus to) {
        super("Invalid dispatch status transition: " + from + " -> " + to);
    }
}
