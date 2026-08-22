package com.financeiro.financialmovement.interfaces.rest;

import com.financeiro.financialmovement.application.SettlementResult;
import com.financeiro.financialmovement.domain.FinancialMovementType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementResponse(
    Long id,
    Long financialAccountId,
    Long installmentId,
    FinancialMovementType type,
    BigDecimal amount,
    LocalDate movementDate,
    Long bankAccountId,
    Long paymentMethodId) {
  public static SettlementResponse from(SettlementResult result) {
    return new SettlementResponse(
        result.id(),
        result.financialAccountId(),
        result.installmentId(),
        result.type(),
        result.amount(),
        result.movementDate(),
        result.bankAccountId(),
        result.paymentMethodId());
  }
}
