package com.financeiro.financialaccount.application;

public final class FinancialAccountNotFoundException extends RuntimeException {
  public FinancialAccountNotFoundException(Long id) {
    super("FinancialAccount não encontrada: " + id);
  }
}
