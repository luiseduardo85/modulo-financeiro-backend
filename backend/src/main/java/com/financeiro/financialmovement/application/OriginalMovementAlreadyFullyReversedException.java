package com.financeiro.financialmovement.application;

public final class OriginalMovementAlreadyFullyReversedException extends RuntimeException {
  public OriginalMovementAlreadyFullyReversedException() {
    super("The original FinancialMovement has no remaining reversible balance.");
  }
}
