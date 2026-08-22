package com.financeiro.financialmovement.domain;

public enum FinancialMovementType {
  PAYMENT,
  RECEIPT,
  REVERSAL_PAYMENT,
  REVERSAL_RECEIPT;

  public boolean isReversal() {
    return this == REVERSAL_PAYMENT || this == REVERSAL_RECEIPT;
  }

  public FinancialMovementType reversalType() {
    return switch (this) {
      case PAYMENT -> REVERSAL_PAYMENT;
      case RECEIPT -> REVERSAL_RECEIPT;
      case REVERSAL_PAYMENT, REVERSAL_RECEIPT ->
          throw new InvalidFinancialMovementException("cannot reverse a reversal");
    };
  }
}
