package com.financeiro.financialmovement.application;

import java.math.BigDecimal;

public interface SettlementBalanceRepository {
  BigDecimal settledAmountByInstallmentId(Long installmentId);

  boolean hasOpenInstallments(Long financialAccountId);
}
