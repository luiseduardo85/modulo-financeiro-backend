package com.financeiro.financialmovement.application;

public final class FinancialAccountNotSettleableException extends RuntimeException {
  public FinancialAccountNotSettleableException() {
    super("The FinancialAccount is not settleable in its current status.");
  }
}
