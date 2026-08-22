package com.financeiro.financialmovement.application;

import com.financeiro.financialmovement.domain.FinancialMovement;
import com.financeiro.financialmovement.domain.InvalidFinancialMovementException;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReverseFinancialMovementCommand(
    Long companyId,
    Long financialAccountId,
    Long installmentId,
    Long movementId,
    BigDecimal amount,
    LocalDate movementDate,
    Long bankAccountId,
    Long paymentMethodId,
    String idempotencyKey) {

  public ReverseFinancialMovementCommand {
    positive(companyId, "companyId");
    positive(financialAccountId, "financialAccountId");
    positive(installmentId, "installmentId");
    positive(movementId, "movementId");
    FinancialMovement.validateAmount(amount);
    if (movementDate == null)
      throw new InvalidFinancialMovementException("movementDate is required");
    positive(bankAccountId, "bankAccountId");
    positive(paymentMethodId, "paymentMethodId");
  }

  private static void positive(Long value, String field) {
    if (value == null || value <= 0)
      throw new InvalidFinancialMovementException(field + " must be positive");
  }
}
