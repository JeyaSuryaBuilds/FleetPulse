package com.fayzo.fleetpulse.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorBody(Instant timestamp, int status, String code, String message, Map<String, String> fields) {}

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorBody> api(ApiException e) {
        return ResponseEntity.status(e.status()).body(new ErrorBody(Instant.now(), e.status().value(), e.code(), e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> invalid(MethodArgumentNotValidException e) {
        Map<String, String> f = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe -> f.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorBody(Instant.now(), 400, "VALIDATION_FAILED", "Validation failed", f));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorBody> constraint(ConstraintViolationException e) {
        return ResponseEntity.badRequest().body(new ErrorBody(Instant.now(), 400, "VALIDATION_FAILED", e.getMessage(), null));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ResponseEntity<ErrorBody> integrity(org.springframework.dao.DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(new ErrorBody(Instant.now(), 409, "CONFLICT",
                "The record conflicts with existing data (duplicate value or it is still referenced elsewhere)", null));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ResponseEntity<ErrorBody> unreadable(org.springframework.http.converter.HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ErrorBody(Instant.now(), 400, "BAD_REQUEST", "Malformed or invalid request body", null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorBody> denied(AccessDeniedException e) {
        return ResponseEntity.status(403).body(new ErrorBody(Instant.now(), 403, "FORBIDDEN", "You do not have permission for this action", null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> other(Exception e) {
        log.error("Unhandled error", e);
        return ResponseEntity.status(500).body(new ErrorBody(Instant.now(), 500, "INTERNAL_ERROR", "Unexpected server error", null));
    }
}
