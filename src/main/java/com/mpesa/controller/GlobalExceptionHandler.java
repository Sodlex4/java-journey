package com.mpesa.controller;

import com.mpesa.dto.ApiResponse;
import com.mpesa.dto.ErrorCode;
import com.mpesa.exception.PaymentException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        String correlationId = UUID.randomUUID().toString();
        List<Map<String, String>> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", e.getDefaultMessage()))
                .toList();

        log.warn("[{}] Validation failed: {}", correlationId, details);

        return ResponseEntity.badRequest().body(
                ApiResponse.error("Validation failed", ErrorCode.VALIDATION_FAILED, details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Constraint violation: {}", correlationId, ex.getMessage());

        return ResponseEntity.badRequest().body(
                ApiResponse.error("Validation failed", ErrorCode.VALIDATION_FAILED, correlationId));
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<?>> handlePaymentException(
            PaymentException ex, WebRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Payment error: {}", correlationId, ex.getMessage());

        String message = ex.getMessage();
        int status = getPaymentErrorStatus(message);
        ErrorCode code = mapPaymentError(message);

        return ResponseEntity.status(status).body(
                ApiResponse.error(sanitizePaymentMessage(message), code, correlationId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Illegal argument: {}", correlationId, ex.getMessage());

        return ResponseEntity.badRequest().body(
                ApiResponse.error("Invalid request", ErrorCode.INVALID_AMOUNT, correlationId));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Malformed request body", correlationId);

        return ResponseEntity.badRequest().body(
                ApiResponse.error("Invalid request body", ErrorCode.MALFORMED_REQUEST, correlationId));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(
            NoResourceFoundException ex, WebRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Resource not found: {}", correlationId, ex.getResourcePath());

        return ResponseEntity.status(404).body(
                ApiResponse.error("Resource not found", ErrorCode.RESOURCE_NOT_FOUND, correlationId));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Method not allowed: {}", correlationId, ex.getMethod());

        return ResponseEntity.status(405).body(
                ApiResponse.error("Method not allowed", ErrorCode.METHOD_NOT_ALLOWED, correlationId));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Access denied", correlationId);

        return ResponseEntity.status(403).body(
                ApiResponse.error("Access denied", ErrorCode.ACCESS_DENIED, correlationId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericException(
            Exception ex, WebRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.error("[{}] Unexpected error", correlationId, ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("An unexpected error occurred. Reference ID: " + correlationId,
                        ErrorCode.INTERNAL_ERROR, correlationId));
    }

    private int getPaymentErrorStatus(String message) {
        if (message.contains("locked")) return 423;
        if (message.contains("PIN") || message.contains("PIN is incorrect")) return 401;
        return 400;
    }

    private ErrorCode mapPaymentError(String message) {
        if (message.contains("Insufficient")) return ErrorCode.INSUFFICIENT_FUNDS;
        if (message.contains("not found")) return ErrorCode.USER_NOT_FOUND;
        if (message.contains("locked")) return ErrorCode.ACCOUNT_LOCKED;
        if (message.contains("PIN")) return ErrorCode.INVALID_PIN;
        return ErrorCode.INTERNAL_ERROR;
    }

    private String sanitizePaymentMessage(String message) {
        if (message.contains("Insufficient")) return "Insufficient funds";
        if (message.contains("not found")) return "User not found";
        if (message.contains("Cannot transfer to yourself")) return "Cannot transfer to yourself";
        if (message.contains("locked")) return message;
        if (message.contains("PIN")) return "Invalid PIN";
        return "Request failed";
    }
}
