package com.financeiro.financialaccount.application;

public final class PartnerInactiveException extends RuntimeException {
  public PartnerInactiveException() {
    super("Partner está inativo");
  }
}
