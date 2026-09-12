package com.cjlogistics.mini.common;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        ErrorCode code,
        String message,
        String path
) {
    public static ErrorResponse of(HttpStatus status, ErrorCode code, String message, String path) {
        return new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path
        );
    }
}
