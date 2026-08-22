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

@WebMvcTest(SettlementController.class)
@ActiveProfiles("test")
@Import({
  IdempotencyKeyValidator.class,
  GlobalExceptionHandler.class,
  TraceIdProvider.class,
  TraceIdFilter.class
})
class SettlementControllerTest {
  private static final String ROUTE =
      "/api/v1/companies/1/financial-accounts/10/installments/20/settlements";
  @Autowired MockMvc mvc;
  @MockitoBean SettleInstallment settle;

  @Test
  void createsStablePaymentAndReceiptResponsesWithExactLocation() throws Exception {
    when(settle.execute(any())).thenReturn(result(FinancialMovementType.PAYMENT));
    mvc.perform(
            post(ROUTE)
                .header("Idempotency-Key", "settlement-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
        .andExpect(status().isCreated())
        .andExpect(
            header()
                .string(
                    "Location",
                    "/api/v1/companies/1/financial-accounts/10/installments/20/settlements/50"))
        .andExpect(jsonPath("$.id").value(50))
        .andExpect(jsonPath("$.financialAccountId").value(10))
        .andExpect(jsonPath("$.installmentId").value(20))
        .andExpect(jsonPath("$.type").value("PAYMENT"))
        .andExpect(jsonPath("$.amount").value(60.0))
        .andExpect(jsonPath("$.movementDate").value("2026-08-22"))
        .andExpect(jsonPath("$.bankAccountId").value(30))
        .andExpect(jsonPath("$.paymentMethodId").value(40))
        .andExpect(jsonPath("$.settledAmount").doesNotExist())
        .andExpect(jsonPath("$.remainingBalance").doesNotExist())
        .andExpect(jsonPath("$.status").doesNotExist());

    when(settle.execute(any())).thenReturn(result(FinancialMovementType.RECEIPT));
    mvc.perform(
            post(ROUTE)
                .header("Idempotency-Key", "receipt-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("RECEIPT"));
  }

  @Test
  void replayReturnsSameCreatedIdentityAndStatus() throws Exception {
    when(settle.execute(any())).thenReturn(result(FinancialMovementType.PAYMENT));
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
                      "/api/v1/companies/1/financial-accounts/10/installments/20/settlements/50"))
          .andExpect(jsonPath("$.id").value(50));
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
    verifyNoInteractions(settle);
  }

  @Test
  void rejectsNonPositivePathAndBodyIdsAsValidationErrors() throws Exception {
    for (String route :
        List.of(
            "/api/v1/companies/0/financial-accounts/10/installments/20/settlements",
            "/api/v1/companies/-1/financial-accounts/10/installments/20/settlements",
            "/api/v1/companies/1/financial-accounts/0/installments/20/settlements",
            "/api/v1/companies/1/financial-accounts/-1/installments/20/settlements",
            "/api/v1/companies/1/financial-accounts/10/installments/0/settlements",
            "/api/v1/companies/1/financial-accounts/10/installments/-1/settlements")) {
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
            body().replace("\"amount\":60.00", "\"amount\":0"))) {
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
            "type",
            "status",
            "balance",
            "settledAmount",
            "remainingBalance",
            "reversalId",
            "reversalAmount",
            "reversed",
            "originalMovementId",
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
                .content(body().replace("60.00", "100.501")))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    when(settle.execute(any())).thenReturn(result(FinancialMovementType.PAYMENT));
    mvc.perform(
            post(ROUTE)
                .header("Idempotency-Key", "trailing-zero-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("60.00", "100.500")))
        .andExpect(status().isCreated());
  }

  @Test
  void mapsStableSettlementErrorsWithTraceId() throws Exception {
    assertError(
        new FinancialAccountNotSettleableException(), 409, "FINANCIAL_ACCOUNT_NOT_SETTLEABLE");
    assertError(new FinancialAccountNotFoundException(10L), 404, "FINANCIAL_ACCOUNT_NOT_FOUND");
    assertError(new InstallmentNotFoundException(20L), 404, "INSTALLMENT_NOT_FOUND");
    assertError(new InstallmentAlreadySettledException(), 409, "INSTALLMENT_ALREADY_SETTLED");
    assertError(
        new SettlementAmountExceedsBalanceException(), 422, "SETTLEMENT_AMOUNT_EXCEEDS_BALANCE");
    assertError(new BankAccountInactiveException(), 422, "BANK_ACCOUNT_INACTIVE");
    assertError(new BankAccountNotFoundException(1L, 30L), 404, "BANK_ACCOUNT_NOT_FOUND");
    assertError(new BankAccountBranchNotAllowedException(), 422, "BANK_ACCOUNT_BRANCH_NOT_ALLOWED");
    assertError(new PaymentMethodInactiveException(), 422, "PAYMENT_METHOD_INACTIVE");
    assertError(new PaymentMethodNotFoundException(1L, 40L), 404, "PAYMENT_METHOD_NOT_FOUND");
    assertError(
        new SettlementConflictException(new RuntimeException()), 409, "SETTLEMENT_CONFLICT");
  }

  private void assertError(RuntimeException exception, int status, String code) throws Exception {
    reset(settle);
    when(settle.execute(any())).thenThrow(exception);
    mvc.perform(
            post(ROUTE)
                .header("Idempotency-Key", "key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
        .andExpect(status().is(status))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.traceId").exists());
  }

  private static SettlementResult result(FinancialMovementType type) {
    return new SettlementResult(
        50L, 10L, 20L, type, new BigDecimal("60.00"), LocalDate.of(2026, 8, 22), 30L, 40L);
  }

  private static String body() {
    return """
        {
          "amount":60.00,
          "movementDate":"2026-08-22",
          "bankAccountId":30,
          "paymentMethodId":40
        }
        """;
  }
}
