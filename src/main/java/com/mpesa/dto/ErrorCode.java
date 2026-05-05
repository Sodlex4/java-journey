package com.mpesa.dto;

public enum ErrorCode {
    INSUFFICIENT_FUNDS,
    USER_NOT_FOUND,
    INVALID_PIN,
    INVALID_AMOUNT,
    ACCOUNT_LOCKED,
    VALIDATION_FAILED,
    RESOURCE_NOT_FOUND,
    METHOD_NOT_ALLOWED,
    MALFORMED_REQUEST,
    ACCESS_DENIED,
    INTERNAL_ERROR;

    public int getHttpStatus() {
        switch (this) {
            case INSUFFICIENT_FUNDS:
            case INVALID_AMOUNT:
            case VALIDATION_FAILED:
            case MALFORMED_REQUEST:
                return 400;
            case USER_NOT_FOUND:
            case RESOURCE_NOT_FOUND:
                return 404;
            case INVALID_PIN:
            case ACCOUNT_LOCKED:
            case ACCESS_DENIED:
                return 401;
            case METHOD_NOT_ALLOWED:
                return 405;
            case INTERNAL_ERROR:
            default:
                return 500;
        }
    }
}
