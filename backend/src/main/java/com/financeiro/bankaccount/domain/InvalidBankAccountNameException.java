package com.financeiro.bankaccount.domain;

public final class InvalidBankAccountNameException extends RuntimeException {
  public InvalidBankAccountNameException() {
    super("Nome de BankAccount inválido.");
  }
}
