package com.financeiro.financialmovement.application;

public final class SettlementAmountExceedsBalanceException extends RuntimeException {
  public SettlementAmountExceedsBalanceException() {
    super("The settlement amount exceeds the remaining Installment balance.");
  }
}
