package com.financeiro.financialmovement.application;

public final class FinancialAccountNotReversibleException extends RuntimeException {
  public FinancialAccountNotReversibleException() {
    super("The FinancialAccount is not reversible in its current status.");
  }
}
