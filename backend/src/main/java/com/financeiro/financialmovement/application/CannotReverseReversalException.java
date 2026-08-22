package com.financeiro.financialmovement.application;

public final class CannotReverseReversalException extends RuntimeException {
  public CannotReverseReversalException() {
    super("A reversal FinancialMovement cannot itself be reversed.");
  }
}
