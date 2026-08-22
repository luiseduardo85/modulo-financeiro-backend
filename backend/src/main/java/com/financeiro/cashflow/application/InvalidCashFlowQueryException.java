package com.financeiro.cashflow.application;

public final class InvalidCashFlowQueryException extends RuntimeException {
  public InvalidCashFlowQueryException(String message) {
    super(message);
  }
}
