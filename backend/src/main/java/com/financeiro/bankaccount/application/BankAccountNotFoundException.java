package com.financeiro.bankaccount.application;

public final class BankAccountNotFoundException extends RuntimeException {
  public BankAccountNotFoundException(Long companyId, Long id) {
    super("BankAccount não encontrada.");
  }
}
