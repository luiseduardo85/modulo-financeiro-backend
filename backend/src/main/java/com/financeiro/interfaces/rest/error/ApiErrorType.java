package com.financeiro.interfaces.rest.error;

import org.springframework.http.HttpStatus;

public enum ApiErrorType {
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND"),
    BRANCH_NOT_FOUND(HttpStatus.NOT_FOUND, "BRANCH_NOT_FOUND"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND"),
    COST_CENTER_NOT_FOUND(HttpStatus.NOT_FOUND, "COST_CENTER_NOT_FOUND"),
    IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED"),
    INVALID_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY"),
    IDEMPOTENCY_KEY_CONFLICT(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT"),
    IDEMPOTENCY_REQUEST_IN_PROGRESS(HttpStatus.CONFLICT, "IDEMPOTENCY_REQUEST_IN_PROGRESS"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND"),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT"),
    SEMANTIC_VALIDATION(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_ERROR");

    private final HttpStatus status;
    private final String code;

    ApiErrorType(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
