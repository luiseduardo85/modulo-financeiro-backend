package com.financeiro.interfaces.rest.error;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
    private static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private final TraceIdProvider traceIdProvider;

    public GlobalExceptionHandler(TraceIdProvider traceIdProvider) {
        this.traceIdProvider = traceIdProvider;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException exception) {
        List<ValidationErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toValidationDetail)
                .sorted(Comparator.comparing(ValidationErrorDetail::field)
                        .thenComparing(ValidationErrorDetail::code))
                .toList();

        return response(
                HttpStatus.UNPROCESSABLE_CONTENT,
                VALIDATION_ERROR,
                "There are invalid fields.",
                details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest() {
        return response(
                HttpStatus.BAD_REQUEST,
                MALFORMED_REQUEST,
                "The request body is malformed.",
                List.of());
    }

    @ExceptionHandler(ApiErrorException.class)
    public ResponseEntity<ErrorResponse> handleApiError(ApiErrorException exception) {
        return response(
                exception.type().status(),
                exception.type().code(),
                exception.getMessage(),
                List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(Exception exception) {
        String traceId = traceIdProvider.currentTraceId();
        LOGGER.error("Unexpected REST error [traceId={}]", traceId, exception);

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR,
                "An internal error occurred.",
                List.of(),
                traceId);
    }

    private ValidationErrorDetail toValidationDetail(FieldError fieldError) {
        return new ValidationErrorDetail(
                fieldError.getField(),
                normalizeValidationCode(fieldError.getCode()),
                fieldError.getDefaultMessage());
    }

    private String normalizeValidationCode(String code) {
        if (code == null || code.isBlank()) {
            return "INVALID";
        }
        return code.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
    }

    private ResponseEntity<ErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            List<ValidationErrorDetail> details) {
        return response(status, code, message, details, traceIdProvider.currentTraceId());
    }

    private ResponseEntity<ErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            List<ValidationErrorDetail> details,
            String traceId) {
        ErrorResponse body = new ErrorResponse(code, message, details, Instant.now(), traceId);
        return ResponseEntity.status(status).body(body);
    }
}
