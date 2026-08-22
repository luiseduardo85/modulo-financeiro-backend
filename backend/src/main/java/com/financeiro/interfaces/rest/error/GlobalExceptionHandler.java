package com.financeiro.interfaces.rest.error;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.financeiro.bankaccount.application.BankAccountNotFoundException;
import com.financeiro.bankaccount.domain.InvalidBankAccountNameException;
import com.financeiro.category.application.CategoryNotFoundException;
import com.financeiro.category.domain.InvalidCategoryNameException;
import com.financeiro.company.application.BranchNotFoundException;
import com.financeiro.company.application.CompanyNotFoundException;
import com.financeiro.company.application.InvalidPageRequestException;
import com.financeiro.company.domain.InvalidNameException;
import com.financeiro.costcenter.application.CostCenterNotFoundException;
import com.financeiro.costcenter.domain.InvalidCostCenterNameException;
import com.financeiro.idempotency.application.IdempotencyConflictException;
import com.financeiro.idempotency.application.IdempotencyInProgressException;
import com.financeiro.paymentmethod.application.PaymentMethodNotFoundException;
import com.financeiro.paymentmethod.domain.InvalidPaymentMethodNameException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCompanyNotFound(CompanyNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", exception.getMessage(), List.of());
    }

    @ExceptionHandler(BranchNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBranchNotFound(BranchNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "BRANCH_NOT_FOUND", exception.getMessage(), List.of());
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound(CategoryNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", exception.getMessage(), List.of());
    }

    @ExceptionHandler(CostCenterNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCostCenterNotFound(CostCenterNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "COST_CENTER_NOT_FOUND", exception.getMessage(), List.of());
    }

    @ExceptionHandler(BankAccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBankAccountNotFound(BankAccountNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "BANK_ACCOUNT_NOT_FOUND", exception.getMessage(), List.of());
    }

    @ExceptionHandler(PaymentMethodNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentMethodNotFound(PaymentMethodNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "PAYMENT_METHOD_NOT_FOUND", exception.getMessage(), List.of());
    }

    @ExceptionHandler({InvalidPageRequestException.class, InvalidNameException.class,
            InvalidCategoryNameException.class, InvalidCostCenterNameException.class,
            InvalidBankAccountNameException.class, InvalidPaymentMethodNameException.class,
            ConstraintViolationException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleSemanticValidation(Exception exception) {
        return response(HttpStatus.UNPROCESSABLE_CONTENT, VALIDATION_ERROR,
                "A requisição contém valores inválidos.", List.of());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict() {
        return response(
                ApiErrorType.IDEMPOTENCY_KEY_CONFLICT.status(),
                ApiErrorType.IDEMPOTENCY_KEY_CONFLICT.code(),
                "The idempotency key was already used for a different request.",
                List.of());
    }

    @ExceptionHandler(IdempotencyInProgressException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyInProgress() {
        return response(
                ApiErrorType.IDEMPOTENCY_REQUEST_IN_PROGRESS.status(),
                ApiErrorType.IDEMPOTENCY_REQUEST_IN_PROGRESS.code(),
                "The idempotent request is still being processed.",
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
