package com.cjlogistics.mini.common;

import com.cjlogistics.mini.dispatch.DispatchNotFoundException;
import com.cjlogistics.mini.dispatch.IllegalStatusTargetException;
import com.cjlogistics.mini.dispatch.InvalidDispatchStatusTransitionException;
import com.cjlogistics.mini.dispatch.NoMatchingDriverException;
import com.cjlogistics.mini.driver.DriverNotFoundException;
import com.cjlogistics.mini.notification.NotificationNotFoundException;
import com.cjlogistics.mini.shipment.InvalidShipmentStatusTransitionException;
import com.cjlogistics.mini.shipment.ShipmentRequestNotFoundException;
import com.cjlogistics.mini.shipper.ShipperNotFoundException;
import com.cjlogistics.mini.shipper.DuplicateShipperEmailException;
import com.cjlogistics.mini.driver.DuplicateDriverEmailException;
import com.cjlogistics.mini.auth.InvalidCredentialsException;
import com.cjlogistics.mini.dispatch.DispatchAccessDeniedException;
import com.cjlogistics.mini.shipment.ShipmentAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            ShipperNotFoundException.class,
            DriverNotFoundException.class,
            ShipmentRequestNotFoundException.class,
            DispatchNotFoundException.class,
            NotificationNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, e.getMessage(), req);
    }

    @ExceptionHandler({
            InvalidShipmentStatusTransitionException.class,
            InvalidDispatchStatusTransitionException.class,
            NoMatchingDriverException.class,
            DuplicateShipperEmailException.class,
            DuplicateDriverEmailException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException e, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ErrorCode.CONFLICT, e.getMessage(), req);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(InvalidCredentialsException e, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS, e.getMessage(), req);
    }

    @ExceptionHandler({DispatchAccessDeniedException.class, ShipmentAccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException e, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, e.getMessage(), req);
    }

    @ExceptionHandler(IllegalStatusTargetException.class)
    public ResponseEntity<ErrorResponse> handleIllegalTarget(IllegalStatusTargetException e, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST, e.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, message, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest(HttpMessageNotReadableException e,
                                                                 HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST,
                "요청 본문을 읽을 수 없습니다.", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e,
                                                                 HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED,
                "지원하지 않는 요청 방식입니다.", req);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, ErrorCode code, String message,
                                                 HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status, code, message, req.getRequestURI()));
    }
}
