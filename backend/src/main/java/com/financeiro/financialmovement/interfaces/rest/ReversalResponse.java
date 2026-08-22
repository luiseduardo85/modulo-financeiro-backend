package com.financeiro.financialmovement.interfaces.rest;

import com.financeiro.financialmovement.application.ReversalResult;
import com.financeiro.financialmovement.domain.FinancialMovementType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReversalResponse(
    Long id,
    Long financialAccountId,
    Long installmentId,
    Long originalMovementId,
    FinancialMovementType type,
    BigDecimal amount,
    LocalDate movementDate,
    Long bankAccountId,
    Long paymentMethodId) {
  public static ReversalResponse from(ReversalResult result) {
    return new ReversalResponse(
        result.id(),
        result.financialAccountId(),
        result.installmentId(),
        result.originalMovementId(),
        result.type(),
        result.amount(),
        result.movementDate(),
        result.bankAccountId(),
        result.paymentMethodId());
  }
}
