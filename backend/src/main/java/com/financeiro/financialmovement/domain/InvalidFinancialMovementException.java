package com.financeiro.financialmovement.domain;

public final class InvalidFinancialMovementException extends RuntimeException {
  public InvalidFinancialMovementException(String message) {
    super(message);
  }
}
