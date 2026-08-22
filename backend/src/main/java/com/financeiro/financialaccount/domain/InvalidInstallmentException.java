package com.financeiro.financialaccount.domain;

public final class InvalidInstallmentException extends RuntimeException {
  public InvalidInstallmentException(String message) {
    super(message);
  }
}
