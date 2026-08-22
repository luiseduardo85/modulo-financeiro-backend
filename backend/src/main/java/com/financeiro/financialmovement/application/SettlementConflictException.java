package com.financeiro.financialmovement.application;

public final class SettlementConflictException extends RuntimeException {
  public SettlementConflictException(Throwable cause) {
    super("The settlement conflicted with another financial operation.", cause);
  }
}
