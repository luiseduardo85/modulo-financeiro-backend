package com.financeiro.financialmovement.application;

public final class ReversalAmountExceedsBalanceException extends RuntimeException {
  public ReversalAmountExceedsBalanceException() {
    super(
        "The reversal amount exceeds the remaining reversible balance of the original FinancialMovement.");
  }
}
