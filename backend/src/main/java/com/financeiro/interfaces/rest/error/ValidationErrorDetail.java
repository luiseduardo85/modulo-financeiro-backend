package com.financeiro.interfaces.rest.error;

public record ValidationErrorDetail(String field, String code, String message) {
}
