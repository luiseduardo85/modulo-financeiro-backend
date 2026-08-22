package com.financeiro.financialmovement.application;

public final class InstallmentNotFoundException extends RuntimeException {
  public InstallmentNotFoundException(Long id) {
    super("Installment not found: " + id);
  }
}
