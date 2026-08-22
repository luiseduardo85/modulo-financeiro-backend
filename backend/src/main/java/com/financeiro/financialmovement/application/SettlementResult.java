package com.financeiro.financialmovement.application;

import com.financeiro.financialmovement.domain.FinancialMovementType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementResult(
    Long id,
    Long financialAccountId,
    Long installmentId,
    FinancialMovementType type,
    BigDecimal amount,
    LocalDate movementDate,
    Long bankAccountId,
    Long paymentMethodId) {}
