package com.financeiro.financialmovement.application;

public final class BankAccountInactiveException extends RuntimeException {
  public BankAccountInactiveException() {
    super("Inactive BankAccount cannot be used for a new settlement.");
  }
}
