package com.mpesa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("timestamp")
    private Instant timestamp = Instant.now();

    public ApiResponse() {}

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> success(String message) {
        return success(null, message);
    }

    public static <T> ApiResponse<T> error(String error) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setError(error);
        response.setCorrelationId(UUID.randomUUID().toString());
        return response;
    }

    public static <T> ApiResponse<T> error(String error, ErrorCode errorCode) {
        ApiResponse<T> response = error(error);
        response.setErrorCode(errorCode.name());
        return response;
    }

    public static <T> ApiResponse<T> error(String error, ErrorCode errorCode, String correlationId) {
        ApiResponse<T> response = error(error);
        response.setErrorCode(errorCode.name());
        response.setCorrelationId(correlationId);
        return response;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
