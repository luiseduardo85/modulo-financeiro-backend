package com.financeiro.interfaces.rest.error;

import org.springframework.http.HttpStatus;

public enum ApiErrorType {
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
