package com.financeiro.interfaces.rest.error;

import com.financeiro.approval.application.ApprovalActorUnavailableException;
import com.financeiro.approval.application.ApprovalConflictException;
import com.financeiro.approval.application.ApproverNotAllowedException;
import com.financeiro.approval.application.SelfApprovalNotAllowedException;
import com.financeiro.approval.domain.InvalidApprovalConfigurationException;
import com.financeiro.approval.domain.InvalidApprovalDecisionException;
import com.financeiro.approval.domain.InvalidApprovalRequestException;
import com.financeiro.approval.domain.InvalidRejectionJustificationException;
import com.financeiro.bankaccount.application.BankAccountNotFoundException;
import com.financeiro.bankaccount.domain.InvalidBankAccountNameException;
import com.financeiro.cashflow.application.InvalidCashFlowQueryException;
import com.financeiro.category.application.CategoryNotFoundException;
import com.financeiro.category.domain.InvalidCategoryNameException;
import com.financeiro.company.application.BranchNotFoundException;
import com.financeiro.company.application.CompanyNotFoundException;
import com.financeiro.company.application.InvalidPageRequestException;
import com.financeiro.company.domain.InvalidNameException;
import com.financeiro.costcenter.application.CostCenterNotFoundException;
import com.financeiro.costcenter.domain.InvalidCostCenterNameException;
import com.financeiro.financialaccount.application.CategoryInactiveException;
import com.financeiro.financialaccount.application.CostCenterInactiveException;
import com.financeiro.financialaccount.application.FinancialAccountNotFoundException;
import com.financeiro.financialaccount.application.InvalidFinancialAccountPageException;
import com.financeiro.financialaccount.application.PartnerInactiveException;
import com.financeiro.financialaccount.application.PartnerRoleNotAllowedException;
import com.financeiro.financialaccount.domain.InvalidFinancialAccountException;
import com.financeiro.financialaccount.domain.InvalidFinancialAccountStatusException;
import com.financeiro.financialaccount.domain.InvalidInstallmentException;
import com.financeiro.financialmovement.application.*;
import com.financeiro.financialmovement.domain.InvalidFinancialMovementException;
import com.financeiro.idempotency.application.IdempotencyConflictException;
import com.financeiro.idempotency.application.IdempotencyInProgressException;
import com.financeiro.partner.application.InvalidPartnerPageException;
import com.financeiro.partner.application.PartnerDocumentAlreadyExistsException;
import com.financeiro.partner.application.PartnerNotFoundException;
import com.financeiro.partner.domain.InvalidPartnerDocumentException;
import com.financeiro.partner.domain.InvalidPartnerNameException;
import com.financeiro.partner.domain.InvalidPartnerRolesException;
import com.financeiro.paymentmethod.application.PaymentMethodNotFoundException;
import com.financeiro.paymentmethod.domain.InvalidPaymentMethodNameException;
import jakarta.validation.ConstraintViolationException;
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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
  public ResponseEntity<ErrorResponse> handleBeanValidation(
      MethodArgumentNotValidException exception) {
    List<ValidationErrorDetail> details =
        exception.getBindingResult().getFieldErrors().stream()
            .map(this::toValidationDetail)
            .sorted(
                Comparator.comparing(ValidationErrorDetail::field)
                    .thenComparing(ValidationErrorDetail::code))
            .toList();

    return response(
        HttpStatus.UNPROCESSABLE_CONTENT, VALIDATION_ERROR, "There are invalid fields.", details);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMalformedRequest() {
    return response(
        HttpStatus.BAD_REQUEST, MALFORMED_REQUEST, "The request body is malformed.", List.of());
  }

  @ExceptionHandler(ApiErrorException.class)
  public ResponseEntity<ErrorResponse> handleApiError(ApiErrorException exception) {
    return response(
        exception.type().status(), exception.type().code(), exception.getMessage(), List.of());
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
  public ResponseEntity<ErrorResponse> handleCostCenterNotFound(
      CostCenterNotFoundException exception) {
    return response(
        HttpStatus.NOT_FOUND, "COST_CENTER_NOT_FOUND", exception.getMessage(), List.of());
  }

  @ExceptionHandler(BankAccountNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleBankAccountNotFound(
      BankAccountNotFoundException exception) {
    return response(
        HttpStatus.NOT_FOUND, "BANK_ACCOUNT_NOT_FOUND", exception.getMessage(), List.of());
  }

  @ExceptionHandler(PaymentMethodNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePaymentMethodNotFound(
      PaymentMethodNotFoundException exception) {
    return response(
        HttpStatus.NOT_FOUND, "PAYMENT_METHOD_NOT_FOUND", exception.getMessage(), List.of());
  }

  @ExceptionHandler(FinancialAccountNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleFinancialAccountNotFound(
      FinancialAccountNotFoundException exception) {
    return response(
        HttpStatus.NOT_FOUND, "FINANCIAL_ACCOUNT_NOT_FOUND", exception.getMessage(), List.of());
  }

  @ExceptionHandler(FinancialAccountNotSettleableException.class)
  public ResponseEntity<ErrorResponse> handleFinancialAccountNotSettleable(
      FinancialAccountNotSettleableException exception) {
    return api(ApiErrorType.FINANCIAL_ACCOUNT_NOT_SETTLEABLE, exception);
  }

  @ExceptionHandler(InstallmentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleInstallmentNotFound(
      InstallmentNotFoundException exception) {
    return api(ApiErrorType.INSTALLMENT_NOT_FOUND, exception);
  }

  @ExceptionHandler(InstallmentAlreadySettledException.class)
  public ResponseEntity<ErrorResponse> handleInstallmentAlreadySettled(
      InstallmentAlreadySettledException exception) {
    return api(ApiErrorType.INSTALLMENT_ALREADY_SETTLED, exception);
  }

  @ExceptionHandler(SettlementAmountExceedsBalanceException.class)
  public ResponseEntity<ErrorResponse> handleSettlementAmountExceedsBalance(
      SettlementAmountExceedsBalanceException exception) {
    return api(ApiErrorType.SETTLEMENT_AMOUNT_EXCEEDS_BALANCE, exception);
  }

  @ExceptionHandler(BankAccountInactiveException.class)
  public ResponseEntity<ErrorResponse> handleBankAccountInactive(
      BankAccountInactiveException exception) {
    return api(ApiErrorType.BANK_ACCOUNT_INACTIVE, exception);
  }

  @ExceptionHandler(BankAccountBranchNotAllowedException.class)
  public ResponseEntity<ErrorResponse> handleBankAccountBranchNotAllowed(
      BankAccountBranchNotAllowedException exception) {
    return api(ApiErrorType.BANK_ACCOUNT_BRANCH_NOT_ALLOWED, exception);
  }

  @ExceptionHandler(PaymentMethodInactiveException.class)
  public ResponseEntity<ErrorResponse> handlePaymentMethodInactive(
      PaymentMethodInactiveException exception) {
    return api(ApiErrorType.PAYMENT_METHOD_INACTIVE, exception);
  }

  @ExceptionHandler(SettlementConflictException.class)
  public ResponseEntity<ErrorResponse> handleSettlementConflict(
      SettlementConflictException exception) {
    return api(ApiErrorType.SETTLEMENT_CONFLICT, exception);
  }

  @ExceptionHandler(FinancialAccountNotReversibleException.class)
  public ResponseEntity<ErrorResponse> handleFinancialAccountNotReversible(
      FinancialAccountNotReversibleException exception) {
    return api(ApiErrorType.FINANCIAL_ACCOUNT_NOT_REVERSIBLE, exception);
  }

  @ExceptionHandler(OriginalMovementNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleOriginalMovementNotFound(
      OriginalMovementNotFoundException exception) {
    return api(ApiErrorType.ORIGINAL_MOVEMENT_NOT_FOUND, exception);
  }

  @ExceptionHandler(CannotReverseReversalException.class)
  public ResponseEntity<ErrorResponse> handleCannotReverseReversal(
      CannotReverseReversalException exception) {
    return api(ApiErrorType.CANNOT_REVERSE_REVERSAL, exception);
  }

  @ExceptionHandler(OriginalMovementAlreadyFullyReversedException.class)
  public ResponseEntity<ErrorResponse> handleOriginalMovementAlreadyFullyReversed(
      OriginalMovementAlreadyFullyReversedException exception) {
    return api(ApiErrorType.ORIGINAL_MOVEMENT_ALREADY_FULLY_REVERSED, exception);
  }

  @ExceptionHandler(ReversalAmountExceedsBalanceException.class)
  public ResponseEntity<ErrorResponse> handleReversalAmountExceedsBalance(
      ReversalAmountExceedsBalanceException exception) {
    return api(ApiErrorType.REVERSAL_AMOUNT_EXCEEDS_BALANCE, exception);
  }

  @ExceptionHandler(PartnerInactiveException.class)
  public ResponseEntity<ErrorResponse> handlePartnerInactive(PartnerInactiveException exception) {
    return response(
        HttpStatus.UNPROCESSABLE_CONTENT, "PARTNER_INACTIVE", exception.getMessage(), List.of());
  }

  @ExceptionHandler(PartnerRoleNotAllowedException.class)
  public ResponseEntity<ErrorResponse> handlePartnerRoleNotAllowed(
      PartnerRoleNotAllowedException exception) {
    return response(
        HttpStatus.UNPROCESSABLE_CONTENT,
        "PARTNER_ROLE_NOT_ALLOWED",
        exception.getMessage(),
        List.of());
  }

  @ExceptionHandler(CategoryInactiveException.class)
  public ResponseEntity<ErrorResponse> handleCategoryInactive(CategoryInactiveException exception) {
    return response(
        HttpStatus.UNPROCESSABLE_CONTENT, "CATEGORY_INACTIVE", exception.getMessage(), List.of());
  }

  @ExceptionHandler(CostCenterInactiveException.class)
  public ResponseEntity<ErrorResponse> handleCostCenterInactive(
      CostCenterInactiveException exception) {
    return response(
        HttpStatus.UNPROCESSABLE_CONTENT,
        "COST_CENTER_INACTIVE",
        exception.getMessage(),
        List.of());
  }

  @ExceptionHandler(InvalidFinancialAccountStatusException.class)
  public ResponseEntity<ErrorResponse> handleInvalidFinancialAccountStatus(
      InvalidFinancialAccountStatusException exception) {
    return response(
        HttpStatus.CONFLICT, "FINANCIAL_ACCOUNT_INVALID_STATUS", exception.getMessage(), List.of());
  }

  @ExceptionHandler(SelfApprovalNotAllowedException.class)
  public ResponseEntity<ErrorResponse> handleSelfApprovalNotAllowed(
      SelfApprovalNotAllowedException exception) {
    return response(
        HttpStatus.UNPROCESSABLE_CONTENT,
        "SELF_APPROVAL_NOT_ALLOWED",
        exception.getMessage(),
        List.of());
  }

  @ExceptionHandler(ApproverNotAllowedException.class)
  public ResponseEntity<ErrorResponse> handleApproverNotAllowed(
      ApproverNotAllowedException exception) {
    return response(
        HttpStatus.FORBIDDEN, "APPROVER_NOT_ALLOWED", exception.getMessage(), List.of());
  }

  @ExceptionHandler(InvalidRejectionJustificationException.class)
  public ResponseEntity<ErrorResponse> handleInvalidRejectionJustification(
      InvalidRejectionJustificationException exception) {
    return response(
        HttpStatus.UNPROCESSABLE_CONTENT,
        "REJECTION_JUSTIFICATION_REQUIRED",
        exception.getMessage(),
        List.of());
  }

  @ExceptionHandler(ApprovalConflictException.class)
  public ResponseEntity<ErrorResponse> handleApprovalConflict(ApprovalConflictException exception) {
    return response(HttpStatus.CONFLICT, "APPROVAL_CONFLICT", exception.getMessage(), List.of());
  }

  @ExceptionHandler(ApprovalActorUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleApprovalActorUnavailable(
      ApprovalActorUnavailableException exception) {
    return response(
        HttpStatus.UNAUTHORIZED, "APPROVAL_ACTOR_REQUIRED", exception.getMessage(), List.of());
  }

  @ExceptionHandler(PartnerNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePartnerNotFound(PartnerNotFoundException exception) {
    return response(HttpStatus.NOT_FOUND, "PARTNER_NOT_FOUND", exception.getMessage(), List.of());
  }

  @ExceptionHandler(PartnerDocumentAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handlePartnerDuplicate(
      PartnerDocumentAlreadyExistsException exception) {
    return response(
        HttpStatus.CONFLICT, "PARTNER_DOCUMENT_ALREADY_EXISTS", exception.getMessage(), List.of());
  }

  @ExceptionHandler(InvalidPartnerDocumentException.class)
  public ResponseEntity<ErrorResponse> handleInvalidPartnerDocument(
      InvalidPartnerDocumentException exception) {
    return response(
        HttpStatus.UNPROCESSABLE_CONTENT,
        "INVALID_PARTNER_DOCUMENT",
        exception.getMessage(),
        List.of());
  }

  @ExceptionHandler({
    InvalidPageRequestException.class,
    InvalidNameException.class,
    InvalidCategoryNameException.class,
    InvalidCostCenterNameException.class,
    InvalidBankAccountNameException.class,
    InvalidPaymentMethodNameException.class,
    InvalidPartnerNameException.class,
    InvalidPartnerRolesException.class,
    InvalidPartnerPageException.class,
    InvalidFinancialAccountException.class,
    InvalidInstallmentException.class,
    InvalidFinancialAccountPageException.class,
    InvalidApprovalConfigurationException.class,
    InvalidApprovalRequestException.class,
    InvalidApprovalDecisionException.class,
    InvalidFinancialMovementException.class,
    InvalidCashFlowQueryException.class,
    ConstraintViolationException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<ErrorResponse> handleSemanticValidation(Exception exception) {
    return response(
        HttpStatus.UNPROCESSABLE_CONTENT,
        VALIDATION_ERROR,
        "A requisição contém valores inválidos.",
        List.of());
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
      HttpStatus status, String code, String message, List<ValidationErrorDetail> details) {
    return response(status, code, message, details, traceIdProvider.currentTraceId());
  }

  private ResponseEntity<ErrorResponse> api(ApiErrorType type, RuntimeException exception) {
    return response(type.status(), type.code(), exception.getMessage(), List.of());
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
