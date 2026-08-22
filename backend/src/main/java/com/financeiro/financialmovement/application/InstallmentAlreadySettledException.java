package com.financeiro.financialmovement.application;

public final class InstallmentAlreadySettledException extends RuntimeException {
  public InstallmentAlreadySettledException() {
    super("The Installment is already settled.");
  }
}
