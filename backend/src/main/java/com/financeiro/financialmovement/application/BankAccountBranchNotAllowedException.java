package com.financeiro.financialmovement.application;

public final class BankAccountBranchNotAllowedException extends RuntimeException {
  public BankAccountBranchNotAllowedException() {
    super("The BankAccount is not available to this FinancialAccount Branch.");
  }
}
