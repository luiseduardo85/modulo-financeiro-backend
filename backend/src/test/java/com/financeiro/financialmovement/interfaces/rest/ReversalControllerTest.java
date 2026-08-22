package com.financeiro.financialmovement.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.financeiro.bankaccount.application.BankAccountNotFoundException;
import com.financeiro.financialaccount.application.FinancialAccountNotFoundException;
import com.financeiro.financialmovement.application.*;
import com.financeiro.financialmovement.domain.FinancialMovementType;
import com.financeiro.idempotency.interfaces.rest.IdempotencyKeyValidator;
import com.financeiro.interfaces.rest.error.GlobalExceptionHandler;
import com.financeiro.interfaces.rest.error.TraceIdProvider;
import com.financeiro.interfaces.rest.trace.TraceIdFilter;
import com.financeiro.paymentmethod.application.PaymentMethodNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReversalController.class)
@ActiveProfiles("test")
@Import({
  IdempotencyKeyValidator.class,
  GlobalExceptionHandler.class,
  TraceIdProvider.class,
  TraceIdFilter.class
})
class ReversalControllerTest {
  private static final String ROUTE =
      "/api/v1/companies/1/financial-accounts/10/installments/20/settlements/50/reversals";
  @Autowired MockMvc mvc;
  @MockitoBean ReverseFinancialMovement reverse;

  @Test
  void createsStableReversalPaymentAndReceiptResponsesWithExactLocation() throws Exception {
    when(reverse.execute(any())).thenReturn(result(FinancialMovementType.REVERSAL_PAYMENT));
    mvc.perform(
            post(ROUTE)
                .header("Idempotency-Key", "reversal-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
        .andExpect(status().isCreated())
        .andExpect(
            header()
                .string(
                    "Location",
                    "/api/v1/companies/1/financial-accounts/10/installments/20/settlements/50/reversals/60"))
        .andExpect(jsonPath("$.id").value(60))
        .andExpect(jsonPath("$.financialAccountId").value(10))
        .andExpect(jsonPath("$.installmentId").value(20))
        .andExpect(jsonPath("$.originalMovementId").value(50))
        .andExpect(jsonPath("$.type").value("REVERSAL_PAYMENT"))
        .andExpect(jsonPath("$.amount").value(40.0))
        .andExpect(jsonPath("$.movementDate").value("2026-08-22"))
        .andExpect(jsonPath("$.bankAccountId").value(30))
        .andExpect(jsonPath("$.paymentMethodId").value(40))
        .andExpect(jsonPath("$.reversed").doesNotExist())
        .andExpect(jsonPath("$.status").doesNotExist());

    when(reverse.execute(any())).thenReturn(result(FinancialMovementType.REVERSAL_RECEIPT));
    mvc.perform(
            post(ROUTE)
                .header("Idempotency-Key", "reversal-receipt-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("REVERSAL_RECEIPT"));
  }

  @Test
  void replayReturnsSameCreatedIdentityAndStatus() throws Exception {
    when(reverse.execute(any())).thenReturn(result(FinancialMovementType.REVERSAL_PAYMENT));
    for (int attempt = 0; attempt < 2; attempt++) {
      mvc.perform(
              post(ROUTE)
                  .header("Idempotency-Key", "same-key")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body()))
          .andExpect(status().isCreated())
          .andExpect(
              header()
                  .string(
                      "Location",
                      "/api/v1/companies/1/financial-accounts/10/installments/20/settlements/50/reversals/60"))
          .andExpect(jsonPath("$.id").value(60));
    }
  }

  @Test
  void requiresAndValidatesIdempotencyKey() throws Exception {
    mvc.perform(post(ROUTE).contentType(MediaType.APPLICATION_JSON).content(body()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"))
        .andExpect(jsonPath("$.traceId").exists());
    mvc.perform(
            post(ROUTE)
                .header("Idempotency-Key", "invalid key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));
    verifyNoInteractions(reverse);
  }

  @Test
  void rejectsNonPositivePathAndBodyIdsAsValidationErrors() throws Exception {
    for (String route :
        List.of(
            "/api/v1/companies/0/financial-accounts/10/installments/20/settlements/50/reversals",
            "/api/v1/companies/-1/financial-accounts/10/installments/20/settlements/50/reversals",
            "/api/v1/companies/1/financial-accounts/0/installments/20/settlements/50/reversals",
            "/api/v1/companies/1/financial-accounts/-1/installments/20/settlements/50/reversals",
            "/api/v1/companies/1/financial-accounts/10/installments/0/settlements/50/reversals",
            "/api/v1/companies/1/financial-accounts/10/installments/-1/settlements/50/reversals",
            "/api/v1/companies/1/financial-accounts/10/installments/20/settlements/0/reversals",
            "/api/v1/companies/1/financial-accounts/10/installments/20/settlements/-1/reversals")) {
      mvc.perform(
              post(route)
                  .header("Idempotency-Key", "key")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body()))
          .andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
    for (String invalidBody :
        List.of(
            body().replace("\"bankAccountId\":30", "\"bankAccountId\":0"),
            body().replace("\"bankAccountId\":30", "\"bankAccountId\":-1"),
            body().replace("\"paymentMethodId\":40", "\"paymentMethodId\":0"),
            body().replace("\"paymentMethodId\":40", "\"paymentMethodId\":-1"),
            body().replace("\"amount\":40.00", "\"amount\":0"))) {
      mvc.perform(
              post(ROUTE)
                  .header("Idempotency-Key", "key")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(invalidBody))
          .andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
  }

  @Test
  void rejectsMalformedDateAndStrictlyProhibitsTechnicalFields() throws Exception {
    mvc.perform(
            post(ROUTE)
                .header("Idempotency-Key", "key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("2026-08-22", "not-a-date")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

    for (String field :
        List.of(
            "id",
            "companyId",
            "financialAccountId",
            "installmentId",
            "movementId",
            "type",
            "status",
            "balance",
            "settledAmount",
            "remainingBalance",
            "originalMovementId",
            "reversalId",
            "reversalAmount",
            "reversed",
            "actorId",
            "userId")) {
      String content = body().replace("}", ",\"" + field + "\":1}");
      mvc.perform(
              post(ROUTE)
                  .header("Idempotency-Key", "key")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(content))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
  }

  @Test
  void rejectsExcessEffectivePrecisionAndAcceptsTrailingZeros() throws Exception {
    mvc.perform(
            post(ROUTE)
                .header("Idempotency-Key", "precision-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("40.00", "100.501")))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    when(reverse.execute(any())).thenReturn(result(FinancialMovementType.REVERSAL_PAYMENT));
    mvc.perform(
            post(ROUTE)
                .header("Idempotency-Key", "trailing-zero-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("40.00", "100.500")))
        .andExpect(status().isCreated());
  }

  @Test
  void mapsStableReversalErrorsWithTraceId() throws Exception {
    assertError(
        new FinancialAccountNotReversibleException(), 409, "FINANCIAL_ACCOUNT_NOT_REVERSIBLE");
    assertError(new FinancialAccountNotFoundException(10L), 404, "FINANCIAL_ACCOUNT_NOT_FOUND");
    assertError(new InstallmentNotFoundException(20L), 404, "INSTALLMENT_NOT_FOUND");
    assertError(new OriginalMovementNotFoundException(50L), 404, "ORIGINAL_MOVEMENT_NOT_FOUND");
    assertError(new CannotReverseReversalException(), 422, "CANNOT_REVERSE_REVERSAL");
    assertError(
        new OriginalMovementAlreadyFullyReversedException(),
        409,
        "ORIGINAL_MOVEMENT_ALREADY_FULLY_REVERSED");
    assertError(
        new ReversalAmountExceedsBalanceException(), 422, "REVERSAL_AMOUNT_EXCEEDS_BALANCE");
    assertError(new BankAccountInactiveException(), 422, "BANK_ACCOUNT_INACTIVE");
    assertError(new BankAccountNotFoundException(1L, 30L), 404, "BANK_ACCOUNT_NOT_FOUND");
    assertError(new BankAccountBranchNotAllowedException(), 422, "BANK_ACCOUNT_BRANCH_NOT_ALLOWED");
    assertError(new PaymentMethodInactiveException(), 422, "PAYMENT_METHOD_INACTIVE");
    assertError(new PaymentMethodNotFoundException(1L, 40L), 404, "PAYMENT_METHOD_NOT_FOUND");
    assertError(
        new SettlementConflictException(new RuntimeException()), 409, "SETTLEMENT_CONFLICT");
  }

  private void assertError(RuntimeException exception, int status, String code) throws Exception {
    reset(reverse);
    when(reverse.execute(any())).thenThrow(exception);
    mvc.perform(
            post(ROUTE)
                .header("Idempotency-Key", "key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
        .andExpect(status().is(status))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.traceId").exists());
  }

  private static ReversalResult result(FinancialMovementType type) {
    return new ReversalResult(
        60L, 10L, 20L, 50L, type, new BigDecimal("40.00"), LocalDate.of(2026, 8, 22), 30L, 40L);
  }

  private static String body() {
    return """
        {
          "amount":40.00,
          "movementDate":"2026-08-22",
          "bankAccountId":30,
          "paymentMethodId":40
        }
        """;
  }
}
