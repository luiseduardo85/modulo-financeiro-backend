package com.financeiro.financialaccount.application;

public final class CostCenterInactiveException extends RuntimeException {
  public CostCenterInactiveException() {
    super("CostCenter está inativo");
  }
}
