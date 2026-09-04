package com.cjlogistics.mini.common;

import com.cjlogistics.mini.dispatch.DispatchNotFoundException;
import com.cjlogistics.mini.dispatch.IllegalStatusTargetException;
import com.cjlogistics.mini.dispatch.InvalidDispatchStatusTransitionException;
import com.cjlogistics.mini.dispatch.NoMatchingDriverException;
import com.cjlogistics.mini.driver.DriverNotFoundException;
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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            ShipperNotFoundException.class,
            DriverNotFoundException.class,
            ShipmentRequestNotFoundException.class,
            DispatchNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), req);
    }

    @ExceptionHandler({
            InvalidShipmentStatusTransitionException.class,
            InvalidDispatchStatusTransitionException.class,
            NoMatchingDriverException.class,
            DuplicateShipperEmailException.class,
            DuplicateDriverEmailException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException e, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, e.getMessage(), req);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(InvalidCredentialsException e, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, e.getMessage(), req);
    }

    @ExceptionHandler({DispatchAccessDeniedException.class, ShipmentAccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException e, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, e.getMessage(), req);
    }

    @ExceptionHandler(IllegalStatusTargetException.class)
    public ResponseEntity<ErrorResponse> handleIllegalTarget(IllegalStatusTargetException e, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, message, req);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status, message, req.getRequestURI()));
    }
}
