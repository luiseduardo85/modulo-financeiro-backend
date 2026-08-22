package com.financeiro.financialaccount.domain;

public final class InvalidFinancialAccountException extends RuntimeException {
  public InvalidFinancialAccountException(String message) {
    super(message);
  }
}
