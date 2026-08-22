package com.financeiro.financialmovement.application;

public final class OriginalMovementNotFoundException extends RuntimeException {
  public OriginalMovementNotFoundException(Long id) {
    super("Original FinancialMovement not found: " + id);
  }
}
